package net.doole.doolestools.network;

import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.blockentity.LogisticsMonitorBlockEntity;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.item.LabelGunItem;
import net.doole.doolestools.logistics.LogisticsGraph;
import net.doole.doolestools.logistics.PortDiscovery;
import net.doole.doolestools.logistics.data.GraphCanvasData;
import net.doole.doolestools.logistics.data.GraphFrameData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.GraphTextData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.menu.LogisticsComputerMenu;
import net.doole.doolestools.menu.LogisticsMonitorMenu;
import net.doole.doolestools.network.payload.ClearScanPayload;
import net.doole.doolestools.network.payload.ComputerStatePayload;
import net.doole.doolestools.network.payload.MonitorStatePayload;
import net.doole.doolestools.network.payload.NearbyLabelsPayload;
import net.doole.doolestools.network.payload.KnownNetworksPayload;
import net.doole.doolestools.network.payload.RequestComputerSyncPayload;
import net.doole.doolestools.network.payload.RequestMonitorSyncPayload;
import net.doole.doolestools.network.payload.RequestNearbyLabelsPayload;
import net.doole.doolestools.network.payload.SaveGraphPayload;
import net.doole.doolestools.network.payload.ScanAreaPayload;
import net.doole.doolestools.network.payload.SetBlockLabelPayload;
import net.doole.doolestools.network.payload.SetComputerNetworkSettingsPayload;
import net.doole.doolestools.network.payload.SetGunLabelPayload;
import net.doole.doolestools.network.payload.SetNetworkEndpointNamePayload;
import net.doole.doolestools.network.payload.SetMonitorModePayload;
import net.doole.doolestools.world.BlockLabelSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side payload handlers. Every handler validates that the sending player actually has the
 * matching menu open for the target position before touching the world, and runs on the main thread.
 * The client is never trusted to mutate arbitrary blocks.
 */
public final class ServerPayloadHandlers {
    private ServerPayloadHandlers() {}

    private static final int MAX_NODES = 256;
    private static final int MAX_LINKS = 512;
    private static final int MAX_FRAMES = 64;
    private static final int MAX_TEXTS = 128;
    private static final int NEARBY_LABEL_RADIUS = 48;
    private static final int KNOWN_NETWORK_RADIUS = 96;
    private static final int MAX_NAME = 48;
    private static final int MAX_NOTES = 256;
    private static final int MAX_PORT_ID = 32;
    private static final int CANVAS_LIMIT = 100_000;

    // --- Computer ---

