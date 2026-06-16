package net.doole.doolestools.logistics;

import net.doole.doolestools.logistics.data.GraphFrameData;
import net.doole.doolestools.logistics.data.GraphCanvasData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.GraphTextData;
import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable-in / immutable-out graph operations shared by the client editor and server validation.
 * Each mutating call returns a new {@link LogisticsGraphData} rather than editing in place.
 */
public final class LogisticsGraph {
    private LogisticsGraph() {}

    public static String newNodeId() {
        return "node_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String newLinkId() {
        return "link_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String newCanvasId() {
        return "canvas_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Adds a node for a scanned block. If one already exists for that block, returns the graph unchanged. */
    public static LogisticsGraphData addNode(LogisticsGraphData graph, ScannedBlockData scanned, int x, int y) {
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            if (n.scannedBlockId().equals(scanned.id())) {
                return graph;
            }
        }
        List<GraphNodeData> nodes = new ArrayList<>(graph.activeCanvas().nodes());
        nodes.add(new GraphNodeData(
                newNodeId(), scanned.id(), shortName(scanned.blockName()),
                scanned.type().toDefaultNodeType(), x, y,
                GraphNodeData.DEFAULT_WIDTH, GraphNodeData.DEFAULT_HEIGHT, PortDiscovery.discover(scanned), false, ""));
        return withNodes(graph, nodes);
    }

    /** Adds an already-built node (e.g. a standalone Filter node not tied to a scanned block). */
    public static LogisticsGraphData addRawNode(LogisticsGraphData graph, GraphNodeData node) {
        List<GraphNodeData> nodes = new ArrayList<>(graph.activeCanvas().nodes());
        nodes.add(node);
        return withNodes(graph, nodes);
    }

