package net.doole.doolestools.blockentity;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.NetworkPowerCalculator;
import net.doole.doolestools.logistics.LogisticsScanner;
import net.doole.doolestools.logistics.lfm.LogiFactoryManager;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNetworkAccess;
import net.doole.doolestools.menu.LogisticsComputerMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.world.NetworkIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Backing store for a Logistics Computer. Holds the most recent scan results and the saved
 * flowgraph. All mutation happens server-side; the client receives copies via network payloads.
 */
public class LogisticsComputerBlockEntity extends BlockEntity implements MenuProvider {

    /** MVP scan radius in every direction. */
    public static final int SCAN_RADIUS = 8;
    private static final int POWER_HISTORY_SAMPLES = 120;

    // Multi-timescale history: each bucket averages the 120-sample "current" buffer over its tick
    // window, then pushes one short into a capped deque. Tick windows / max slot counts:
    private static final int BUCKET_30M = 600,    SLOTS_30M = 60;
    private static final int BUCKET_1H = 1200,    SLOTS_1H = 60;
    private static final int BUCKET_12H = 14400,  SLOTS_12H = 72;
    private static final int BUCKET_1D = 57600,   SLOTS_1D = 48;
    private static final int BUCKET_ALL = 57600,  SLOTS_ALL = 500;
    private static final int THROUGHPUT_SAMPLES = 60;

