package net.doole.doolestools.client;

import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.logistics.LogisticsGraph;
import net.doole.doolestools.logistics.LinkType;
import net.doole.doolestools.logistics.PortDiscovery;
import net.doole.doolestools.logistics.data.GraphCanvasData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side mutable editor state shared by the computer screen and its widgets. Holds the scan,
 * the working graph copy, selection, filter, and canvas pan/zoom. All graph mutations go through
 * {@link LogisticsGraph} so the data stays immutable in transit and the same rules apply server-side.
 */
public class EditorContext {

    public enum Filter {
        ALL("All"), STORAGE("Storage"), MACHINES("Machines"),
        TRANSPORT("Transport"), WARNINGS("Warnings"), UNKNOWN("Unknown");

        public final String label;
        Filter(String label) { this.label = label; }
        public Filter next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final BlockPos pos;
    private List<ScannedBlockData> scan = new ArrayList<>();
    private final Map<String, ScannedBlockData> scanById = new HashMap<>();
    private LogisticsGraphData graph = LogisticsGraphData.EMPTY;
    private NetworkPowerData power = NetworkPowerData.EMPTY;
    private java.util.Set<String> activeRouteIds = java.util.Set.of();
    private String networkId = "";
    private String networkName = "";
    private String accessMode = "shared";
    private List<String> editorWhitelist = List.of();
    private List<Integer> powerSupplyHistory = List.of();
    private List<Integer> powerDemandHistory = List.of();
    private List<Short> supply30m = List.of();
    private List<Short> demand30m = List.of();
    private List<Short> supply1h = List.of();
    private List<Short> demand1h = List.of();
    private List<Short> supply12h = List.of();
    private List<Short> demand12h = List.of();
    private List<Short> supply1d = List.of();
    private List<Short> demand1d = List.of();
    private List<Short> supplyAllTime = List.of();
    private List<Short> demandAllTime = List.of();
    private Map<String, List<Integer>> linkThroughput = Map.of();
    private boolean canEdit = true;
    private long lastScanTime = -1L;

    public String selectedScannedId;
    public String selectedScanNetworkId = "";
    public String selectedNodeId;
    /** Additional nodes selected via the marquee tool; {@link #selectedNodeId} is the primary one. */
    public final java.util.Set<String> selectedNodeIds = new java.util.LinkedHashSet<>();
    public String selectedFrameId;
    public String selectedTextId;
    public Filter filter = Filter.ALL;

    public float panX = 0f;
    public float panY = 0f;
    public float zoom = 1f;

    public boolean linkMode = false;
    public String linkSourceId;
    public String draggingSourceNodeId;
    public String draggingSourcePortId;
    public String draggingLinkId;
    public double dragMouseX;
    public double dragMouseY;

    private boolean dirty = false;
    /** Bumped whenever the working graph reference changes, so the canvas can cache projected geometry. */
    private int graphRevision = 0;

    public EditorContext(BlockPos pos) {
        this.pos = pos;
    }

    public int graphRevision() { return graphRevision; }

    public BlockPos pos() { return pos; }

    public void setState(List<ScannedBlockData> scan, LogisticsGraphData graph, long lastScanTime, NetworkPowerData power) {
        this.scan = new ArrayList<>(scan);
        this.power = power == null ? NetworkPowerData.EMPTY : power;
        this.scanById.clear();
        for (ScannedBlockData s : this.scan) scanById.put(s.id(), s);
        if (!hasNetwork(selectedScanNetworkId)) selectedScanNetworkId = firstNetworkId();
        // Keep local edits if the player has unsaved changes; otherwise take the server's graph.
        if (!dirty) {
            this.graph = graph;
        }
        refreshNodePortsFromScan();
        this.lastScanTime = lastScanTime;
        graphRevision++;
    }

