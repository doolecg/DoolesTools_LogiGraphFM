package net.doole.doolestools.logistics;

import net.doole.doolestools.blockentity.NetworkBatteryBlockEntity;
import net.doole.doolestools.blockentity.NetworkGeneratorBlockEntity;
import net.doole.doolestools.blockentity.NetworkModemBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.network.NetworkNodeIndex;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NetworkPowerCalculator {
    // fallback if the config isnt loaded yet (shouldnt happen in normal play)
    private static final int MAX_COMPONENT_STEPS_FALLBACK = 256;

    private static int maxSteps() {
        return ModServerConfig.MAX_WIRE_TRAVERSAL_STEPS.get();
    }

    private NetworkPowerCalculator() {}

    /** Power snapshot plus the wired batteries the caller (server tick) may charge/discharge. */
    public record Result(NetworkPowerData data, List<NetworkBatteryBlockEntity> batteries) {}

    public static NetworkPowerData calculate(ServerLevel level, BlockPos computerPos, List<ScannedBlockData> scan, LogisticsGraphData graph, String networkId) {
        return calculateResult(level, computerPos, scan, graph, networkId).data();
    }

    public static Result calculateResult(ServerLevel level, BlockPos computerPos, List<ScannedBlockData> scan, LogisticsGraphData graph, String networkId) {
        Counts counts = countWiredComponents(level, computerPos);
        List<RelayNode> relays = discoverReachableRelays(level, computerPos, networkId);
        int wirelessRouters = countStandaloneWirelessRouters(level, computerPos, relays);
        counts.endpoints += wirelessRouters;
        counts.routerEndpoints += wirelessRouters;
        counts.endpoints += relays.size();
        counts.relayEndpoints += relays.size();
        int deviceCount = scan == null ? 0 : scan.size();
        int routeCount = 0;
        int routeCost = 0;
        Map<String, BlockPos> positions = scannedPositions(scan);
        if (graph != null && !graph.isEmpty()) {
            for (GraphLinkData link : graph.activeCanvas().links()) {
                routeCount++;
                routeCost += routeCost(level, graph, positions, link);
            }
        }

        int batteryCount = counts.batteries.size();
        int batteryCost = batteryCount * ModServerConfig.BATTERY_POWER_COST.get();
        long batteryStored = 0L, batteryCapacity = 0L;
        for (NetworkBatteryBlockEntity battery : counts.batteries) {
            batteryStored += battery.energy();
            batteryCapacity += battery.capacity();
        }

        int computerCost = ModServerConfig.COMPUTER_POWER_COST.get();
        int endpointCost = counts.routerEndpoints * ModServerConfig.WIRELESS_ROUTER_POWER_COST.get()
                + counts.modemEndpoints * ModServerConfig.MODEM_POWER_COST.get()
                + relayPowerCost(relays);
        int wireCost = counts.wires * ModServerConfig.WIRE_SEGMENT_POWER_COST.get();
        int deviceCost = deviceCount * ModServerConfig.VISIBLE_DEVICE_POWER_COST.get();
        int demand = computerCost + endpointCost + wireCost + deviceCost + routeCost + batteryCost;
        // Power comes ONLY from real FE sources (Network Generator, battery, other mods' energy).
        // There is no virtual/demo supply: no source means 0 FE/t and automation stops. All generators
        // discovered on the wired network are summed, plus anything directly adjacent to the computer.
        int supply = realSupplyCentiFe(level, computerPos, counts.generators);
        NetworkPowerData data = new NetworkPowerData(supply, demand,
                computerCost, endpointCost, wireCost, deviceCost, routeCost,
                counts.endpoints, counts.wires, deviceCount, routeCount,
                batteryCost, batteryStored, batteryCapacity, batteryCount, counts.generators.size());
        return new Result(data, List.copyOf(counts.batteries));
    }

    /**
     * Pull this tick's power from adjacent energy sources and report how satisfied the network is, in
     * [0,1]: 1 = fully powered, a fraction = brownout (run degraded/slow), 0 = no power (stop). Unlike a
     * hard all-or-nothing gate, this commits whatever it can pull so a partially-fed network keeps
     * limping along rather than freezing. With no real source attached, supply is 0, so this reports 0
     * (starved) and automation stops.
     */
    public static float consumePower(ServerLevel level, BlockPos computerPos, NetworkPowerData power) {
        if (power == null || power.demandCentiFe() <= 0) return 1f;
        if (!hasRealEnergySource(level, computerPos)) return power.satisfaction();
        int interval = Math.max(1, ModServerConfig.EASY_FACTORY_TICK_INTERVAL.get());
        int needed = Math.max(1, (int) Math.ceil(power.demandCentiFe() * interval / 100.0));
        int pulled = 0;
        try (Transaction tx = Transaction.openRoot()) {
            for (Direction direction : Direction.values()) {
                EnergyHandler handler = energyHandler(level, computerPos.relative(direction), direction.getOpposite());
                if (handler == null) continue;
                int extracted = handler.extract(needed - pulled, tx);
                if (extracted > 0) pulled += extracted;
                if (pulled >= needed) break;
            }
            tx.commit();
        } catch (RuntimeException ignored) {
        }
        return Math.min(1f, pulled / (float) needed);
    }

    private static int realSupplyCentiFe(ServerLevel level, BlockPos computerPos, List<NetworkGeneratorBlockEntity> wiredGenerators) {
        int interval = Math.max(1, ModServerConfig.EASY_FACTORY_TICK_INTERVAL.get());
        long extractable = 0L;
        // Sources directly adjacent to the computer (any energy provider, incl. other mods).
        for (Direction direction : Direction.values()) {
            EnergyHandler handler = energyHandler(level, computerPos.relative(direction), direction.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                extractable += Math.max(0, handler.extract(Integer.MAX_VALUE, tx));
            } catch (RuntimeException ignored) {
            }
            if (extractable > Integer.MAX_VALUE / 100L) break;
        }
        // All Network Generators reachable over the wired network are summed in.
        for (NetworkGeneratorBlockEntity generator : wiredGenerators) {
            try (Transaction tx = Transaction.openRoot()) {
                extractable += Math.max(0, generator.energyHandler().extract(Integer.MAX_VALUE, tx));
            } catch (RuntimeException ignored) {
            }
            if (extractable > Integer.MAX_VALUE / 100L) break;
        }
        return (int) Math.min(Integer.MAX_VALUE, extractable * 100L / interval);
    }

    private static boolean hasRealEnergySource(ServerLevel level, BlockPos computerPos) {
        for (Direction direction : Direction.values()) {
            EnergyHandler handler = energyHandler(level, computerPos.relative(direction), direction.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                if (handler.extract(1, tx) > 0) return true;
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private static EnergyHandler energyHandler(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        try {
            return Capabilities.Energy.BLOCK.getCapability(level, pos, state, be, side);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int routeCost(ServerLevel level, LogisticsGraphData graph, Map<String, BlockPos> positions, GraphLinkData link) {
        int base = switch (link.type()) {
            case ITEMS -> ModServerConfig.ITEM_ROUTE_POWER_COST.get();
            case FLUIDS -> ModServerConfig.FLUID_ROUTE_POWER_COST.get();
            case ENERGY -> ModServerConfig.ENERGY_ROUTE_POWER_COST.get();
            case MANUAL -> 0;
        };
        if (!isWirelessRoute(level, graph, positions, link)) return base;
        int efficiency = Math.max(endpointUpgradeCount(level, targetPosOf(graph.findNode(link.sourceNodeId()), positions), "efficiency"),
                endpointUpgradeCount(level, targetPosOf(graph.findNode(link.targetNodeId()), positions), "efficiency"));
        return base + WirelessNetworkPolicy.wirelessRouteSurcharge(ModServerConfig.WIRELESS_ROUTE_POWER_SURCHARGE.get(), efficiency);
    }

    private static boolean isWirelessRoute(ServerLevel level, LogisticsGraphData graph, Map<String, BlockPos> positions, GraphLinkData link) {
        return hasWirelessEndpoint(level, targetPosOf(graph.findNode(link.sourceNodeId()), positions))
                || hasWirelessEndpoint(level, targetPosOf(graph.findNode(link.targetNodeId()), positions));
    }

    private static Map<String, BlockPos> scannedPositions(List<ScannedBlockData> scan) {
        Map<String, BlockPos> positions = new HashMap<>();
        if (scan == null) return positions;
        for (ScannedBlockData scanned : scan) positions.put(scanned.id(), scanned.pos());
        return positions;
    }

    private static BlockPos targetPosOf(GraphNodeData node, Map<String, BlockPos> positions) {
        if (node == null || node.scannedBlockId() == null) return null;
        BlockPos scanned = positions.get(node.scannedBlockId());
        if (scanned != null) return scanned;
        if (!node.scannedBlockId().startsWith("blk_")) return null;
        try {
            return BlockPos.of(Long.parseLong(node.scannedBlockId().substring(4)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Counts countWiredComponents(ServerLevel level, BlockPos computerPos) {
        Counts counts = new Counts();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> sources = new HashSet<>();
        // generators and batteries can sit directly against the computer, not just behind a modem
        // i'll add a config to raise MAX_COMPONENT_STEPS later if servers complain about huge wire grids
        collectSources(level, computerPos, counts, sources);
        for (Direction direction : Direction.values()) {
            BlockPos pos = computerPos.relative(direction);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkModemBlockEntity modem && modem.attachedPos().equals(computerPos)) {
                queue.add(pos);
            } else if (be instanceof NetworkWireBlockEntity wire && wire.hasModem() && wire.attachedPos().equals(computerPos)) {
                queue.add(pos);
            }
        }

        int limit = maxSteps();
        while (!queue.isEmpty() && seen.size() < limit) {
            BlockPos pos = queue.removeFirst();
            if (!seen.add(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkWireBlockEntity wire) {
                counts.wires++;
                if (wire.hasEndpoint()) {
                    counts.endpoints++;
                    if (wire.hasRouter()) counts.routerEndpoints++; else counts.modemEndpoints++;
                }
                collectSources(level, pos, counts, sources);
                for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
            } else if (be instanceof NetworkModemBlockEntity) {
                counts.endpoints++;
                counts.modemEndpoints++;
                collectSources(level, pos, counts, sources);
                for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
            }
        }
        // warn once if we bailed early - raise maxWireTraversalSteps in server config if this fires often
        if (!queue.isEmpty()) {
            net.doole.doolestools.DoolesTools.LOGGER.debug(
                "wire traversal capped at {} steps near {} - raise maxWireTraversalSteps in config if you see this a lot", limit, computerPos);
        }
        return counts;
    }

    /** Records any Network Generators / Batteries adjacent to {@code center}, deduped by position. */
    private static void collectSources(ServerLevel level, BlockPos center, Counts counts, Set<BlockPos> sources) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = center.relative(direction);
            if (!sources.add(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkGeneratorBlockEntity generator) {
                counts.generators.add(generator);
            } else if (be instanceof NetworkBatteryBlockEntity battery) {
                counts.batteries.add(battery);
            }
        }
    }

    private static int countStandaloneWirelessRouters(ServerLevel level, BlockPos computerPos, List<RelayNode> relays) {
        Set<BlockPos> found = new HashSet<>();
        countStandaloneWirelessRoutersAround(level, computerPos, computerPos, relays, found);
        for (RelayNode relay : relays) countStandaloneWirelessRoutersAround(level, computerPos, relay.pos(), relays, found);
        return found.size();
    }

    private static void countStandaloneWirelessRoutersAround(ServerLevel level, BlockPos computerPos, BlockPos center, List<RelayNode> relays, Set<BlockPos> found) {
        int radius = ModServerConfig.WIRELESS_MAX_RANGE.get();
        long radiusSqr = (long) radius * radius;
        NetworkNodeIndex.forEachRouter(level, router -> {
            BlockPos pos = router.getBlockPos();
            if (center.distSqr(pos) <= radiusSqr && wirelessReachable(computerPos, pos, router.upgradeCount("range"), relays)) found.add(pos);
        });
    }

    private static List<RelayNode> discoverReachableRelays(ServerLevel level, BlockPos computerPos, String networkId) {
        List<RelayNode> reachable = new java.util.ArrayList<>();
        ArrayDeque<RelayNode> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        for (RelayNode relay : discoverRelaysAround(level, computerPos, networkId)) {
            if (wirelessInRange(computerPos, relay.pos(), relay.rangeUpgrades()) && seen.add(relay.pos())) queue.add(relay);
        }
        int steps = 0;
        int maxSteps = ModServerConfig.MAX_RELAY_TRAVERSAL.get();
        while (!queue.isEmpty() && steps++ < maxSteps) {
            RelayNode current = queue.removeFirst();
            reachable.add(current);
            for (RelayNode relay : discoverRelaysAround(level, current.pos(), networkId)) {
                if (seen.contains(relay.pos())) continue;
                if (wirelessInRange(current.pos(), relay.pos(), relay.rangeUpgrades()) && seen.add(relay.pos())) queue.add(relay);
            }
        }
        return reachable;
    }

    private static List<RelayNode> discoverRelaysAround(ServerLevel level, BlockPos center, String networkId) {
        List<RelayNode> relays = new java.util.ArrayList<>();
        int radius = ModServerConfig.WIRELESS_MAX_RANGE.get();
        long radiusSqr = (long) radius * radius;
        // Query the persistent node index rather than walking chunks: O(relays) instead of
        // O(chunks * block entities). The distance/network filtering is unchanged.
        NetworkNodeIndex.forEachRelay(level, relay -> {
            BlockPos pos = relay.getBlockPos();
            if (center.distSqr(pos) <= radiusSqr && relayNetworkMatches(relay.networkId(), networkId)) {
                relays.add(new RelayNode(pos, relay.upgradeCount("range"), relay.upgradeCount("efficiency")));
            }
        });
        return relays;
    }

    private static boolean wirelessReachable(BlockPos computerPos, BlockPos endpointPos, int endpointRangeUpgrades, List<RelayNode> relays) {
        if (wirelessInRange(computerPos, endpointPos, endpointRangeUpgrades)) return true;
        for (RelayNode relay : relays) {
            if (wirelessInRange(relay.pos(), endpointPos, endpointRangeUpgrades)) return true;
        }
        return false;
    }

    private static int relayPowerCost(List<RelayNode> relays) {
        int total = 0;
        for (RelayNode relay : relays) {
            total += WirelessNetworkPolicy.wirelessRouteSurcharge(ModServerConfig.NETWORK_RELAY_POWER_COST.get(), relay.efficiencyUpgrades());
        }
        return total;
    }

    private static boolean relayNetworkMatches(String relayNetworkId, String computerNetworkId) {
        return relayNetworkId == null || relayNetworkId.isBlank() || (computerNetworkId != null && relayNetworkId.equals(computerNetworkId));
    }

    private static boolean hasWirelessEndpoint(ServerLevel level, BlockPos attachedPos) {
        if (attachedPos == null) return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = attachedPos.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof NetworkEndpointBlockEntity router && router.attachedPos().equals(attachedPos)) return true;
            if (be instanceof NetworkWireBlockEntity wire && wire.hasRouter() && wire.attachedPos().equals(attachedPos)) return true;
        }
        return false;
    }

    private static int endpointUpgradeCount(ServerLevel level, BlockPos attachedPos, String type) {
        if (attachedPos == null) return 0;
        int total = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = attachedPos.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof NetworkEndpointBlockEntity router && router.attachedPos().equals(attachedPos)) {
                total += router.upgradeCount(type);
            } else if (be instanceof NetworkWireBlockEntity wire && wire.hasEndpoint() && wire.attachedPos().equals(attachedPos)) {
                total += wire.upgradeCount(type);
            }
        }
        return total;
    }

    private static boolean wirelessInRange(BlockPos computerPos, BlockPos endpointPos, int rangeUpgrades) {
        return WirelessNetworkPolicy.inRange(
                ModServerConfig.WIRELESS_BASE_RANGE.get(),
                ModServerConfig.WIRELESS_RANGE_UPGRADE_BLOCKS.get(),
                rangeUpgrades,
                ModServerConfig.WIRELESS_MAX_RANGE.get(),
                distanceSqr(computerPos, endpointPos));
    }

    private static long distanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record RelayNode(BlockPos pos, int rangeUpgrades, int efficiencyUpgrades) {}

    private static final class Counts {
        int wires;
        int endpoints;
        int routerEndpoints;
        int modemEndpoints;
        int relayEndpoints;
        final List<NetworkGeneratorBlockEntity> generators = new java.util.ArrayList<>();
        final List<NetworkBatteryBlockEntity> batteries = new java.util.ArrayList<>();
    }
}