    public static void handleRequestComputerSync(RequestComputerSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> sendComputerState(player, payload.pos(), be)));
    }

    public static void handleScanArea(ScanAreaPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) { sendComputerState(player, payload.pos(), be); return; }
            be.performScan();
            sendComputerState(player, payload.pos(), be);
        }));
    }

    public static void handleClearScan(ClearScanPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) { sendComputerState(player, payload.pos(), be); return; }
            be.clearScan();
            sendComputerState(player, payload.pos(), be);
        }));
    }

    public static void handleSaveGraph(SaveGraphPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) { sendComputerState(player, payload.pos(), be); return; }
            be.saveGraph(sanitize(payload.graph(), be.getLastScanTime()));
            sendComputerState(player, payload.pos(), be);
        }));
    }

    public static void handleSetComputerNetworkSettings(SetComputerNetworkSettingsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) { sendComputerState(player, payload.pos(), be); return; }
            // Whitelist entries may be UUIDs or player names; resolve names to UUIDs so typing a name works.
            be.setNetworkSettings(payload.name(), payload.accessMode(),
                    resolveWhitelist(player.level().getServer(), payload.editorWhitelist()), player);
            sendComputerState(player, payload.pos(), be);
        }));
    }

    private static List<String> resolveWhitelist(net.minecraft.server.MinecraftServer server, List<String> entries) {
        List<String> out = new ArrayList<>();
        if (entries == null || server == null) return out;
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            String trimmed = entry.trim();
            try {
                out.add(java.util.UUID.fromString(trimmed).toString());
                continue;
            } catch (IllegalArgumentException ignored) {
                // Not a UUID — treat it as the name of a currently-online player.
            }
            ServerPlayer online = server.getPlayerList().getPlayerByName(trimmed);
            if (online != null) out.add(online.getUUID().toString());
        }
        return out;
    }

    private static void sendComputerState(ServerPlayer player, BlockPos pos, LogisticsComputerBlockEntity be) {
        PacketDistributor.sendToPlayer(player,
                new ComputerStatePayload(pos, be.getLastScan(), be.getGraph(), be.getLastScanTime(), be.getNetworkPower(), be.getActiveRouteIds(),
                        be.networkId(), be.networkName(), be.accessMode(), be.editorWhitelist(), be.canEdit(player),
                        be.getPowerSupplyHistory(), be.getPowerDemandHistory(),
                        be.getSupply30m(), be.getDemand30m(), be.getSupply1h(), be.getDemand1h(),
                        be.getSupply12h(), be.getDemand12h(), be.getSupply1d(), be.getDemand1d(),
                        be.getSupplyAllTime(), be.getDemandAllTime(), be.getLinkThroughputHistory()));
        sendKnownNetworks(player);
    }

    // --- Monitor ---

    public static void handleSetBlockLabel(SetBlockLabelPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!level.hasChunkAt(payload.pos())) return;
            if (player.distanceToSqr(payload.pos().getCenter()) > 64.0D) return;
            BlockLabelSavedData.get(level).setLabel(level.dimension().identifier(), payload.pos(), payload.label());
        });
    }

    public static void handleSetGunLabel(SetGunLabelPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            String cleaned = BlockLabelSavedData.sanitize(payload.label());
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof LabelGunItem) {
                    if (cleaned.isBlank()) {
                        stack.remove(DataComponents.CUSTOM_NAME);
                    } else {
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(cleaned));
                    }
                    return;
                }
            }
        });
    }

    public static void handleRequestNearbyLabels(RequestNearbyLabelsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;
            List<BlockPos> positions = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            BlockLabelSavedData.get(level).gatherNearby(level.dimension().identifier(),
                    player.blockPosition(), NEARBY_LABEL_RADIUS, positions, labels);
            // Also surface the names players gave network devices on the naming screen, so the gun
            // shows them as holograms too. Block labels are added first and win on shared positions.
            appendNetworkDeviceNames(level, player.blockPosition(), NEARBY_LABEL_RADIUS, positions, labels);
            PacketDistributor.sendToPlayer(player, new NearbyLabelsPayload(positions, labels));
        });
    }

    /** Read-only sweep of loaded network block entities near the player, collecting their device names. */
    private static void appendNetworkDeviceNames(ServerLevel level, BlockPos center, int radius, List<BlockPos> positions, List<String> labels) {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>(positions);
        long radiusSqr = (long) radius * radius;
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (center.distSqr(pos) > radiusSqr || seen.contains(pos)) continue;
                    String name = networkDeviceName(be);
                    if (name == null || name.isBlank()) continue;
                    positions.add(pos.immutable());
                    labels.add(name);
                    seen.add(pos.immutable());
                }
            }
        }
    }

    private static String networkDeviceName(BlockEntity be) {
        if (be instanceof NetworkRelayBlockEntity relay) return relay.displayName();
        if (be instanceof NetworkEndpointBlockEntity endpoint) return endpoint.deviceName();
        if (be instanceof NetworkWireBlockEntity wire && wire.hasEndpoint()) return wire.endpointName();
        return null;
    }

    public static void handleRequestKnownNetworks(net.doole.doolestools.network.payload.RequestKnownNetworksPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) sendKnownNetworks(player);
        });
    }

    public static void handleSetNetworkEndpointName(SetNetworkEndpointNamePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.level().hasChunkAt(payload.pos())) return;
            if (player.blockPosition().distSqr(payload.pos()) > 64.0) return;
            String networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(payload.networkId());
            if (!networkId.isBlank() && !canAssignNetwork(player, networkId)) return;
            if (player.level().getBlockEntity(payload.pos()) instanceof NetworkEndpointBlockEntity endpoint) {
                endpoint.setIdentity(payload.name(), networkId);
                player.level().sendBlockUpdated(payload.pos(), endpoint.getBlockState(), endpoint.getBlockState(), 3);
            } else if (player.level().getBlockEntity(payload.pos()) instanceof NetworkWireBlockEntity wire) {
                wire.setEndpointIdentity(payload.name(), networkId);
                player.level().sendBlockUpdated(payload.pos(), wire.getBlockState(), wire.getBlockState(), 3);
            } else if (player.level().getBlockEntity(payload.pos()) instanceof NetworkRelayBlockEntity relay) {
                relay.setIdentity(payload.name(), networkId);
                player.level().sendBlockUpdated(payload.pos(), relay.getBlockState(), relay.getBlockState(), 3);
            }
        });
    }

    private static void sendKnownNetworks(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Map<String, NetworkEntry> entries = new LinkedHashMap<>();
        BlockPos center = player.blockPosition();
        int minY = center.getY() - 32;
        int maxY = center.getY() + 32;
        forEachLoadedBlockEntity(level, center, KNOWN_NETWORK_RADIUS, blockEntity -> {
            BlockPos pos = blockEntity.getBlockPos();
            if (pos.getY() < minY || pos.getY() > maxY) return;
            if (Math.abs(pos.getX() - center.getX()) > KNOWN_NETWORK_RADIUS || Math.abs(pos.getZ() - center.getZ()) > KNOWN_NETWORK_RADIUS) return;
            if (blockEntity instanceof LogisticsComputerBlockEntity computer) {
                String id = computer.networkId();
                if (id.isBlank()) return;
                boolean canEdit = computer.canEdit(player);
                if (canEdit || !"private".equals(computer.accessMode())) {
                    entries.put(id, new NetworkEntry(id, computer.networkName(), canEdit));
                }
            }
        });
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Boolean> editable = new ArrayList<>();
        for (NetworkEntry entry : entries.values()) {
            ids.add(entry.id());
            names.add(entry.name());
            editable.add(entry.editable());
        }
        PacketDistributor.sendToPlayer(player, new KnownNetworksPayload(ids, names, editable));
    }

    private static boolean canAssignNetwork(ServerPlayer player, String networkId) {
        if (networkId == null || networkId.isBlank()) return true;
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos center = player.blockPosition();
        int minY = center.getY() - 16;
        int maxY = center.getY() + 16;
        final boolean[] allowed = { false };
        forEachLoadedBlockEntity(level, center, 32, blockEntity -> {
            if (allowed[0]) return;
            BlockPos pos = blockEntity.getBlockPos();
            if (pos.getY() < minY || pos.getY() > maxY) return;
            if (Math.abs(pos.getX() - center.getX()) > 32 || Math.abs(pos.getZ() - center.getZ()) > 32) return;
            if (blockEntity instanceof LogisticsComputerBlockEntity computer && computer.networkId().equals(networkId)) {
                allowed[0] = computer.canEdit(player);
            }
        });
        return allowed[0];
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

    private record NetworkEntry(String id, String name, boolean editable) {}

    public static void handleRequestMonitorSync(RequestMonitorSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withMonitor(ctx, payload.pos(), (player, be) -> sendMonitorState(player, payload.pos(), be)));
    }

    public static void handleSetMonitorMode(SetMonitorModePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withMonitor(ctx, payload.pos(), (player, be) -> {
            int mode = Math.floorMod(payload.mode(), LogisticsMonitorBlockEntity.Mode.values().length);
            be.setMode(LogisticsMonitorBlockEntity.Mode.values()[mode]);
            sendMonitorState(player, payload.pos(), be);
        }));
    }

    private static void sendMonitorState(ServerPlayer player, BlockPos pos, LogisticsMonitorBlockEntity be) {
        LogisticsComputerBlockEntity computer = be.resolveComputer();
        boolean linked = computer != null;
        BlockPos computerPos = be.getLinkedComputer() != null ? be.getLinkedComputer() : pos;
        LogisticsGraphData graph = linked ? computer.getGraph() : LogisticsGraphData.EMPTY;
        var scan = linked ? computer.getLastScan() : List.<net.doole.doolestools.logistics.data.ScannedBlockData>of();
        PacketDistributor.sendToPlayer(player,
                new MonitorStatePayload(pos, linked, computerPos, be.getMode().ordinal(), graph, scan));
    }

    // --- Validation helpers ---

    private interface ComputerAction {
        void run(ServerPlayer player, LogisticsComputerBlockEntity be);
    }

    private interface MonitorAction {
        void run(ServerPlayer player, LogisticsMonitorBlockEntity be);
    }

    private static void withComputer(IPayloadContext ctx, BlockPos pos, ComputerAction action) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof LogisticsComputerMenu menu) || !menu.getPos().equals(pos)) return;
        if (!player.level().hasChunkAt(pos)) return;
        if (player.level().getBlockEntity(pos) instanceof LogisticsComputerBlockEntity be) {
            action.run(player, be);
        }
    }

    private static void withMonitor(IPayloadContext ctx, BlockPos pos, MonitorAction action) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof LogisticsMonitorMenu menu) || !menu.getPos().equals(pos)) return;
        if (!player.level().hasChunkAt(pos)) return;
        if (player.level().getBlockEntity(pos) instanceof LogisticsMonitorBlockEntity be) {
            action.run(player, be);
        }
    }

    /** Clamp/limit a client-supplied graph so a malicious client cannot bloat saved data. */
    private static LogisticsGraphData sanitize(LogisticsGraphData graph, long now) {
        List<GraphCanvasData> canvases = new ArrayList<>();
        String requestedActiveId = graph.activeCanvas().canvasId();
        for (GraphCanvasData canvas : graph.canvasesOrLegacy()) {
            if (canvases.size() >= 12) break;
            canvases.add(sanitizeCanvas(canvas));
        }
        if (canvases.isEmpty()) {
            canvases.add(new GraphCanvasData("main", "Untitled Graph", List.of(), List.of(), List.of(), List.of()));
        }
        GraphCanvasData active = canvases.get(0);
        for (GraphCanvasData canvas : canvases) {
            if (canvas.canvasId().equals(requestedActiveId)) {
                active = canvas;
                break;
            }
        }
        return new LogisticsGraphData(graph.graphId(), clampStr(graph.graphName(), MAX_NAME),
                active.nodes(), active.links(), now, graph.linkedMonitors(), active.frames(), active.texts(), active.canvasId(), canvases);
    }

    private static GraphCanvasData sanitizeCanvas(GraphCanvasData canvas) {
        List<GraphNodeData> nodes = new ArrayList<>();
        for (GraphNodeData n : canvas.nodes()) {
            if (nodes.size() >= MAX_NODES) break;
            List<GraphPortData> ports = new ArrayList<>();
            for (GraphPortData p : n.ports()) {
                if (ports.size() >= 12) break;
                ports.add(new GraphPortData(clampStr(p.portId(), MAX_PORT_ID), p.direction(), p.kind(), clampStr(p.label(), MAX_NAME)));
            }
            if (ports.isEmpty()) ports.addAll(PortDiscovery.fallback());
            nodes.add(new GraphNodeData(
                    n.nodeId(), n.scannedBlockId(),
                    clampStr(n.displayName(), MAX_NAME), n.type(),
                    clampCoord(n.x()), clampCoord(n.y()),
                    Math.max(32, Math.min(256, n.width())), Math.max(24, Math.min(256, n.height())),
                    ports, n.collapsed(), clampStr(n.notes(), MAX_NOTES)));
        }
        List<GraphLinkData> links = new ArrayList<>();
        for (GraphLinkData l : canvas.links()) {
            if (links.size() >= MAX_LINKS) break;
            GraphNodeData source = findNode(nodes, l.sourceNodeId());
            GraphNodeData target = findNode(nodes, l.targetNodeId());
            if (source == null || target == null || source.nodeId().equals(target.nodeId())) continue;
            GraphPortData sourcePort = source.findPort(l.sourcePortId());
            GraphPortData targetPort = target.findPort(l.targetPortId());
            if (!PortDiscovery.compatible(sourcePort, targetPort)) continue;
            links.add(new GraphLinkData(l.linkId(), l.sourceNodeId(), l.sourcePortId(), l.targetNodeId(), l.targetPortId(),
                    clampStr(l.label(), MAX_NAME), l.type(), LogisticsGraph.normalizeSideOverride(l.sideOverride())));
        }
        List<GraphFrameData> frames = new ArrayList<>();
        for (GraphFrameData f : canvas.frames()) {
            if (frames.size() >= MAX_FRAMES) break;
            frames.add(new GraphFrameData(f.frameId(), clampStr(f.label(), MAX_NAME),
                    clampCoord(f.x()), clampCoord(f.y()),
                    Math.max(48, Math.min(1024, f.width())), Math.max(40, Math.min(1024, f.height()))));
        }
        List<GraphTextData> texts = new ArrayList<>();
        for (GraphTextData t : canvas.texts()) {
            if (texts.size() >= MAX_TEXTS) break;
            texts.add(new GraphTextData(t.textId(), clampStr(t.text(), MAX_NAME),
                    clampCoord(t.x()), clampCoord(t.y())));
        }
        return new GraphCanvasData(clampStr(canvas.canvasId(), MAX_PORT_ID), clampStr(canvas.title(), MAX_NAME), nodes, links, frames, texts);
    }

    private static String clampStr(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static int clampCoord(int v) {
        return Math.max(-CANVAS_LIMIT, Math.min(CANVAS_LIMIT, v));
    }

    private static GraphNodeData findNode(List<GraphNodeData> nodes, String nodeId) {
        for (GraphNodeData node : nodes) {
            if (node.nodeId().equals(nodeId)) return node;
        }
        return null;
    }

    // --- Multi-computer mesh ---

    public static void handleLinkComputer(net.doole.doolestools.network.payload.LinkComputerPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;
            String targetId = payload.targetNetworkId().trim();
            BlockPos found = null;
            // scan loaded chunks around the player to find the target computer by networkId
            // not indexed so this is O(loaded block entities) - fine since linking is rare
            outer:
            for (int cx = (player.getBlockX() - 192) >> 4; cx <= (player.getBlockX() + 192) >> 4; cx++) {
                for (int cz = (player.getBlockZ() - 192) >> 4; cz <= (player.getBlockZ() + 192) >> 4; cz++) {
                    if (!(level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                            instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) continue;
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof LogisticsComputerBlockEntity peer && !peer.getBlockPos().equals(payload.pos())) {
                            if (peer.networkId().equals(targetId) || peer.formattedNetworkNumber().equals(targetId)) {
                                found = peer.getBlockPos().immutable();
                                break outer;
                            }
                        }
                    }
                }
            }
            if (found == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[LogiGraph] Network not found or not loaded: " + targetId));
                return;
            }
            if (be.linkPeer(found, level)) {
                sendLinkedComputers(player, payload.pos(), be);
                sendComputerState(player, payload.pos(), be);
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[LogiGraph] Link failed (already linked, at limit, or same computer)"));
            }
        }));
    }

    public static void handleUnlinkComputer(net.doole.doolestools.network.payload.UnlinkComputerPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> withComputer(ctx, payload.pos(), (player, be) -> {
            if (!be.canEdit(player)) return;
            be.unlinkPeer(payload.peerPos());
            sendLinkedComputers(player, payload.pos(), be);
            sendComputerState(player, payload.pos(), be);
        }));
    }

    private static void sendLinkedComputers(ServerPlayer player, BlockPos pos, LogisticsComputerBlockEntity be) {
        List<BlockPos> peers = be.getLinkedPeerPositions();
        List<String> ids = new ArrayList<>(peers.size());
        if (player.level() instanceof ServerLevel level) {
            for (BlockPos peerPos : peers) {
                String id = "";
                if (level.hasChunkAt(peerPos) && level.getBlockEntity(peerPos) instanceof LogisticsComputerBlockEntity peer) {
                    id = peer.networkId();
                }
                ids.add(id);
            }
        }
        PacketDistributor.sendToPlayer(player, new net.doole.doolestools.network.payload.LinkedComputersPayload(pos, peers, ids));
    }
}
