package net.doole.doolestools.logistics.easyfactory;

import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.LinkType;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.WirelessNetworkPolicy;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNetworkAccess;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkModemBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class EasyFactoryManager {
    private record ItemTargetSlot(ResourceHandler<ItemResource> handler, int slot) {}

    private EasyFactoryManager() {}

    public static java.util.Set<String> tick(LogisticsGraphData graph, ServerLevel level, java.util.List<ScannedBlockData> scan) {
        return tick(graph, level, scan, 1f);
    }

    public static java.util.Set<String> tick(LogisticsGraphData graph, ServerLevel level, java.util.List<ScannedBlockData> scan, float satisfaction) {
        return tickWithCounts(graph, level, scan, satisfaction, null).keySet();
    }

    public static java.util.Map<String, Integer> tickWithCounts(LogisticsGraphData graph, ServerLevel level, java.util.List<ScannedBlockData> scan, float satisfaction) {
        return tickWithCounts(graph, level, scan, satisfaction, null);
    }

    /**
     * @param satisfaction  power satisfaction in [0,1]; under 1 the per-tick route budget is scaled
     *                      down so an underpowered network visibly transports slower (brownout).
     * @param linkBirthTimes mutable map of linkId → game-time the link was first processed; used to
     *                       compute the 10-second speed ramp-up. Pass null to skip ramp (instant full speed).
     *
     * NOTE: this runs every computer tick on the server. keep it lean - getBlockEntity calls add up fast
     * on big servers with lots of computers. the outbound index below is the main thing keeping this cheap.
     */
    public static java.util.Map<String, Integer> tickWithCounts(LogisticsGraphData graph, ServerLevel level,
            java.util.List<ScannedBlockData> scan, float satisfaction,
            java.util.Map<String, Long> linkBirthTimes) {
        java.util.Map<String, Integer> movedByLink = new java.util.LinkedHashMap<>();
        if (!ModServerConfig.ENABLE_LFM_TRANSPORT.get() || graph.isEmpty()) return movedByLink;
        java.util.Map<String, BlockPos> positions = scannedPositions(scan);
        java.util.Map<String, ScannedBlockData> scannedById = scannedById(scan);
        int processed = 0;
        int routeBudget = routeBudget(level, graph, positions);
        if (satisfaction < 1f) routeBudget = Math.max(1, Math.round(routeBudget * Math.max(0f, satisfaction)));
        // build the outbound index once so DFS routes dont scan all links on every node visit
        java.util.Map<String, java.util.List<GraphLinkData>> outboundIndex = buildOutboundIndex(graph);
        java.util.List<GraphLinkData> orderedLinks = new java.util.ArrayList<>(graph.activeCanvas().links());
        orderedLinks.sort((a, b) -> Integer.compare(routePriority(level, graph, b, scannedById), routePriority(level, graph, a, scannedById)));
        long gameTime = level.getGameTime();
        for (GraphLinkData link : orderedLinks) {
            if (processed >= routeBudget) break;
            if (linkBirthTimes != null) linkBirthTimes.putIfAbsent(link.linkId(), gameTime);
            GraphNodeData sourceNode = graph.findNode(link.sourceNodeId());
            GraphNodeData targetNode = graph.findNode(link.targetNodeId());
            BlockPos sourcePos = targetPosOf(sourceNode, positions);
            BlockPos targetPos = targetPosOf(targetNode, positions);
            float rampFraction = computeRampFraction(link.linkId(), gameTime, linkBirthTimes, level, sourcePos, targetPos);
            if (sourcePos != null && targetNode != null && isRoutingNode(targetNode.type())) {
                if (!level.hasChunkAt(sourcePos)) continue;
                if (!switchboardAllows(level, link, sourceNode, targetNode, scannedById)) continue;
                if (routeThroughNodesCount(graph, level, link, sourceNode, sourcePos, positions, movedByLink, outboundIndex, rampFraction) > 0) processed++;
                continue;
            }
            if (sourcePos == null || targetPos == null) continue;
            if (!level.hasChunkAt(sourcePos) || !level.hasChunkAt(targetPos)) continue;
            if (!switchboardAllows(level, link, sourceNode, targetNode, scannedById)) continue;
            int moved = transferCount(link, level, sourceNode, targetNode, sourcePos, targetPos, rampFraction);
            if (moved > 0) {
                movedByLink.merge(link.linkId(), moved, Integer::sum);
                processed++;
            }
        }
        return movedByLink;
    }

    /**
     * Fraction of full item throughput to apply based on how long this link has been running.
     * Ramp duration is 200 ticks (10 s) at 0 efficiency, reduced by 50 ticks per efficiency upgrade,
     * reaching 0 (instant) at 4 efficiency upgrades.
     */
    private static float computeRampFraction(String linkId, long gameTime,
            java.util.Map<String, Long> linkBirthTimes, ServerLevel level,
            BlockPos sourcePos, BlockPos targetPos) {
        if (linkBirthTimes == null) return 1f;
        long birthTime = linkBirthTimes.getOrDefault(linkId, gameTime);
        long warmupTicks = Math.min(200L, gameTime - birthTime);
        int efficiency = 0;
        if (sourcePos != null) efficiency = Math.max(efficiency, endpointUpgradeCount(level, sourcePos, UpgradeType.EFFICIENCY));
        if (targetPos != null) efficiency = Math.max(efficiency, endpointUpgradeCount(level, targetPos, UpgradeType.EFFICIENCY));
        int rampTicks = 200 * (4 - Math.min(4, Math.max(0, efficiency))) / 4; // 200, 150, 100, 50, 0
        if (rampTicks <= 0) return 1f;
        return Math.min(1f, warmupTicks / (float) rampTicks);
    }

    private static java.util.Map<String, BlockPos> scannedPositions(java.util.List<ScannedBlockData> scan) {
        java.util.Map<String, BlockPos> positions = new java.util.HashMap<>();
        if (scan == null) return positions;
        for (ScannedBlockData scanned : scan) positions.put(scanned.id(), scanned.pos());
        return positions;
    }

    private static java.util.Map<String, ScannedBlockData> scannedById(java.util.List<ScannedBlockData> scan) {
        java.util.Map<String, ScannedBlockData> byId = new java.util.HashMap<>();
        if (scan == null) return byId;
        for (ScannedBlockData scanned : scan) byId.put(scanned.id(), scanned);
        return byId;
    }

    private static boolean switchboardAllows(ServerLevel level, GraphLinkData link, GraphNodeData sourceNode, GraphNodeData targetNode,
                                             java.util.Map<String, ScannedBlockData> scannedById) {
        ScannedBlockData source = scannedFor(sourceNode, scannedById);
        ScannedBlockData target = scannedFor(targetNode, scannedById);
        if (source == null || target == null) return true;
        return SwitchboardNetworkAccess.canRoute(level, source.networkId(), target.networkId(), link.type());
    }

    private static int routePriority(ServerLevel level, LogisticsGraphData graph, GraphLinkData link,
                                     java.util.Map<String, ScannedBlockData> scannedById) {
        int base = filterPriority(graph, link);
        GraphNodeData sourceNode = graph.findNode(link.sourceNodeId());
        GraphNodeData targetNode = graph.findNode(link.targetNodeId());
        ScannedBlockData source = scannedFor(sourceNode, scannedById);
        ScannedBlockData target = scannedFor(targetNode, scannedById);
        if (source == null || target == null) return base;
        return base + SwitchboardNetworkAccess.priority(level, source.networkId(), target.networkId(), link.type());
    }

    private static ScannedBlockData scannedFor(GraphNodeData node, java.util.Map<String, ScannedBlockData> scannedById) {
        if (node == null || node.scannedBlockId() == null) return null;
        return scannedById.get(node.scannedBlockId());
    }

    static BlockPos targetPosOf(GraphNodeData node, java.util.Map<String, BlockPos> scannedPositions) {
        if (node == null || node.scannedBlockId() == null) return null;
        BlockPos scanned = scannedPositions.get(node.scannedBlockId());
        if (scanned != null) return scanned;
        if (!node.scannedBlockId().startsWith("blk_")) return null;
        try {
            return BlockPos.of(Long.parseLong(node.scannedBlockId().substring(4)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int transferCount(GraphLinkData link, ServerLevel level, GraphNodeData sourceNode, GraphNodeData targetNode, BlockPos sourcePos, BlockPos targetPos, float rampFraction) {
        return switch (link.type()) {
            case ITEMS -> ModServerConfig.ENABLE_ITEM_ROUTES.get() ? transferItemsCount(level, link, sourceNode, targetNode, sourcePos, targetPos, r -> true, Integer.MAX_VALUE, rampFraction) : 0;
            case FLUIDS -> ModServerConfig.ENABLE_FLUID_ROUTES.get() ? transferFluidsCount(level, sourcePos, targetPos) : 0;
            case ENERGY -> ModServerConfig.ENABLE_ENERGY_ROUTES.get() ? transferEnergyCount(level, sourcePos, targetPos) : 0;
            case MANUAL -> 0;
        };
    }

    // ---- Multi-hop routing through Filter / Splitter / Combine / Channel nodes ----
    //
    // Items keep flowing from the original real source; routing nodes only shape WHERE they go and WHICH
    // ones qualify. We DFS forward from the source, carrying the colour channel a Channel node stamps and
    // an accumulating list of item gates. A Filter node branches: its "out" port carries the matched
    // items, its "reject" port carries the rest. Each real destination reached becomes a route we move to.

    /** An item test contributed by a Filter on the path. {@code negate} flips it for the reject branch. */
    private record Gate(FilterSettings settings, boolean negate) {
        boolean passes(ItemResource resource) {
            return settings.allows(resource) != negate;
        }
    }

    private record ResolvedRoute(BlockPos targetPos, GraphNodeData targetNode, GraphLinkData finalLink,
                                 java.util.List<Gate> gates, java.util.List<String> linkPath) {}

    private static int routeThroughNodesCount(LogisticsGraphData graph, ServerLevel level, GraphLinkData inbound,
            GraphNodeData sourceNode, BlockPos sourcePos, java.util.Map<String, BlockPos> positions,
            java.util.Map<String, Integer> movedByLink, java.util.Map<String, java.util.List<GraphLinkData>> outboundIndex,
            float rampFraction) {
        if (inbound.type() != LinkType.ITEMS) return 0;
        GraphNodeData first = graph.findNode(inbound.targetNodeId());
        if (first == null || !isRoutingNode(first.type())) return 0;
        java.util.List<ResolvedRoute> routes = new java.util.ArrayList<>();
        java.util.List<String> path = new java.util.ArrayList<>();
        path.add(inbound.linkId());
        enterNode(graph, first, "none", java.util.List.of(), path, positions, routes, new java.util.HashSet<>(), outboundIndex);
        if (routes.isEmpty()) return 0;
        // rotate by game time so no single destination hogs the per-tick budget
        java.util.Collections.rotate(routes, (int) Math.floorMod(level.getGameTime(), routes.size()));
        int movedTotal = 0;
        for (ResolvedRoute r : routes) {
            if (!level.hasChunkAt(r.targetPos())) continue;
            GraphLinkData route = new GraphLinkData(inbound.linkId(), inbound.sourceNodeId(), inbound.sourcePortId(),
                    r.finalLink().targetNodeId(), r.finalLink().targetPortId(), r.finalLink().label(),
                    LinkType.ITEMS, r.finalLink().sideOverride());
            int moved = transferItemsCount(level, route, sourceNode, r.targetNode(), sourcePos, r.targetPos(),
                    resource -> gatesAllow(r.gates(), resource), gateLimit(r.gates()), rampFraction);
            if (moved > 0) {
                for (String linkId : r.linkPath()) movedByLink.merge(linkId, moved, Integer::sum);
                movedTotal += moved;
            }
        }
        return movedTotal;
    }

    /** Walk a routing node's outputs, updating the channel (Channel) and branching matched/reject (Filter). */
    private static void enterNode(LogisticsGraphData graph, GraphNodeData node, String channel, java.util.List<Gate> gates,
            java.util.List<String> path, java.util.Map<String, BlockPos> positions,
            java.util.List<ResolvedRoute> routes, java.util.Set<String> visiting,
            java.util.Map<String, java.util.List<GraphLinkData>> outboundIndex) {
        if (routes.size() > 256 || !visiting.add(node.nodeId())) return;
        if (node.type() == NodeType.FILTER) {
            FilterSettings fs = FilterSettings.parse(node.notes());
            boolean channelMatch = fs.channel().equals("none") || fs.channel().equals(channel);
            for (GraphLinkData out : outboundIndex.getOrDefault(node.nodeId(), java.util.List.of())) {
                boolean reject = "reject".equals(out.sourcePortId());
                java.util.List<Gate> branch;
                if (reject) {
                    branch = channelMatch ? append(gates, new Gate(fs, true)) : gates;
                } else {
                    if (!channelMatch) continue;
                    branch = append(gates, new Gate(fs, false));
                }
                descend(graph, out, channel, branch, path, positions, routes, visiting, outboundIndex);
            }
        } else {
            String nextChannel = node.type() == NodeType.CHANNEL ? FilterSettings.parse(node.notes()).channel() : channel;
            for (GraphLinkData out : outboundIndex.getOrDefault(node.nodeId(), java.util.List.of())) {
                descend(graph, out, nextChannel, gates, path, positions, routes, visiting, outboundIndex);
            }
        }
        visiting.remove(node.nodeId());
    }

    private static void descend(LogisticsGraphData graph, GraphLinkData link, String channel, java.util.List<Gate> gates,
            java.util.List<String> path, java.util.Map<String, BlockPos> positions,
            java.util.List<ResolvedRoute> routes, java.util.Set<String> visiting,
            java.util.Map<String, java.util.List<GraphLinkData>> outboundIndex) {
        if (link.type() != LinkType.ITEMS) return;
        GraphNodeData target = graph.findNode(link.targetNodeId());
        if (target == null) return;
        java.util.List<String> newPath = append(path, link.linkId());
        BlockPos pos = targetPosOf(target, positions);
        if (pos != null) {
            routes.add(new ResolvedRoute(pos, target, link, gates, newPath));
            return;
        }
        if (isRoutingNode(target.type())) {
            enterNode(graph, target, channel, gates, newPath, positions, routes, visiting, outboundIndex);
        }
    }

    // build once per tick so the DFS never scans the full link list repeatedly
    private static java.util.Map<String, java.util.List<GraphLinkData>> buildOutboundIndex(LogisticsGraphData graph) {
        java.util.Map<String, java.util.List<GraphLinkData>> index = new java.util.HashMap<>();
        for (GraphLinkData link : graph.activeCanvas().links()) {
            if (link.type() != LinkType.ITEMS) continue;
            index.computeIfAbsent(link.sourceNodeId(), k -> new java.util.ArrayList<>()).add(link);
        }
        return index;
    }

    private static <T> java.util.List<T> append(java.util.List<T> base, T value) {
        java.util.List<T> copy = new java.util.ArrayList<>(base);
        copy.add(value);
        return copy;
    }

    private static boolean gatesAllow(java.util.List<Gate> gates, ItemResource resource) {
        for (Gate gate : gates) if (!gate.passes(resource)) return false;
        return true;
    }

    private static int gateLimit(java.util.List<Gate> gates) {
        int limit = Integer.MAX_VALUE;
        for (Gate gate : gates) limit = Math.min(limit, gate.settings().limit());
        return limit;
    }

    private static ResourceHandler<ItemResource> itemHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, state, level, pos, false);
            if (chestContainer != null) return VanillaContainerWrapper.of(chestContainer);
        }
        if (be instanceof WorldlyContainer container && side != null) return new WorldlyContainerWrapper(container, side);
        if (be instanceof Container container) return VanillaContainerWrapper.of(container);
        return Capabilities.Item.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static ResourceHandler<FluidResource> fluidHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return Capabilities.Fluid.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static EnergyHandler energyHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return Capabilities.Energy.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static Direction sideFrom(BlockPos source, BlockPos target) {
        int dx = Integer.compare(target.getX(), source.getX());
        int dy = Integer.compare(target.getY(), source.getY());
        int dz = Integer.compare(target.getZ(), source.getZ());
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) return null;
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dy > 0) return Direction.UP;
        if (dy < 0) return Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int transferItemsCount(ServerLevel level, GraphLinkData link, GraphNodeData sourceNode, GraphNodeData targetNode, BlockPos sourcePos, BlockPos targetPos,
                                         java.util.function.Predicate<ItemResource> allow, int filterLimit, float rampFraction) {
        Direction physicalSourceSide = sideFrom(sourcePos, targetPos);
        Direction physicalTargetSide = physicalSourceSide == null ? null : physicalSourceSide.getOpposite();
        Direction overrideSide = sideOverride(link.sideOverride());
        boolean sourceMachine = sourceNode != null && sourceNode.type() == NodeType.MACHINE;
        boolean targetMachine = targetNode != null && targetNode.type() == NodeType.MACHINE;
        Direction sourceSide = overrideSide != null && sourceMachine ? overrideSide : null;
        Direction targetSide = overrideSide != null && targetMachine ? overrideSide : null;
        ResourceHandler<ItemResource> source = itemHandler(level, sourcePos, null);
        ResourceHandler<ItemResource> target = itemHandler(level, targetPos, null);
        if (sourceSide != null) source = itemHandler(level, sourcePos, sourceSide);
        if (targetSide != null) target = itemHandler(level, targetPos, targetSide);
        if (source == null && physicalSourceSide != null) {
            sourceSide = physicalSourceSide;
            source = itemHandler(level, sourcePos, sourceSide);
        }
        if (target == null && physicalTargetSide != null) {
            targetSide = physicalTargetSide;
            target = itemHandler(level, targetPos, targetSide);
        }
        if (source == null || target == null) return 0;

        int rawLimit = itemLimit(level, sourcePos, targetPos);
        int rampedLimit = rampFraction >= 1f ? rawLimit : Math.max(1, (int)(rawLimit * rampFraction));
        int max = Math.min(rampedLimit, filterLimit);
        for (int slot = 0; slot < source.size(); slot++) {
            ItemResource resource = source.getResource(slot);
            long available = source.getAmountAsLong(slot);
            if (resource.isEmpty() || available <= 0) continue;
            if (!allow.test(resource)) continue;
            if (!canExtractItemFromSlot(level, link, sourceNode, sourcePos, sourceSide, source, slot, resource)) continue;
            int amount = (int) Math.min(max, available);
            for (ItemTargetSlot targetSlot : targetSlots(level, link, targetPos, targetSide, target, resource, overrideSide != null && targetMachine)) {
                // Probe how much the target slot can accept, then do a single atomic move.
                // This replaces the old O(amount) retry loop (which opened up to 64 transactions
                // per slot pair on a full stack) with at most 2 transactions total.
                int insertable;
                try (Transaction probe = Transaction.openRoot()) {
                    insertable = (int) Math.min(amount, targetSlot.handler().insert(targetSlot.slot(), resource, amount, probe));
                }
                if (insertable <= 0) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    long extracted = source.extract(slot, resource, insertable, tx);
                    if (extracted <= 0) continue;
                    long inserted = targetSlot.handler().insert(targetSlot.slot(), resource, (int) extracted, tx);
                    if (inserted != extracted) continue;
                    tx.commit();
                    return (int) Math.min(Integer.MAX_VALUE, inserted);
                }
            }
        }
        return 0;
    }

    private static boolean canExtractItemFromSlot(ServerLevel level, GraphLinkData link, GraphNodeData sourceNode, BlockPos pos, Direction side, ResourceHandler<ItemResource> source, int handlerSlot, ItemResource resource) {
        BlockEntity be = level.getBlockEntity(pos);
        boolean machineSource = sourceNode != null && sourceNode.type() == NodeType.MACHINE;
        if (machineSource && !isOutputPort(link.sourcePortId())) return false;
        if (!(be instanceof WorldlyContainer container)) {
            return !machineSource || !source.isValid(handlerSlot, resource);
        }
        if (!machineSource) return true;
        int containerSlot = containerSlotForHandlerSlot(container, side, handlerSlot);
        if (containerSlot < 0) return false;
        if (container.canPlaceItem(containerSlot, resource.toStack())) return false;
        if (side != null) return container.canTakeItemThroughFace(containerSlot, resource.toStack(), side);
        for (Direction face : Direction.values()) {
            for (int slot : container.getSlotsForFace(face)) {
                if (slot == containerSlot && container.canTakeItemThroughFace(slot, resource.toStack(), face)) return true;
            }
        }
        return false;
    }

    private static int containerSlotForHandlerSlot(WorldlyContainer container, Direction side, int handlerSlot) {
        if (handlerSlot < 0) return -1;
        if (side == null) return handlerSlot < container.getContainerSize() ? handlerSlot : -1;
        int[] slots = container.getSlotsForFace(side);
        return handlerSlot < slots.length ? slots[handlerSlot] : -1;
    }

    private static ItemTargetSlot[] targetSlots(ServerLevel level, GraphLinkData link, BlockPos pos, Direction side, ResourceHandler<ItemResource> target, ItemResource resource, boolean manualSide) {
        ItemTargetRole role = ItemTargetRole.fromPort(link.targetPortId());
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WorldlyContainer container)) return capabilityTargetSlotOrder(level, target, resource, role);
        if (side != null) return sideTargetSlots(level, container, side, resource, role, manualSide);
        return worldlyTargetSlots(level, container, resource, role);
    }

    private static ItemTargetSlot[] worldlyTargetSlots(ServerLevel level, WorldlyContainer container, ItemResource resource, ItemTargetRole role) {
        // Allocate only as many slots as the container has — each container slot can appear at most once
        // thanks to the `added` dedup flag. The old size×6 allocation was a 6× over-allocation.
        ItemTargetSlot[] slots = new ItemTargetSlot[container.getContainerSize()];
        boolean[] added = new boolean[container.getContainerSize()];
        int next = 0;
        if (role != ItemTargetRole.MATERIAL) {
            next = addFaceSlots(level, container, resource, slots, added, next, role, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN);
        }
        if (role != ItemTargetRole.FUEL) {
            next = addFaceSlots(level, container, resource, slots, added, next, role, Direction.UP);
            next = addFaceSlots(level, container, resource, slots, added, next, role, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN);
        }
        return copyPrefix(slots, next);
    }

    private static int addFaceSlots(ServerLevel level, WorldlyContainer container, ItemResource resource, ItemTargetSlot[] slots, boolean[] added, int next, ItemTargetRole role, Direction... faces) {
        for (Direction face : faces) {
            int[] faceSlots = container.getSlotsForFace(face);
            ResourceHandler<ItemResource> handler = new WorldlyContainerWrapper(container, face);
            for (int exposed = 0; exposed < faceSlots.length; exposed++) {
                int containerSlot = faceSlots[exposed];
                if (containerSlot < 0 || containerSlot >= added.length || added[containerSlot]) continue;
                if (!container.canPlaceItemThroughFace(containerSlot, resource.toStack(), face)) continue;
                if (!slotMatchesRole(level, container, containerSlot, resource, role)) continue;
                slots[next++] = new ItemTargetSlot(handler, exposed);
                added[containerSlot] = true;
            }
        }
        return next;
    }

    private static ItemTargetSlot[] sideTargetSlots(ServerLevel level, WorldlyContainer container, Direction side, ItemResource resource, ItemTargetRole role, boolean manualSide) {
        int[] exposedSlots = container.getSlotsForFace(side);
        ResourceHandler<ItemResource> handler = new WorldlyContainerWrapper(container, side);
        ItemTargetSlot[] slots = new ItemTargetSlot[exposedSlots.length];
        int next = 0;
        for (int exposed = 0; exposed < exposedSlots.length; exposed++) {
            int containerSlot = exposedSlots[exposed];
            if (containerSlot < 0 || containerSlot >= container.getContainerSize()) continue;
            if (!container.canPlaceItemThroughFace(containerSlot, resource.toStack(), side)) continue;
            if (!slotMatchesRole(level, container, containerSlot, resource, role)) continue;
            slots[next++] = new ItemTargetSlot(handler, exposed);
        }
        ItemTargetSlot[] matched = copyPrefix(slots, next);
        return matched.length > 0 || manualSide ? matched : worldlyTargetSlots(level, container, resource, role);
    }

    private static ItemTargetSlot[] capabilityTargetSlotOrder(ServerLevel level, ResourceHandler<ItemResource> target, ItemResource resource, ItemTargetRole role) {
        ItemTargetSlot[] slots = new ItemTargetSlot[target.size()];
        int next = 0;
        for (int i = 0; i < target.size(); i++) {
            if (role == ItemTargetRole.FUEL && !isFuelResource(level, resource)) return new ItemTargetSlot[0];
            if (target.isValid(i, resource)) slots[next++] = new ItemTargetSlot(target, i);
        }
        return copyPrefix(slots, next);
    }

    private static ItemTargetSlot[] copyPrefix(ItemTargetSlot[] values, int size) {
        ItemTargetSlot[] out = new ItemTargetSlot[size];
        System.arraycopy(values, 0, out, 0, size);
        return out;
    }

    private static boolean isOutputPort(String sourcePortId) {
        String port = sourcePortId == null ? "" : sourcePortId.toLowerCase(java.util.Locale.ROOT);
        return port.contains("out") || port.equals("output");
    }

    private static Direction sideOverride(String sideOverride) {
        return switch (sideOverride == null ? "auto" : sideOverride.toLowerCase(java.util.Locale.ROOT)) {
            case "up"    -> Direction.UP;
            case "down"  -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east"  -> Direction.EAST;
            case "west"  -> Direction.WEST;
            default -> null;
        };
    }

    private static int routeBudget(ServerLevel level, LogisticsGraphData graph, java.util.Map<String, BlockPos> scannedPositions) {
        int base = ModServerConfig.MAX_LFM_ROUTES_PER_TICK.get();
        int bonus = 0;
        // Cache per-block upgrade lookups within this tick — multiple nodes can reference the same pos.
        java.util.Map<BlockPos, Integer> upgradeCache = new java.util.HashMap<>();
        for (GraphNodeData node : graph.activeCanvas().nodes()) {
            BlockPos pos = targetPosOf(node, scannedPositions);
            if (pos == null || !level.hasChunkAt(pos)) continue;
            int speed = upgradeCache.computeIfAbsent(pos, p -> endpointUpgradeCount(level, p, UpgradeType.SPEED));
            bonus += WirelessNetworkPolicy.routeBudgetBonus(speed, hasWirelessEndpoint(level, pos));
        }
        return Math.max(1, Math.min(512, base + bonus));
    }

    /** Filter, Splitter, Combine and Channel all route along graph links through the same engine. */
    public static boolean isRoutingNode(NodeType type) {
        return type == NodeType.FILTER || type == NodeType.SPLITTER
                || type == NodeType.COMBINE || type == NodeType.CHANNEL;
    }

    private static int filterPriority(LogisticsGraphData graph, GraphLinkData link) {
        GraphNodeData target = graph.findNode(link.targetNodeId());
        if (target == null || !isRoutingNode(target.type())) return Integer.MAX_VALUE;
        int priority = FilterSettings.parse(target.notes()).priority();
        return priority <= 0 ? Integer.MAX_VALUE - 1 : priority;
    }

    private static int itemLimit(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        int base = ModServerConfig.MAX_ITEMS_MOVED_PER_ROUTE.get();
        int speed = Math.max(endpointUpgradeCount(level, sourcePos, UpgradeType.SPEED), endpointUpgradeCount(level, targetPos, UpgradeType.SPEED));
        int stack = Math.max(endpointUpgradeCount(level, sourcePos, UpgradeType.STACK), endpointUpgradeCount(level, targetPos, UpgradeType.STACK));
        return WirelessNetworkPolicy.speedItemLimit(base, speed, stack);
    }

    private static boolean hasWirelessEndpoint(ServerLevel level, BlockPos attachedPos) {
        if (attachedPos == null) return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = attachedPos.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof NetworkEndpointBlockEntity ep && !(ep instanceof NetworkModemBlockEntity) && ep.attachedPos().equals(attachedPos)) return true;
            if (be instanceof NetworkWireBlockEntity wire && wire.hasRouterAttachedTo(attachedPos)) return true;
        }
        return false;
    }

    private static int endpointUpgradeCount(ServerLevel level, BlockPos attachedPos, UpgradeType type) {
        int total = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = attachedPos.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof NetworkEndpointBlockEntity endpoint && endpoint.attachedPos().equals(attachedPos)) {
                total += endpoint.upgradeCount(type);
            } else if (be instanceof NetworkWireBlockEntity wire) {
                total += wire.upgradeCountForAttachedPos(attachedPos, type);
            }
        }
        return total;
    }

    private static boolean isFuelLikeInsertionSlot(WorldlyContainer container, int slot, ItemResource resource) {
        if (slot < 0 || slot >= container.getContainerSize()) return false;
        if (!container.canPlaceItem(slot, resource.toStack())) return false;
        boolean nonTop = isSlotExposedForInsert(container, slot, resource, Direction.NORTH)
                || isSlotExposedForInsert(container, slot, resource, Direction.SOUTH)
                || isSlotExposedForInsert(container, slot, resource, Direction.EAST)
                || isSlotExposedForInsert(container, slot, resource, Direction.WEST)
                || isSlotExposedForInsert(container, slot, resource, Direction.DOWN);
        boolean top = isSlotExposedForInsert(container, slot, resource, Direction.UP);
        return nonTop && !top;
    }

    private static boolean isSlotExposedForInsert(WorldlyContainer container, int slot, ItemResource resource, Direction face) {
        for (int exposed : container.getSlotsForFace(face)) {
            if (exposed == slot) return container.canPlaceItemThroughFace(slot, resource.toStack(), face);
        }
        return false;
    }

    private static boolean slotMatchesRole(ServerLevel level, WorldlyContainer container, int slot, ItemResource resource, ItemTargetRole role) {
        return switch (role) {
            case MATERIAL -> !isFuelLikeInsertionSlot(container, slot, resource);
            case FUEL -> isFuelResource(level, resource) && (isFuelLikeInsertionSlot(container, slot, resource) || container.canPlaceItem(slot, resource.toStack()));
            case GENERIC -> true;
        };
    }

    private static boolean isFuelResource(ServerLevel level, ItemResource resource) {
        return resource != null && !resource.isEmpty() && level.fuelValues().isFuel(resource.toStack());
    }

    private static int transferFluidsCount(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        Direction sourceSide = sideFrom(sourcePos, targetPos);
        Direction targetSide = sourceSide == null ? null : sourceSide.getOpposite();
        ResourceHandler<FluidResource> source = fluidHandler(level, sourcePos, sourceSide);
        ResourceHandler<FluidResource> target = fluidHandler(level, targetPos, targetSide);
        if (source == null) source = fluidHandler(level, sourcePos, null);
        if (target == null) target = fluidHandler(level, targetPos, null);
        if (source == null || target == null) return 0;

        long max = ModServerConfig.MAX_FLUID_MOVED_PER_ROUTE.get();
        for (int tank = 0; tank < source.size(); tank++) {
            FluidResource resource = source.getResource(tank);
            long available = source.getAmountAsLong(tank);
            if (resource.isEmpty() || available <= 0) continue;
            int amount = (int) Math.min(Integer.MAX_VALUE, Math.min(max, available));
            try (Transaction tx = Transaction.openRoot()) {
                long extracted = source.extract(resource, amount, tx);
                if (extracted <= 0) continue;
                long inserted = target.insert(resource, (int) extracted, tx);
                if (inserted <= 0) continue;
                if (inserted < extracted) continue;
                tx.commit();
                return (int) Math.min(Integer.MAX_VALUE, inserted);
            }
        }
        return 0;
    }

    private static int transferEnergyCount(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        Direction sourceSide = sideFrom(sourcePos, targetPos);
        Direction targetSide = sourceSide == null ? null : sourceSide.getOpposite();
        EnergyHandler source = energyHandler(level, sourcePos, sourceSide);
        EnergyHandler target = energyHandler(level, targetPos, targetSide);
        if (source == null) source = energyHandler(level, sourcePos, null);
        if (target == null) target = energyHandler(level, targetPos, null);
        if (source == null || target == null) return 0;

        int amount = (int) Math.min(Integer.MAX_VALUE, Math.min(ModServerConfig.MAX_ENERGY_MOVED_PER_ROUTE.get(), source.getAmountAsLong()));
        if (amount <= 0) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            long extracted = source.extract(amount, tx);
            if (extracted <= 0) return 0;
            long inserted = target.insert((int) extracted, tx);
            if (inserted <= 0) return 0;
            if (inserted < extracted) return 0;
            tx.commit();
            return (int) Math.min(Integer.MAX_VALUE, inserted);
        }
    }
}
