package net.doole.doolestools.logistics;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.block.ScannerHiddenBlock;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.integration.ModProviderRegistry;
import net.doole.doolestools.integration.ModStorageProvider;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkModemBlockEntity;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.blockentity.WirelessRouterBlockEntity;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.FluidEntry;
import net.doole.doolestools.logistics.data.FluidSummary;
import net.doole.doolestools.logistics.data.FurnaceSummary;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.doole.doolestools.logistics.data.MachineProgressData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.doole.doolestools.logistics.tags.ModBlockTags;
import net.doole.doolestools.world.BlockLabelSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

/**
 * Read-only area scanner. Walks a cube around the computer and snapshots inventories/furnaces.
 *
 * <p>Hard rules (MVP): manual-trigger only; never force-loads chunks; never mutates, extracts, or
 * inserts; never changes machine state; never crashes on an unreadable block.</p>
 *
 * <p>Item inventories are read through vanilla {@link Container} when available, then NeoForge item,
 * fluid, and energy capabilities. All capability access is read-only: amount/capacity/resource metadata
 * only, no insert/extract transactions.</p>
 */
public final class LogisticsScanner {
    private LogisticsScanner() {}

    private static final int TOP_STACKS = 500;
    private static final int MAX_WIRED_NETWORK_STEPS = 256;

    /**
     * Capability probe directions: {@code null} first (meaning "no specific side"), then all six
     * faces. Declared as a constant so the array is not allocated on every capability query.
     */
    private static final Direction[] PROBE_DIRECTIONS =
            { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public static List<ScannedBlockData> scan(ServerLevel level, BlockPos center, int radius) {
        return scan(level, center, radius, List.of(), "");
    }

    public static List<ScannedBlockData> scan(ServerLevel level, BlockPos center, int radius, List<ScannedBlockData> previousScan) {
        return scan(level, center, radius, previousScan, "");
    }

    public static List<ScannedBlockData> scan(ServerLevel level, BlockPos center, int radius, List<ScannedBlockData> previousScan, String networkId) {
        Map<String, ScannedBlockData> resultsById = new LinkedHashMap<>();
        long gameTime = level.getGameTime();
        BlockLabelSavedData labels = BlockLabelSavedData.get(level);
        List<RelayNode> relays = discoverReachableRelays(level, center, networkId);
        Map<String, ScannedBlockData> previousById = new LinkedHashMap<>();
        for (ScannedBlockData previous : previousScan) previousById.put(previous.id(), previous);

        scanLoadedBlockEntities(level, center, center, radius, gameTime, labels, networkId, relays, previousById, resultsById);
        int relayRadius = ModServerConfig.WIRELESS_MAX_RANGE.get();
        for (RelayNode relay : relays) {
            scanRelayReach(level, center, relay.pos(), relayRadius, gameTime, labels, networkId, relays, previousById, resultsById);
        }
        List<ScannedBlockData> results = new ArrayList<>(resultsById.values());
        results.sort(Comparator.comparingDouble(ScannedBlockData::distance));
        return results;
    }

    private static void scanRelayReach(ServerLevel level, BlockPos computerPos, BlockPos relayPos, int radius, long gameTime,
                                       BlockLabelSavedData labels, String networkId, List<RelayNode> relays,
                                       Map<String, ScannedBlockData> previousById, Map<String, ScannedBlockData> resultsById) {
        scanLoadedBlockEntities(level, computerPos, relayPos, radius, gameTime, labels, networkId, relays, previousById, resultsById);
    }

    private static void scanLoadedBlockEntities(ServerLevel level, BlockPos computerPos, BlockPos center, int radius, long gameTime,
                                                BlockLabelSavedData labels, String networkId, List<RelayNode> relays,
                                                Map<String, ScannedBlockData> previousById, Map<String, ScannedBlockData> resultsById) {
        long radiusSqr = (long) radius * radius;
        forEachLoadedBlockEntity(level, center, radius, blockEntity -> {
            BlockPos pos = blockEntity.getBlockPos();
            if (pos.equals(computerPos) || center.distSqr(pos) > radiusSqr) return;
            try {
                if (blockEntity instanceof NetworkWireBlockEntity wire) {
                    // Multi-endpoint wires produce one scan result per endpoint face.
                    for (Direction dir : Direction.values()) {
                        if (!wire.hasEndpointAt(dir)) continue;
                        if (!networkMatches(wire.endpointNetworkId(dir), networkId)) continue;
                        ScannedBlockData data = readWireHostedEndpointAtFace(level, computerPos, wire, dir, gameTime, labels, relays);
                        if (data != null) resultsById.put(data.id(), data.withProgress(progressFor(data, previousById.get(data.id()))));
                    }
                } else {
                    ScannedBlockData data = readBlock(level, computerPos, pos, gameTime, labels, true, networkId, relays);
                    if (data != null) resultsById.put(data.id(), data.withProgress(progressFor(data, previousById.get(data.id()))));
                }
            } catch (Exception e) {
                DoolesTools.LOGGER.debug("Scan skipped unreadable block at {}: {}", pos, e.toString());
            }
        });
    }

    private static ScannedBlockData readBlock(ServerLevel level, BlockPos center, BlockPos pos, long gameTime, BlockLabelSavedData labels) {
        return readBlock(level, center, pos, gameTime, labels, true, "");
    }

    private static ScannedBlockData readBlock(ServerLevel level, BlockPos center, BlockPos pos, long gameTime, BlockLabelSavedData labels, boolean networkOnly) {
        return readBlock(level, center, pos, gameTime, labels, networkOnly, "");
    }

    private static ScannedBlockData readBlock(ServerLevel level, BlockPos center, BlockPos pos, long gameTime, BlockLabelSavedData labels, boolean networkOnly, String networkId) {
        return readBlock(level, center, pos, gameTime, labels, networkOnly, networkId, List.of());
    }

    private static ScannedBlockData readBlock(ServerLevel level, BlockPos center, BlockPos pos, long gameTime, BlockLabelSavedData labels, boolean networkOnly, String networkId, List<RelayNode> relays) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null; // Plain blocks carry no logistics data.
        }