    private void refreshNodePortsFromScan() {
        List<GraphCanvasData> canvases = new ArrayList<>();
        boolean anyChanged = false;
        for (GraphCanvasData canvas : graph.canvasesOrLegacy()) {
            List<GraphNodeData> nodes = new ArrayList<>();
            boolean changed = false;
            for (GraphNodeData node : canvas.nodes()) {
                ScannedBlockData scanned = scanById.get(node.scannedBlockId());
                GraphNodeData updated = scanned == null ? node : node.withPorts(preserveLinkedPorts(
                        PortDiscovery.discover(scanned), node, canvas.links()));
                nodes.add(updated);
                changed |= updated != node;
            }
            if (changed) {
                canvases.add(new GraphCanvasData(canvas.canvasId(), canvas.title(), nodes, canvas.links(), canvas.frames(), canvas.texts()));
                anyChanged = true;
            } else {
                canvases.add(canvas);
            }
        }
        if (!anyChanged) return;
        GraphCanvasData active = canvases.get(0);
        for (GraphCanvasData canvas : canvases) {
            if (canvas.canvasId().equals(graph.activeCanvasId())) {
                active = canvas;
                break;
            }
        }
        this.graph = new LogisticsGraphData(graph.graphId(), graph.graphName(), active.nodes(), active.links(),
                graph.lastSavedTime(), graph.linkedMonitors(), active.frames(), active.texts(), active.canvasId(), canvases);
    }

    private static List<GraphPortData> preserveLinkedPorts(List<GraphPortData> discovered, GraphNodeData node, List<GraphLinkData> links) {
        List<GraphPortData> ports = new ArrayList<>(discovered);
        for (GraphLinkData link : links) {
            if (link.sourceNodeId().equals(node.nodeId()) && missingPort(ports, link.sourcePortId())) {
                GraphPortData old = node.findPort(link.sourcePortId());
                if (old != null) ports.add(old);
            }
            if (link.targetNodeId().equals(node.nodeId()) && missingPort(ports, link.targetPortId())) {
                GraphPortData old = node.findPort(link.targetPortId());
                if (old != null) ports.add(old);
            }
        }
        return ports;
    }

    private static boolean missingPort(List<GraphPortData> ports, String portId) {
        for (GraphPortData port : ports) {
            if (port.portId().equals(portId)) return false;
        }
        return true;
    }

    public List<ScannedBlockData> scan() { return scan; }
    public Map<String, ScannedBlockData> scanById() { return scanById; }
    public LogisticsGraphData graph() { return graph; }
    public NetworkPowerData power() { return power; }
    public boolean isRouteActive(String linkId) { return activeRouteIds.contains(linkId); }
    public void setActiveRouteIds(java.util.List<String> ids) { this.activeRouteIds = ids == null ? java.util.Set.of() : new java.util.HashSet<>(ids); }
    public long lastScanTime() { return lastScanTime; }
    public boolean isDirty() { return dirty; }
    public String networkId() { return networkId; }
    public String networkName() { return networkName; }
    public String accessMode() { return accessMode; }
    public List<String> editorWhitelist() { return editorWhitelist; }
    public List<Integer> powerSupplyHistory() { return powerSupplyHistory; }
    public List<Integer> powerDemandHistory() { return powerDemandHistory; }
    public List<Short> supply30m() { return supply30m; }
    public List<Short> demand30m() { return demand30m; }
    public List<Short> supply1h() { return supply1h; }
    public List<Short> demand1h() { return demand1h; }
    public List<Short> supply12h() { return supply12h; }
    public List<Short> demand12h() { return demand12h; }
    public List<Short> supply1d() { return supply1d; }
    public List<Short> demand1d() { return demand1d; }
    public List<Short> supplyAllTime() { return supplyAllTime; }
    public List<Short> demandAllTime() { return demandAllTime; }
    public Map<String, List<Integer>> linkThroughput() { return linkThroughput; }
    public boolean canEdit() { return canEdit; }

    public void setNetworkState(String networkId, String networkName, String accessMode, List<String> editorWhitelist, boolean canEdit) {
        this.networkId = networkId == null ? "" : networkId;
        this.networkName = networkName == null ? "" : networkName;
        this.accessMode = accessMode == null ? "shared" : accessMode;
        this.editorWhitelist = editorWhitelist == null ? List.of() : List.copyOf(editorWhitelist);
        this.canEdit = canEdit;
        ClientKnownNetworks.remember(this.networkId, this.networkName);
    }