    private List<ScannedBlockData> lastScan = new ArrayList<>();
    private LogisticsGraphData graph = LogisticsGraphData.EMPTY;
    private java.util.Set<String> activeRouteIds = java.util.Set.of();
    // Ring buffers of recent supply/demand samples for the power dashboard. ArrayDeque gives O(1)
    // FIFO eviction; a plain ArrayList would pay O(n) on every remove(0).
    private final java.util.ArrayDeque<Integer> powerSupplyHistory = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Integer> powerDemandHistory = new java.util.ArrayDeque<>();
    // Longer-timescale rollups (values clamped to short range).
    private final java.util.ArrayDeque<Short> supply30m = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> demand30m = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> supply1h = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> demand1h = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> supply12h = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> demand12h = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> supply1d = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> demand1d = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> supplyAll = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Short> demandAll = new java.util.ArrayDeque<>();
    private final java.util.Map<String, java.util.ArrayDeque<Integer>> linkThroughputHistory = new java.util.HashMap<>();
    private int countdown30m = BUCKET_30M;
    private int countdown1h = BUCKET_1H;
    private int countdown12h = BUCKET_12H;
    private int countdown1d = BUCKET_1D;
    private int countdownAll = BUCKET_ALL;
    private long lastScanTime = -1L;
    private String networkId = "";
    private String networkName = "";
    private int networkNumber;
    private String ownerUuid = "";
    private String accessMode = "shared";
    private List<String> editorWhitelist = new ArrayList<>();
    private NetworkPowerData cachedPower = NetworkPowerData.EMPTY;
    private List<net.doole.doolestools.blockentity.NetworkBatteryBlockEntity> cachedBatteries = List.of();
    private long cachedPowerGameTime = Long.MIN_VALUE;
    // auto-scan countdown. counts down every tick and rescans at zero if autoScanIntervalTicks > 0
    private int autoScanCountdown = 0;
    // tracks if any ERROR-level warning is active so the block can emit a redstone signal
    private boolean hasErrorWarnings = false;
    // linked peer computers - scan results are merged from these each time we scan
    // max 8 peers, stored as block positions in the same dimension
    private List<BlockPos> linkedPeerPositions = new ArrayList<>();
    private static final int MAX_PEERS = 8;
    public LogisticsComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_COMPUTER.get(), pos, state);
    }

    public boolean hasErrorWarnings() {
        return hasErrorWarnings;
    }

    private void recomputeWarnings(ServerLevel level) {
        java.util.Map<String, net.doole.doolestools.logistics.data.ScannedBlockData> byId = new java.util.HashMap<>();
        for (net.doole.doolestools.logistics.data.ScannedBlockData s : lastScan) byId.put(s.id(), s);
        java.util.List<net.doole.doolestools.logistics.data.WarningData> warnings =
            net.doole.doolestools.logistics.WarningGenerator.forGraph(graph, byId);
        // also fold in per-block errors from the scan itself
        for (net.doole.doolestools.logistics.data.ScannedBlockData s : lastScan) {
            for (net.doole.doolestools.logistics.data.WarningData w : s.warnings()) {
                if (w.severity() == net.doole.doolestools.logistics.data.WarningData.Severity.ERROR) {
                    warnings = new java.util.ArrayList<>(warnings);
                    warnings.add(w);
                    break;
                }
            }
        }
        boolean prev = hasErrorWarnings;
        hasErrorWarnings = warnings.stream().anyMatch(
            w -> w.severity() == net.doole.doolestools.logistics.data.WarningData.Severity.ERROR);
        // only notify neighbours if the state actually changed, avoids redundant updates every tick
        if (prev != hasErrorWarnings && ModServerConfig.REDSTONE_ALERT_ON_ERROR.get()) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public void serverTick(ServerLevel level) {
        // auto-scan: rescan the area on a configurable interval, no player needed
        int autoInterval = ModServerConfig.AUTO_SCAN_INTERVAL_TICKS.get();
        if (autoInterval > 0) {
            if (--autoScanCountdown <= 0) {
                autoScanCountdown = autoInterval;
                performScan();
                recomputeWarnings(level);
            }
        }

        if (!ModServerConfig.ENABLE_LFM_TRANSPORT.get() || graph == LogisticsGraphData.EMPTY) return;
        int interval = ModServerConfig.LFM_TICK_INTERVAL.get();
        if (level.getGameTime() % interval != 0L) return;
        NetworkPowerData power = getNetworkPower();
        samplePower(power);
        bufferBatteries(level, power);
        // Brownout model: pull what power we can and run scaled to how satisfied the network is.
        // No usable power → automation stops (the dashboard shows "NOT ENOUGH POWER"); partial power
        // → routes throttle down so transport visibly slows rather than hard-stopping.
        float satisfaction = NetworkPowerCalculator.consumePower(level, worldPosition, power);
        if (satisfaction <= 0f) {
            activeRouteIds = java.util.Set.of();
            return;
        }
        java.util.Map<String, Integer> tickCounts = LogiFactoryManager.tickWithCounts(graph, level, lastScan, satisfaction);
        activeRouteIds = tickCounts.keySet();
        updateThroughputHistory(tickCounts);
    }

    /**
     * Wired batteries passively soak up surplus FE and feed back deficit FE, smoothing the network.
     * Server-authoritative: only runs in the server tick, and only on batteries discovered as wired
     * neighbours during the power calculation. Each move is two-phase ({@code openOuter} + commit).
     */
    private void bufferBatteries(ServerLevel level, NetworkPowerData power) {
        List<net.doole.doolestools.blockentity.NetworkBatteryBlockEntity> batteries = getNetworkBatteries();
        if (batteries.isEmpty()) return;
        int surplus = Math.max(0, (power.supplyCentiFe() - power.demandCentiFe()) / 100);
        int deficit = Math.max(0, (power.demandCentiFe() - power.supplyCentiFe()) / 100);
        if (surplus <= 0 && deficit <= 0) return;
        int maxIo = ModServerConfig.BATTERY_MAX_IO.get();
        for (net.doole.doolestools.blockentity.NetworkBatteryBlockEntity battery : batteries) {
            if (surplus > 0) {
                int amount = Math.min(surplus, maxIo);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    int inserted = battery.energyHandler().insert(amount, tx);
                    if (inserted > 0) { tx.commit(); surplus -= inserted; }
                } catch (RuntimeException ignored) {
                }
            } else if (deficit > 0) {
                int amount = Math.min(deficit, maxIo);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    int extracted = battery.energyHandler().extract(amount, tx);
                    if (extracted > 0) { tx.commit(); deficit -= extracted; }
                } catch (RuntimeException ignored) {
                }
            } else {
                break;
            }
        }
    }

    /** Runs a manual, read-only scan around this computer and stores the result. Server-side only. */
    public void performScan() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        int radius = Math.max(ModServerConfig.SCAN_RADIUS.get(), ModServerConfig.WIRELESS_MAX_RANGE.get());
        this.lastScan = scanVisibleNetworks(serverLevel, radius);
        // merge scan results from linked peer computers
        // each peer contributes its own lastScan - read-only, no mutations
        mergePeerScans(serverLevel);
        this.lastScanTime = serverLevel.getGameTime();
        invalidatePowerCache();
        recomputeWarnings(serverLevel);
        setChanged();
    }

    private List<ScannedBlockData> scanVisibleNetworks(ServerLevel serverLevel, int radius) {
        java.util.Map<String, ScannedBlockData> merged = new java.util.LinkedHashMap<>();
        List<SwitchboardNetworkAccess.NetworkRef> networks = SwitchboardNetworkAccess.visibleNetworks(serverLevel, networkId(), networkName());
        if (networks.isEmpty()) networks = List.of(new SwitchboardNetworkAccess.NetworkRef(networkId(), networkName()));
        for (SwitchboardNetworkAccess.NetworkRef network : networks) {
            List<ScannedBlockData> scan = LogisticsScanner.scan(serverLevel, this.worldPosition, radius, this.lastScan, network.id());
            for (ScannedBlockData data : scan) {
                ScannedBlockData sourced = data.withNetworkSource(network.id(), network.name());
                merged.putIfAbsent(network.id() + ":" + sourced.id(), sourced);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void mergePeerScans(ServerLevel serverLevel) {
        if (linkedPeerPositions.isEmpty()) return;
        java.util.Map<String, net.doole.doolestools.logistics.data.ScannedBlockData> merged = new java.util.LinkedHashMap<>();
        for (net.doole.doolestools.logistics.data.ScannedBlockData s : lastScan) merged.put(s.id(), s);
        for (BlockPos peerPos : linkedPeerPositions) {
            if (!serverLevel.hasChunkAt(peerPos)) continue;
            if (!(serverLevel.getBlockEntity(peerPos) instanceof LogisticsComputerBlockEntity peer)) continue;
            if (!peer.hasScanned()) continue;
            for (net.doole.doolestools.logistics.data.ScannedBlockData s : peer.getLastScan()) {
                // dont overwrite blocks our own scan already knows about
                merged.putIfAbsent(s.id(), s);
            }
        }
        this.lastScan = new ArrayList<>(merged.values());
    }

    public List<BlockPos> getLinkedPeerPositions() {
        return List.copyOf(linkedPeerPositions);
    }

    /** Link a peer computer. Returns false if already linked, at limit, or its the same pos. */
    public boolean linkPeer(BlockPos peerPos, ServerLevel level) {
        if (peerPos.equals(worldPosition)) return false;
        if (linkedPeerPositions.contains(peerPos)) return false;
        if (linkedPeerPositions.size() >= MAX_PEERS) return false;
        if (!(level.getBlockEntity(peerPos) instanceof LogisticsComputerBlockEntity)) return false;
        linkedPeerPositions.add(peerPos.immutable());
        setChanged();
        return true;
    }

    public boolean unlinkPeer(BlockPos peerPos) {
        boolean removed = linkedPeerPositions.remove(peerPos);
        if (removed) setChanged();
        return removed;
    }

    public String networkId() {
        if (networkId == null || networkId.isBlank()) networkId = "net_" + Long.toUnsignedString(worldPosition.asLong());
        ensureNetworkNumber();
        return networkId;
    }

    public int networkNumber() {
        ensureNetworkNumber();
        return networkNumber;
    }

    public String formattedNetworkNumber() {
        return NetworkIdentitySavedData.formatFourDigits(networkNumber());
    }

    public String networkName() {
        if (networkName == null || networkName.isBlank()) return "NETWORK#" + formattedNetworkNumber();
        return networkName;
    }

    private void ensureNetworkNumber() {
        if (networkNumber > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            networkNumber = NetworkIdentitySavedData.get(serverLevel).allocateNetworkNumber();
            setChanged();
        }
    }

    public String accessMode() {
        return normalizeAccessMode(accessMode);
    }

    public List<String> editorWhitelist() {
        return List.copyOf(editorWhitelist);
    }

    public boolean canEdit(Player player) {
        if (player == null) return false;
        ensureOwner(player);
        String mode = accessMode();
        if ("shared".equals(mode)) return true;
        String playerId = player.getUUID().toString();
        if (playerId.equals(ownerUuid)) return true;
        return "whitelist".equals(mode) && editorWhitelist.contains(playerId);
    }

    public void ensureOwner(Player player) {
        if (player != null && (ownerUuid == null || ownerUuid.isBlank())) {
            ownerUuid = player.getUUID().toString();
            setChanged();
        }
    }

    public void setNetworkSettings(String name, String mode, List<String> whitelist, Player player) {
        ensureOwner(player);
        this.networkName = sanitizeName(name);
        this.accessMode = normalizeAccessMode(mode);
        this.editorWhitelist = sanitizeWhitelist(whitelist);
        invalidatePowerCache();
        setChanged();
    }

    private static String normalizeAccessMode(String mode) {
        String value = mode == null ? "shared" : mode.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "private", "whitelist" -> value;
            default -> "shared";
        };
    }

    private static String sanitizeName(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length() && out.length() < 48; i++) {
            char c = trimmed.charAt(i);
            if (!Character.isISOControl(c)) out.append(c);
        }
        return out.toString();
    }

    private static List<String> sanitizeWhitelist(List<String> values) {
        List<String> out = new ArrayList<>();
        if (values == null) return out;
        for (String value : values) {
            try {
                out.add(UUID.fromString(value).toString());
            } catch (IllegalArgumentException ignored) {
            }
            if (out.size() >= 32) break;
        }
        return out;
    }

    public List<ScannedBlockData> getLastScan() {
        return lastScan;
    }

    public LogisticsGraphData getGraph() {
        return graph;
    }

    public long getLastScanTime() {
        return lastScanTime;
    }

    public boolean hasScanned() {
        return lastScanTime >= 0;
    }

    public NetworkPowerData getNetworkPower() {
        if (!(level instanceof ServerLevel serverLevel)) return NetworkPowerData.EMPTY;
        long gameTime = serverLevel.getGameTime();
        if (cachedPowerGameTime == gameTime) return cachedPower;
        NetworkPowerCalculator.Result result = NetworkPowerCalculator.calculateResult(serverLevel, worldPosition, lastScan, graph, networkId());
        cachedPower = result.data();
        cachedBatteries = result.batteries();
        cachedPowerGameTime = gameTime;
        return cachedPower;
    }

    /** Wired batteries discovered on the last power calculation this tick. Server-side only. */
    public List<net.doole.doolestools.blockentity.NetworkBatteryBlockEntity> getNetworkBatteries() {
        getNetworkPower();
        return cachedBatteries;
    }

    public void sampleCurrentPower() {
        samplePower(getNetworkPower());
    }

    private void samplePower(NetworkPowerData power) {
        if (power == null) power = NetworkPowerData.EMPTY;
        addPowerSample(powerSupplyHistory, power.supplyCentiFe());
        addPowerSample(powerDemandHistory, power.demandCentiFe());
        rollupTimescales();
    }

    private static void addPowerSample(java.util.ArrayDeque<Integer> samples, int value) {
        samples.addLast(Math.max(0, value));
        while (samples.size() > POWER_HISTORY_SAMPLES) samples.removeFirst();
    }

    /** Decrement each timescale counter; when one elapses, average the current buffer into it. */
    private void rollupTimescales() {
        if (--countdown30m <= 0) { countdown30m = BUCKET_30M; pushAverage(supply30m, demand30m, SLOTS_30M); }
        if (--countdown1h <= 0)  { countdown1h = BUCKET_1H;   pushAverage(supply1h, demand1h, SLOTS_1H); }
        if (--countdown12h <= 0) { countdown12h = BUCKET_12H; pushAverage(supply12h, demand12h, SLOTS_12H); }
        if (--countdown1d <= 0)  { countdown1d = BUCKET_1D;   pushAverage(supply1d, demand1d, SLOTS_1D); }
        if (--countdownAll <= 0) { countdownAll = BUCKET_ALL; pushAverage(supplyAll, demandAll, SLOTS_ALL); }
    }

    private void pushAverage(java.util.ArrayDeque<Short> supplyOut, java.util.ArrayDeque<Short> demandOut, int maxSlots) {
        addShortSample(supplyOut, average(powerSupplyHistory), maxSlots);
        addShortSample(demandOut, average(powerDemandHistory), maxSlots);
    }

    private static int average(java.util.ArrayDeque<Integer> samples) {
        if (samples.isEmpty()) return 0;
        long sum = 0;
        for (int v : samples) sum += v;
        return (int) (sum / samples.size());
    }

    private static void addShortSample(java.util.ArrayDeque<Short> samples, int value, int maxSlots) {
        samples.addLast((short) Math.max(0, Math.min(Short.MAX_VALUE, value)));
        while (samples.size() > maxSlots) samples.removeFirst();
    }

    private static List<Short> copyShorts(java.util.ArrayDeque<Short> samples) {
        return List.copyOf(samples);
    }

    public List<Integer> getPowerSupplyHistory() {
        if (powerSupplyHistory.isEmpty()) sampleCurrentPower();
        return List.copyOf(powerSupplyHistory);
    }

    public List<Integer> getPowerDemandHistory() {
        if (powerDemandHistory.isEmpty()) sampleCurrentPower();
        return List.copyOf(powerDemandHistory);
    }

    public List<Short> getSupply30m() { return copyShorts(supply30m); }
    public List<Short> getDemand30m() { return copyShorts(demand30m); }
    public List<Short> getSupply1h() { return copyShorts(supply1h); }
    public List<Short> getDemand1h() { return copyShorts(demand1h); }
    public List<Short> getSupply12h() { return copyShorts(supply12h); }
    public List<Short> getDemand12h() { return copyShorts(demand12h); }
    public List<Short> getSupply1d() { return copyShorts(supply1d); }
    public List<Short> getDemand1d() { return copyShorts(demand1d); }
    public List<Short> getSupplyAllTime() { return copyShorts(supplyAll); }
    public List<Short> getDemandAllTime() { return copyShorts(demandAll); }

    public java.util.List<String> getActiveRouteIds() {
        return java.util.List.copyOf(activeRouteIds);
    }

    private void updateThroughputHistory(java.util.Map<String, Integer> tickCounts) {
        java.util.Set<String> graphLinks = new java.util.HashSet<>();
        for (var link : graph.activeCanvas().links()) graphLinks.add(link.linkId());
        linkThroughputHistory.keySet().removeIf(id -> !graphLinks.contains(id));
        for (String linkId : graphLinks) {
            java.util.ArrayDeque<Integer> samples = linkThroughputHistory.computeIfAbsent(linkId, k -> new java.util.ArrayDeque<>());
            samples.addLast(Math.max(0, tickCounts.getOrDefault(linkId, 0)));
            while (samples.size() > THROUGHPUT_SAMPLES) samples.removeFirst();
        }
    }

    public java.util.Map<String, List<Integer>> getLinkThroughputHistory() {
        java.util.Map<String, List<Integer>> out = new java.util.HashMap<>();
        for (var entry : linkThroughputHistory.entrySet()) {
            out.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return out;
    }

    /** Persist a graph edited on the client. Validation of payload contents happens before this call. */
    public void saveGraph(LogisticsGraphData newGraph) {
        this.graph = newGraph;
        invalidatePowerCache();
        if (level instanceof ServerLevel sl) recomputeWarnings(sl);
        setChanged();
    }

    public void clearScan() {
        this.lastScan = new ArrayList<>();
        this.lastScanTime = -1L;
        invalidatePowerCache();
        setChanged();
    }

    private void invalidatePowerCache() {
        cachedPowerGameTime = Long.MIN_VALUE;
        cachedPower = NetworkPowerData.EMPTY;
        cachedBatteries = List.of();
    }

    // --- Persistence (Value I/O) ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("graph", LogisticsGraphData.CODEC, graph);
        output.store("scan", ScannedBlockData.CODEC.listOf(), lastScan);
        output.putLong("lastScanTime", lastScanTime);
        output.store("networkId", com.mojang.serialization.Codec.STRING, networkId());
        output.putInt("networkNumber", networkNumber());
        output.store("networkName", com.mojang.serialization.Codec.STRING, networkName == null ? "" : networkName);
        output.store("ownerUuid", com.mojang.serialization.Codec.STRING, ownerUuid == null ? "" : ownerUuid);
        output.store("accessMode", com.mojang.serialization.Codec.STRING, accessMode());
        output.store("editorWhitelist", com.mojang.serialization.Codec.STRING.listOf(), editorWhitelist());
        List<Long> peerLongs = new ArrayList<>(linkedPeerPositions.size());
        for (BlockPos p : linkedPeerPositions) peerLongs.add(p.asLong());
        output.store("linkedPeers", com.mojang.serialization.Codec.LONG.listOf(), peerLongs);
        storeShorts(output, "supply30m", supply30m);
        storeShorts(output, "demand30m", demand30m);
        storeShorts(output, "supply1h", supply1h);
        storeShorts(output, "demand1h", demand1h);
        storeShorts(output, "supply12h", supply12h);
        storeShorts(output, "demand12h", demand12h);
        storeShorts(output, "supply1d", supply1d);
        storeShorts(output, "demand1d", demand1d);
        storeShorts(output, "supplyAll", supplyAll);
        storeShorts(output, "demandAll", demandAll);
    }

    private static void storeShorts(ValueOutput output, String key, java.util.ArrayDeque<Short> samples) {
        List<Integer> ints = new ArrayList<>(samples.size());
        for (short v : samples) ints.add((int) v);
        output.store(key, com.mojang.serialization.Codec.INT.listOf(), ints);
    }

    private static void loadShorts(ValueInput input, String key, java.util.ArrayDeque<Short> target, int maxSlots) {
        target.clear();
        for (int v : input.read(key, com.mojang.serialization.Codec.INT.listOf()).orElse(List.of())) {
            target.addLast((short) Math.max(0, Math.min(Short.MAX_VALUE, v)));
            while (target.size() > maxSlots) target.removeFirst();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.graph = input.read("graph", LogisticsGraphData.CODEC).orElse(LogisticsGraphData.EMPTY);
        this.lastScan = new ArrayList<>(input.read("scan", ScannedBlockData.CODEC.listOf()).orElse(List.of()));
        this.lastScanTime = input.getLongOr("lastScanTime", -1L);
        this.networkId = input.read("networkId", com.mojang.serialization.Codec.STRING).orElse("");
        this.networkNumber = Math.max(0, input.getIntOr("networkNumber", 0));
        this.networkName = input.read("networkName", com.mojang.serialization.Codec.STRING).orElse("");
        this.ownerUuid = input.read("ownerUuid", com.mojang.serialization.Codec.STRING).orElse("");
        this.accessMode = normalizeAccessMode(input.read("accessMode", com.mojang.serialization.Codec.STRING).orElse("shared"));
        this.editorWhitelist = sanitizeWhitelist(input.read("editorWhitelist", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
        this.linkedPeerPositions = new ArrayList<>();
        for (long packed : input.read("linkedPeers", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of())) {
            this.linkedPeerPositions.add(BlockPos.of(packed).immutable());
            if (this.linkedPeerPositions.size() >= MAX_PEERS) break;
        }
        loadShorts(input, "supply30m", supply30m, SLOTS_30M);
        loadShorts(input, "demand30m", demand30m, SLOTS_30M);
        loadShorts(input, "supply1h", supply1h, SLOTS_1H);
        loadShorts(input, "demand1h", demand1h, SLOTS_1H);
        loadShorts(input, "supply12h", supply12h, SLOTS_12H);
        loadShorts(input, "demand12h", demand12h, SLOTS_12H);
        loadShorts(input, "supply1d", supply1d, SLOTS_1D);
        loadShorts(input, "demand1d", demand1d, SLOTS_1D);
        loadShorts(input, "supplyAll", supplyAll, SLOTS_ALL);
        loadShorts(input, "demandAll", demandAll, SLOTS_ALL);
        invalidatePowerCache();
    }

    // --- Menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.doolestools.logistics_computer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ensureOwner(player);
        return new LogisticsComputerMenu(containerId, playerInventory, this.worldPosition);
    }

    public static void logMissing() {
        DoolesTools.LOGGER.debug("Logistics computer reference missing during sync");
    }
}