        if (be instanceof NetworkEndpointBlockEntity endpoint) {
            if (!networkMatches(endpoint.networkId(), networkId)) return null;
            return readNetworkEndpoint(level, center, endpoint, gameTime, labels, relays);
        }
        if (be instanceof NetworkWireBlockEntity wire) {
            if (!networkMatches(wire.endpointNetworkId(), networkId)) return null;
            return readWireHostedEndpoint(level, center, wire, gameTime, labels, relays);
        }

        if (networkOnly) return null;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (state.is(ModBlockTags.SCANNER_BLACKLIST) || block instanceof ScannerHiddenBlock) {
            return null;
        }
        String label = labels.getLabel(level.dimension().identifier(), pos);
        boolean largeChest = block instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
        String blockName = label != null && !label.isBlank() ? label : largeChest ? "Large Chest" : block.getName().getString();
        String registryId = BuiltInRegistries.BLOCK.getKey(block).toString();
        double distance = Math.sqrt(center.distSqr(pos));
        String dimension = level.dimension().identifier().toString();
        String id = blockId(pos);

        ScannedType type;
        InventorySummary inventory = InventorySummary.EMPTY;
        FurnaceSummary furnace = FurnaceSummary.EMPTY;
        FluidSummary fluids = readFluidCapability(level, pos, state, be);
        EnergySummary energy = readEnergyCapability(level, pos, state, be);