    public void setPowerHistory(List<Integer> supply, List<Integer> demand) {
        this.powerSupplyHistory = supply == null ? List.of() : List.copyOf(supply);
        this.powerDemandHistory = demand == null ? List.of() : List.copyOf(demand);
    }

    public void setLinkThroughput(Map<String, List<Integer>> throughput) {
        this.linkThroughput = throughput == null ? Map.of() : Map.copyOf(throughput);
    }

    public double linkAvgPerMinute(String linkId) {
        List<Integer> history = linkThroughput.get(linkId);
        if (history == null || history.isEmpty()) return -1.0;
        long sum = 0;
        for (int value : history) sum += value;
        int interval = Math.max(1, net.doole.doolestools.config.ModServerConfig.LFM_TICK_INTERVAL.get());
        double samplesPerMinute = 1200.0 / interval;
        return (sum / (double) history.size()) * samplesPerMinute;
    }

    private static List<Short> safeShorts(List<Short> in) {
        return in == null ? List.of() : List.copyOf(in);
    }

    // linked peer computers - positions and their network IDs, received from server
    private List<net.minecraft.core.BlockPos> linkedPeerPositions = List.of();
    private List<String> linkedPeerNetworkIds = List.of();

    public void setLinkedPeers(List<net.minecraft.core.BlockPos> positions, List<String> ids) {
        this.linkedPeerPositions = positions == null ? List.of() : List.copyOf(positions);
        this.linkedPeerNetworkIds = ids == null ? List.of() : List.copyOf(ids);
    }

    public List<net.minecraft.core.BlockPos> linkedPeerPositions() { return linkedPeerPositions; }
    public List<String> linkedPeerNetworkIds() { return linkedPeerNetworkIds; }

    public void setTimescaleHistory(List<Short> s30, List<Short> d30, List<Short> s1h, List<Short> d1h,
                                    List<Short> s12h, List<Short> d12h, List<Short> s1d, List<Short> d1d,
                                    List<Short> sAll, List<Short> dAll) {
        this.supply30m = safeShorts(s30);
        this.demand30m = safeShorts(d30);
        this.supply1h = safeShorts(s1h);
        this.demand1h = safeShorts(d1h);
        this.supply12h = safeShorts(s12h);
        this.demand12h = safeShorts(d12h);
        this.supply1d = safeShorts(s1d);
        this.demand1d = safeShorts(d1d);
        this.supplyAllTime = safeShorts(sAll);
        this.demandAllTime = safeShorts(dAll);
    }

    public void setGraph(LogisticsGraphData newGraph) {
        if (!canEdit) return;
        this.graph = newGraph;
        this.dirty = true;
        graphRevision++;
    }

    public void markSaved() {
        this.dirty = false;
    }

    public ScannedBlockData selectedScanned() {
        return selectedScannedId == null ? null : scanById.get(selectedScannedId);
    }

    public GraphNodeData selectedNode() {
        return selectedNodeId == null ? null : graph.findNode(selectedNodeId);
    }

    public ScannedBlockData scannedFor(GraphNodeData node) {
        return node == null ? null : scanById.get(node.scannedBlockId());
    }

    public List<ScannedBlockData> filteredScan() {
        List<ScannedBlockData> out = new ArrayList<>();
        for (ScannedBlockData s : scan) {
            if (networkMatches(s) && matches(s)) out.add(s);
        }
        return out;
    }

    public List<NetworkTab> scanNetworkTabs() {
        java.util.Map<String, String> tabs = new java.util.LinkedHashMap<>();
        for (ScannedBlockData s : scan) {
            String id = normalizeNetworkId(s.networkId());
            tabs.putIfAbsent(id, networkLabel(s, id));
        }
        List<NetworkTab> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : tabs.entrySet()) out.add(new NetworkTab(entry.getKey(), entry.getValue()));
        return out;
    }