    public static LogisticsGraphData removeNode(LogisticsGraphData graph, String nodeId) {
        List<GraphNodeData> nodes = new ArrayList<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            if (!n.nodeId().equals(nodeId)) nodes.add(n);
        }
        List<GraphLinkData> links = new ArrayList<>();
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (!l.sourceNodeId().equals(nodeId) && !l.targetNodeId().equals(nodeId)) links.add(l);
        }
        return withNodesLinks(graph, nodes, links);
    }

    private static LogisticsGraphData withNodesLinks(LogisticsGraphData g, List<GraphNodeData> nodes, List<GraphLinkData> links) {
        GraphCanvasData c = g.activeCanvas();
        return g.withActiveCanvas(new GraphCanvasData(c.canvasId(), c.title(), nodes, links, c.frames(), c.texts()));
    }

    public static LogisticsGraphData updateNode(LogisticsGraphData graph, GraphNodeData updated) {
        List<GraphNodeData> nodes = new ArrayList<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            nodes.add(n.nodeId().equals(updated.nodeId()) ? updated : n);
        }
        return withNodes(graph, nodes);
    }

    public static LogisticsGraphData renameGraph(LogisticsGraphData graph, String name) {
        String cleaned = name == null || name.isBlank() ? "Untitled Graph" : name;
        GraphCanvasData c = graph.activeCanvas();
        return graph.withActiveCanvas(new GraphCanvasData(c.canvasId(), cleaned, c.nodes(), c.links(), c.frames(), c.texts()));
    }

    public static LogisticsGraphData addCanvas(LogisticsGraphData graph) {
        List<GraphCanvasData> canvases = new ArrayList<>(graph.canvasesOrLegacy());
        String id = newCanvasId();
        GraphCanvasData canvas = new GraphCanvasData(id, "Canvas " + (canvases.size() + 1), List.of(), List.of(), List.of(), List.of());
        canvases.add(canvas);
        return new LogisticsGraphData(graph.graphId(), graph.graphName(), canvas.nodes(), canvas.links(),
                graph.lastSavedTime(), graph.linkedMonitors(), canvas.frames(), canvas.texts(), id, canvases);
    }

    public static LogisticsGraphData switchCanvas(LogisticsGraphData graph, String canvasId) {
        for (GraphCanvasData canvas : graph.canvasesOrLegacy()) {
            if (canvas.canvasId().equals(canvasId)) {
                return new LogisticsGraphData(graph.graphId(), graph.graphName(), canvas.nodes(), canvas.links(),
                        graph.lastSavedTime(), graph.linkedMonitors(), canvas.frames(), canvas.texts(), canvasId, graph.canvasesOrLegacy());
            }
        }
        return graph;
    }

    public static LogisticsGraphData removeCanvas(LogisticsGraphData graph, String canvasId) {
        List<GraphCanvasData> existing = graph.canvasesOrLegacy();
        if (existing.size() <= 1) return graph;
        List<GraphCanvasData> canvases = new ArrayList<>();
        int removedIndex = -1;
        for (int i = 0; i < existing.size(); i++) {
            GraphCanvasData canvas = existing.get(i);
            if (canvas.canvasId().equals(canvasId)) {
                removedIndex = i;
            } else {
                canvases.add(canvas);
            }
        }
        if (removedIndex < 0 || canvases.isEmpty()) return graph;
        int nextIndex = Math.min(removedIndex, canvases.size() - 1);
        GraphCanvasData active = canvases.get(nextIndex);
        return new LogisticsGraphData(graph.graphId(), graph.graphName(), active.nodes(), active.links(),
                graph.lastSavedTime(), graph.linkedMonitors(), active.frames(), active.texts(), active.canvasId(), canvases);
    }

    /** Adds a directed link if both nodes exist, it is not a self-link, and no duplicate exists. */
    public static LogisticsGraphData addLink(LogisticsGraphData graph, String sourceId, String targetId, LinkType type) {
        if (sourceId.equals(targetId)) return graph;
        if (graph.findNode(sourceId) == null || graph.findNode(targetId) == null) return graph;
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (l.sourceNodeId().equals(sourceId) && l.targetNodeId().equals(targetId)) {
                return graph;
            }
        }
        List<GraphLinkData> links = new ArrayList<>(graph.activeCanvas().links());
        links.add(new GraphLinkData(newLinkId(), sourceId, targetId, "", type));
        return withLinks(graph, links);
    }

    /** Adds a directed port link if both endpoints exist and are compatible. */
    public static LogisticsGraphData addLink(LogisticsGraphData graph, String sourceId, String sourcePortId,
                                             String targetId, String targetPortId) {
        if (sourceId.equals(targetId)) return graph;
        GraphNodeData source = graph.findNode(sourceId);
        GraphNodeData target = graph.findNode(targetId);
        if (source == null || target == null) return graph;
        GraphPortData sourcePort = source.findPort(sourcePortId);
        GraphPortData targetPort = target.findPort(targetPortId);
        if (!PortDiscovery.compatible(sourcePort, targetPort)) return graph;
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (l.sourceNodeId().equals(sourceId) && l.sourcePortId().equals(sourcePortId)
                    && l.targetNodeId().equals(targetId) && l.targetPortId().equals(targetPortId)) {
                return graph;
            }
        }
        List<GraphLinkData> links = new ArrayList<>(graph.activeCanvas().links());
        links.add(new GraphLinkData(newLinkId(), sourceId, sourcePortId, targetId, targetPortId, "", PortDiscovery.linkType(sourcePort)));
        return withLinks(graph, links);
    }

    public static LogisticsGraphData removeLink(LogisticsGraphData graph, String linkId) {
        List<GraphLinkData> links = new ArrayList<>();
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (!l.linkId().equals(linkId)) links.add(l);
        }
        return withLinks(graph, links);
    }

    public static LogisticsGraphData setLinkSideOverride(LogisticsGraphData graph, String linkId, String sideOverride) {
        List<GraphLinkData> links = new ArrayList<>();
        for (GraphLinkData l : graph.activeCanvas().links()) {
            links.add(l.linkId().equals(linkId) ? l.withSideOverride(normalizeSideOverride(sideOverride)) : l);
        }
        return withLinks(graph, links);
    }

    public static String nextSideOverride(String current) {
        return switch (normalizeSideOverride(current)) {
            case "auto" -> "up";
            case "up" -> "down";
            case "down" -> "north";
            case "north" -> "west";
            case "west" -> "east";
            default -> "auto";
        };
    }

    public static String normalizeSideOverride(String sideOverride) {
        if (sideOverride == null) return "auto";
        return switch (sideOverride.toLowerCase(java.util.Locale.ROOT)) {
            case "up", "down", "north", "south", "east", "west" -> sideOverride.toLowerCase(java.util.Locale.ROOT);
            default -> "auto";
        };
    }

    public static LogisticsGraphData insertNodeIntoLink(LogisticsGraphData graph, String linkId, String nodeId) {
        GraphNodeData inserted = graph.findNode(nodeId);
        if (inserted == null) return graph;
        for (GraphLinkData link : graph.activeCanvas().links()) {
            if (!link.linkId().equals(linkId)) continue;
            GraphNodeData source = graph.findNode(link.sourceNodeId());
            GraphNodeData target = graph.findNode(link.targetNodeId());
            if (source == null || target == null) return graph;
            if (inserted.nodeId().equals(source.nodeId()) || inserted.nodeId().equals(target.nodeId())) return graph;
            GraphPortData sourcePort = source.findPort(link.sourcePortId());
            GraphPortData targetPort = target.findPort(link.targetPortId());
            GraphPortData insertedIn = firstCompatibleIn(inserted, sourcePort);
            GraphPortData insertedOut = firstCompatibleOut(inserted, targetPort);
            if (insertedIn == null || insertedOut == null) return graph;

            List<GraphLinkData> links = new ArrayList<>();
            for (GraphLinkData existing : graph.activeCanvas().links()) {
                if (!existing.linkId().equals(linkId)) links.add(existing);
            }
            links.add(new GraphLinkData(newLinkId(), source.nodeId(), sourcePort.portId(), inserted.nodeId(), insertedIn.portId(), "", PortDiscovery.linkType(sourcePort)));
            links.add(new GraphLinkData(newLinkId(), inserted.nodeId(), insertedOut.portId(), target.nodeId(), targetPort.portId(), "", PortDiscovery.linkType(insertedOut)));
            return withLinks(graph, links);
        }
        return graph;
    }

    private static GraphPortData firstCompatibleIn(GraphNodeData node, GraphPortData sourcePort) {
        for (GraphPortData port : node.ports()) {
            if (PortDiscovery.compatible(sourcePort, port)) return port;
        }
        return null;
    }

    private static GraphPortData firstCompatibleOut(GraphNodeData node, GraphPortData targetPort) {
        for (GraphPortData port : node.ports()) {
            if (PortDiscovery.compatible(port, targetPort)) return port;
        }
        return null;
    }

    public static LogisticsGraphData retargetLinkTarget(LogisticsGraphData graph, String linkId, String targetId, String targetPortId) {
        List<GraphLinkData> links = new ArrayList<>();
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (!l.linkId().equals(linkId)) {
                links.add(l);
                continue;
            }
            GraphNodeData source = graph.findNode(l.sourceNodeId());
            GraphNodeData target = graph.findNode(targetId);
            if (source == null || target == null || source.nodeId().equals(target.nodeId())) {
                links.add(l);
                continue;
            }
            GraphPortData sourcePort = source.findPort(l.sourcePortId());
            GraphPortData targetPort = target.findPort(targetPortId);
            if (PortDiscovery.compatible(sourcePort, targetPort)) {
                links.add(new GraphLinkData(l.linkId(), l.sourceNodeId(), l.sourcePortId(), targetId, targetPortId, l.label(), l.type(), l.sideOverride()));
            } else {
                links.add(l);
            }
        }
        return withLinks(graph, links);
    }

    /** Simple layered left-to-right auto-arrange by link depth. */
    public static LogisticsGraphData autoArrange(LogisticsGraphData graph) {
        if (graph.activeCanvas().nodes().isEmpty()) return graph;

        Map<String, Integer> incoming = new HashMap<>();
        Map<String, List<String>> out = new HashMap<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            incoming.put(n.nodeId(), 0);
            out.put(n.nodeId(), new ArrayList<>());
        }
        for (GraphLinkData l : graph.activeCanvas().links()) {
            if (incoming.containsKey(l.targetNodeId()) && out.containsKey(l.sourceNodeId())) {
                incoming.merge(l.targetNodeId(), 1, Integer::sum);
                out.get(l.sourceNodeId()).add(l.targetNodeId());
            }
        }

        // BFS depth assignment from roots (no incoming).
        Map<String, Integer> depth = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            if (incoming.get(n.nodeId()) == 0) {
                depth.put(n.nodeId(), 0);
                queue.add(n.nodeId());
            }
        }
        // Any nodes left out of a cycle get depth 0.
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            depth.putIfAbsent(n.nodeId(), 0);
        }
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (!visited.add(cur)) continue;
            int d = depth.getOrDefault(cur, 0);
            for (String next : out.getOrDefault(cur, List.of())) {
                if (depth.getOrDefault(next, 0) < d + 1) depth.put(next, d + 1);
                queue.add(next);
            }
        }

        Map<Integer, Integer> rowInColumn = new HashMap<>();
        // Leave generous breathing room so nodes (and their ports/labels) never touch.
        int marginX = 32, marginY = 28, colW = 200, rowH = 116;
        List<GraphNodeData> nodes = new ArrayList<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) {
            int col = depth.getOrDefault(n.nodeId(), 0);
            int row = rowInColumn.merge(col, 1, Integer::sum) - 1;
            nodes.add(n.withPosition(marginX + col * colW, marginY + row * rowH));
        }
        return withNodes(graph, nodes);
    }

    // --- Frames (annotation regions) ---

    public static String newFrameId() {
        return "frame_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static LogisticsGraphData addFrame(LogisticsGraphData graph, int x, int y, int w, int h, String label) {
        List<GraphFrameData> frames = new ArrayList<>(graph.activeCanvas().frames());
        frames.add(new GraphFrameData(newFrameId(), label == null ? "Frame" : label, x, y, w, h));
        return withFrames(graph, frames);
    }

    public static LogisticsGraphData removeFrame(LogisticsGraphData graph, String frameId) {
        List<GraphFrameData> frames = new ArrayList<>();
        for (GraphFrameData f : graph.activeCanvas().frames()) {
            if (!f.frameId().equals(frameId)) frames.add(f);
        }
        return withFrames(graph, frames);
    }

    public static LogisticsGraphData updateFrame(LogisticsGraphData graph, GraphFrameData updated) {
        List<GraphFrameData> frames = new ArrayList<>();
        for (GraphFrameData f : graph.activeCanvas().frames()) {
            frames.add(f.frameId().equals(updated.frameId()) ? updated : f);
        }
        return withFrames(graph, frames);
    }

    // --- Text labels (annotations) ---

    public static String newTextId() {
        return "text_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static LogisticsGraphData addText(LogisticsGraphData graph, int x, int y, String text) {
        List<GraphTextData> texts = new ArrayList<>(graph.activeCanvas().texts());
        texts.add(new GraphTextData(newTextId(), text == null ? "Label" : text, x, y));
        return withTexts(graph, texts);
    }

    public static LogisticsGraphData removeText(LogisticsGraphData graph, String textId) {
        List<GraphTextData> texts = new ArrayList<>();
        for (GraphTextData t : graph.activeCanvas().texts()) {
            if (!t.textId().equals(textId)) texts.add(t);
        }
        return withTexts(graph, texts);
    }

    public static LogisticsGraphData updateText(LogisticsGraphData graph, GraphTextData updated) {
        List<GraphTextData> texts = new ArrayList<>();
        for (GraphTextData t : graph.activeCanvas().texts()) {
            texts.add(t.textId().equals(updated.textId()) ? updated : t);
        }
        return withTexts(graph, texts);
    }

    private static LogisticsGraphData withNodes(LogisticsGraphData g, List<GraphNodeData> nodes) {
        GraphCanvasData c = g.activeCanvas();
        return g.withActiveCanvas(new GraphCanvasData(c.canvasId(), c.title(), nodes, c.links(), c.frames(), c.texts()));
    }

    private static LogisticsGraphData withLinks(LogisticsGraphData g, List<GraphLinkData> links) {
        GraphCanvasData c = g.activeCanvas();
        return g.withActiveCanvas(new GraphCanvasData(c.canvasId(), c.title(), c.nodes(), links, c.frames(), c.texts()));
    }

    private static LogisticsGraphData withFrames(LogisticsGraphData g, List<GraphFrameData> frames) {
        GraphCanvasData c = g.activeCanvas();
        return g.withActiveCanvas(new GraphCanvasData(c.canvasId(), c.title(), c.nodes(), c.links(), frames, c.texts()));
    }

    private static LogisticsGraphData withTexts(LogisticsGraphData g, List<GraphTextData> texts) {
        GraphCanvasData c = g.activeCanvas();
        return g.withActiveCanvas(new GraphCanvasData(c.canvasId(), c.title(), c.nodes(), c.links(), c.frames(), texts));
    }

    private static String shortName(String name) {
        return name == null ? "Node" : (name.length() > 18 ? name.substring(0, 18) : name);
    }
}