        if (block instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, state, level, pos, false);
            if (chestContainer == null) return null;
            type = ScannedType.STORAGE;
            inventory = readContainer(chestContainer);
        } else if (be instanceof AbstractFurnaceBlockEntity furnaceBe) {
            type = ScannedType.MACHINE;
            inventory = readContainer(furnaceBe);
            furnace = readFurnace(level, furnaceBe);
        } else if (be instanceof Container container) {
            inventory = readContainer(container);
            if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity || be instanceof DropperBlockEntity) {
                type = ScannedType.TRANSPORT;
            } else {
                type = ScannedType.STORAGE;
            }
        } else {
            inventory = readItemCapability(level, pos, state, be);
            if (energy.hasData() || fluids.hasData()) {
                type = ScannedType.MACHINE;
            } else if (inventory.hasData()) {
                type = ScannedType.STORAGE;
            } else {
                // Has a block entity but no readable standard data — likely a modded machine.
                type = ScannedType.UNKNOWN_MACHINE;
            }
        }

        // Probe non-vanilla machines for their real process: a blockstate "active" flag
        // and reflected recipe progress. Energy is only an input to that process, so a machine that
        // exposes recipe progress is headlined by it — not by its (usually topped-up) energy buffer.
        MachineProgressData liveProgress = MachineProgressData.EMPTY;
        if (!furnace.hasData()) {
            ModMachineProgress.Reading reading = ModMachineProgress.read(state, be);
            if (reading != null) {
                liveProgress = new MachineProgressData(true, "", reading.percent(), reading.remainingTicks(),
                        reading.active(), false, reading.active() ? "Working" : "Standby");
                if (type != ScannedType.TRANSPORT) type = ScannedType.MACHINE;
            }
        }

        // Ask mod-aware providers for extra data (AE2 cells, Mekanism chemicals, Create basins).
        // Only merge fields the standard read left empty - dont replace good capability data with provider data.
        ModStorageProvider.ModStorageResult providerResult = ModProviderRegistry.tryRead(level, be);
        if (providerResult != null) {
            if (providerResult.inventory() != null && providerResult.inventory().hasData() && !inventory.hasData()) {
                inventory = providerResult.inventory();
            }
            if (providerResult.fluids() != null && providerResult.fluids().hasData() && !fluids.hasData()) {
                fluids = providerResult.fluids();
            }
            if (providerResult.energy() != null && providerResult.energy().hasData() && !energy.hasData()) {
                energy = providerResult.energy();
            }
            // let the provider upgrade an UNKNOWN_MACHINE classification if it knows the block better
            if (providerResult.typeOverride() != null && providerResult.typeOverride() != ScannedType.UNKNOWN
                    && type == ScannedType.UNKNOWN_MACHINE) {
                type = providerResult.typeOverride();
            }
        }

        List<WarningData> warnings = WarningGenerator.forScannedBlock(type, inventory, furnace);

        ScannedBlockData snapshot = new ScannedBlockData(id, pos, dimension, blockName, registryId, type, distance,
                inventory, fluids, energy, furnace, warnings, gameTime);
        return liveProgress.present() ? snapshot.withProgress(liveProgress) : snapshot;
    }

    private static boolean networkMatches(String endpointNetworkId, String computerNetworkId) {
        if (computerNetworkId == null || computerNetworkId.isBlank()) return false;
        if (endpointNetworkId == null || endpointNetworkId.isBlank()) return false;
        return endpointNetworkId.equals(computerNetworkId);
    }

    private static boolean relayNetworkMatches(String relayNetworkId, String computerNetworkId) {
        return networkMatches(relayNetworkId, computerNetworkId);
    }

    private static ScannedBlockData readNetworkEndpoint(ServerLevel level, BlockPos center, NetworkEndpointBlockEntity endpoint,
                                                        long gameTime, BlockLabelSavedData labels, List<RelayNode> relays) {
        if (endpoint instanceof NetworkModemBlockEntity modem && !isWiredModemOnline(level, modem)) return null;
        if (endpoint instanceof WirelessRouterBlockEntity) return null;
        if (!(endpoint instanceof NetworkModemBlockEntity) && !wirelessReachable(center, endpoint.getBlockPos(), endpoint.upgradeCount("range"), relays)) return null;
        BlockPos attached = endpoint.attachedPos();
        if (!level.hasChunkAt(attached)) return null;
        // For double chests, redirect to the canonical half so a dongle on either side works.
        BlockState attachedState = level.getBlockState(attached);
        attached = canonicalScanPos(attached, attachedState);
        if (!level.hasChunkAt(attached)) return null;
        BlockEntity attachedBe = level.getBlockEntity(attached);
        if (attachedBe == null || attachedBe instanceof NetworkEndpointBlockEntity) return null;
        ScannedBlockData attachedData = readBlock(level, center, attached, gameTime, labels, false);
        if (attachedData == null) return null;
        return attachedData.withNetworkIdentity("net_" + endpoint.deviceId(), endpoint.deviceName());
    }

    private static ScannedBlockData readWireHostedEndpoint(ServerLevel level, BlockPos center, NetworkWireBlockEntity wire,
                                                           long gameTime, BlockLabelSavedData labels, List<RelayNode> relays) {
        if (!wire.hasEndpoint()) return null;
        return readWireHostedEndpointAtFace(level, center, wire, wire.endpointFace(), gameTime, labels, relays);
    }

    private static ScannedBlockData readWireHostedEndpointAtFace(ServerLevel level, BlockPos center, NetworkWireBlockEntity wire,
                                                                  Direction face, long gameTime, BlockLabelSavedData labels, List<RelayNode> relays) {
        if (!wire.hasEndpointAt(face)) return null;
        if (wire.hasModem() && !isWiredEndpointOnline(level, wire.getBlockPos())) return null;
        if (wire.hasRouter() && !wirelessReachable(center, wire.getBlockPos(), wire.upgradeCount(face, "range"), relays)) return null;
        BlockPos attached = wire.attachedPos(face);
        if (!level.hasChunkAt(attached)) return null;
        // For double chests, redirect to the canonical half so a socket on either side works.
        BlockState attachedState = level.getBlockState(attached);
        attached = canonicalScanPos(attached, attachedState);
        if (!level.hasChunkAt(attached)) return null;
        BlockEntity attachedBe = level.getBlockEntity(attached);
        if (attachedBe == null || attachedBe instanceof NetworkEndpointBlockEntity || attachedBe instanceof NetworkWireBlockEntity) return null;
        ScannedBlockData attachedData = readBlock(level, center, attached, gameTime, labels, false);
        if (attachedData == null) return null;
        return attachedData.withNetworkIdentity("net_" + wire.endpointId(face), wire.endpointName(face));
    }

    private static boolean isWiredModemOnline(ServerLevel level, NetworkModemBlockEntity start) {
        if (level.getBlockEntity(start.attachedPos()) instanceof LogisticsComputerBlockEntity) return true;
        return isWiredEndpointOnline(level, start.getBlockPos());
    }

    private static boolean wirelessInRange(BlockPos computerPos, BlockPos endpointPos, int rangeUpgrades) {
        return WirelessNetworkPolicy.inRange(
                ModServerConfig.WIRELESS_BASE_RANGE.get(),
                ModServerConfig.WIRELESS_RANGE_UPGRADE_BLOCKS.get(),
                rangeUpgrades,
                ModServerConfig.WIRELESS_MAX_RANGE.get(),
                distanceSqr(computerPos, endpointPos));
    }

    private static boolean wirelessReachable(BlockPos computerPos, BlockPos endpointPos, int endpointRangeUpgrades, List<RelayNode> relays) {
        if (wirelessInRange(computerPos, endpointPos, endpointRangeUpgrades)) return true;
        for (RelayNode relay : relays) {
            if (wirelessInRange(relay.pos(), endpointPos, endpointRangeUpgrades)) return true;
        }
        return false;
    }

    private static List<RelayNode> discoverReachableRelays(ServerLevel level, BlockPos center, String networkId) {
        List<RelayNode> reachable = new ArrayList<>();
        Queue<RelayNode> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        for (RelayNode relay : discoverRelaysAround(level, center, networkId)) {
            if (wirelessInRange(center, relay.pos(), relay.rangeUpgrades()) && seen.add(relay.pos())) queue.add(relay);
        }
        int steps = 0;
        int maxSteps = ModServerConfig.MAX_RELAY_TRAVERSAL.get();
        while (!queue.isEmpty() && steps++ < maxSteps) {
            RelayNode current = queue.remove();
            reachable.add(current);
            for (RelayNode relay : discoverRelaysAround(level, current.pos(), networkId)) {
                if (seen.contains(relay.pos())) continue;
                if (wirelessInRange(current.pos(), relay.pos(), relay.rangeUpgrades()) && seen.add(relay.pos())) queue.add(relay);
            }
        }
        return reachable;
    }

    private static List<RelayNode> discoverRelaysAround(ServerLevel level, BlockPos center, String networkId) {
        List<RelayNode> relays = new ArrayList<>();
        int radius = ModServerConfig.WIRELESS_MAX_RANGE.get();
        long radiusSqr = (long) radius * radius;
        forEachLoadedBlockEntity(level, center, radius, blockEntity -> {
            if (!(blockEntity instanceof NetworkRelayBlockEntity relay)) return;
            BlockPos pos = relay.getBlockPos();
            if (center.distSqr(pos) <= radiusSqr && relayNetworkMatches(relay.networkId(), networkId)) {
                relays.add(new RelayNode(pos, relay.upgradeCount("range")));
            }
        });
        return relays;
    }

    private static void forEachLoadedBlockEntity(ServerLevel level, BlockPos center, int radius, java.util.function.Consumer<BlockEntity> consumer) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) consumer.accept(blockEntity);
            }
        }
    }

    /** True if any endpoint face of the wire block attaches to a LogisticsComputer. */
    private static boolean wireAttachedToComputer(ServerLevel level, NetworkWireBlockEntity wire) {
        for (Direction dir : Direction.values()) {
            if (wire.hasEndpointAt(dir) && level.getBlockEntity(wire.attachedPos(dir)) instanceof LogisticsComputerBlockEntity) return true;
        }
        return false;
    }

    private static long distanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record RelayNode(BlockPos pos, int rangeUpgrades) {}

    private static boolean isWiredEndpointOnline(ServerLevel level, BlockPos startPos) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(startPos);
        seen.add(startPos);

        int steps = 0;
        while (!queue.isEmpty() && steps++ < MAX_WIRED_NETWORK_STEPS) {
            BlockPos pos = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next) || !level.hasChunkAt(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof NetworkModemBlockEntity modem) {
                    if (!next.equals(startPos) && level.getBlockEntity(modem.attachedPos()) instanceof LogisticsComputerBlockEntity) return true;
                    queue.add(next);
                } else if (be instanceof NetworkWireBlockEntity wire) {
                    if (!next.equals(startPos) && wire.hasModem() && wireAttachedToComputer(level, wire)) return true;
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static MachineProgressData progressFor(ScannedBlockData current, ScannedBlockData previous) {
        boolean error = current.warnings().stream().anyMatch(w -> w.severity() == WarningData.Severity.ERROR);
        if (current.furnace().hasData()) {
            FurnaceSummary f = current.furnace();
            // Use the furnace's real, current cook progress. Do NOT extrapolate by the inter-scan gap
            // here: the scan already read live cookProgress, so adding elapsed ticks double-counts.
            int percent = f.cookPercent();
            boolean active = f.isCooking();
            boolean blocked = "Output Full".equals(f.status()) || "Not Progressing".equals(f.status());
            long remaining = active ? Math.max(0L, f.cookTotal() - f.cookProgress()) : -1L;
            return new MachineProgressData(true, "Recipe", percent, remaining, active, error || blocked, f.status());
        }

        // A modded machine probed in readBlock carries an instantaneous reading (blockstate active flag
        // and/or reflected recipe progress). Prefer it: it doesn't flicker and headlines the real
        // process rather than the energy buffer.
        MachineProgressData probe = current.progress();
        ProgressSample sample = sample(current);
        if (probe.present()) {
            boolean probeActive = probe.active() && !error;
            boolean hasRecipe = probe.percent() >= 0;
            int percent = hasRecipe ? probe.percent() : (sample != null ? sample.percent : 0);
            long remaining = hasRecipe ? probe.remainingTicks() : -1L;
            String label = hasRecipe ? "Recipe" : (sample != null ? sample.label : "Machine");
            String status = error ? "Blocked" : probeActive ? "Working" : "Standby";
            return new MachineProgressData(true, label, percent, remaining, probeActive, error, status);
        }

        if (sample == null) return MachineProgressData.EMPTY;
        // No probe signal (unknown mod): a generic machine is "working" if ANY monitored quantity moved
        // since the last scan — energy, any fluid tank, or inventory contents. Activity needs two scans.
        boolean active = previous != null && stateChanged(current, previous) && !error;
        long deltaTicks = previous == null ? 0L : Math.max(1L, current.lastScannedGameTime() - previous.lastScannedGameTime());
        ProgressSample previousSample = previous == null ? null : sample(previous);
        int sampleDelta = previousSample == null ? 0 : sample.percent - previousSample.percent;
        long remaining = -1L;
        if (active && sampleDelta != 0) {
            int targetDelta = sampleDelta > 0 ? 100 - sample.percent : sample.percent;
            remaining = Math.max(0L, Math.round(targetDelta * (deltaTicks / (double) Math.abs(sampleDelta))));
        }
        String status = error ? "Blocked" : active ? "Working" : "Standby";
        return new MachineProgressData(true, sample.label, sample.percent, remaining, active, error, status);
    }

    /** True if any read-only monitored quantity changed between two scans of the same block. */
    private static boolean stateChanged(ScannedBlockData a, ScannedBlockData b) {
        if (a.energy().stored() != b.energy().stored()) return true;
        if (fluidsChanged(a.fluids(), b.fluids())) return true;
        return inventoryChanged(a.inventory(), b.inventory());
    }

    private static boolean fluidsChanged(FluidSummary a, FluidSummary b) {
        if (a.tanks().size() != b.tanks().size()) return true;
        for (int i = 0; i < a.tanks().size(); i++) {
            if (a.tanks().get(i).amount() != b.tanks().get(i).amount()) return true;
        }
        return false;
    }

    private static boolean inventoryChanged(InventorySummary a, InventorySummary b) {
        if (a.usedSlots() != b.usedSlots() || a.topStacks().size() != b.topStacks().size()) return true;
        for (int i = 0; i < a.topStacks().size(); i++) {
            ItemEntry ea = a.topStacks().get(i);
            ItemEntry eb = b.topStacks().get(i);
            if (ea.count() != eb.count() || !ea.registryId().equals(eb.registryId())) return true;
        }
        return false;
    }

    private static ProgressSample sample(ScannedBlockData data) {
        // Energy is usually just one input to a machine's process, so headline the material throughput
        // (fluids, then items) first and fall back to the energy buffer only when nothing else is read.
        if (data.fluids().hasData()) {
            for (FluidEntry tank : data.fluids().tanks()) {
                if (tank.capacity() > 0) return new ProgressSample(tank.fluidName(), percent(tank.amount(), tank.capacity()));
            }
        }
        if (data.inventory().hasData()) return new ProgressSample("Inventory", data.inventory().fillPercent());
        if (data.energy().hasData()) return new ProgressSample("Energy", data.energy().fillPercent());
        return null;
    }

    private static int percent(long amount, long capacity) {
        if (capacity <= 0) return 0;
        return (int) Math.max(0L, Math.min(100L, Math.round(amount * 100.0 / capacity)));
    }

    private record ProgressSample(String label, int percent) {}

    private static BlockPos canonicalScanPos(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
            return pos.asLong() <= other.asLong() ? pos : other;
        }
        return pos;
    }

    /** Stable id derived from position so re-scans keep graph node links intact. */
    public static String blockId(BlockPos pos) {
        return "blk_" + pos.asLong();
    }

    private static InventorySummary readContainer(Container container) {
        int size = container.getContainerSize();
        int used = 0;
        // Tally by item registry id so identical items merge and the GUI can render real icons.
        Map<String, Integer> tally = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            used++;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            tally.merge(id, stack.getCount(), Integer::sum);
            names.putIfAbsent(id, stack.getHoverName().getString());
        }
        List<ItemEntry> top = tally.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_STACKS)
                .map(e -> new ItemEntry(names.get(e.getKey()), e.getKey(), e.getValue()))
                .toList();
        return new InventorySummary(used, size, top);
    }

    private static InventorySummary readItemCapability(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        ResourceHandler<ItemResource> handler = firstItemHandler(level, pos, state, be);
        if (handler == null) return InventorySummary.EMPTY;
        try {
            int slots = Math.max(0, handler.size());
            int used = 0;
            Map<String, Integer> tally = new LinkedHashMap<>();
            Map<String, String> names = new LinkedHashMap<>();
            for (int i = 0; i < slots; i++) {
                ItemResource resource = handler.getResource(i);
                long amount = handler.getAmountAsLong(i);
                if (resource == null || resource.isEmpty() || amount <= 0) continue;
                used++;
                String id = BuiltInRegistries.ITEM.getKey(resource.getItem()).toString();
                int count = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
                tally.merge(id, count, Integer::sum);
                names.putIfAbsent(id, resource.getHoverName().getString());
            }
            List<ItemEntry> top = tally.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(TOP_STACKS)
                    .map(e -> new ItemEntry(names.get(e.getKey()), e.getKey(), e.getValue()))
                    .toList();
            return new InventorySummary(used, slots, top);
        } catch (RuntimeException e) {
            return InventorySummary.EMPTY;
        }
    }

    private static FluidSummary readFluidCapability(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        ResourceHandler<FluidResource> handler = firstFluidHandler(level, pos, state, be);
        if (handler == null) return FluidSummary.EMPTY;
        try {
            List<FluidEntry> tanks = new ArrayList<>();
            for (int i = 0; i < handler.size(); i++) {
                FluidResource resource = handler.getResource(i);
                long amount = Math.max(0L, handler.getAmountAsLong(i));
                long capacity = Math.max(0L, handler.getCapacityAsLong(i, resource));
                if (amount <= 0 && capacity <= 0) continue;
                boolean empty = resource == null || resource.isEmpty();
                String name = empty ? "Empty" : resource.getHoverName().getString();
                String id = "";
                if (!empty) {
                    net.minecraft.resources.Identifier key = BuiltInRegistries.FLUID.getKey(resource.getFluid());
                    if (key != null) id = key.toString();
                }
                tanks.add(new FluidEntry(name, id, amount, capacity));
            }
            return tanks.isEmpty() ? FluidSummary.EMPTY : new FluidSummary(tanks);
        } catch (RuntimeException e) {
            return FluidSummary.EMPTY;
        }
    }

    private static EnergySummary readEnergyCapability(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        EnergyHandler handler = firstEnergyHandler(level, pos, state, be);
        if (handler == null) return EnergySummary.EMPTY;
        try {
            long capacity = Math.max(0L, handler.getCapacityAsLong());
            long stored = Math.max(0L, handler.getAmountAsLong());
            return capacity <= 0 ? EnergySummary.EMPTY : new EnergySummary(true, stored, capacity);
        } catch (RuntimeException e) {
            return EnergySummary.EMPTY;
        }
    }

    private static ResourceHandler<ItemResource> firstItemHandler(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        for (Direction direction : probeDirections()) {
            ResourceHandler<ItemResource> handler = null;
            try {
                handler = Capabilities.Item.BLOCK.getCapability(level, pos, state, be, direction);
            } catch (RuntimeException ignored) {
                // Some mod providers are side-sensitive or throw while their BE is mid-update; keep scanning.
            }
            if (handler != null) return handler;
        }
        return null;
    }

    private static ResourceHandler<FluidResource> firstFluidHandler(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        for (Direction direction : probeDirections()) {
            ResourceHandler<FluidResource> handler = null;
            try {
                handler = Capabilities.Fluid.BLOCK.getCapability(level, pos, state, be, direction);
            } catch (RuntimeException ignored) {
                // Capability probing must never make a modded block disappear from the scan.
            }
            if (handler != null) return handler;
        }
        return null;
    }

    private static EnergyHandler firstEnergyHandler(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        for (Direction direction : probeDirections()) {
            EnergyHandler handler = null;
            try {
                handler = Capabilities.Energy.BLOCK.getCapability(level, pos, state, be, direction);
            } catch (RuntimeException ignored) {
                // Keep the scanner read-only and fault-tolerant per block/side.
            }
            if (handler != null) return handler;
        }
        return null;
    }

    private static Direction[] probeDirections() {
        return PROBE_DIRECTIONS;
    }

    private static FurnaceSummary readFurnace(ServerLevel level, AbstractFurnaceBlockEntity furnaceBe) {
        ItemStack input = furnaceBe.getItem(0);
        ItemStack fuel = furnaceBe.getItem(1);
        ItemStack output = furnaceBe.getItem(2);

        // Real burn/cook progress via the furnace's ContainerData (0=lit,1=litTotal,2=cook,3=cookTotal).
        int[] data = readFurnaceData(furnaceBe);
        int litTime = data[0], litDuration = data[1], cookProgress = data[2], cookTotal = data[3];

        // Resolve the active smelting recipe result from the input item.
        ItemStack result = resolveRecipeResult(level, furnaceBe, input);
        String resultName = result.isEmpty() ? "" : result.getHoverName().getString();
        String resultId = result.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(result.getItem()).toString();

        String status;
        boolean cooking = cookProgress > 0 || litTime > 0;
        boolean outputFull = !output.isEmpty() && output.getCount() >= output.getMaxStackSize();
        if (cooking) {
            status = "Running";
        } else if (input.isEmpty()) {
            status = "Standby";          // nothing queued on the input — idle, not a problem
        } else if (result.isEmpty()) {
            status = "No Recipe";        // has an input item, but it can't be smelted here
        } else if (outputFull) {
            status = "Output Full";
        } else if (fuel.isEmpty() && litTime <= 0) {
            status = "No Fuel";
        } else {
            status = "Idle";
        }

        return new FurnaceSummary(true,
                itemName(input), itemName(fuel), itemName(output),
                litTime, litDuration, cookProgress, cookTotal, status,
                itemId(input), resultName, resultId);
    }

    /** Reads {lit, litTotal, cook, cookTotal} from the furnace's protected {@code dataAccess}. */
    private static int[] readFurnaceData(AbstractFurnaceBlockEntity furnaceBe) {
        try {
            java.lang.reflect.Field f = AbstractFurnaceBlockEntity.class.getDeclaredField("dataAccess");
            f.setAccessible(true);
            net.minecraft.world.inventory.ContainerData d = (net.minecraft.world.inventory.ContainerData) f.get(furnaceBe);
            return new int[] { d.get(0), d.get(1), d.get(2), d.get(3) };
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new int[] { 0, 0, 0, 0 };
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack resolveRecipeResult(ServerLevel level, AbstractFurnaceBlockEntity furnaceBe, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type;
        if (furnaceBe instanceof net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity) {
            type = net.minecraft.world.item.crafting.RecipeType.BLASTING;
        } else if (furnaceBe instanceof net.minecraft.world.level.block.entity.SmokerBlockEntity) {
            type = net.minecraft.world.item.crafting.RecipeType.SMOKING;
        } else {
            type = net.minecraft.world.item.crafting.RecipeType.SMELTING;
        }
        try {
            var inputHolder = new net.minecraft.world.item.crafting.SingleRecipeInput(input);
            var recipe = level.recipeAccess().getRecipeFor(
                    (net.minecraft.world.item.crafting.RecipeType<net.minecraft.world.item.crafting.AbstractCookingRecipe>) type,
                    inputHolder, level);
            if (recipe.isEmpty()) return ItemStack.EMPTY;
            return recipe.get().value().assemble(inputHolder);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private static String itemName(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
