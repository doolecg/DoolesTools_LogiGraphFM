package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.ClientKnownNetworks;
import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.TerminalButton;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.doole.doolestools.menu.NetworkSwitchboardMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NetworkSwitchboardScreen extends AbstractContainerScreen<NetworkSwitchboardMenu> {
    private static final int NODE_W = 118;
    private static final int NODE_H = 48;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");

    private final List<SwitchboardLinkData> links = new ArrayList<>();
    private final Map<String, NodeBox> nodePositions = new HashMap<>();
    private String selectedNetworkId = "";
    private int selectedLink = -1;
    private String linkSourceId = "";
    private String draggingNetworkId = "";
    private String clickNetworkId = "";
    private boolean draggedNetwork;
    private boolean panning;
    private double lastMouseX, lastMouseY;
    private float canvasZoom = 1.0f;
    private float canvasPanX = 0.0f;
    private float canvasPanY = 0.0f;

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
        DUTheme.bezel(g, leftPos, topPos, imageWidth, imageHeight);
        DUTheme.screw(g, leftPos + 8, topPos + 8);
        DUTheme.screw(g, leftPos + imageWidth - 8, topPos + 8);
        DUTheme.screw(g, leftPos + 8, topPos + imageHeight - 8);
        DUTheme.screw(g, leftPos + imageWidth - 8, topPos + imageHeight - 8);
        g.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + imageHeight - 6, DUTheme.SCREEN);
        DUTheme.outline(g, leftPos + 6, topPos + 6, imageWidth - 12, imageHeight - 12, DUTheme.PANEL_BORDER);

        renderHeader(g);
        renderNetworkList(g);
        renderGraph(g, mouseX, mouseY);
        renderInfo(g);
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        DUTheme.glowText(g, font, "LOGIGRAPH SWITCHBOARD", leftPos + 14, topPos + 12, DUTheme.TEXT_GREEN);
        g.text(font, "LFM CROSS-NETWORK BRIDGE MATRIX", leftPos + 14, topPos + 25, DUTheme.TEXT_DIM, false);
        g.text(font, LocalTime.now().format(CLOCK).toUpperCase(Locale.ROOT), leftPos + imageWidth - 74, topPos + 12, DUTheme.TEXT_DIM, false);
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

    private void renderGraph(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        panel(g, canvasX, canvasY, canvasW, canvasH, "SWITCHING CANVAS");
        renderCanvasGrid(g);
        g.enableScissor(canvasX + 1, canvasY + 15, canvasX + canvasW - 1, canvasY + canvasH - 1);
        Map<String, NodeBox> boxes = nodeBoxes();
        for (int i = 0; i < links.size(); i++) {
            SwitchboardLinkData link = links.get(i);
            NodeBox a = boxes.get(link.sourceNetworkId());
            NodeBox b = boxes.get(link.targetNetworkId());
            if (a == null || b == null) continue;
            drawBridge(g, a.outX(), a.outY(), b.inX(), b.inY(), linkColor(link, i == selectedLink), i == selectedLink, perms(link));
        }
        if (!linkSourceId.isBlank()) {
            NodeBox source = boxes.get(linkSourceId);
            if (source != null) drawBridge(g, source.outX(), source.outY(), mouseX, mouseY, DUTheme.PROGRESS_ORANGE, true, "ARMED");
        }
        for (Map.Entry<String, NodeBox> entry : boxes.entrySet()) {
            drawNetworkNode(g, entry.getKey(), entry.getValue());
        }
        if (boxes.isEmpty()) {
            g.centeredText(font, "WAITING FOR NETWORK INDEX", canvasX + canvasW / 2, canvasY + canvasH / 2 - 4, DUTheme.TEXT_DIM);
        }
        g.disableScissor();
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
        if (middle && canvasContains(mx, my)) {
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
        String node = nodeAt(mx, my);
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
        int link = linkAt(mx, my);
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
            int cdx = (int) Math.round((mx - lastMouseX) / canvasZoom);
            int cdy = (int) Math.round((my - lastMouseY) / canvasZoom);
            if (cdx != 0 || cdy != 0) {
                NodeBox current = logicalNodeBoxes().get(draggingNetworkId);
                if (current != null) {
                    nodePositions.put(draggingNetworkId, clampLogical(current.x() + cdx, current.y() + cdy));
                    draggedNetwork = true;
                }
                lastMouseX = mx;
                lastMouseY = my;
            }
            return true;
        }
        if (panning) {
            double mx = event.x();
            double my = event.y();
            canvasPanX += (float) (mx - lastMouseX);
            canvasPanY += (float) (my - lastMouseY);
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
        if (!canvasContains(rawX, rawY)) return super.mouseScrolled(rawX, rawY, sx, sy);
        float oldZoom = canvasZoom;
        float nextZoom = Math.max(0.5f, Math.min(2.5f, canvasZoom + (float) Math.signum(sy) * 0.15f));
        if (nextZoom == oldZoom) return true;
        double logicalX = (rawX - canvasX - canvasPanX) / oldZoom;
        double logicalY = (rawY - canvasY - canvasPanY) / oldZoom;
        canvasZoom = nextZoom;
        canvasPanX = (float) (rawX - canvasX - logicalX * canvasZoom);
        canvasPanY = (float) (rawY - canvasY - logicalY * canvasZoom);
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
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL, DUTheme.PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 14, DUTheme.PANEL_HEADER);
        DUTheme.outline(g, x + 1, y + 1, w - 2, 14, 0xFF244323);
        g.text(font, title, x + 6, y + 4, DUTheme.TEXT_GREEN, false);
    }

    private void renderCanvasGrid(GuiGraphicsExtractor g) {
        g.enableScissor(canvasX + 1, canvasY + 15, canvasX + canvasW - 1, canvasY + canvasH - 1);
        int spacing = Math.max(12, Math.round(24 * canvasZoom));
        int ox = Math.floorMod(Math.round(canvasPanX), spacing);
        int oy = Math.floorMod(Math.round(canvasPanY), spacing);
        for (int gx = canvasX + ox; gx < canvasX + canvasW; gx += spacing) {
            g.fill(gx, canvasY + 15, gx + 1, canvasY + canvasH - 1, 0x3632452f);
        }
        for (int gy = canvasY + 15 + oy; gy < canvasY + canvasH; gy += spacing) {
            g.fill(canvasX + 1, gy, canvasX + canvasW - 1, gy + 1, 0x3632452f);
        }
        g.disableScissor();
    }

    private void drawNetworkNode(GuiGraphicsExtractor g, String id, NodeBox box) {
        boolean selected = id.equals(selectedNetworkId);
        boolean source = id.equals(linkSourceId);
        int border = source ? DUTheme.PROGRESS_ORANGE : selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER;
        int header = source ? 0xFF332713 : selected ? 0xFF14303a : DUTheme.PANEL_HEADER;
        DUTheme.box(g, box.x(), box.y(), NODE_W, NODE_H, DUTheme.PANEL_ALT, border);
        g.fill(box.x() + 1, box.y() + 1, box.x() + NODE_W - 1, box.y() + 13, header);
        g.text(font, "NETWORK", box.x() + 5, box.y() + 4, source ? DUTheme.PROGRESS_ORANGE : DUTheme.TEXT_GREEN, false);
        g.text(font, trim(nameFor(id), 18), box.x() + 6, box.y() + 18, DUTheme.TEXT, false);
        g.text(font, trim(id, 20), box.x() + 6, box.y() + 31, DUTheme.TEXT_DIM, false);
        port(g, box.inX(), box.inY(), border);
        port(g, box.outX(), box.outY(), border);
    }

    private void drawBridge(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, boolean selected, String label) {
        int mx = (x1 + x2) / 2;
        int bend = Math.max(24, Math.abs(x2 - x1) / 2);
        int c1x = x1 + bend;
        int c2x = x2 - bend;
        int lastX = x1;
        int lastY = y1;
        for (int i = 1; i <= 18; i++) {
            double t = i / 18.0;
            int x = (int) Math.round(cubic(x1, c1x, c2x, x2, t));
            int y = (int) Math.round(cubic(y1, y1, y2, y2, t));
            DUTheme.line(g, lastX, lastY, x, y, color);
            if (selected && i % 5 == 0) g.fill(x - 1, y - 1, x + 2, y + 2, color);
            lastX = x;
            lastY = y;
        }
        int labelW = font.width(label) + 8;
        int labelY = (y1 + y2) / 2 - 6;
        DUTheme.box(g, mx - labelW / 2, labelY, labelW, 12, 0xE60b0f0a, selected ? color : DUTheme.PANEL_BORDER);
        g.centeredText(font, label, mx, labelY + 2, color);
    }

    private void port(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 3, y + 3, DUTheme.SCREEN);
        g.fill(x - 1, y - 1, x + 2, y + 2, color);
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

    private String nodeAt(double mx, double my) {
        for (Map.Entry<String, NodeBox> entry : nodeBoxes().entrySet()) {
            NodeBox b = entry.getValue();
            if (mx >= b.x() && mx <= b.x() + NODE_W && my >= b.y() && my <= b.y() + NODE_H) return entry.getKey();
        }
        return "";
    }

    private boolean canvasContains(double mx, double my) {
        return mx >= canvasX && mx < canvasX + canvasW && my >= canvasY + 15 && my < canvasY + canvasH;
    }

    private int linkAt(double mx, double my) {
        Map<String, NodeBox> boxes = nodeBoxes();
        for (int i = 0; i < links.size(); i++) {
            SwitchboardLinkData link = links.get(i);
            NodeBox a = boxes.get(link.sourceNetworkId());
            NodeBox b = boxes.get(link.targetNetworkId());
            if (a == null || b == null) continue;
            if (distanceToCurve(mx, my, a.outX(), a.outY(), b.inX(), b.inY()) <= 8.0) return i;
        }
        return -1;
    }

    private Map<String, NodeBox> nodeBoxes() {
        Map<String, NodeBox> logical = logicalNodeBoxes();
        Map<String, NodeBox> projected = new LinkedHashMap<>();
        for (Map.Entry<String, NodeBox> entry : logical.entrySet()) projected.put(entry.getKey(), project(entry.getValue()));
        return projected;
    }

    private Map<String, NodeBox> logicalNodeBoxes() {
        Map<String, NodeBox> out = new LinkedHashMap<>();
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
            NodeBox saved = nodePositions.get(id);
            if (saved != null) {
                out.put(id, clampLogical(saved.x(), saved.y()));
            } else {
                int row = i / cols;
                int col = i % cols;
                int x = 22 + col * (NODE_W + 34);
                int y = startY + row * (NODE_H + gapY);
                out.put(id, clampLogical(x, y));
            }
            i++;
        }
        return out;
    }

    private NodeBox clampLogical(int x, int y) {
        int minX = 8;
        int maxX = Math.max(minX, canvasW - NODE_W - 8);
        int minY = 20;
        int maxY = Math.max(minY, canvasH - NODE_H - 8);
        return new NodeBox(Math.max(minX, Math.min(maxX, x)), Math.max(minY, Math.min(maxY, y)));
    }

    private NodeBox project(NodeBox logical) {
        return new NodeBox(
                canvasX + Math.round(canvasPanX + logical.x() * canvasZoom),
                canvasY + Math.round(canvasPanY + logical.y() * canvasZoom));
    }

    private List<SwitchboardNodePositionData> positionData() {
        List<SwitchboardNodePositionData> out = new ArrayList<>();
        for (Map.Entry<String, NodeBox> entry : logicalNodeBoxes().entrySet()) {
            NodeBox box = entry.getValue();
            out.add(new SwitchboardNodePositionData(entry.getKey(), box.x(), box.y()));
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

    private static double distanceToCurve(double px, double py, int x1, int y1, int x2, int y2) {
        double best = Double.MAX_VALUE;
        double lastX = x1;
        double lastY = y1;
        int bend = Math.max(24, Math.abs(x2 - x1) / 2);
        int c1x = x1 + bend;
        int c2x = x2 - bend;
        for (int i = 1; i <= 18; i++) {
            double t = i / 18.0;
            double x = cubic(x1, c1x, c2x, x2, t);
            double y = cubic(y1, y1, y2, y2, t);
            best = Math.min(best, distanceToSegment(px, py, lastX, lastY, x, y));
            lastX = x;
            lastY = y;
        }
        return best;
    }

    private static double cubic(double a, double b, double c, double d, double t) {
        double u = 1.0 - t;
        return u * u * u * a + 3.0 * u * u * t * b + 3.0 * u * t * t * c + t * t * t * d;
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) return Math.hypot(px - ax, py - ay);
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double x = ax + t * dx;
        double y = ay + t * dy;
        return Math.hypot(px - x, py - y);
    }

    private record NodeBox(int x, int y) {
        int inX() { return x - 1; }
        int inY() { return y + NODE_H / 2; }
        int outX() { return x + NODE_W; }
        int outY() { return y + NODE_H / 2; }
    }
}