    public void selectNextScanNetwork() {
        List<NetworkTab> tabs = scanNetworkTabs();
        if (tabs.isEmpty()) { selectedScanNetworkId = ""; return; }
        int idx = 0;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(selectedScanNetworkId)) { idx = i; break; }
        }
        selectedScanNetworkId = tabs.get((idx + 1) % tabs.size()).id();
    }

    public String selectedScanNetworkLabel() {
        for (NetworkTab tab : scanNetworkTabs()) if (tab.id().equals(selectedScanNetworkId)) return tab.name();
        return selectedScanNetworkId == null || selectedScanNetworkId.isBlank() ? "NETWORK" : selectedScanNetworkId;
    }

    public record NetworkTab(String id, String name) {}

    private boolean networkMatches(ScannedBlockData s) {
        if (selectedScanNetworkId == null || selectedScanNetworkId.isBlank()) return true;
        return selectedScanNetworkId.equals(normalizeNetworkId(s.networkId()));
    }

    private boolean hasNetwork(String id) {
        if (id == null || id.isBlank()) return true;
        for (ScannedBlockData s : scan) if (id.equals(normalizeNetworkId(s.networkId()))) return true;
        return false;
    }

    private String firstNetworkId() {
        return scan.isEmpty() ? "" : normalizeNetworkId(scan.get(0).networkId());
    }

    private static String normalizeNetworkId(String id) {
        return id == null ? "" : id;
    }

    private static String networkLabel(ScannedBlockData s, String id) {
        if (s.networkName() != null && !s.networkName().isBlank()) return s.networkName();
        return id == null || id.isBlank() ? "LOCAL" : id;
    }

    private boolean matches(ScannedBlockData s) {
        return switch (filter) {
            case ALL -> true;
            case STORAGE -> s.isStorageLike();
            case MACHINES -> s.isMachineLike();
            case TRANSPORT -> s.type() == net.doole.doolestools.logistics.ScannedType.TRANSPORT;
            case WARNINGS -> s.hasWarnings();
            case UNKNOWN -> s.isUnknown();
        };
    }

    // --- Graph editing convenience ---

    public void addNodeForSelectedScan() {
        int count = graph.activeCanvas().nodes().size();
        int spawnX = (int) (-panX / zoom) + 30 + (count % 4) * 16;
        int spawnY = (int) (-panY / zoom) + 30 + (count % 4) * 16;
        addNodeForSelectedScanAt(spawnX, spawnY);
    }

    public void addNodeForSelectedScanAt(int canvasX, int canvasY) {
        ScannedBlockData s = selectedScanned();
        if (s == null) return;
        LogisticsGraphData updated = LogisticsGraph.addNode(graph, s, canvasX, canvasY);
        setGraph(updated);
        // Select the new node if one was added.
        for (GraphNodeData n : updated.activeCanvas().nodes()) {
            if (n.scannedBlockId().equals(s.id())) selectSingleNode(n.nodeId());
        }
    }

    /**
     * Drops a node for each scanned block, laid out left-to-right starting at the drop point. Each node
     * is offset by its width + 8px gap. The last successfully added node becomes the selection.
     */
    public void addNodesForScans(List<ScannedBlockData> scans, int canvasX, int canvasY) {
        if (scans == null || scans.isEmpty()) return;
        LogisticsGraphData updated = graph;
        int cursorX = canvasX;
        String lastId = null;
        for (ScannedBlockData s : scans) {
            if (s == null) continue;
            LogisticsGraphData next = LogisticsGraph.addNode(updated, s, cursorX, canvasY);
            if (next != updated) {
                updated = next;
                cursorX += GraphNodeData.DEFAULT_WIDTH + 8;
            }
            for (GraphNodeData n : updated.activeCanvas().nodes()) {
                if (n.scannedBlockId().equals(s.id())) lastId = n.nodeId();
            }
        }
        if (updated == graph) return;
        setGraph(updated);
        if (lastId != null) selectSingleNode(lastId);
    }

    /** Filter: one input, two outputs — matched items out of "Out", the rest out of "Reject". */
    public void addFilterNode(int canvasX, int canvasY) {
        addRoutingNode(net.doole.doolestools.logistics.NodeType.FILTER, "Filter", "", canvasX, canvasY,
                List.of(itemPort("in", net.doole.doolestools.logistics.PortDirection.IN, "In"),
                        itemPort("out", net.doole.doolestools.logistics.PortDirection.OUT, "Out"),
                        itemPort("reject", net.doole.doolestools.logistics.PortDirection.OUT, "Reject")));
    }

    /** Splitter: one input distributed round-robin across two outputs. */
    public void addSplitterNode(int canvasX, int canvasY) {
        addRoutingNode(net.doole.doolestools.logistics.NodeType.SPLITTER, "Splitter",
                net.doole.doolestools.logistics.FilterSettings.empty().nextRouting().serialize(), canvasX, canvasY,
                List.of(itemPort("in", net.doole.doolestools.logistics.PortDirection.IN, "In"),
                        itemPort("out_a", net.doole.doolestools.logistics.PortDirection.OUT, "Out A"),
                        itemPort("out_b", net.doole.doolestools.logistics.PortDirection.OUT, "Out B")));
    }

    /** Combine: merges two inputs into one output. */
    public void addCombineNode(int canvasX, int canvasY) {
        addRoutingNode(net.doole.doolestools.logistics.NodeType.COMBINE, "Combine",
                net.doole.doolestools.logistics.FilterSettings.empty().serialize(), canvasX, canvasY,
                List.of(itemPort("in_a", net.doole.doolestools.logistics.PortDirection.IN, "In A"),
                        itemPort("in_b", net.doole.doolestools.logistics.PortDirection.IN, "In B"),
                        itemPort("out", net.doole.doolestools.logistics.PortDirection.OUT, "Out")));
    }

    /** Channel: stamps its selected colour channel onto the items passing through. */
    public void addChannelNode(int canvasX, int canvasY) {
        addRoutingNode(net.doole.doolestools.logistics.NodeType.CHANNEL, "Channel",
                net.doole.doolestools.logistics.FilterSettings.empty().serialize(), canvasX, canvasY,
                List.of(itemPort("in", net.doole.doolestools.logistics.PortDirection.IN, "In"),
                        itemPort("out", net.doole.doolestools.logistics.PortDirection.OUT, "Out")));
    }

    private static GraphPortData itemPort(String id, net.doole.doolestools.logistics.PortDirection dir, String label) {
        return new GraphPortData(id, dir, net.doole.doolestools.logistics.PortKind.ITEM, label);
    }

    private void addRoutingNode(net.doole.doolestools.logistics.NodeType type, String label, String notes,
                                int canvasX, int canvasY, List<GraphPortData> ports) {
        String id = LogisticsGraph.newNodeId();
        GraphNodeData node = new GraphNodeData(id, "", label, type,
                canvasX, canvasY, GraphNodeData.DEFAULT_WIDTH, GraphNodeData.DEFAULT_HEIGHT, ports, false, notes);
        setGraph(LogisticsGraph.addRawNode(graph, node));
        selectSingleNode(id);
    }

    /**
     * Creates an instanced copy of each selected node (tied to the same scanned block, marked instanced=true).
     * Works for all node types. The new nodes are placed 18px offset from the originals and selected.
     */
    public void instanceSelectedNodes() {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>(selectedNodeIds);
        if (ids.isEmpty() && selectedNodeId != null) ids.add(selectedNodeId);
        if (ids.isEmpty()) return;
        LogisticsGraphData g = graph;
        List<String> newIds = new ArrayList<>();
        for (String id : ids) {
            GraphNodeData n = g.findNode(id);
            if (n == null) continue;
            String newId = LogisticsGraph.newNodeId();
            g = LogisticsGraph.addRawNode(g, new GraphNodeData(newId, n.scannedBlockId(), n.displayName(), n.type(),
                    n.x() + 18, n.y() + 18, n.width(), n.height(), n.ports(), n.collapsed(), n.notes(), true));
            newIds.add(newId);
        }
        if (newIds.isEmpty()) return;
        setGraph(g);
        clearSelection();
        selectedNodeIds.addAll(newIds);
        selectedNodeId = newIds.get(newIds.size() - 1);
    }

    /** Duplicates the selected node(s), keeping all settings, and selects the copies. */
    public void duplicateSelectedNodes() {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>(selectedNodeIds);
        if (ids.isEmpty() && selectedNodeId != null) ids.add(selectedNodeId);
        if (ids.isEmpty()) return;
        LogisticsGraphData g = graph;
        List<String> newIds = new ArrayList<>();
        for (String id : ids) {
            GraphNodeData n = g.findNode(id);
            if (n == null) continue;
            String newId = LogisticsGraph.newNodeId();
            g = LogisticsGraph.addRawNode(g, new GraphNodeData(newId, n.scannedBlockId(), n.displayName(), n.type(),
                    n.x() + 18, n.y() + 18, n.width(), n.height(), n.ports(), n.collapsed(), n.notes()));
            newIds.add(newId);
        }
        if (newIds.isEmpty()) return;
        setGraph(g);
        clearSelection();
        selectedNodeIds.addAll(newIds);
        selectedNodeId = newIds.get(newIds.size() - 1);
    }

    /** If nodes are selected, drops a frame sized to wrap them (with padding). Returns false if none. */
    public boolean addFrameAroundSelection() {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>(selectedNodeIds);
        if (ids.isEmpty() && selectedNodeId != null) ids.add(selectedNodeId);
        if (ids.isEmpty()) return false;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (String id : ids) {
            GraphNodeData n = graph.findNode(id);
            if (n == null) continue;
            minX = Math.min(minX, n.x());
            minY = Math.min(minY, n.y());
            maxX = Math.max(maxX, n.x() + n.width());
            maxY = Math.max(maxY, n.y() + n.height());
        }
        if (minX == Integer.MAX_VALUE) return false;
        int pad = 12;
        int titleH = 13;
        var updated = LogisticsGraph.addFrame(graph, minX - pad, minY - pad - titleH,
                (maxX - minX) + pad * 2, (maxY - minY) + pad * 2 + titleH, "Frame");
        setGraph(updated);
        var frames = updated.activeCanvas().frames();
        if (!frames.isEmpty()) selectedFrameId = frames.get(frames.size() - 1).frameId();
        return true;
    }

    public void deleteSelectedNode() {
        if (!selectedNodeIds.isEmpty()) {
            LogisticsGraphData g = graph;
            for (String id : selectedNodeIds) g = LogisticsGraph.removeNode(g, id);
            setGraph(g);
            selectedNodeIds.clear();
            selectedNodeId = null;
            return;
        }
        if (selectedNodeId == null) return;
        setGraph(LogisticsGraph.removeNode(graph, selectedNodeId));
        selectedNodeId = null;
    }

    /** Deletes a single node by id (used by right-click delete). */
    public void deleteNode(String nodeId) {
        if (nodeId == null) return;
        setGraph(LogisticsGraph.removeNode(graph, nodeId));
        selectedNodeIds.remove(nodeId);
        if (nodeId.equals(selectedNodeId)) selectedNodeId = null;
    }

    public void selectSingleNode(String nodeId) {
        selectedNodeId = nodeId;
        selectedFrameId = null;
        selectedTextId = null;
        selectedNodeIds.clear();
        if (nodeId != null) selectedNodeIds.add(nodeId);
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedFrameId = null;
        selectedTextId = null;
        selectedNodeIds.clear();
    }

    public boolean isNodeSelected(String nodeId) {
        return nodeId != null && (nodeId.equals(selectedNodeId) || selectedNodeIds.contains(nodeId));
    }

    // --- Frames ---

    public void addFrame(int x, int y) {
        var updated = LogisticsGraph.addFrame(graph, x, y,
                net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_WIDTH,
                net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_HEIGHT, "Frame");
        setGraph(updated);
        // Select the newest frame.
        var frames = updated.activeCanvas().frames();
        if (!frames.isEmpty()) {
            clearSelection();
            selectedFrameId = frames.get(frames.size() - 1).frameId();
        }
    }

    public net.doole.doolestools.logistics.data.GraphFrameData selectedFrame() {
        return selectedFrameId == null ? null : graph.findFrame(selectedFrameId);
    }

    public void deleteFrame(String frameId) {
        if (frameId == null) return;
        setGraph(LogisticsGraph.removeFrame(graph, frameId));
        if (frameId.equals(selectedFrameId)) selectedFrameId = null;
    }

    // --- Text labels ---

    public void addTextLabel(int x, int y) {
        var updated = LogisticsGraph.addText(graph, x, y, "Label");
        setGraph(updated);
        var texts = updated.activeCanvas().texts();
        if (!texts.isEmpty()) {
            clearSelection();
            selectedTextId = texts.get(texts.size() - 1).textId();
        }
    }

    public net.doole.doolestools.logistics.data.GraphTextData selectedText() {
        return selectedTextId == null ? null : graph.findText(selectedTextId);
    }

    public void deleteText(String textId) {
        if (textId == null) return;
        setGraph(LogisticsGraph.removeText(graph, textId));
        if (textId.equals(selectedTextId)) selectedTextId = null;
    }

    public void autoArrange() {
        setGraph(LogisticsGraph.autoArrange(graph));
    }

    public void tryLink(String targetNodeId) {
        if (linkSourceId != null && targetNodeId != null && !linkSourceId.equals(targetNodeId)) {
            setGraph(LogisticsGraph.addLink(graph, linkSourceId, targetNodeId, LinkType.ITEMS));
        }
        linkSourceId = null;
    }

    public void startPortLink(String sourceNodeId, String sourcePortId, double mx, double my) {
        draggingSourceNodeId = sourceNodeId;
        draggingSourcePortId = sourcePortId;
        draggingLinkId = null;
        dragMouseX = mx;
        dragMouseY = my;
    }

    public void startLinkRetarget(String linkId, double mx, double my) {
        draggingLinkId = linkId;
        draggingSourceNodeId = null;
        draggingSourcePortId = null;
        dragMouseX = mx;
        dragMouseY = my;
    }

    public boolean isDraggingPortLink() {
        return draggingSourceNodeId != null && draggingSourcePortId != null;
    }

    public boolean isDraggingLinkRetarget() {
        return draggingLinkId != null;
    }

    public void updateLinkDrag(double mx, double my) {
        dragMouseX = mx;
        dragMouseY = my;
    }

    public void completePortLink(String targetNodeId, String targetPortId) {
        if (isDraggingPortLink() && targetNodeId != null && targetPortId != null) {
            if (selectedNodeIds.size() > 1 && selectedNodeIds.contains(draggingSourceNodeId)) {
                setGraph(addSelectedLinksToTarget(targetNodeId, targetPortId));
            } else {
                setGraph(LogisticsGraph.addLink(graph, draggingSourceNodeId, draggingSourcePortId, targetNodeId, targetPortId));
            }
        } else if (isDraggingLinkRetarget() && targetNodeId != null && targetPortId != null) {
            setGraph(LogisticsGraph.retargetLinkTarget(graph, draggingLinkId, targetNodeId, targetPortId));
        } else if (isDraggingLinkRetarget()) {
            setGraph(LogisticsGraph.removeLink(graph, draggingLinkId));
        }
        clearPortDrag();
    }

    private LogisticsGraphData addSelectedLinksToTarget(String targetNodeId, String targetPortId) {
        // Use the kind of the dragged source port to prefer like-kind ports on the other selected nodes.
        GraphNodeData dragSource = graph.findNode(draggingSourceNodeId);
        GraphPortData dragPort = dragSource == null ? null : dragSource.findPort(draggingSourcePortId);
        net.doole.doolestools.logistics.PortKind kind = dragPort == null ? null : dragPort.kind();
        return LogisticsGraph.addMultiLink(graph, new ArrayList<>(selectedNodeIds), kind, targetNodeId, targetPortId);
    }

    public void clearPortDrag() {
        draggingSourceNodeId = null;
        draggingSourcePortId = null;
        draggingLinkId = null;
    }

    /** Status colour for a scanned block's worst warning. */
    public int statusColorFor(ScannedBlockData s) {
        int color = DUTheme.OK;
        for (var w : s.warnings()) {
            switch (w.severity()) {
                case ERROR -> { return DUTheme.ERROR; }
                case WARNING -> color = DUTheme.WARN;
                default -> { }
            }
        }
        return color;
    }
}
