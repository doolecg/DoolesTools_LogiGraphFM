package net.doole.doolestools.blockentity;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.NetworkPowerCalculator;
import net.doole.doolestools.logistics.LogisticsScanner;
import net.doole.doolestools.logistics.easyfactory.EasyFactoryManager;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
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

    private List<ScannedBlockData> lastScan = new ArrayList<>();
    private LogisticsGraphData graph = LogisticsGraphData.EMPTY;
    private java.util.Set<String> activeRouteIds = java.util.Set.of();
    // Ring buffers of recent supply/demand samples for the power dashboard. ArrayDeque gives O(1)
    // FIFO eviction; a plain ArrayList would pay O(n) on every remove(0).
    private final java.util.ArrayDeque<Integer> powerSupplyHistory = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Integer> powerDemandHistory = new java.util.ArrayDeque<>();
    private long lastScanTime = -1L;
    private String networkId = "";
    private String networkName = "";
    private int networkNumber;
    private String ownerUuid = "";
    private String accessMode = "shared";
    private List<String> editorWhitelist = new ArrayList<>();
    private NetworkPowerData cachedPower = NetworkPowerData.EMPTY;
    private long cachedPowerGameTime = Long.MIN_VALUE;
    public LogisticsComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_COMPUTER.get(), pos, state);
    }

    public void serverTick(ServerLevel level) {
        if (!ModServerConfig.ENABLE_EASY_FACTORY_TRANSPORT.get() || graph == LogisticsGraphData.EMPTY) return;
        int interval = ModServerConfig.EASY_FACTORY_TICK_INTERVAL.get();
        if (level.getGameTime() % interval != 0L) return;
        NetworkPowerData power = getNetworkPower();
        samplePower(power);
        // Brownout model: pull what power we can and run scaled to how satisfied the network is.
        // No usable power → automation stops (the dashboard shows "NOT ENOUGH POWER"); partial power
        // → routes throttle down so transport visibly slows rather than hard-stopping.
        float satisfaction = NetworkPowerCalculator.consumePower(level, worldPosition, power);
        if (satisfaction <= 0f) {
            activeRouteIds = java.util.Set.of();
            return;
        }
        activeRouteIds = EasyFactoryManager.tick(graph, level, lastScan, satisfaction);
    }

    /** Runs a manual, read-only scan around this computer and stores the result. Server-side only. */
    public void performScan() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        int radius = Math.max(SCAN_RADIUS, ModServerConfig.WIRELESS_MAX_RANGE.get());
        this.lastScan = LogisticsScanner.scan(serverLevel, this.worldPosition, radius, this.lastScan, networkId());
        this.lastScanTime = serverLevel.getGameTime();
        invalidatePowerCache();
        setChanged();
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
        cachedPower = NetworkPowerCalculator.calculate(serverLevel, worldPosition, lastScan, graph, networkId());
        cachedPowerGameTime = gameTime;
        return cachedPower;
    }

    public void sampleCurrentPower() {
        samplePower(getNetworkPower());
    }

    private void samplePower(NetworkPowerData power) {
        if (power == null) power = NetworkPowerData.EMPTY;
        addPowerSample(powerSupplyHistory, power.supplyCentiFe());
        addPowerSample(powerDemandHistory, power.demandCentiFe());
    }

    private static void addPowerSample(java.util.ArrayDeque<Integer> samples, int value) {
        samples.addLast(Math.max(0, value));
        while (samples.size() > POWER_HISTORY_SAMPLES) samples.removeFirst();
    }

    public List<Integer> getPowerSupplyHistory() {
        if (powerSupplyHistory.isEmpty()) sampleCurrentPower();
        return List.copyOf(powerSupplyHistory);
    }

    public List<Integer> getPowerDemandHistory() {
        if (powerDemandHistory.isEmpty()) sampleCurrentPower();
        return List.copyOf(powerDemandHistory);
    }

    public java.util.List<String> getActiveRouteIds() {
        return java.util.List.copyOf(activeRouteIds);
    }

    /** Persist a graph edited on the client. Validation of payload contents happens before this call. */
    public void saveGraph(LogisticsGraphData newGraph) {
        this.graph = newGraph;
        invalidatePowerCache();
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
