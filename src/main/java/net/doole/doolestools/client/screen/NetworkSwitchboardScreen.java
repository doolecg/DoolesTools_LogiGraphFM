package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.ClientKnownNetworks;
import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.gui.CanvasAdapter;
import net.doole.doolestools.client.gui.CanvasViewState;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.GraphCanvasWidget;
import net.doole.doolestools.client.gui.TerminalButton;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.doole.doolestools.menu.NetworkSwitchboardMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NetworkSwitchboardScreen extends AbstractContainerScreen<NetworkSwitchboardMenu> {
    private static final int NODE_W = 118;
    private static final int NODE_H = 48;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");

    private final List<SwitchboardLinkData> links = new ArrayList<>();
    private final Map<String, int[]> nodePositions = new java.util.HashMap<>();
    private String selectedNetworkId = "";
    private int selectedLink = -1;
    private String linkSourceId = "";
    private String draggingNetworkId = "";
    private String clickNetworkId = "";
    private boolean draggedNetwork;
    private boolean panning;
    private double lastMouseX, lastMouseY;

    private final CanvasViewState canvasView = new CanvasViewState();
    private GraphCanvasWidget canvasWidget;

    private TerminalButton itemButton;
    private TerminalButton fluidButton;
    private TerminalButton energyButton;
    private TerminalButton priorityButton;
    private TerminalButton saveButton;

    private int leftX, leftY, leftW, leftH;
    private int canvasX, canvasY, canvasW, canvasH;
    private int rightX, rightY, rightW, rightH;

    public NetworkSwitchboardScreen(NetworkSwitchboardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 600, 400);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = -9999;
        titleLabelY = -9999;
        inventoryLabelX = -9999;
        inventoryLabelY = -9999;

        leftX = leftPos + 10;
        leftY = topPos + 44;
        leftW = 140;
        leftH = imageHeight - 78;
        rightW = 154;
        rightX = leftPos + imageWidth - rightW - 10;
        rightY = leftY;
        rightH = leftH;
        canvasX = leftX + leftW + 8;
        canvasY = leftY;
        canvasW = rightX - canvasX - 8;
        canvasH = leftH;

        canvasWidget = new GraphCanvasWidget(new SwitchboardCanvasAdapter(), canvasView, canvasX, canvasY + 15, canvasW, canvasH - 15);

        int bx = rightX + 8;
        int by = rightY + rightH - 80;
        int bw = (rightW - 22) / 2;
        itemButton = new TerminalButton(bx, by, bw, 14, Component.literal("ITEM"), this::toggleItems).accent(DUTheme.OK);
        fluidButton = new TerminalButton(bx + bw + 6, by, bw, 14, Component.literal("FLUID"), this::toggleFluids).accent(DUTheme.PROGRESS_BLUE);
        energyButton = new TerminalButton(bx, by + 18, bw, 14, Component.literal("FE"), this::toggleEnergy).accent(DUTheme.WARN);
        priorityButton = new TerminalButton(bx + bw + 6, by + 18, bw, 14, Component.literal("PRI+"), this::bumpPriority).accent(DUTheme.SELECTED);
        saveButton = new TerminalButton(bx, by + 42, rightW - 16, 16, Component.literal("SAVE SWITCHBOARD"), () -> ClientNetworkSender.saveSwitchboard(menu.pos(), links, positionData())).accent(DUTheme.TEXT_GREEN);
        addRenderableWidget(itemButton);
        addRenderableWidget(fluidButton);
        addRenderableWidget(energyButton);
        addRenderableWidget(priorityButton);
        addRenderableWidget(saveButton);

        ClientNetworkSender.requestKnownNetworks();
        ClientNetworkSender.requestSwitchboardState(menu.pos());
    }

    public void applyState(List<SwitchboardLinkData> newLinks, List<SwitchboardNodePositionData> newPositions) {
        links.clear();
        if (newLinks != null) links.addAll(newLinks);
        nodePositions.clear();
        if (newPositions != null) {
            for (SwitchboardNodePositionData position : newPositions) {
                SwitchboardNodePositionData clean = position.sanitized();
                if (clean.valid()) nodePositions.put(clean.networkId(), clampLogical(clean.x(), clean.y()));
            }
        }
        selectedLink = links.isEmpty() ? -1 : Math.max(0, Math.min(selectedLink, links.size() - 1));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        syncButtons();
        DUTheme.frame(g, leftPos, topPos, imageWidth, imageHeight);
        renderHeader(g);
        renderNetworkList(g);
        panel(g, canvasX, canvasY, canvasW, canvasH, "SWITCHING CANVAS");
        canvasWidget.render(g, font, mouseX, mouseY);
        renderInfo(g);
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        DUTheme.glowText(g, font, "LOGIGRAPH SWITCHBOARD", leftPos + 14, topPos + 12, DUTheme.TEXT_GREEN);
        g.text(font, "LFM CROSS-NETWORK BRIDGE MATRIX", leftPos + 14, topPos + 25, DUTheme.TEXT_DIM, false);
        g.text(font, LocalTime.now().format(CLOCK).toUpperCase(java.util.Locale.ROOT), leftPos + imageWidth - 74, topPos + 12, DUTheme.TEXT_DIM, false);
        g.text(font, links.size() + " LINKS", leftPos + imageWidth - 74, topPos + 25, DUTheme.TEXT_GREEN_DIM, false);
    }

    private void renderNetworkList(GuiGraphicsExtractor g) {
        panel(g, leftX, leftY, leftW, leftH, "SCANNED NETWORKS");
        List<ClientKnownNetworks.Entry> nets = networks();
        for (int i = 0; i < nets.size(); i++) {
            int y = leftY + 24 + i * 22;
            if (y > leftY + leftH - 20) break;
            ClientKnownNetworks.Entry net = nets.get(i);
            boolean selected = net.id().equals(selectedNetworkId);
            boolean source = net.id().equals(linkSourceId);
            int fill = source ? 0xFF2a2514 : selected ? 0xFF14303a : DUTheme.PANEL_ALT;
            int border = source ? DUTheme.PROGRESS_ORANGE : selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER;
            DUTheme.box(g, leftX + 5, y, leftW - 10, 18, fill, border);
            DUTheme.dot(g, leftX + 9, y + 6, source ? DUTheme.PROGRESS_ORANGE : DUTheme.TEXT_GREEN);
            g.text(font, trim(net.name(), 16), leftX + 18, y + 4, selected || source ? DUTheme.TEXT : DUTheme.TEXT_DIM, false);
        }
        if (nets.isEmpty()) {
            g.text(font, "No loaded networks", leftX + 8, leftY + 30, DUTheme.TEXT_DIM, false);
        }
        g.text(font, "Click two nodes to bridge", leftX + 8, leftY + leftH - 24, DUTheme.TEXT_DIM, false);
        g.text(font, "Right-click link to cut", leftX + 8, leftY + leftH - 12, DUTheme.TEXT_DIM, false);
    }

    private void renderInfo(GuiGraphicsExtractor g) {
        panel(g, rightX, rightY, rightW, rightH, "NETWORK INFO");
        int y = rightY + 24;
        if (selectedLink >= 0 && selectedLink < links.size()) {
            SwitchboardLinkData link = links.get(selectedLink);
            section(g, "ACTIVE BRIDGE", rightX + 8, y); y += 16;
            y = labelValue(g, "FROM", nameFor(link.sourceNetworkId()), y);
            y = labelValue(g, "TO", nameFor(link.targetNetworkId()), y);
            y += 4;
            y = labelValue(g, "ITEMS", onOff(link.items()), y);
            y = labelValue(g, "FLUIDS", onOff(link.fluids()), y);
            y = labelValue(g, "ENERGY", onOff(link.energy()), y);
            y = labelValue(g, "PRIORITY", String.valueOf(link.priority()), y);
            g.text(font, "Right-click bridge to delete", rightX + 8, y + 6, DUTheme.TEXT_DIM, false);
            return;
        }
        if (!selectedNetworkId.isBlank()) {
            section(g, linkSourceId.isBlank() ? "SELECTED NETWORK" : "SOURCE ARMED", rightX + 8, y); y += 16;
            y = labelValue(g, "NAME", nameFor(selectedNetworkId), y);
            y = labelValue(g, "ID", selectedNetworkId, y);
            y += 4;
            g.text(font, linkSourceId.isBlank() ? "Click it again to arm," : "Click another network", rightX + 8, y, DUTheme.TEXT_DIM, false); y += 11;
            g.text(font, linkSourceId.isBlank() ? "then click target." : "to create a bridge.", rightX + 8, y, DUTheme.TEXT_DIM, false);
            return;
        }
        section(g, "NO SELECTION", rightX + 8, y); y += 16;
        g.text(font, "Select a network node", rightX + 8, y, DUTheme.TEXT_DIM, false); y += 11;
        g.text(font, "or bridge on canvas.", rightX + 8, y, DUTheme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        boolean right = event.button() == 1;
        boolean middle = event.button() == 2;
        if (middle && canvasWidget.contains(mx, my)) {
            panning = true;
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        String listNode = listNodeAt(mx, my);
        if (!listNode.isBlank()) {
            selectedNetworkId = listNode;
            selectedLink = -1;
            if (!right) handleNodeConnect(listNode);
            return true;
        }
        String node = canvasWidget.adapterNodeAt(mx, my);
        if (!node.isBlank()) {
            selectedNetworkId = node;
            selectedLink = -1;
            if (!right) {
                draggingNetworkId = node;
                clickNetworkId = node;
                draggedNetwork = false;
                lastMouseX = mx;
                lastMouseY = my;
            }
            return true;
        }
        String edgeId = canvasWidget.adapterEdgeAt(mx, my);
        int link = edgeIdToIndex(edgeId);
        if (link >= 0) {
            if (right) {
                links.remove(link);
                selectedLink = -1;
            } else {
                selectedLink = link;
                selectedNetworkId = "";
                linkSourceId = "";
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!draggingNetworkId.isBlank()) {
            double mx = event.x();
            double my = event.y();
            int cdx = (int) Math.round((mx - lastMouseX) / canvasView.zoom);
            int cdy = (int) Math.round((my - lastMouseY) / canvasView.zoom);
            if (cdx != 0 || cdy != 0) {
                int[] current = logicalPos(draggingNetworkId);
                nodePositions.put(draggingNetworkId, clampLogical(current[0] + cdx, current[1] + cdy));
                draggedNetwork = true;
                lastMouseX = mx;
                lastMouseY = my;
            }
            return true;
        }
        if (panning) {
            double mx = event.x();
            double my = event.y();
            canvasView.panX += (float) (mx - lastMouseX);
            canvasView.panY += (float) (my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!draggingNetworkId.isBlank()) {
            if (!draggedNetwork && clickNetworkId.equals(draggingNetworkId)) {
                handleNodeConnect(draggingNetworkId);
            }
            draggingNetworkId = "";
            clickNetworkId = "";
            draggedNetwork = false;
            return true;
        }
        if (panning) {
            panning = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double sx, double sy) {
        if (!canvasWidget.contains(rawX, rawY)) return super.mouseScrolled(rawX, rawY, sx, sy);
        float oldZoom = canvasView.zoom;
        float nextZoom = Math.max(0.5f, Math.min(2.5f, canvasView.zoom + (float) Math.signum(sy) * 0.15f));
        if (nextZoom == oldZoom) return true;
        double logicalX = (rawX - canvasX - canvasView.panX) / oldZoom;
        double logicalY = (rawY - canvasY - canvasView.panY) / oldZoom;
        canvasView.zoom = nextZoom;
        canvasView.panX = (float) (rawX - canvasX - logicalX * canvasView.zoom);
        canvasView.panY = (float) (rawY - canvasY - logicalY * canvasView.zoom);
        return true;
    }

    private void handleNodeConnect(String node) {
        if (linkSourceId.isBlank()) {
            linkSourceId = node;
            return;
        }
        if (linkSourceId.equals(node)) {
            linkSourceId = "";
            return;
        }
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).connects(linkSourceId, node)) {
                selectedLink = i;
                linkSourceId = "";
                return;
            }
        }
        links.add(new SwitchboardLinkData(linkSourceId, node, true, true, true, 0));
        selectedLink = links.size() - 1;
        selectedNetworkId = "";
        linkSourceId = "";
    }

    private void toggleItems() { replaceSelected(link -> new SwitchboardLinkData(link.sourceNetworkId(), link.targetNetworkId(), !link.items(), link.fluids(), link.energy(), link.priority())); }
    private void toggleFluids() { replaceSelected(link -> new SwitchboardLinkData(link.sourceNetworkId(), link.targetNetworkId(), link.items(), !link.fluids(), link.energy(), link.priority())); }
    private void toggleEnergy() { replaceSelected(link -> new SwitchboardLinkData(link.sourceNetworkId(), link.targetNetworkId(), link.items(), link.fluids(), !link.energy(), link.priority())); }
    private void bumpPriority() { replaceSelected(link -> new SwitchboardLinkData(link.sourceNetworkId(), link.targetNetworkId(), link.items(), link.fluids(), link.energy(), (link.priority() + 1) % 10)); }

    private void replaceSelected(java.util.function.Function<SwitchboardLinkData, SwitchboardLinkData> updater) {
        if (selectedLink < 0 || selectedLink >= links.size()) return;
        links.set(selectedLink, updater.apply(links.get(selectedLink)));
    }

    private void syncButtons() {
        boolean hasLink = selectedLink >= 0 && selectedLink < links.size();
        if (itemButton == null) return;
        itemButton.active = hasLink;
        fluidButton.active = hasLink;
        energyButton.active = hasLink;
        priorityButton.active = hasLink;
        saveButton.active = true;
        if (!hasLink) {
            itemButton.setToggled(false);
            fluidButton.setToggled(false);
            energyButton.setToggled(false);
            priorityButton.setMessage(Component.literal("PRI+"));
            return;
        }
        SwitchboardLinkData link = links.get(selectedLink);
        itemButton.setToggled(link.items());
        fluidButton.setToggled(link.fluids());
        energyButton.setToggled(link.energy());
        priorityButton.setMessage(Component.literal("P" + link.priority() + "+"));
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        DUTheme.panelWithHeader(g, font, x, y, w, h, title);
    }

    private int labelValue(GuiGraphicsExtractor g, String label, String value, int y) {
        g.text(font, label, rightX + 8, y, DUTheme.TEXT_GREEN_DIM, false);
        g.text(font, trim(value, 18), rightX + 50, y, DUTheme.TEXT, false);
        return y + 12;
    }

    private void section(GuiGraphicsExtractor g, String label, int x, int y) {
        g.text(font, label, x, y, DUTheme.SELECTED, false);
        g.fill(x, y + 10, rightX + rightW - 8, y + 11, DUTheme.PANEL_BORDER);
    }

    private String listNodeAt(double mx, double my) {
        List<ClientKnownNetworks.Entry> nets = networks();
        for (int i = 0; i < nets.size(); i++) {
            int y = leftY + 24 + i * 22;
            if (y > leftY + leftH - 20) break;
            if (mx >= leftX + 5 && mx <= leftX + leftW - 5 && my >= y && my <= y + 18) return nets.get(i).id();
        }
        return "";
    }

    /** Maps an edge id (built as "sourceId->targetId" by the adapter) back to the link list index. */
    private int edgeIdToIndex(String edgeId) {
        if (edgeId.isBlank()) return -1;
        for (int i = 0; i < links.size(); i++) {
            SwitchboardLinkData l = links.get(i);
            if (edgeId.equals(l.sourceNetworkId() + "->" + l.targetNetworkId())) return i;
        }
        return -1;
    }

    private int[] clampLogical(int x, int y) {
        int minX = 8, maxX = Math.max(minX, canvasW - NODE_W - 8);
        int minY = 20, maxY = Math.max(minY, canvasH - NODE_H - 8);
        return new int[]{ Math.max(minX, Math.min(maxX, x)), Math.max(minY, Math.min(maxY, y)) };
    }

    private Map<String, int[]> logicalNodeMap() {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (ClientKnownNetworks.Entry net : networks()) out.put(net.id(), null);
        for (SwitchboardLinkData link : links) {
            out.putIfAbsent(link.sourceNetworkId(), null);
            out.putIfAbsent(link.targetNetworkId(), null);
        }
        int count = out.size();
        int cols = count <= 2 ? 1 : Math.max(1, canvasW / (NODE_W + 32));
        int rows = Math.max(1, (count + cols - 1) / cols);
        int startY = 34;
        int usableH = Math.max(1, canvasH - 52);
        int gapY = rows <= 1 ? 0 : Math.max(18, (usableH - rows * NODE_H) / (rows - 1));
        int i = 0;
        for (String id : new ArrayList<>(out.keySet())) {
            int[] saved = nodePositions.get(id);
            if (saved != null) {
                out.put(id, clampLogical(saved[0], saved[1]));
            } else {
                int row = i / cols, col = i % cols;
                out.put(id, clampLogical(22 + col * (NODE_W + 34), startY + row * (NODE_H + gapY)));
            }
            i++;
        }
        return out;
    }

    private int[] logicalPos(String id) {
        int[] pos = logicalNodeMap().get(id);
        return pos != null ? pos : new int[]{ 22, 34 };
    }

    private List<SwitchboardNodePositionData> positionData() {
        List<SwitchboardNodePositionData> out = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : logicalNodeMap().entrySet()) {
            int[] box = entry.getValue();
            if (box != null) out.add(new SwitchboardNodePositionData(entry.getKey(), box[0], box[1]));
        }
        return out;
    }

    private int linkColor(SwitchboardLinkData link, boolean selected) {
        if (selected) return DUTheme.SELECTED;
        if (link.energy()) return DUTheme.WARN;
        if (link.fluids()) return DUTheme.PROGRESS_BLUE;
        if (link.items()) return DUTheme.OK;
        return DUTheme.DISABLED;
    }

    private List<ClientKnownNetworks.Entry> networks() { return ClientKnownNetworks.entries(); }

    private String nameFor(String id) {
        for (ClientKnownNetworks.Entry entry : networks()) if (entry.id().equals(id)) return entry.name();
        return id;
    }

    private static String perms(SwitchboardLinkData link) {
        return (link.items() ? "I" : "-") + (link.fluids() ? "F" : "-") + (link.energy() ? "E" : "-") + " P" + link.priority();
    }

    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    /** {@link CanvasAdapter} implementation that exposes switchboard networks as canvas nodes and links as edges. */
    private final class SwitchboardCanvasAdapter implements CanvasAdapter {

        @Override
        public List<CanvasNode> nodes() {
            Map<String, int[]> logical = logicalNodeMap();
            List<CanvasNode> out = new ArrayList<>(logical.size());
            for (Map.Entry<String, int[]> entry : logical.entrySet()) {
                int[] pos = entry.getValue();
                if (pos != null) out.add(new CanvasNode(entry.getKey(), pos[0], pos[1], NODE_W, NODE_H));
            }
            return out;
        }

        @Override
        public List<CanvasEdge> edges() {
            List<CanvasEdge> out = new ArrayList<>(links.size());
            for (int i = 0; i < links.size(); i++) {
                SwitchboardLinkData link = links.get(i);
                boolean sel = i == selectedLink;
                String edgeId = link.sourceNetworkId() + "->" + link.targetNetworkId();
                out.add(new CanvasEdge(edgeId, link.sourceNetworkId(), link.targetNetworkId(), perms(link), linkColor(link, sel), sel));
            }
            return out;
        }

        @Override
        public void onNodeMoved(String id, int logicalX, int logicalY) {
            nodePositions.put(id, clampLogical(logicalX, logicalY));
        }

        @Override
        public void renderNode(GuiGraphicsExtractor g, Font font, CanvasNode node, boolean selected) {
            String id = node.id();
            boolean source = id.equals(linkSourceId);
            int border = source ? DUTheme.PROGRESS_ORANGE : selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER;
            int header = source ? 0xFF332713 : selected ? 0xFF14303a : DUTheme.PANEL_HEADER;
            int x = node.x(), y = node.y();
            DUTheme.box(g, x, y, NODE_W, NODE_H, DUTheme.PANEL_ALT, border);
            g.fill(x + 1, y + 1, x + NODE_W - 1, y + 13, header);
            g.text(font, "NETWORK", x + 5, y + 4, source ? DUTheme.PROGRESS_ORANGE : DUTheme.TEXT_GREEN, false);
            g.text(font, trim(nameFor(id), 18), x + 6, y + 18, DUTheme.TEXT, false);
            g.text(font, trim(id, 20), x + 6, y + 31, DUTheme.TEXT_DIM, false);
            port(g, portInX(node), portInY(node), border);
            port(g, portOutX(node), portOutY(node), border);
        }

        private void port(GuiGraphicsExtractor g, int x, int y, int color) {
            g.fill(x - 2, y - 2, x + 3, y + 3, DUTheme.SCREEN);
            g.fill(x - 1, y - 1, x + 2, y + 2, color);
        }

        @Override
        public String dragSourceId() { return linkSourceId; }

        @Override
        public boolean isSelected(String id) { return id.equals(selectedNetworkId); }
    }
}
