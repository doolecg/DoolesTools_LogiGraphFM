package net.doole.doolestools.client.screen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.ClientPrefs;
import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.client.gui.CanvasContextMenu;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.Glyphs;
import net.doole.doolestools.client.gui.GraphCanvasWidget;
import net.doole.doolestools.client.gui.GuiSprites;
import net.doole.doolestools.client.gui.InventoryViewPopup;
import net.doole.doolestools.client.gui.ItemIcons;
import net.doole.doolestools.client.gui.NodeDetailsPanel;
import net.doole.doolestools.client.gui.ScannedBlockListWidget;
import net.doole.doolestools.client.gui.TerminalButton;
import net.doole.doolestools.client.gui.WarningBarWidget;
import net.doole.doolestools.logistics.LogisticsGraph;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.PortDirection;
import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.GraphCanvasData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.client.Minecraft;
import net.doole.doolestools.menu.LogisticsComputerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Large terminal-style multi-page console for the Logistics Computer. */
public class LogisticsComputerScreen extends AbstractContainerScreen<LogisticsComputerMenu> {

    private static final int MIN_GUI_W = 420;
    private static final int MIN_GUI_H = 246;
    // Fixed design canvas: the whole GUI is laid out at this size, then uniformly scaled to fit the
    // window so text/buttons/cards keep the same proportions at every GUI scale (like the concept).
    private static final int DESIGN_W = 600;
    private static final int DESIGN_H = 400;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");

    private static final int PAGE_SCANNED = 0, PAGE_GRAPH = 1, PAGE_STATS = 2, PAGE_SETTINGS = 3;
    private static final String[] PAGE_TITLE_KEYS = {
            "doolestools.computer.page.network",
            "doolestools.computer.page.factory_manager",
            "doolestools.computer.page.power",
            "doolestools.computer.page.settings"};
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_DELETE = 261;
    private static final int KEY_F = 70;
    private static final int KEY_N = 78;
    private static final int KEY_S = 83;
    private static final int KEY_D = 68;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_LEFT = 263;
    private static final int MOD_ALT = 4;
    private static final int MOD_SHIFT = 1;
    /** Bottom-left page bar order + short labels. */
    private static final int[] PAGE_ORDER = {PAGE_SCANNED, PAGE_GRAPH, PAGE_STATS, PAGE_SETTINGS};
    private static final String[] PAGE_TABS = {"NETWORK", "FACTORY", "POWER", "SETTINGS"};
    private static final int SNAP = 12;
    private static final int PICKER_RECENT_LIMIT = 24;
    private static final List<String> FILTER_PICKER_RECENTS = new ArrayList<>();

    private final EditorContext ctx;

    private ScannedBlockListWidget listWidget;
    private GraphCanvasWidget canvasWidget;
    private NodeDetailsPanel detailsPanel;
    private WarningBarWidget warningBar;
    private EditBox nameField;
    private EditBox filterRuleField;
    private EditBox whitelistEntryField;
    private TerminalButton linkButton;
    private TerminalButton filterButton;

    private int guiW = MIN_GUI_W;
    private int guiH = MIN_GUI_H;
    private int leftX, leftW, canvasX, canvasW, rightX, rightW, contentY, contentH;
    private int warningY, tabBarY;

    // Global UI scale: 1.0 when the window is large enough; shrinks everything uniformly when not.
    private float uiScale = 1f;
    private int uiOffsetX, uiOffsetY;

    private int page = PAGE_GRAPH;
    private int scannedScroll;
    private final TerminalButton[] tabButtons = new TerminalButton[4];
    private final List<AbstractWidget>[] pageWidgets = new List[]{
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};

    // interaction state
    private CanvasContextMenu contextMenu;
    private InventoryViewPopup inventoryPopup;
    private String draggingNodeId;
    private String draggingFrameId;
    private String resizingFrameId;
    /** Node ids captured when a frame drag starts, so the frame carries its contents. */
    private java.util.List<String> framedNodeIds = java.util.List.of();
    private String draggingTextId;
    private boolean panning;
    private boolean marqueeing;
    private boolean draggingScanned;
    private String draggingScannedName;
    private double marqueeStartX, marqueeStartY, marqueeCurX, marqueeCurY;
    private double scannedDragX, scannedDragY;
    private double lastMouseX, lastMouseY;
    private String lastClickScannedId;
    private long lastClickTime;
    private String lastSelectedNodeId;
    private String lastFilterNodeId;
    private String lastSelectedFrameId;
    private String lastSelectedTextId;
    private boolean editingGraphName;
    private boolean startMenuOpen;
    private int autoRefreshTicks;
    private boolean filterPickerOpen;
    private int filterPickerSlot = -1;
    private int filterPickerScroll;
    private String filterPickerSearch = "";
    private boolean filterPickerRecentTab;
    private List<PickerEntry> filterPickerEntries;

    public LogisticsComputerScreen(LogisticsComputerMenu menu, Inventory inv, Component title) {
        // Pass an oversized imageWidth/imageHeight so JEI/REI/EMI calculate no available
        // margin and keep their panels off-screen. Our rendering uses guiW/guiH, not these.
        super(menu, inv, title, 10000, 10000);
        this.ctx = new EditorContext(menu.getPos());
    }

    public EditorContext context() {
        return ctx;
    }

    @Override
    protected void init() {
        super.init();
        for (List<AbstractWidget> l : pageWidgets) l.clear();

        // Lay out in a fixed "design space" anchored at (0,0); a pose transform centres and scales it.
        float preferredScale = Math.max(0.75f, Math.min(1.5f, ClientPrefs.uiScale));
        if (this.width - 24 >= MIN_GUI_W && this.height - 24 >= MIN_GUI_H) {
            guiW = Math.max(MIN_GUI_W, Math.round((this.width - 24) / preferredScale));
            guiH = Math.max(MIN_GUI_H, Math.round((this.height - 24) / preferredScale));
            uiScale = Math.min(preferredScale, Math.min((this.width - 8f) / guiW, (this.height - 8f) / guiH));
        } else {
            guiW = MIN_GUI_W;
            guiH = MIN_GUI_H;
            uiScale = Math.min(1f, Math.min((this.width - 8f) / MIN_GUI_W, (this.height - 8f) / MIN_GUI_H));
        }
        uiOffsetX = Math.round((this.width - guiW * uiScale) / 2f);
        uiOffsetY = Math.round((this.height - guiH * uiScale) / 2f);
        this.leftPos = 0;
        this.topPos = 0;
        int lx = leftPos;
        int ty = topPos;

        // Panel geometry.
        leftX = lx + 10;
        leftW = 132;
        rightW = Math.max(126, Math.min(176, guiW / 4));
        rightX = lx + guiW - rightW - 10;
        canvasX = leftX + leftW + 8;
        canvasW = Math.max(140, rightX - canvasX - 8);
        contentY = ty + 44;
        tabBarY = ty + guiH - 24;
        warningY = tabBarY - 24;
        contentH = Math.max(96, warningY - contentY - 6);

        // Inset the list strictly inside the panel, ending above the SCAN/REFRESH buttons.
        int listBottom = contentY + contentH - 44;
        listWidget = new ScannedBlockListWidget(ctx, leftX + 2, contentY + 13, leftW - 4, Math.max(44, listBottom - (contentY + 13)));
        canvasWidget = new GraphCanvasWidget(ctx, canvasX, contentY, canvasW, contentH);
        // Details content must end above the Set Type / Remove buttons (at contentY + contentH - 16).
        int detailsBottom = contentY + contentH - 20;
        detailsPanel = new NodeDetailsPanel(ctx, rightX + 4, contentY + 30, rightW - 8, detailsBottom - (contentY + 30), this::openInventoryPopup);
        warningBar = new WarningBarWidget(ctx, lx + 10, warningY + 2, guiW - 20, 16);

        // --- Header buttons (top-right): ? / settings / X ---
        int hbY = ty + 11;
        addRenderableWidget(new TerminalButton(lx + guiW - 24, hbY, 16, 16, Component.literal("X"), this::onClose)
                .spriteOnly(GuiSprites.CLOSE));
        addRenderableWidget(new TerminalButton(lx + guiW - 42, hbY, 16, 16, Component.empty(), () -> setPage(PAGE_SETTINGS))
                .spriteOnly(GuiSprites.SETTINGS));
        addRenderableWidget(new TerminalButton(lx + guiW - 60, hbY, 16, 16, Component.literal("?"), () -> {})
                .spriteOnly(GuiSprites.HELP));
        // Always-visible Save: persists the graph and pulls a fresh server state so routing picks it up.
        addRenderableWidget(new TerminalButton(lx + guiW - 106, hbY, 42, 16, Component.literal("SAVE"), this::saveAndRefresh)
                .accent(DUTheme.OK));

        initGraphPage(lx, ty);
        initSettingsPage();

        setPage(page);
        // Refresh data every time the menu opens: a fresh scan re-baselines machine progress, etc.
        ClientNetworkSender.scanArea(ctx.pos());
    }

    private TerminalButton addTab(int x, String tip, int targetPage) {
        TerminalButton b = new TerminalButton(x, tabBarY, 30, 16, Component.literal(tip), () -> setPage(targetPage))
                .spriteOnly(pageSprite(targetPage)).accent(DUTheme.TEXT_GREEN);
        addRenderableWidget(b);
        return b;
    }

    private net.minecraft.resources.Identifier pageSprite(int targetPage) {
        return switch (targetPage) {
            case PAGE_SCANNED -> GuiSprites.LIST;
            case PAGE_STATS -> GuiSprites.STATS;
            case PAGE_SETTINGS -> GuiSprites.SETTINGS;
            default -> GuiSprites.CANVAS;
        };
    }

    private void initGraphPage(int lx, int ty) {
        // Left panel buttons (with icons).
        int leftButtonY = contentY + contentH - 40;
        paged(PAGE_GRAPH, new TerminalButton(leftX, leftButtonY, leftW, 14,
                Component.literal(tr("doolestools.computer.button.scan_network")), () -> ClientNetworkSender.scanArea(ctx.pos()))
                .sprite(GuiSprites.RADAR).accent(DUTheme.OK));
        paged(PAGE_GRAPH, new TerminalButton(leftX, leftButtonY + 16, leftW, 14,
                Component.literal(tr("doolestools.computer.button.refresh_data")), () -> ClientNetworkSender.requestComputerSync(ctx.pos()))
                .sprite(GuiSprites.REFRESH).accent(DUTheme.PROGRESS_BLUE));
        paged(PAGE_GRAPH, new TerminalButton(leftX, leftButtonY + 32, 60, 12,
                Component.literal(tr("doolestools.computer.button.clear")), () -> ClientNetworkSender.clearScan(ctx.pos()))
                .sprite(GuiSprites.CLEAR).accent(DUTheme.ERROR));
        filterButton = new TerminalButton(leftX + 64, leftButtonY + 32, leftW - 64, 12,
                Component.literal("All"), this::cycleFilter)
                .sprite(GuiSprites.FILTER).accent(DUTheme.TEXT_GREEN);
        paged(PAGE_GRAPH, filterButton);

        // Floating tool palette inside the canvas (top-left corner).
        int px = canvasX + 4, py = contentY + 14;
        paged(PAGE_GRAPH, toolBtn(px, py, "Add Selected", Glyphs::plus, () -> ctx.addNodeForSelectedScan()));
        linkButton = toolBtn(px, py + 18, "Link Mode", Glyphs::link, this::toggleLinkMode);
        paged(PAGE_GRAPH, linkButton);
        paged(PAGE_GRAPH, toolBtn(px, py + 36, "Delete", Glyphs::trash, () -> ctx.deleteSelectedNode()).accent(DUTheme.ERROR));
        paged(PAGE_GRAPH, toolBtn(px, py + 54, "Auto Arrange", Glyphs::arrange, () -> ctx.autoArrange()));
        paged(PAGE_GRAPH, toolBtn(px, py + 72, "Add Frame", Glyphs::frame, this::addFrameAtView));
        paged(PAGE_GRAPH, toolBtn(px, py + 90, "Add Filter Node", Glyphs::link, this::addFilterAtView).accent(DUTheme.PROGRESS_BLUE));
        paged(PAGE_GRAPH, toolBtn(px, py + 108, "Fit View", Glyphs::fit, () -> canvasWidget.fitView()));

        // Canvas header zoom controls.
        paged(PAGE_GRAPH, new TerminalButton(canvasX + canvasW - 24, contentY + 1, 11, 9,
                Component.literal("+"), () -> zoomBy(0.2f)));
        paged(PAGE_GRAPH, new TerminalButton(canvasX + canvasW - 12, contentY + 1, 11, 9,
                Component.literal("-"), () -> zoomBy(-0.2f)));

        // Right panel: editable custom name + Set Type / Remove.
        nameField = new EditBox(font, rightX + 4, contentY + 14, rightW - 8, 12, Component.literal("name"));
        nameField.setMaxLength(48);
        nameField.setResponder(this::onNameChanged);
        paged(PAGE_GRAPH, nameField);
        paged(PAGE_GRAPH, new TerminalButton(rightX + 4, contentY + contentH - 16, (rightW - 12) / 2, 13,
                Component.literal(tr("doolestools.computer.button.set_type")), this::cycleType).sprite(GuiSprites.FILTER));
        paged(PAGE_GRAPH, new TerminalButton(rightX + 8 + (rightW - 12) / 2, contentY + contentH - 16, (rightW - 12) / 2, 13,
                Component.literal(tr("doolestools.computer.button.remove")), () -> ctx.deleteSelectedNode()).sprite(GuiSprites.CLEAR).accent(DUTheme.ERROR));
    }

    private static final int SETTINGS_COL_W = 168;

    private void initSettingsPage() {
        int sx = leftX + 12, sy = contentY + 28, sw = SETTINGS_COL_W, sh = 16, row = 22;
        // DISPLAY column (client-side preferences).
        paged(PAGE_SETTINGS, settingToggle(sx, sy, sw, sh, "Show Grid", () -> ClientPrefs.showGrid, v -> ClientPrefs.showGrid = v));
        paged(PAGE_SETTINGS, settingToggle(sx, sy + row, sw, sh, "Animations", () -> ClientPrefs.animate, v -> ClientPrefs.animate = v));
        paged(PAGE_SETTINGS, settingToggle(sx, sy + 2 * row, sw, sh, "Item Icons", () -> ClientPrefs.showItemIcons, v -> ClientPrefs.showItemIcons = v));
        paged(PAGE_SETTINGS, settingToggle(sx, sy + 3 * row, sw, sh, "Auto-Refresh Scan", () -> ClientPrefs.autoRefresh, v -> ClientPrefs.autoRefresh = v));
        paged(PAGE_SETTINGS, uiScaleButton(sx, sy + 4 * row, sw, sh));

        // GRAPH DATA column.
        int bx = sx + sw + 20;
        paged(PAGE_SETTINGS, new TerminalButton(bx, sy, sw, sh, Component.literal(tr("doolestools.computer.button.export_graph")), this::exportGraphToClipboard).accent(DUTheme.OK));
        paged(PAGE_SETTINGS, new TerminalButton(bx, sy + row, sw, sh, Component.literal(tr("doolestools.computer.button.import_graph")), this::importGraphFromClipboard).accent(DUTheme.PROGRESS_BLUE));
        paged(PAGE_SETTINGS, new TerminalButton(bx, sy + 2 * row, sw, sh, Component.literal(tr("doolestools.computer.button.duplicate_canvas")), this::createCanvas).accent(DUTheme.TEXT_GREEN));
        paged(PAGE_SETTINGS, new TerminalButton(bx, sy + 3 * row, sw, sh, Component.literal(tr("doolestools.computer.button.reset_graph")), this::resetGraph).accent(DUTheme.ERROR));

        // NETWORK & SHARING column.
        int wx = bx + sw + 20;
        paged(PAGE_SETTINGS, new TerminalButton(wx, sy, sw, sh, Component.literal("Cycle Access"), this::cycleNetworkAccess).accent(DUTheme.WARN));
        paged(PAGE_SETTINGS, new TerminalButton(wx, sy + row, sw, sh, Component.literal("Name From Graph"), this::nameNetworkFromGraph).accent(DUTheme.OK));
        whitelistEntryField = new EditBox(font, wx, sy + 2 * row, sw, sh, Component.literal("player name or UUID"));
        whitelistEntryField.setMaxLength(64);
        whitelistEntryField.setHint(Component.literal("player name or UUID"));
        paged(PAGE_SETTINGS, whitelistEntryField);
        paged(PAGE_SETTINGS, new TerminalButton(wx, sy + 3 * row, (sw - 6) / 2, sh, Component.literal("Add"), this::addWhitelistEntry).accent(DUTheme.OK));
        paged(PAGE_SETTINGS, new TerminalButton(wx + (sw + 6) / 2, sy + 3 * row, (sw - 6) / 2, sh, Component.literal("Remove"), this::removeLastWhitelistEntry).accent(DUTheme.ERROR));
    }

    private TerminalButton settingToggle(int x, int y, int w, int h, String label,
                                         java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        TerminalButton[] ref = new TerminalButton[1];
        ref[0] = new TerminalButton(x, y, w, h, Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), () -> {
            boolean nv = !get.getAsBoolean();
            set.accept(nv);
            ref[0].setMessage(Component.literal(label + ": " + (nv ? "ON" : "OFF")));
            ref[0].setToggled(nv);
        }).accent(DUTheme.OK);
        ref[0].setToggled(get.getAsBoolean());
        return ref[0];
    }

    private TerminalButton uiScaleButton(int x, int y, int w, int h) {
        TerminalButton[] ref = new TerminalButton[1];
        ref[0] = new TerminalButton(x, y, w, h, Component.literal(uiScaleLabel()), () -> {
            float current = ClientPrefs.uiScale;
            ClientPrefs.uiScale = current < 1.0f ? 1.0f : current < 1.25f ? 1.25f : current < 1.5f ? 1.5f : 0.75f;
            ref[0].setMessage(Component.literal(uiScaleLabel()));
            rebuildWidgets();
        }).accent(DUTheme.PROGRESS_BLUE);
        return ref[0];
    }

    private static String uiScaleLabel() {
        return "UI Scale: " + Math.round(ClientPrefs.uiScale * 100f) + "%";
    }

    private TerminalButton toolBtn(int x, int y, String tip, Glyphs.Drawer icon, Runnable action) {
        return new TerminalButton(x, y, 16, 16, Component.literal(tip), action).iconOnly(icon);
    }

    private <T extends AbstractWidget> T paged(int p, T widget) {
        addRenderableWidget(widget);
        pageWidgets[p].add(widget);
        return widget;
    }

    private void setPage(int p) {
        this.page = p;
        for (int i = 0; i < pageWidgets.length; i++) {
            boolean vis = i == p;
            for (AbstractWidget w : pageWidgets[i]) { w.visible = vis; w.active = vis; }
        }
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i] != null) tabButtons[i].setToggled(i == p);
        }
        contextMenu = null;
        inventoryPopup = null;
        closeFilterPicker();
        startMenuOpen = false;
    }

    // --- actions ---

    private void saveGraph() {
        ClientNetworkSender.saveGraph(ctx.pos(), ctx.graph());
        ctx.markSaved();
    }

    /** Save + pull a fresh server state so graph changes are immediately reflected in the UI. */
    private void saveAndRefresh() {
        saveGraph();
        ClientNetworkSender.requestComputerSync(ctx.pos());
    }

    private void exportGraphToClipboard() {
        LogisticsGraphData.CODEC.encodeStart(JsonOps.INSTANCE, ctx.graph()).result().ifPresent(json ->
                Minecraft.getInstance().keyboardHandler.setClipboard(new GsonBuilder().setPrettyPrinting().create().toJson(json)));
    }

    private void importGraphFromClipboard() {
        try {
            String text = Minecraft.getInstance().keyboardHandler.getClipboard();
            LogisticsGraphData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(text)).result().ifPresent(graph -> {
                ctx.setGraph(graph);
                ClientNetworkSender.saveGraph(ctx.pos(), graph);
                ctx.markSaved();
            });
        } catch (RuntimeException ignored) {
        }
    }

    private void resetGraph() {
        ctx.setGraph(LogisticsGraphData.EMPTY);
        ClientNetworkSender.saveGraph(ctx.pos(), ctx.graph());
        ctx.markSaved();
    }

    private void createCanvas() {
        ctx.setGraph(LogisticsGraph.addCanvas(ctx.graph()));
        ctx.clearSelection();
        editingGraphName = true;
        if (nameField != null) nameField.setValue(ctx.graph().activeCanvas().title());
        focusNameField();
    }

    private void cycleFilter() {
        ctx.filter = ctx.filter.next();
        filterButton.setMessage(Component.literal(ctx.filter.label));
    }

    private void toggleLinkMode() {
        ctx.linkMode = !ctx.linkMode;
        ctx.linkSourceId = null;
        linkButton.setToggled(ctx.linkMode);
    }

    private void cycleType() {
        GraphNodeData n = ctx.selectedNode();
        if (n == null) return;
        NodeType[] types = NodeType.values();
        NodeType next = types[(n.type().ordinal() + 1) % types.length];
        ctx.setGraph(LogisticsGraph.updateNode(ctx.graph(), n.withType(next)));
    }

    private void openInventoryPopup() {
        GraphNodeData node = ctx.selectedNode();
        if (node == null) return;
        ScannedBlockData s = ctx.scannedFor(node);
        if (s == null || !s.inventory().hasData()) return;
        inventoryPopup = new InventoryViewPopup(s.blockName(), s.inventory());
    }

    private void zoomBy(float delta) {
        float old = ctx.zoom;
        ctx.zoom = Math.max(0.4f, Math.min(2.0f, old + delta));
        double cx = canvasWidget.x + canvasWidget.w / 2.0;
        double cy = canvasWidget.y + canvasWidget.h / 2.0;
        ctx.panX -= (cx - canvasWidget.x - ctx.panX) * (ctx.zoom / old - 1f);
        ctx.panY -= (cy - canvasWidget.y - ctx.panY) * (ctx.zoom / old - 1f);
    }

    private void addFilterAtView() {
        int cx = snap((int) canvasWidget.toCanvasX(canvasWidget.x + canvasWidget.w / 2.0) - GraphNodeData.DEFAULT_WIDTH / 2);
        int cy = snap((int) canvasWidget.toCanvasY(canvasWidget.y + canvasWidget.h / 2.0) - GraphNodeData.DEFAULT_HEIGHT / 2);
        ctx.addFilterNode(cx, cy);
    }

    private void addFrameAtView() {
        // With nodes selected, wrap them; otherwise drop a default frame at the view centre.
        if (ctx.addFrameAroundSelection()) return;
        int cx = snap((int) canvasWidget.toCanvasX(canvasWidget.x + canvasWidget.w / 2.0)
                - net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_WIDTH / 2);
        int cy = snap((int) canvasWidget.toCanvasY(canvasWidget.y + canvasWidget.h / 2.0)
                - net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_HEIGHT / 2);
        ctx.addFrame(cx, cy);
    }

    private static int snap(int value) {
        return Math.round(value / (float) SNAP) * SNAP;
    }

    private void focusNameField() {
        if (nameField != null) {
            nameField.setEditable(true);
            setFocused(nameField);
            nameField.setFocused(true);
        }
    }

    private void openContextMenu(double mx, double my, GraphNodeData node,
                                 net.doole.doolestools.logistics.data.GraphFrameData frame,
                                 net.doole.doolestools.logistics.data.GraphTextData text) {
        List<CanvasContextMenu.Item> items = new ArrayList<>();
        if (node != null) {
            items.add(new CanvasContextMenu.Item("Rename", () -> { ctx.selectSingleNode(node.nodeId()); focusNameField(); }));
            items.add(new CanvasContextMenu.Item("Set Type", () -> { ctx.selectSingleNode(node.nodeId()); cycleType(); }));
            items.add(new CanvasContextMenu.Item("Delete Node", () -> ctx.deleteNode(node.nodeId()), DUTheme.ERROR));
        } else if (text != null) {
            items.add(new CanvasContextMenu.Item("Edit Text", () -> { ctx.clearSelection(); ctx.selectedTextId = text.textId(); focusNameField(); }));
            items.add(new CanvasContextMenu.Item("Delete Text", () -> ctx.deleteText(text.textId()), DUTheme.ERROR));
        } else if (frame != null) {
            items.add(new CanvasContextMenu.Item("Rename Frame", () -> { ctx.clearSelection(); ctx.selectedFrameId = frame.frameId(); focusNameField(); }));
            items.add(new CanvasContextMenu.Item("Delete Frame", () -> ctx.deleteFrame(frame.frameId()), DUTheme.ERROR));
        } else if (canvasWidget.linkAt(mx, my) instanceof GraphLinkData link) {
            items.add(new CanvasContextMenu.Item("Delete Link", () -> { ctx.setGraph(LogisticsGraph.removeLink(ctx.graph(), link.linkId())); saveGraph(); }, DUTheme.ERROR));
        } else {
            int cx = (int) canvasWidget.toCanvasX(mx);
            int cy = (int) canvasWidget.toCanvasY(my);
            int nodeX = snap(cx - GraphNodeData.DEFAULT_WIDTH / 2);
            int nodeY = snap(cy - GraphNodeData.DEFAULT_HEIGHT / 2);
            items.add(new CanvasContextMenu.Item("Add Filter Node", () -> ctx.addFilterNode(nodeX, nodeY)));
            items.add(new CanvasContextMenu.Item("Add Splitter Node", () -> ctx.addSplitterNode(nodeX, nodeY)));
            items.add(new CanvasContextMenu.Item("Add Combine Node", () -> ctx.addCombineNode(nodeX, nodeY)));
            items.add(new CanvasContextMenu.Item("Add Channel Node", () -> ctx.addChannelNode(nodeX, nodeY)));
            items.add(new CanvasContextMenu.Item("Add Frame", () -> ctx.addFrame(
                    snap(cx - net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_WIDTH / 2),
                    snap(cy - net.doole.doolestools.logistics.data.GraphFrameData.DEFAULT_HEIGHT / 2))));
            items.add(new CanvasContextMenu.Item("Add Text Label", () -> { ctx.addTextLabel(snap(cx), snap(cy)); focusNameField(); }));
            items.add(new CanvasContextMenu.Item("Auto Arrange", () -> ctx.autoArrange()));
            items.add(new CanvasContextMenu.Item("Fit View", () -> canvasWidget.fitView()));
        }
        contextMenu = new CanvasContextMenu((int) mx, (int) my, items, font, leftPos + guiW - 6, topPos + guiH - 6);
    }

    private void onNameChanged(String text) {
        if (editingGraphName) {
            ctx.setGraph(LogisticsGraph.renameGraph(ctx.graph(), text));
            return;
        }
        var label = ctx.selectedText();
        if (label != null) {
            if (!label.text().equals(text)) ctx.setGraph(LogisticsGraph.updateText(ctx.graph(), label.withText(text)));
            return;
        }
        var frame = ctx.selectedFrame();
        if (frame != null) {
            if (!frame.label().equals(text)) ctx.setGraph(LogisticsGraph.updateFrame(ctx.graph(), frame.withLabel(text)));
            return;
        }
        GraphNodeData n = ctx.selectedNode();
        if (n != null && !n.displayName().equals(text)) ctx.setGraph(LogisticsGraph.updateNode(ctx.graph(), n.withName(text)));
    }

    private void onFilterRuleChanged(String text) {
        GraphNodeData n = ctx.selectedNode();
        if (n != null && n.type() == NodeType.FILTER && !n.notes().equals(text)) {
            ctx.setGraph(LogisticsGraph.updateNode(ctx.graph(), n.withNotes(text)));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (ClientPrefs.autoRefresh && ++autoRefreshTicks >= 40) {
            autoRefreshTicks = 0;
            ClientNetworkSender.scanArea(ctx.pos());
        }
    }

    // --- UI scale helpers ---

    private int designX(double screenX) { return (int) Math.round((screenX - uiOffsetX) / uiScale); }
    private int designY(double screenY) { return (int) Math.round((screenY - uiOffsetY) / uiScale); }

    private void pushUi(GuiGraphicsExtractor g) {
        g.pose().pushMatrix();
        g.pose().translate(uiOffsetX, uiOffsetY);
        g.pose().scale(uiScale, uiScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // Render widgets under the same scale/offset as the background, with design-space mouse coords.
        pushUi(g);
        int designMouseX = designX(mouseX), designMouseY = designY(mouseY);
        super.extractRenderState(g, designMouseX, designMouseY, a);
        if (startMenuOpen) renderStartMenu(g, leftPos, topPos, designMouseX, designMouseY);
        if (filterPickerOpen) renderFilterPicker(g, designMouseX, designMouseY);
        g.pose().popMatrix();
    }

    // --- rendering ---

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int rawMouseX, int rawMouseY, float partialTick) {
        pushUi(g);
        int mouseX = designX(rawMouseX), mouseY = designY(rawMouseY);
        int lx = leftPos, ty = topPos;

        DUTheme.bezel(g, lx, ty, guiW, guiH);
        DUTheme.screw(g, lx + 7, ty + 7);
        DUTheme.screw(g, lx + guiW - 7, ty + 7);
        DUTheme.screw(g, lx + 7, ty + guiH - 7);
        DUTheme.screw(g, lx + guiW - 7, ty + guiH - 7);
        DUTheme.box(g, lx + 5, ty + 5, guiW - 10, guiH - 10, DUTheme.SCREEN, DUTheme.PANEL_BORDER);

        renderHeader(g, lx, ty);

        switch (page) {
            case PAGE_SCANNED -> renderScannedPage(g, lx, ty);
            case PAGE_STATS -> renderStatsPage(g, lx, ty);
            case PAGE_SETTINGS -> renderSettingsPage(g, lx, ty);
            default -> renderGraphPage(g, lx, ty, mouseX, mouseY);
        }

        renderTaskbar(g, lx, ty);

        if (contextMenu != null) contextMenu.render(g, font, mouseX, mouseY);
        if (inventoryPopup != null) inventoryPopup.render(g, font, guiW, guiH);

        g.pose().popMatrix();
    }

    private void renderGraphPage(GuiGraphicsExtractor g, int lx, int ty, double mouseX, double mouseY) {
        panel(g, leftX, contentY, leftW, contentH, tr("doolestools.computer.panel.network"));
        panel(g, canvasX, contentY, canvasW, contentH, tr("doolestools.computer.panel.canvas"));
        panel(g, rightX, contentY, rightW, contentH, tr("doolestools.computer.panel.details"));

        hamburgerGlyph(g, leftX + leftW - 11, contentY + 3);
        panGlyph(g, canvasX + canvasW - 36, contentY + 2);
        pinGlyph(g, rightX + rightW - 11, contentY + 3);

        canvasWidget.render(g, font, mouseX, mouseY);
        renderCanvasTitleStrip(g);
        if (marqueeing) renderMarquee(g);
        if (draggingScanned) renderScannedDragPreview(g);
        if (draggingScanned) renderConnectionDropPreview(g);
        listWidget.render(g, font);
        renderCanvasLegend(g, canvasX, contentY + contentH);

        syncNameField();
        GraphNodeData sel = ctx.selectedNode();
        g.text(font, sel != null ? sel.displayName() : "—", rightX + 4, contentY + 14, DUTheme.TEXT, false);
        int detailsTop = contentY + 28;
        if (sel != null) {
            switch (sel.type()) {
                case FILTER -> detailsTop = renderFilterEditor(g, sel);
                case CHANNEL -> detailsTop = renderChannelEditor(g, sel);
                case SPLITTER -> detailsTop = renderRoutingInfo(g, "SPLITTER", "Round-robins the input", "across every output link.");
                case COMBINE -> detailsTop = renderRoutingInfo(g, "COMBINE", "Merges every input link", "into one output stream.");
                default -> { }
            }
        }
        detailsPanel.render(g, font, detailsTop);

        DUTheme.box(g, lx + 10, warningY, guiW - 20, 20, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        warningBar.render(g, font);
    }

    // --- Page 1: full scanned-blocks table ---

    private static final int SCAN_ROW_H = 28;

    private void renderScannedPage(GuiGraphicsExtractor g, int lx, int ty) {
        int x = lx + 10, y = contentY, w = guiW - 20, h = tabBarY - contentY - 8;
        panel(g, x, y, w, h, tr("doolestools.computer.panel.network") + " - " + ctx.scan().size() + " " + tr("doolestools.computer.label.devices"));

        int rowsX = x + 2, rowsY = y + 14, rowsW = w - 4, rowsH = h - 16;
        g.enableScissor(rowsX, rowsY, rowsX + rowsW, rowsY + rowsH);
        List<ScannedBlockData> list = ctx.scan();
        int visible = rowsH / SCAN_ROW_H;
        scannedScroll = Math.max(0, Math.min(scannedScroll, Math.max(0, list.size() - visible)));
        int row = 0;
        for (int i = scannedScroll; i < list.size(); i++) {
            int ry = rowsY + (row++) * SCAN_ROW_H;
            if (ry > rowsY + rowsH) break;
            renderScannedRow(g, list.get(i), rowsX, ry, rowsW, i % 2 == 0);
        }
        g.disableScissor();
        if (list.isEmpty()) {
            g.centeredText(font, tr("doolestools.computer.empty.network"), x + w / 2, y + h / 2, DUTheme.TEXT_DIM);
        }
    }

    private void renderScannedRow(GuiGraphicsExtractor g, ScannedBlockData s, int x, int y, int w, boolean alt) {
        g.fill(x, y, x + w, y + SCAN_ROW_H - 2, alt ? DUTheme.PANEL_ALT : DUTheme.PANEL);
        DUTheme.outline(g, x, y, w, SCAN_ROW_H - 2, DUTheme.PANEL_BORDER);
        ItemIcons.render(g, s.registryId(), x + 5, y + 5, ItemIcons.SIZE, iconColor(s));

        g.text(font, s.blockName(), x + 26, y + 4, DUTheme.TEXT, false);
        g.text(font, typeLabel(s) + "  •  " + Math.round(s.distance()) + "m  •  "
                + s.pos().getX() + "," + s.pos().getY() + "," + s.pos().getZ(), x + 26, y + 15, DUTheme.TEXT_DIM, false);

        // Middle: fill bar for storage / status for machines.
        int midX = x + Math.max(220, w / 2);
        if (s.inventory().hasData()) {
            int pct = s.inventory().fillPercent();
            DUTheme.progress(g, midX, y + 6, 90, 8, pct / 100f, pct >= 100 ? DUTheme.ERROR : pct >= 85 ? DUTheme.WARN : DUTheme.OK);
            g.text(font, pct + "%  (" + s.inventory().usedSlots() + "/" + s.inventory().totalSlots() + ")", midX, y + 16, DUTheme.TEXT_DIM, false);
        } else if (s.furnace().hasData()) {
            g.text(font, s.furnace().status(), midX, y + 6, DUTheme.OK, false);
            if (s.furnace().hasRecipe()) g.text(font, "→ " + s.furnace().resultName(), midX, y + 16, DUTheme.TEXT_DIM, false);
        }

        // Right: top item icons.
        int ix = x + w - 6 - ItemIcons.SIZE;
        for (int k = 0; k < s.inventory().topStacks().size() && k < 4; k++) {
            ItemIcons.render(g, s.inventory().topStacks().get(k).registryId(), ix, y + 5, ItemIcons.SIZE, DUTheme.PANEL_ALT);
            ix -= 18;
        }
        if (s.hasWarnings()) Glyphs.warning(g, x + w - 12, y + SCAN_ROW_H - 13, DUTheme.WARN);
    }

    // --- Page 3: power usage ---

    private void renderStatsPage(GuiGraphicsExtractor g, int lx, int ty) {
        int x = lx + 10, y = contentY, w = guiW - 20, h = tabBarY - contentY - 8;
        panel(g, x, y, w, h, tr("doolestools.computer.page.power"));

        List<ScannedBlockData> scan = ctx.scan();
        int storage = 0, machine = 0, transport = 0, unknown = 0, warnings = 0, errors = 0, running = 0;
        long usedSlots = 0, totalSlots = 0;
        for (ScannedBlockData s : scan) {
            if (s.isStorageLike()) storage++;
            else if (s.isMachineLike()) machine++;
            else if (s.type() == ScannedType.TRANSPORT) transport++;
            else unknown++;
            if (s.inventory().hasData()) { usedSlots += s.inventory().usedSlots(); totalSlots += s.inventory().totalSlots(); }
            if (s.furnace().hasData() && "Running".equals(s.furnace().status())) running++;
            for (var wd : s.warnings()) {
                if (wd.severity() == net.doole.doolestools.logistics.data.WarningData.Severity.ERROR) errors++;
                else if (wd.severity() == net.doole.doolestools.logistics.data.WarningData.Severity.WARNING) warnings++;
            }
        }

        NetworkPowerData power = ctx.power();
        int cx = x + 16, cy = y + 22, colW = (w - 48) / 2;
        // Three-state, AE2/Factorio-style: satisfied / brownout (degraded, slow) / no power (stopped).
        int statusColor = power.starved() ? DUTheme.ERROR : power.degraded() ? DUTheme.WARN : DUTheme.OK;
        String statusText = power.starved() ? "NOT ENOUGH POWER — STOPPED"
                : power.degraded() ? "LOW POWER — RUNNING SLOW (" + Math.round(power.satisfaction() * 100) + "%)"
                : "ELECTRIC NETWORK SATISFIED";
        g.text(font, statusText, cx, cy, statusColor, false);
        g.text(font, "Production", cx, cy + 14, DUTheme.OK, false);
        g.text(font, formatFe(power.supplyCentiFe()) + "/t", cx + 74, cy + 14, DUTheme.TEXT, false);
        DUTheme.progress(g, cx + 150, cy + 12, Math.max(80, colW - 150), 10, powerRatio(power.supplyCentiFe(), Math.max(power.supplyCentiFe(), power.demandCentiFe())), DUTheme.OK);
        g.text(font, "Consumption", cx, cy + 28, DUTheme.PROGRESS_ORANGE, false);
        g.text(font, formatFe(power.demandCentiFe()) + "/t", cx + 74, cy + 28, DUTheme.TEXT, false);
        DUTheme.progress(g, cx + 150, cy + 26, Math.max(80, colW - 150), 10, powerRatio(power.demandCentiFe(), Math.max(power.supplyCentiFe(), power.demandCentiFe())), power.powered() ? DUTheme.PROGRESS_ORANGE : DUTheme.ERROR);
        g.text(font, "Deficit", cx, cy + 42, DUTheme.TEXT_DIM, false);
        g.text(font, formatFe(power.deficitCentiFe()) + "/t", cx + 74, cy + 42, power.deficitCentiFe() > 0 ? DUTheme.ERROR : DUTheme.TEXT_DIM, false);

        int graphX = cx;
        int graphY = cy + 60;
        int graphW = colW;
        int graphH = 76;
        renderPowerHistoryGraph(g, graphX, graphY, graphW, graphH);
        cy = graphY + graphH + 14;

        g.text(font, "TOP CONSUMERS", cx, cy, DUTheme.TEXT_GREEN, false); cy += 14;
        cy = stat(g, cx, cy, "Computer", formatFe(power.computerCentiFe()) + "/t");
        cy = stat(g, cx, cy, "Routers/modems", formatFe(power.endpointCentiFe()) + "/t  (" + power.endpointCount() + ")");
        cy = stat(g, cx, cy, "Cable", formatFe(power.wireCentiFe()) + "/t  (" + power.wireCount() + ")");
        cy = stat(g, cx, cy, "Devices", formatFe(power.deviceCentiFe()) + "/t  (" + power.deviceCount() + ")");
        cy = stat(g, cx, cy, "Routes", formatFe(power.routeCentiFe()) + "/t  (" + power.routeCount() + ")");

        cy += 6;
        g.text(font, "OVERVIEW", cx, cy, DUTheme.TEXT_GREEN, false); cy += 14;
        cy = stat(g, cx, cy, "Network devices", String.valueOf(scan.size()));
        cy = stat(g, cx, cy, "Graph nodes", String.valueOf(ctx.graph().activeCanvas().nodes().size()));
        cy = stat(g, cx, cy, "Machines running", running + " / " + machine);
        int fill = totalSlots > 0 ? (int) Math.round(usedSlots * 100.0 / totalSlots) : 0;
        g.text(font, "Total storage", cx, cy + 1, DUTheme.TEXT_DIM, false);
        DUTheme.progress(g, cx + 96, cy, 120, 9, fill / 100f, fill >= 90 ? DUTheme.ERROR : fill >= 70 ? DUTheme.WARN : DUTheme.OK);
        g.text(font, fill + "%", cx + 222, cy + 1, DUTheme.TEXT, false); cy += 18;

        // Right column: block-type bar chart.
        int bx = x + 16 + colW + 16, by = y + 24;
        g.text(font, "NETWORK INVENTORY", bx, by, DUTheme.TEXT_GREEN, false); by += 14;
        int max = Math.max(1, Math.max(Math.max(storage, machine), Math.max(transport, unknown)));
        by = bar(g, bx, by, colW, "Storage", storage, max, 0xFFb07a3a);
        by = bar(g, bx, by, colW, "Machines", machine, max, 0xFF8a8f96);
        by = bar(g, bx, by, colW, "Transport", transport, max, 0xFF6a7066);
        by = bar(g, bx, by, colW, "Unknown", unknown, max, 0xFF55405a);

        // Warnings summary.
        g.text(font, "ALERTS", bx, by + 6, DUTheme.TEXT_GREEN, false); by += 20;
        Glyphs.warning(g, bx, by, DUTheme.WARN);
        g.text(font, warnings + " warnings", bx + 14, by + 1, DUTheme.WARN, false);
        Glyphs.warning(g, bx + 120, by, DUTheme.ERROR);
        g.text(font, errors + " errors", bx + 134, by + 1, DUTheme.ERROR, false);
    }

    private int stat(GuiGraphicsExtractor g, int x, int y, String key, String value) {
        g.text(font, key, x, y, DUTheme.TEXT_DIM, false);
        g.text(font, value, x + 130, y, DUTheme.TEXT, false);
        return y + 13;
    }

    private void renderPowerHistoryGraph(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.text(font, "POWER HISTORY", x + 5, y + 5, DUTheme.TEXT_GREEN, false);
        List<Integer> supply = ctx.powerSupplyHistory();
        List<Integer> demand = ctx.powerDemandHistory();
        int max = 1;
        for (int value : supply) max = Math.max(max, value);
        for (int value : demand) max = Math.max(max, value);
        int gx = x + 5;
        int gy = y + 18;
        int gw = w - 10;
        int gh = h - 26;
        g.fill(gx, gy, gx + gw, gy + gh, 0xFF070b08);
        for (int i = 1; i < 4; i++) {
            int ly = gy + i * gh / 4;
            g.fill(gx, ly, gx + gw, ly + 1, 0x332c3a2a);
        }
        drawPowerSeries(g, supply, gx, gy, gw, gh, max, DUTheme.OK);
        drawPowerSeries(g, demand, gx, gy, gw, gh, max, powerColor());
        g.text(font, "Prod", x + 5, y + h - 8, DUTheme.OK, false);
        g.text(font, "Cons", x + 44, y + h - 8, powerColor(), false);
        String top = formatFe(max) + "/t";
        g.text(font, top, x + w - 5 - font.width(top), y + 5, DUTheme.TEXT_DIM, false);
    }

    private int powerColor() {
        return ctx.power().powered() ? DUTheme.PROGRESS_ORANGE : DUTheme.ERROR;
    }

    private static void drawPowerSeries(GuiGraphicsExtractor g, List<Integer> values, int x, int y, int w, int h, int max, int color) {
        if (values == null || values.size() < 2) return;
        int count = Math.min(values.size(), w);
        int start = values.size() - count;
        int lastX = x;
        int lastY = y + h - Math.round(values.get(start) / (float) max * h);
        for (int i = 1; i < count; i++) {
            int index = start + i;
            int px = x + Math.round(i * (w - 1) / (float) Math.max(1, count - 1));
            int py = y + h - Math.round(values.get(index) / (float) max * h);
            DUTheme.line(g, lastX, lastY, px, py, color);
            lastX = px;
            lastY = py;
        }
    }

    private static float powerRatio(int value, int max) {
        return max <= 0 ? 0f : Math.max(0f, Math.min(1f, value / (float) max));
    }

    private static String formatFe(int centiFe) {
        int whole = centiFe / 100;
        int frac = Math.abs(centiFe % 100);
        if (frac == 0) return whole + " FE";
        if (frac % 10 == 0) return whole + "." + (frac / 10) + " FE";
        return whole + "." + (frac < 10 ? "0" : "") + frac + " FE";
    }

    private int bar(GuiGraphicsExtractor g, int x, int y, int w, String label, int value, int max, int color) {
        g.text(font, label, x, y, DUTheme.TEXT_DIM, false);
        int barX = x + 70, barW = w - 100;
        DUTheme.progress(g, barX, y - 1, barW, 9, value / (float) max, color);
        g.text(font, String.valueOf(value), barX + barW + 4, y, DUTheme.TEXT, false);
        return y + 14;
    }

    private int renderFilterEditor(GuiGraphicsExtractor g, GraphNodeData node) {
        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4;
        int y = contentY + 30;
        int slot = 15;
        int gap = 2;
        g.text(font, "FILTER", x, y, DUTheme.TEXT_GREEN, false);
        y += 11;

        String[] modes = {"PASS", "ALLOW", "BLOCK"};
        for (int i = 0; i < modes.length; i++) {
            int bx = x + i * 42;
            boolean on = (i == 0 && settings.mode() == FilterSettings.Mode.PASS_ALL)
                    || (i == 1 && settings.mode() == FilterSettings.Mode.WHITELIST)
                    || (i == 2 && settings.mode() == FilterSettings.Mode.BLACKLIST);
            DUTheme.box(g, bx, y, 39, 12, on ? 0xFF14303a : DUTheme.PANEL_ALT, on ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            g.text(font, modes[i], bx + 4, y + 3, on ? DUTheme.SELECTED : DUTheme.TEXT_DIM, false);
        }
        y += 16;

        var items = settings.paddedItems();
        for (int i = 0; i < FilterSettings.GHOST_SLOTS; i++) {
            int gx = x + (i % 3) * (slot + gap);
            int gy = y + (i / 3) * (slot + gap);
            DUTheme.box(g, gx, gy, slot, slot, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
            String id = items.get(i);
            if (!id.isBlank()) ItemIcons.render(g, id, gx, gy, ItemIcons.SIZE, DUTheme.PANEL_ALT);
        }
        int controlX = x + 58;
        int cy = y;
        cy = filterChannelControl(g, controlX, cy, settings.channel());
        cy = filterControl(g, controlX, cy, "Priority", String.valueOf(settings.priority()));
        cy = filterControl(g, controlX, cy, "Limit", String.valueOf(settings.limit()));
        cy = filterControl(g, controlX, cy, "Tick", settings.tickSpeed() + "t");
        cy = filterControl(g, controlX, cy, "Route", settings.routing() == FilterSettings.Routing.ROUND_ROBIN ? "Round" : "First");
        return Math.max(y + 54, cy) + 4;
    }

    /** Channel node editor: a single channel swatch control (pass-all routing onto matching links). */
    private int renderChannelEditor(GuiGraphicsExtractor g, GraphNodeData node) {
        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4;
        int y = contentY + 30;
        g.text(font, "CHANNEL", x, y, DUTheme.TEXT_GREEN, false);
        g.text(font, "Routes onto outputs of this channel", x, y + 11, DUTheme.TEXT_DIM, false);
        filterChannelControl(g, x, y + 23, settings.channel());
        return y + 39;
    }

    /** Small informational panel for nodes that have no configurable settings (Splitter/Combine). */
    private int renderRoutingInfo(GuiGraphicsExtractor g, String title, String line1, String line2) {
        int x = rightX + 4;
        int y = contentY + 30;
        g.text(font, title, x, y, DUTheme.TEXT_GREEN, false);
        g.text(font, line1, x, y + 11, DUTheme.TEXT_DIM, false);
        g.text(font, line2, x, y + 21, DUTheme.TEXT_DIM, false);
        return y + 35;
    }

    private int filterControl(GuiGraphicsExtractor g, int x, int y, String key, String value) {
        DUTheme.box(g, x, y, rightX + rightW - x - 4, 10, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.text(font, key, x + 3, y + 2, DUTheme.TEXT_DIM, false);
        g.text(font, value, x + 56, y + 2, DUTheme.TEXT, false);
        return y + 12;
    }

    /** Channel row: shows the dye colour as a swatch rather than spelling out the colour name. */
    private int filterChannelControl(GuiGraphicsExtractor g, int x, int y, String channel) {
        DUTheme.box(g, x, y, rightX + rightW - x - 4, 10, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.text(font, "Channel", x + 3, y + 2, DUTheme.TEXT_DIM, false);
        int color = FilterSettings.channelColor(channel);
        if (color == 0) {
            g.text(font, "None", x + 56, y + 2, DUTheme.TEXT_DIM, false);
        } else {
            g.fill(x + 56, y + 2, x + 66, y + 9, 0xFF0B0F0A);
            g.fill(x + 57, y + 3, x + 65, y + 8, color);
        }
        return y + 12;
    }

    private boolean handleFilterEditorClick(double mx, double my) {
        GraphNodeData node = ctx.selectedNode();
        if (node == null) return false;
        if (node.type() == NodeType.CHANNEL) {
            int cxr = rightX + 4;
            int cyr = contentY + 53;
            if (mx >= cxr && mx <= rightX + rightW - 4 && my >= cyr && my <= cyr + 10) {
                return updateFilter(node, FilterSettings.parse(node.notes()).nextChannel());
            }
            return false;
        }
        if (node.type() != NodeType.FILTER) return false;
        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4;
        int y = contentY + 41;
        if (my >= y && my <= y + 12) {
            if (mx >= x && mx < x + 39) return updateFilter(node, settings.withMode(FilterSettings.Mode.PASS_ALL));
            if (mx >= x + 42 && mx < x + 81) return updateFilter(node, settings.withMode(FilterSettings.Mode.WHITELIST));
            if (mx >= x + 84 && mx < x + 123) return updateFilter(node, settings.withMode(FilterSettings.Mode.BLACKLIST));
        }
        y += 16;
        int slot = 15;
        int gap = 2;
        for (int i = 0; i < FilterSettings.GHOST_SLOTS; i++) {
            int gx = x + (i % 3) * (slot + gap);
            int gy = y + (i / 3) * (slot + gap);
            if (mx >= gx && mx <= gx + slot && my >= gy && my <= gy + slot) {
                openFilterPicker(i);
                return true;
            }
        }
        int controlX = x + 58;
        int controlW = rightX + rightW - controlX - 4;
        String hit = filterControlHit(mx, my, controlX, y, controlW);
        if (hit == null) return false;
        return switch (hit) {
            case "Channel" -> updateFilter(node, settings.nextChannel());
            case "Priority" -> updateFilter(node, settings.nextPriority());
            case "Limit" -> updateFilter(node, settings.nextLimit());
            case "Tick" -> updateFilter(node, settings.nextTickSpeed());
            case "Route" -> updateFilter(node, settings.nextRouting());
            default -> false;
        };
    }

    private static String filterControlHit(double mx, double my, int x, int y, int w) {
        String[] keys = {"Channel", "Priority", "Limit", "Tick", "Route"};
        for (int i = 0; i < keys.length; i++) {
            int cy = y + i * 12;
            if (mx >= x && mx <= x + w && my >= cy && my <= cy + 10) return keys[i];
        }
        return null;
    }

    private boolean updateFilter(GraphNodeData node, FilterSettings settings) {
        ctx.setGraph(LogisticsGraph.updateNode(ctx.graph(), node.withNotes(settings.serialize())));
        return true;
    }

    private void openFilterPicker(int slot) {
        filterPickerOpen = true;
        filterPickerSlot = slot;
        filterPickerScroll = 0;
        filterPickerSearch = "";
        filterPickerRecentTab = !FILTER_PICKER_RECENTS.isEmpty();
        contextMenu = null;
        inventoryPopup = null;
    }

    private void closeFilterPicker() {
        filterPickerOpen = false;
        filterPickerSlot = -1;
        filterPickerScroll = 0;
        filterPickerSearch = "";
        filterPickerRecentTab = false;
    }

    private List<PickerEntry> filterPickerEntries() {
        if (filterPickerEntries == null) {
            List<PickerEntry> entries = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) continue;
                String registryId = id.toString();
                String name = new ItemStack(item).getHoverName().getString();
                entries.add(new PickerEntry(registryId, name, (name + " " + registryId).toLowerCase(Locale.ROOT)));
            }
            entries.sort(Comparator.comparing(PickerEntry::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PickerEntry::registryId));
            filterPickerEntries = List.copyOf(entries);
        }
        return filterPickerEntries;
    }

    private List<PickerEntry> filteredPickerEntries() {
        if (filterPickerRecentTab) {
            List<PickerEntry> recent = new ArrayList<>();
            for (String id : FILTER_PICKER_RECENTS) {
                PickerEntry entry = pickerEntry(id);
                if (entry != null) recent.add(entry);
            }
            return recent;
        }
        String query = filterPickerSearch.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return filterPickerEntries();
        List<PickerEntry> matches = new ArrayList<>();
        for (PickerEntry entry : filterPickerEntries()) {
            if (entry.searchText().contains(query)) matches.add(entry);
        }
        return matches;
    }

    private PickerEntry pickerEntry(String registryId) {
        for (PickerEntry entry : filterPickerEntries()) {
            if (entry.registryId().equals(registryId)) return entry;
        }
        return null;
    }

    private void renderFilterPicker(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int x = (guiW - w) / 2;
        int y = contentY + 10;
        DUTheme.box(g, x - 3, y - 3, w + 6, h + 6, 0xF01b3320, DUTheme.SELECTED);
        DUTheme.box(g, x, y, w, h, 0xFF111b14, DUTheme.SELECTED);
        g.text(font, "CHOOSE FILTER ITEM", x + 8, y + 8, DUTheme.TEXT_GREEN, false);
        String slotText = filterPickerSlot >= 0 ? "Slot " + (filterPickerSlot + 1) : "Slot";
        g.text(font, slotText, x + w - 8 - font.width(slotText), y + 8, DUTheme.TEXT_DIM, false);

        int tabY = y + 22;
        pickerTab(g, x + 8, tabY, 44, "ALL", !filterPickerRecentTab);
        pickerTab(g, x + 56, tabY, 58, "RECENT", filterPickerRecentTab);

        int searchY = y + 39;
        DUTheme.box(g, x + 8, searchY, w - 16, 16, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        String search = filterPickerRecentTab ? "Recent picks" : filterPickerSearch.isBlank() ? "Search item/block name or id..." : filterPickerSearch;
        g.text(font, trim(font, search, w - 26), x + 12, searchY + 5, filterPickerSearch.isBlank() || filterPickerRecentTab ? DUTheme.TEXT_DIM : DUTheme.TEXT, false);
        if (!filterPickerRecentTab && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = x + 12 + font.width(trim(font, filterPickerSearch, w - 34));
            g.fill(cursorX, searchY + 4, cursorX + 1, searchY + 13, DUTheme.SELECTED);
        }

        int listY = y + 61;
        int listH = h - 85;
        int rowH = 20;
        List<PickerEntry> entries = filteredPickerEntries();
        int visibleRows = Math.max(1, listH / rowH);
        filterPickerScroll = Math.max(0, Math.min(filterPickerScroll, Math.max(0, entries.size() - visibleRows)));
        g.enableScissor(x + 8, listY, x + w - 8, listY + listH);
        for (int row = 0; row < visibleRows && filterPickerScroll + row < entries.size(); row++) {
            PickerEntry entry = entries.get(filterPickerScroll + row);
            int ry = listY + row * rowH;
            boolean hover = mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= ry && mouseY < ry + rowH;
            DUTheme.box(g, x + 8, ry, w - 16, rowH - 1, hover ? 0xFF1f4a32 : (row % 2 == 0 ? 0xFF152018 : 0xFF101710), hover ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            ItemIcons.render(g, entry.registryId(), x + 10, ry + 2, ItemIcons.SIZE, DUTheme.PANEL_ALT);
            g.text(font, trim(font, entry.name(), w - 54), x + 30, ry + 3, hover ? DUTheme.SELECTED : DUTheme.TEXT, false);
            g.text(font, trim(font, entry.registryId(), w - 54), x + 30, ry + 12, DUTheme.TEXT_DIM, false);
        }
        g.disableScissor();
        if (entries.isEmpty()) {
            g.centeredText(font, filterPickerRecentTab ? "No recent items yet" : "No matching items", x + w / 2, listY + listH / 2, DUTheme.TEXT_DIM);
        }

        int footerY = y + h - 18;
        DUTheme.box(g, x + 8, footerY, 58, 12, DUTheme.PANEL_ALT, DUTheme.WARN);
        g.centeredText(font, "CLEAR", x + 37, footerY + 3, DUTheme.WARN);
        g.text(font, "Esc closes", x + 74, footerY + 3, DUTheme.TEXT_DIM, false);
    }

    private void pickerTab(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean selected) {
        DUTheme.box(g, x, y, w, 13, selected ? 0xFF1f4a32 : DUTheme.PANEL_ALT, selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
        g.centeredText(font, label, x + w / 2, y + 3, selected ? DUTheme.SELECTED : DUTheme.TEXT_DIM);
    }

    private boolean handleFilterPickerClick(double mx, double my) {
        if (!filterPickerOpen) return false;
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int x = (guiW - w) / 2;
        int y = contentY + 10;
        if (mx < x || mx > x + w || my < y || my > y + h) {
            closeFilterPicker();
            return true;
        }
        int tabY = y + 22;
        if (my >= tabY && my <= tabY + 13) {
            if (mx >= x + 8 && mx <= x + 52) { filterPickerRecentTab = false; filterPickerScroll = 0; return true; }
            if (mx >= x + 56 && mx <= x + 114) { filterPickerRecentTab = true; filterPickerScroll = 0; return true; }
        }
        int footerY = y + h - 18;
        if (mx >= x + 8 && mx <= x + 66 && my >= footerY && my <= footerY + 12) {
            setFilterPickerSlot("");
            return true;
        }
        int listY = y + 61;
        int listH = h - 85;
        int rowH = 20;
        if (mx >= x + 8 && mx <= x + w - 8 && my >= listY && my < listY + listH) {
            int index = filterPickerScroll + (int) ((my - listY) / rowH);
            List<PickerEntry> entries = filteredPickerEntries();
            if (index >= 0 && index < entries.size()) setFilterPickerSlot(entries.get(index).registryId());
            return true;
        }
        return true;
    }

    private void setFilterPickerSlot(String registryId) {
        GraphNodeData node = ctx.selectedNode();
        if (node != null && node.type() == NodeType.FILTER && filterPickerSlot >= 0) {
            FilterSettings settings = FilterSettings.parse(node.notes()).withItem(filterPickerSlot, registryId);
            updateFilter(node, settings);
            addFilterPickerRecent(registryId);
        }
        closeFilterPicker();
    }

    private static void addFilterPickerRecent(String registryId) {
        if (registryId == null || registryId.isBlank()) return;
        FILTER_PICKER_RECENTS.remove(registryId);
        FILTER_PICKER_RECENTS.add(0, registryId);
        while (FILTER_PICKER_RECENTS.size() > PICKER_RECENT_LIMIT) FILTER_PICKER_RECENTS.remove(FILTER_PICKER_RECENTS.size() - 1);
    }

    private boolean handleFilterPickerKey(KeyEvent event) {
        if (!filterPickerOpen) return false;
        if (event.key() == KEY_ESCAPE) {
            closeFilterPicker();
            return true;
        }
        if (event.key() == 259) {
            if (!filterPickerSearch.isEmpty()) {
                filterPickerSearch = filterPickerSearch.substring(0, filterPickerSearch.length() - 1);
                filterPickerScroll = 0;
            }
            return true;
        }
        if (filterPickerRecentTab) return true;
        String typed = pickerKeyText(event);
        if (!typed.isEmpty() && filterPickerSearch.length() < 64) {
            filterPickerSearch += typed;
            filterPickerScroll = 0;
            return true;
        }
        return true;
    }

    private static String pickerKeyText(KeyEvent event) {
        int key = event.key();
        boolean shift = (event.modifiers() & 1) != 0;
        if (key >= 65 && key <= 90) return String.valueOf((char) (shift ? key : key + 32));
        if (key >= 48 && key <= 57) return String.valueOf((char) key);
        return switch (key) {
            case 32 -> " ";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ":";
            case 95 -> "_";
            default -> "";
        };
    }

    private void scrollFilterPicker(double delta) {
        if (!filterPickerOpen) return;
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int visibleRows = Math.max(1, (h - 85) / 20);
        int max = Math.max(0, filteredPickerEntries().size() - visibleRows);
        filterPickerScroll = Math.max(0, Math.min(max, filterPickerScroll - (int) Math.signum(delta) * 3));
    }

    private record PickerEntry(String registryId, String name, String searchText) {}

    // --- Page 4: settings ---

    private void renderSettingsPage(GuiGraphicsExtractor g, int lx, int ty) {
        int x = lx + 10, y = contentY, w = guiW - 20, h = tabBarY - contentY - 8;
        panel(g, x, y, w, h, tr("doolestools.computer.page.settings"));

        int sx = leftX + 12;
        int bx = sx + SETTINGS_COL_W + 20;
        int wx = bx + SETTINGS_COL_W + 20;
        int headY = contentY + 16;
        // Section headers, each above its column of controls.
        g.text(font, "DISPLAY", sx, headY, DUTheme.TEXT_GREEN, false);
        g.text(font, "GRAPH DATA", bx, headY, DUTheme.TEXT_GREEN, false);
        g.text(font, "NETWORK & SHARING", wx, headY, DUTheme.TEXT_GREEN, false);

        // Network & sharing readout, below its controls (cycle access / name / whitelist add+remove).
        int netY = contentY + 28 + 4 * 22 + 4;
        g.text(font, "Name: " + ctx.networkName(), wx, netY, DUTheme.TEXT, false);
        g.text(font, "ID: " + ctx.networkId(), wx, netY + 11, DUTheme.TEXT_DIM, false);
        g.text(font, "Access: " + ctx.accessMode().toUpperCase(java.util.Locale.ROOT) + (ctx.canEdit() ? "" : "  (VIEW ONLY)"),
                wx, netY + 22, ctx.canEdit() ? DUTheme.OK : DUTheme.WARN, false);
        String whitelist;
        if (ctx.editorWhitelist().isEmpty()) {
            whitelist = "(none)";
        } else {
            List<String> names = new ArrayList<>();
            for (String entry : ctx.editorWhitelist()) names.add(whitelistDisplayName(entry));
            whitelist = String.join(", ", names);
        }
        if (whitelist.length() > 44) whitelist = whitelist.substring(0, 41) + "...";
        g.text(font, "Whitelist: " + whitelist, wx, netY + 33, DUTheme.TEXT_DIM, false);
        g.text(font, "Add an online player's name or a UUID.", wx, netY + 44, DUTheme.TEXT_DIM, false);

        g.text(font, "Display options are client-side. Graph JSON uses your clipboard + server validation.",
                sx, tabBarY - 14, DUTheme.TEXT_DIM, false);
    }

    /** Resolves a stored whitelist UUID to the player's name via the tab list, falling back to a short id. */
    private static String whitelistDisplayName(String entry) {
        try {
            java.util.UUID id = java.util.UUID.fromString(entry);
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                var info = connection.getPlayerInfo(id);
                if (info != null && info.getProfile() != null) {
                    String name = info.getProfile().name();
                    if (name != null && !name.isBlank()) return name;
                }
            }
            return entry.substring(0, Math.min(8, entry.length()));
        } catch (IllegalArgumentException ignored) {
            return entry;
        }
    }

    private void cycleNetworkAccess() {
        if (!ctx.canEdit()) return;
        String next = switch (ctx.accessMode()) {
            case "shared" -> "private";
            case "private" -> "whitelist";
            default -> "shared";
        };
        ClientNetworkSender.setComputerNetworkSettings(ctx.pos(), ctx.networkName(), next, ctx.editorWhitelist());
    }

    private void nameNetworkFromGraph() {
        if (!ctx.canEdit()) return;
        ClientNetworkSender.setComputerNetworkSettings(ctx.pos(), ctx.graph().graphName(), ctx.accessMode(), ctx.editorWhitelist());
    }

    private void addWhitelistEntry() {
        if (!ctx.canEdit() || whitelistEntryField == null) return;
        String value = whitelistEntryField.getValue() == null ? "" : whitelistEntryField.getValue().trim();
        if (value.isBlank()) return;
        List<String> next = new ArrayList<>(ctx.editorWhitelist());
        if (!next.contains(value)) next.add(value);
        ClientNetworkSender.setComputerNetworkSettings(ctx.pos(), ctx.networkName(), ctx.accessMode(), next);
        whitelistEntryField.setValue("");
    }

    private void removeLastWhitelistEntry() {
        if (!ctx.canEdit()) return;
        List<String> next = new ArrayList<>(ctx.editorWhitelist());
        if (next.isEmpty()) return;
        next.remove(next.size() - 1);
        ClientNetworkSender.setComputerNetworkSettings(ctx.pos(), ctx.networkName(), ctx.accessMode(), next);
    }

    private void renderHeader(GuiGraphicsExtractor g, int lx, int ty) {
        DUTheme.box(g, lx + 10, ty + 8, guiW - 20, 28, DUTheme.PANEL, DUTheme.PANEL_BORDER);
        GuiSprites.draw(g, GuiSprites.LOGO, lx + 14, ty + 10, 24);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(lx + 40, ty + 12);
        pose.scale(1.35f, 1.35f);
        DUTheme.glowText(g, font, tr("doolestools.logigraph.brand"), 0, 0, DUTheme.TEXT_GREEN);
        pose.popMatrix();
        // Subtitle sits directly under the brand wordmark, left-aligned with it.
        g.text(font, tr("doolestools.logigraph.subtitle"), lx + 41, ty + 25, DUTheme.TEXT_DIM, false);
    }

    private void renderTaskbar(GuiGraphicsExtractor g, int lx, int ty) {
        int y = tabBarY - 2;
        DUTheme.box(g, lx + 5, y, guiW - 10, 21, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        // Horizontal page bar bottom-left: one button per page for quick switching (also Left/Right keys).
        int bx = lx + 10;
        int by = y + 3;
        for (int i = 0; i < PAGE_ORDER.length; i++) {
            boolean active = page == PAGE_ORDER[i];
            int bw = font.width(PAGE_TABS[i]) + 12;
            DUTheme.box(g, bx, by, bw, 15, active ? 0xFF14303a : DUTheme.PANEL, active ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            g.text(font, PAGE_TABS[i], bx + 6, by + 4, active ? DUTheme.SELECTED : DUTheme.TEXT_DIM, false);
            bx += bw + 3;
        }

        int startX = lx + 10;
        int startY = y + 3;
        boolean connected = ctx.lastScanTime() >= 0 && !ctx.scan().isEmpty();
        String net = connected ? "NET ONLINE" : "NO NETWORK";
        int netColor = connected ? DUTheme.OK : DUTheme.WARN;
        String graphStatus = ctx.isDirty() ? "GRAPH EDITED" : "GRAPH SAVED";
        int graphStatusColor = ctx.isDirty() ? DUTheme.WARN : DUTheme.OK;
        String clock = LocalTime.now().format(CLOCK);
        int clockX = lx + guiW - 12 - font.width(clock);
        g.text(font, clock, clockX, startY + 4, DUTheme.TEXT_DIM, false);
        int netX = clockX - 12 - font.width(net);
        DUTheme.dot(g, netX - 8, startY + 5, netColor);
        g.text(font, net, netX, startY + 4, netColor, false);
        int graphX = netX - 14 - font.width(graphStatus);
        DUTheme.dot(g, graphX - 8, startY + 5, graphStatusColor);
        g.text(font, graphStatus, graphX, startY + 4, graphStatusColor, false);
    }

    private void renderStartMenu(GuiGraphicsExtractor g, int lx, int ty, int mouseX, int mouseY) {
        int x = lx + 10;
        int y = tabBarY - 92;
        int w = 142;
        int h = 88;
        DUTheme.box(g, x, y, w, h, 0xFA0b0f0a, DUTheme.SELECTED);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, DUTheme.PANEL_HEADER);
        DUTheme.glowText(g, font, "DOOLE OS", x + 6, y + 3, DUTheme.TEXT_GREEN);
        startMenuEntry(g, x, y + 17, w, tr("doolestools.computer.page.network"), PAGE_SCANNED, mouseX, mouseY);
        startMenuEntry(g, x, y + 34, w, tr("doolestools.computer.page.factory_manager"), PAGE_GRAPH, mouseX, mouseY);
        startMenuEntry(g, x, y + 51, w, tr("doolestools.computer.page.power"), PAGE_STATS, mouseX, mouseY);
        startMenuEntry(g, x, y + 68, w, tr("doolestools.computer.page.settings"), PAGE_SETTINGS, mouseX, mouseY);
    }

    private void startMenuEntry(GuiGraphicsExtractor g, int x, int y, int w, String label, int targetPage, int mouseX, int mouseY) {
        boolean hover = mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= y && mouseY <= y + 14;
        boolean active = page == targetPage;
        if (hover || active) g.fill(x + 4, y, x + w - 4, y + 14, active ? 0xFF14303a : 0xFF111d16);
        g.text(font, label, x + 9, y + 3, active ? DUTheme.SELECTED : hover ? DUTheme.TEXT : DUTheme.TEXT_DIM, false);
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL, DUTheme.PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 11, DUTheme.PANEL_HEADER);
        DUTheme.glowText(g, font, title, x + 4, y + 2, DUTheme.TEXT_GREEN);
    }

    private void renderCanvasLegend(GuiGraphicsExtractor g, int x, int yBottom) {
        int ly = yBottom - 10;
        g.enableScissor(canvasX, ly - 3, canvasX + canvasW, ly + 12);
        int lx = x + 4;
        lx += legendDot(g, lx, ly - 2, DUTheme.OK, "OK") + 4;
        lx += legendDot(g, lx, ly - 2, DUTheme.WARN, "Warning") + 4;
        lx += legendDot(g, lx, ly - 2, DUTheme.ERROR, "Error") + 4;
        lx += legendDot(g, lx, ly - 2, DUTheme.SELECTED, "Selected") + 4;
        String hint = "Drag network devices here · OUT→IN links · MMB pan · Scroll zoom";
        int hintX = canvasX + canvasW - font.width(hint) - 8;
        if (hintX > lx + 8) g.text(font, hint, hintX, ly + 1, DUTheme.TEXT_DIM, false);
        g.disableScissor();
    }

    private void renderCanvasTitleStrip(GuiGraphicsExtractor g) {
        List<GraphCanvasData> canvases = ctx.graph().canvasesOrLegacy();
        int x = canvasX + 4;
        int y = contentY + 1;
        int plusW = 12;
        int gap = 3;
        int tabW = Math.max(44, Math.min(108, (canvasW - 14 - plusW - gap * Math.max(0, canvases.size())) / Math.max(1, canvases.size())));
        for (int i = 0; i < canvases.size(); i++) {
            GraphCanvasData canvas = canvases.get(i);
            int tx = x + i * (tabW + gap);
            boolean active = canvas.canvasId().equals(ctx.graph().activeCanvas().canvasId());
            DUTheme.box(g, tx, y, tabW, 10, active ? 0xF014201b : 0xC00a100d,
                    active ? (editingGraphName ? DUTheme.SELECTED : DUTheme.TEXT_GREEN) : DUTheme.PANEL_BORDER);
            GuiSprites.draw(g, GuiSprites.CANVAS, tx + 2, y + 1, 8);
            String title = canvas.title() == null || canvas.title().isBlank() ? "Untitled" : canvas.title();
            int closeReserve = canvases.size() > 1 ? 10 : 0;
            g.text(font, trim(font, title, tabW - 15 - closeReserve), tx + 13, y + 1, active ? DUTheme.TEXT : DUTheme.TEXT_DIM, false);
            if (canvases.size() > 1) {
                int cx = tx + tabW - 9;
                GuiSprites.draw(g, GuiSprites.CLOSE, cx + 1, y + 2, 7);
            }
        }
        int px = x + canvases.size() * (tabW + gap);
        if (px + plusW <= canvasX + canvasW - 4) {
            DUTheme.box(g, px, y, plusW, 10, 0xE80a100d, DUTheme.PANEL_BORDER);
            g.centeredText(font, "+", px + plusW / 2, y + 1, DUTheme.TEXT_GREEN);
        }
    }

    private boolean handleCanvasTitleStripClick(double mx, double my) {
        List<GraphCanvasData> canvases = ctx.graph().canvasesOrLegacy();
        int x = canvasX + 4;
        int y = contentY + 1;
        if (my < y || my > y + 11) return false;
        int plusW = 12;
        int gap = 3;
        int tabW = Math.max(44, Math.min(108, (canvasW - 14 - plusW - gap * Math.max(0, canvases.size())) / Math.max(1, canvases.size())));
        for (int i = 0; i < canvases.size(); i++) {
            int tx = x + i * (tabW + gap);
            if (mx >= tx && mx <= tx + tabW) {
                GraphCanvasData canvas = canvases.get(i);
                if (canvases.size() > 1 && mx >= tx + tabW - 11) {
                    ctx.setGraph(LogisticsGraph.removeCanvas(ctx.graph(), canvas.canvasId()));
                    ctx.clearSelection();
                    editingGraphName = false;
                    return true;
                }
                ctx.setGraph(LogisticsGraph.switchCanvas(ctx.graph(), canvas.canvasId()));
                ctx.clearSelection();
                editingGraphName = true;
                nameField.setValue(canvas.title() == null || canvas.title().isBlank() ? "Untitled" : canvas.title());
                focusNameField();
                return true;
            }
        }
        int px = x + canvases.size() * (tabW + gap);
        if (mx >= px && mx <= px + plusW && px + plusW <= canvasX + canvasW - 4) {
            createCanvas();
            return true;
        }
        return false;
    }

    private void hamburgerGlyph(GuiGraphicsExtractor g, int x, int y) {
        for (int i = 0; i < 3; i++) g.fill(x, y + i * 3, x + 8, y + i * 3 + 1, DUTheme.TEXT_DIM);
    }

    private void panGlyph(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 3, y, x + 5, y + 8, DUTheme.TEXT_DIM);
        g.fill(x, y + 3, x + 8, y + 5, DUTheme.TEXT_DIM);
    }

    private void pinGlyph(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 3, y, x + 5, y + 5, DUTheme.TEXT_DIM);
        g.fill(x + 1, y + 4, x + 7, y + 6, DUTheme.TEXT_DIM);
        g.fill(x + 3, y + 6, x + 5, y + 8, DUTheme.TEXT_DIM);
    }

    private void renderMarquee(GuiGraphicsExtractor g) {
        int x1 = (int) Math.min(marqueeStartX, marqueeCurX);
        int y1 = (int) Math.min(marqueeStartY, marqueeCurY);
        int x2 = (int) Math.max(marqueeStartX, marqueeCurX);
        int y2 = (int) Math.max(marqueeStartY, marqueeCurY);
        x1 = Math.max(canvasX, x1); y1 = Math.max(contentY, y1);
        x2 = Math.min(canvasX + canvasW, x2); y2 = Math.min(contentY + contentH, y2);
        if (x2 <= x1 || y2 <= y1) return;
        g.fill(x1, y1, x2, y2, 0x223fd2e0);
        DUTheme.outline(g, x1, y1, x2 - x1, y2 - y1, DUTheme.SELECTED);
    }

    private void renderScannedDragPreview(GuiGraphicsExtractor g) {
        String label = draggingScannedName == null ? "Network Device" : draggingScannedName;
        int x = (int) scannedDragX + 8;
        int y = (int) scannedDragY + 8;
        int w = Math.min(130, Math.max(72, font.width(label) + 18));
        DUTheme.box(g, x, y, w, 18, 0xEE0a100d, canvasWidget.contains(scannedDragX, scannedDragY) ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
        g.fill(x + 4, y + 5, x + 10, y + 11, DUTheme.TEXT_GREEN);
        g.text(font, label, x + 14, y + 5, DUTheme.TEXT, false);
    }

    private void renderConnectionDropPreview(GuiGraphicsExtractor g) {
        if (!canvasWidget.contains(scannedDragX, scannedDragY)) return;
        GraphLinkData link = canvasWidget.linkAt(scannedDragX, scannedDragY);
        if (link == null) return;
        int x = (int) scannedDragX - 42;
        int y = (int) scannedDragY - 20;
        DUTheme.box(g, x, y, 84, 14, 0xEE14201b, DUTheme.SELECTED);
        GuiSprites.draw(g, GuiSprites.CANVAS, x + 4, y + 2, 10);
        g.text(font, "INSERT HERE", x + 18, y + 4, DUTheme.SELECTED, false);
    }

    private int legendDot(GuiGraphicsExtractor g, int x, int y, int color, String label) {
        int w = font.width(label) + 20;
        DUTheme.box(g, x, y, w, 13, 0xD50a100d, color);
        g.fill(x, y, x + 1, y + 1, DUTheme.SCREEN);
        g.fill(x + w - 1, y, x + w, y + 1, DUTheme.SCREEN);
        g.fill(x, y + 12, x + 1, y + 13, DUTheme.SCREEN);
        g.fill(x + w - 1, y + 12, x + w, y + 13, DUTheme.SCREEN);
        if (color == DUTheme.OK) GuiSprites.draw(g, GuiSprites.STATUS_OK, x + 4, y + 1, 10);
        else if (color == DUTheme.WARN) GuiSprites.draw(g, GuiSprites.STATUS_WARN, x + 4, y + 1, 10);
        else if (color == DUTheme.ERROR) GuiSprites.draw(g, GuiSprites.STATUS_ERROR, x + 4, y + 1, 10);
        else g.fill(x + 6, y + 4, x + 12, y + 10, color);
        g.text(font, label, x + 16, y + 3, DUTheme.TEXT_DIM, false);
        return w;
    }

    private static int iconColor(ScannedBlockData s) {
        return switch (s.type()) {
            case STORAGE -> 0xFFb07a3a;
            case MACHINE -> 0xFF8a8f96;
            case TRANSPORT -> 0xFF6a7066;
            default -> 0xFF55405a;
        };
    }

    private static String typeLabel(ScannedBlockData s) {
        return switch (s.type()) {
            case STORAGE -> "Storage";
            case MACHINE -> "Machine";
            case TRANSPORT -> "Transport";
            case UNKNOWN_STORAGE -> "Storage?";
            case UNKNOWN_MACHINE -> "Machine?";
            default -> "Unknown";
        };
    }

    private void syncNameField() {
        if (editingGraphName) {
            nameField.setEditable(true);
            linkButton.setToggled(ctx.linkMode);
            return;
        }
        var label = ctx.selectedText();
        if (label != null) {
            if (!label.textId().equals(lastSelectedTextId)) {
                nameField.setValue(label.text());
                lastSelectedTextId = label.textId();
                lastSelectedFrameId = null;
                lastSelectedNodeId = null;
            }
            nameField.setEditable(true);
            linkButton.setToggled(ctx.linkMode);
            return;
        }
        lastSelectedTextId = null;

        var frame = ctx.selectedFrame();
        if (frame != null) {
            if (!frame.frameId().equals(lastSelectedFrameId)) {
                nameField.setValue(frame.label());
                lastSelectedFrameId = frame.frameId();
                lastSelectedNodeId = null;
            }
            nameField.setEditable(true);
            linkButton.setToggled(ctx.linkMode);
            return;
        }
        lastSelectedFrameId = null;

        String sel = ctx.selectedNodeId;
        if (sel != null && !sel.equals(lastSelectedNodeId)) {
            GraphNodeData n = ctx.selectedNode();
            if (n != null) nameField.setValue(n.displayName());
            lastSelectedNodeId = sel;
        } else if (sel == null) {
            lastSelectedNodeId = null;
        }
        nameField.setEditable(ctx.selectedNode() != null);
        linkButton.setToggled(ctx.linkMode);
    }

    private static String tr(String key) {
        return I18n.get(key);
    }

    private void syncFilterRuleField() {
        if (filterRuleField == null) return;
        GraphNodeData n = ctx.selectedNode();
        boolean show = n != null && n.type() == NodeType.FILTER;
        filterRuleField.visible = page == PAGE_GRAPH && show;
        filterRuleField.active = page == PAGE_GRAPH && show;
        filterRuleField.setEditable(show);
        if (!show) {
            lastFilterNodeId = null;
            return;
        }
        if (!n.nodeId().equals(lastFilterNodeId)) {
            filterRuleField.setValue(n.notes());
            lastFilterNodeId = n.nodeId();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }

    // --- input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = designX(event.x());
        double my = designY(event.y());
        MouseButtonEvent de = new MouseButtonEvent(mx, my, event.buttonInfo());
        boolean right = event.button() == 1;
        boolean middle = event.button() == 2;

        if (!right && handleFilterPickerClick(mx, my)) return true;
        if (filterPickerOpen) return true;

        if (inventoryPopup != null) {
            boolean keep = inventoryPopup.mouseClicked(mx, my);
            if (!keep) inventoryPopup = null;
            return true;
        }

        if (contextMenu != null) {
            contextMenu.click(mx, my);
            contextMenu = null;
            return true;
        }

        if (!right && handleStartMenuClick(mx, my)) return true;
        if (startMenuOpen && !right) {
            startMenuOpen = false;
            return true;
        }

        if (page == PAGE_SCANNED) {
            // Row click selects the block (so it's preselected when you switch to the flowgraph).
            int rowsY = contentY + 14;
            int idx = (int) ((my - rowsY) / SCAN_ROW_H) + scannedScroll;
            if (!right && my >= rowsY && idx >= 0 && idx < ctx.scan().size()
                    && mx >= leftX + 12 && mx <= leftPos + guiW - 12) {
                ctx.selectedScannedId = ctx.scan().get(idx).id();
                return true;
            }
            return super.mouseClicked(de, doubleClick);
        }
        if (page != PAGE_GRAPH) return super.mouseClicked(de, doubleClick);

        if (!right && warningBar.click(mx, my)) return true;
        if (!right && handleCanvasTitleStripClick(mx, my)) return true;

        // Right-panel detail clicks (e.g. SHOW ALL inventory button)
        if (!right && mx >= rightX && mx <= rightX + rightW && my >= contentY && my <= contentY + contentH) {
            if (handleFilterEditorClick(mx, my)) return true;
            if (detailsPanel.mouseClicked(mx, my)) return true;
        }

        ScannedBlockData row = listWidget.rowAt(mx, my);
        if (row != null && !right) {
            editingGraphName = false;
            ctx.selectedScannedId = row.id();
            draggingScanned = true;
            draggingScannedName = row.blockName();
            scannedDragX = mx;
            scannedDragY = my;
            long now = System.currentTimeMillis();
            if (row.id().equals(lastClickScannedId) && now - lastClickTime < 300) ctx.addNodeForSelectedScan();
            lastClickScannedId = row.id();
            lastClickTime = now;
            return true;
        }

        if (canvasWidget.contains(mx, my)) {
            // Overlay widgets (zoom, tool palette) take click priority.
            for (var child : children()) {
                if (child instanceof AbstractWidget w && w.visible && w.isMouseOver(mx, my)) {
                    return super.mouseClicked(de, doubleClick);
                }
            }
            lastMouseX = mx;
            lastMouseY = my;

            GraphNodeData node = canvasWidget.nodeAt(mx, my);
            var frameTitle = canvasWidget.frameTitleAt(mx, my);
            var text = canvasWidget.textAt(mx, my);

            if (middle) { panning = true; return true; }
            if (right) { openContextMenu(mx, my, node, frameTitle, text); return true; }

            GraphCanvasWidget.LinkSideHit sideHit = canvasWidget.linkSideAt(mx, my);
            if (sideHit != null) {
                String nextSide = LogisticsGraph.nextSideOverride(sideHit.link().sideOverride());
                ctx.setGraph(LogisticsGraph.setLinkSideOverride(ctx.graph(), sideHit.link().linkId(), nextSide));
                saveGraph();
                return true;
            }

            GraphCanvasWidget.LinkEndpointHit endpoint = canvasWidget.linkTargetAt(mx, my);
            if (endpoint != null) { ctx.startLinkRetarget(endpoint.link().linkId(), mx, my); return true; }
            GraphCanvasWidget.PortHit outPort = canvasWidget.portAt(mx, my, PortDirection.OUT);
            if (outPort != null) {
                ctx.selectSingleNode(outPort.node().nodeId());
                ctx.startPortLink(outPort.node().nodeId(), outPort.port().portId(), mx, my);
                return true;
            }

            // Frame resize grip (only when no node sits on top of the corner).
            var resizeFrame = node == null ? canvasWidget.frameResizeHandleAt(mx, my) : null;
            if (resizeFrame != null) {
                editingGraphName = false;
                ctx.selectedFrameId = resizeFrame.frameId();
                ctx.selectedNodeId = null;
                ctx.selectedNodeIds.clear();
                resizingFrameId = resizeFrame.frameId();
                return true;
            }

            if (text != null && node == null) {
                editingGraphName = false;
                ctx.clearSelection();
                ctx.selectedTextId = text.textId();
                draggingTextId = text.textId();
                return true;
            }
            if (node != null) {
                editingGraphName = false;
                if (ctx.linkMode) {
                    if (ctx.linkSourceId == null) ctx.linkSourceId = node.nodeId();
                    else ctx.tryLink(node.nodeId());
                } else {
                    if (!ctx.selectedNodeIds.contains(node.nodeId())) ctx.selectSingleNode(node.nodeId());
                    else { ctx.selectedNodeId = node.nodeId(); ctx.selectedFrameId = null; }
                    ctx.selectedScannedId = node.scannedBlockId();
                    // Shift-drag duplicates the selection and drags the copies instead.
                    if ((event.modifiers() & MOD_SHIFT) != 0) {
                        ctx.duplicateSelectedNodes();
                        draggingNodeId = ctx.selectedNodeId;
                    } else {
                        draggingNodeId = node.nodeId();
                    }
                }
            } else if (frameTitle != null) {
                editingGraphName = false;
                ctx.selectedFrameId = frameTitle.frameId();
                ctx.selectedNodeId = null;
                ctx.selectedNodeIds.clear();
                draggingFrameId = frameTitle.frameId();
                framedNodeIds = canvasWidget.nodeIdsInFrame(frameTitle);
            } else {
                editingGraphName = false;
                if (!ctx.linkMode) ctx.clearSelection();
                marqueeing = true;
                marqueeStartX = marqueeCurX = mx;
                marqueeStartY = marqueeCurY = my;
            }
            return true;
        }
        return super.mouseClicked(de, doubleClick);
    }

    private boolean handleStartMenuClick(double mx, double my) {
        int by = tabBarY + 1;
        if (my < by || my > by + 15) return false;
        int bx = leftPos + 10;
        for (int i = 0; i < PAGE_ORDER.length; i++) {
            int bw = font.width(PAGE_TABS[i]) + 12;
            if (mx >= bx && mx <= bx + bw) {
                setPage(PAGE_ORDER[i]);
                return true;
            }
            bx += bw + 3;
        }
        return false;
    }

    private void cyclePage(int dir) {
        int idx = 0;
        for (int i = 0; i < PAGE_ORDER.length; i++) if (PAGE_ORDER[i] == page) idx = i;
        setPage(PAGE_ORDER[Math.floorMod(idx + dir, PAGE_ORDER.length)]);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = designX(event.x());
        double my = designY(event.y());
        MouseButtonEvent de = new MouseButtonEvent(mx, my, event.buttonInfo());
        double ddx = dx / uiScale, ddy = dy / uiScale;
        if (page != PAGE_GRAPH) return super.mouseDragged(de, ddx, ddy);
        if (draggingScanned) { scannedDragX = mx; scannedDragY = my; return true; }
        if (ctx.isDraggingPortLink() || ctx.isDraggingLinkRetarget()) { ctx.updateLinkDrag(mx, my); return true; }
        if (marqueeing) { marqueeCurX = mx; marqueeCurY = my; return true; }
        int cdx = (int) Math.round((mx - lastMouseX) / ctx.zoom);
        int cdy = (int) Math.round((my - lastMouseY) / ctx.zoom);
        if (resizingFrameId != null) {
            var f = ctx.graph().findFrame(resizingFrameId);
            if (f != null && (cdx != 0 || cdy != 0)) {
                int nw = Math.max(80, f.width() + cdx);
                int nh = Math.max(48, f.height() + cdy);
                ctx.setGraph(LogisticsGraph.updateFrame(ctx.graph(), f.withSize(nw, nh)));
                lastMouseX = mx; lastMouseY = my;
            }
            return true;
        }
        if (draggingFrameId != null) {
            var f = ctx.graph().findFrame(draggingFrameId);
            if (f != null && (cdx != 0 || cdy != 0)) {
                LogisticsGraphData gg = LogisticsGraph.updateFrame(ctx.graph(), f.withPosition(f.x() + cdx, f.y() + cdy));
                // Carry the nodes captured inside the frame at drag start.
                for (String id : framedNodeIds) {
                    GraphNodeData n = gg.findNode(id);
                    if (n != null) gg = LogisticsGraph.updateNode(gg, n.withPosition(n.x() + cdx, n.y() + cdy));
                }
                ctx.setGraph(gg);
                lastMouseX = mx; lastMouseY = my;
            }
            return true;
        }
        if (draggingTextId != null) {
            var t = ctx.graph().findText(draggingTextId);
            if (t != null && (cdx != 0 || cdy != 0)) {
                ctx.setGraph(LogisticsGraph.updateText(ctx.graph(), t.withPosition(t.x() + cdx, t.y() + cdy)));
                lastMouseX = mx; lastMouseY = my;
            }
            return true;
        }
        if (draggingNodeId != null) {
            if (cdx != 0 || cdy != 0) {
                java.util.Collection<String> moving = ctx.selectedNodeIds.size() > 1
                        ? new ArrayList<>(ctx.selectedNodeIds) : List.of(draggingNodeId);
                LogisticsGraphData gg = ctx.graph();
                for (String id : moving) {
                    GraphNodeData n = gg.findNode(id);
                    if (n != null) gg = LogisticsGraph.updateNode(gg, n.withPosition(n.x() + cdx, n.y() + cdy));
                }
                ctx.setGraph(gg);
                lastMouseX = mx; lastMouseY = my;
            }
            return true;
        }
        if (panning) {
            ctx.panX += (mx - lastMouseX); ctx.panY += (my - lastMouseY);
            lastMouseX = mx; lastMouseY = my;
            return true;
        }
        return super.mouseDragged(de, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mx = designX(event.x()), my = designY(event.y());
        MouseButtonEvent de = new MouseButtonEvent(mx, my, event.buttonInfo());
        if (page == PAGE_GRAPH) {
            if (draggingScanned) {
                if (canvasWidget.contains(mx, my)) {
                    GraphLinkData link = canvasWidget.linkAt(mx, my);
                    String scannedId = ctx.selectedScannedId;
                    ctx.addNodeForSelectedScanAt(snap((int) canvasWidget.toCanvasX(mx)), snap((int) canvasWidget.toCanvasY(my)));
                    if (link != null && scannedId != null) {
                        GraphNodeData inserted = null;
                        for (GraphNodeData node : ctx.graph().activeCanvas().nodes()) {
                            if (node.scannedBlockId().equals(scannedId)) {
                                inserted = node;
                                break;
                            }
                        }
                        if (inserted != null) {
                            ctx.setGraph(LogisticsGraph.insertNodeIntoLink(ctx.graph(), link.linkId(), inserted.nodeId()));
                            ctx.selectSingleNode(inserted.nodeId());
                        }
                    }
                }
                draggingScanned = false;
                draggingScannedName = null;
                return true;
            }
            if (ctx.isDraggingPortLink() || ctx.isDraggingLinkRetarget()) {
                GraphCanvasWidget.PortHit inPort = canvasWidget.portAt(mx, my, PortDirection.IN);
                if (inPort != null) ctx.completePortLink(inPort.node().nodeId(), inPort.port().portId());
                else ctx.completePortLink(null, null);
                return true;
            }
            if (marqueeing) {
                List<String> ids = canvasWidget.nodesInScreenRect(marqueeStartX, marqueeStartY, marqueeCurX, marqueeCurY);
                ctx.selectedNodeIds.clear();
                ctx.selectedNodeIds.addAll(ids);
                ctx.selectedNodeId = ids.isEmpty() ? null : ids.get(ids.size() - 1);
                ctx.selectedFrameId = null;
                marqueeing = false;
                return true;
            }
        }
        snapActiveDragTarget();
        // Dropping a single node onto a link splices it in (source → node → target), removing the old link.
        if (page == PAGE_GRAPH && draggingNodeId != null && ctx.selectedNodeIds.size() <= 1) {
            GraphLinkData link = canvasWidget.linkAt(mx, my);
            if (link != null && !draggingNodeId.equals(link.sourceNodeId()) && !draggingNodeId.equals(link.targetNodeId())) {
                ctx.setGraph(LogisticsGraph.insertNodeIntoLink(ctx.graph(), link.linkId(), draggingNodeId));
            }
        }
        draggingNodeId = null;
        draggingFrameId = null;
        resizingFrameId = null;
        framedNodeIds = java.util.List.of();
        draggingTextId = null;
        draggingScanned = false;
        draggingScannedName = null;
        panning = false;
        return super.mouseReleased(de);
    }

    private void snapActiveDragTarget() {
        LogisticsGraphData graph = ctx.graph();
        if (resizingFrameId != null) {
            var f = graph.findFrame(resizingFrameId);
            if (f != null) ctx.setGraph(LogisticsGraph.updateFrame(graph, f.withSize(snap(f.width()), snap(f.height()))));
        } else if (draggingFrameId != null) {
            var f = graph.findFrame(draggingFrameId);
            if (f != null) {
                LogisticsGraphData gg = LogisticsGraph.updateFrame(graph, f.withPosition(snap(f.x()), snap(f.y())));
                for (String id : framedNodeIds) {
                    GraphNodeData n = gg.findNode(id);
                    if (n != null) gg = LogisticsGraph.updateNode(gg, n.withPosition(snap(n.x()), snap(n.y())));
                }
                ctx.setGraph(gg);
            }
        } else if (draggingTextId != null) {
            var t = graph.findText(draggingTextId);
            if (t != null) ctx.setGraph(LogisticsGraph.updateText(graph, t.withPosition(snap(t.x()), snap(t.y()))));
        } else if (draggingNodeId != null) {
            java.util.Collection<String> moving = ctx.selectedNodeIds.size() > 1
                    ? new ArrayList<>(ctx.selectedNodeIds) : List.of(draggingNodeId);
            LogisticsGraphData gg = graph;
            for (String id : moving) {
                GraphNodeData n = gg.findNode(id);
                if (n != null) gg = LogisticsGraph.updateNode(gg, n.withPosition(snap(n.x()), snap(n.y())));
            }
            ctx.setGraph(gg);
        }
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double sx, double sy) {
        double mx = designX(rawX), my = designY(rawY);
        if (filterPickerOpen) { scrollFilterPicker(sy); return true; }
        if (inventoryPopup != null) { inventoryPopup.mouseScrolled(sy); return true; }
        contextMenu = null;
        if (page == PAGE_SCANNED) {
            scannedScroll = Math.max(0, scannedScroll - (int) Math.signum(sy));
            return true;
        }
        if (page == PAGE_GRAPH) {
            if (listWidget.contains(mx, my)) { listWidget.scroll(sy, ctx.filteredScan().size()); return true; }
            if (canvasWidget.contains(mx, my)) {
                float old = ctx.zoom;
                ctx.zoom = Math.max(0.4f, Math.min(2.0f, ctx.zoom + (float) sy * 0.1f));
                ctx.panX -= (mx - canvasWidget.x - ctx.panX) * (ctx.zoom / old - 1f);
                ctx.panY -= (my - canvasWidget.y - ctx.panY) * (ctx.zoom / old - 1f);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }


    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleFilterPickerKey(event)) return true;
        if (event.key() == KEY_ESCAPE) {
            if (inventoryPopup != null) { inventoryPopup = null; return true; }
            if (contextMenu != null) { contextMenu = null; return true; }
            return super.keyPressed(event);
        }
        // Route keys to whichever text field is focused (name, whitelist entry, filter rule…).
        if (getFocused() instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused()) {
            return box.keyPressed(event);
        }
        if (event.key() == KEY_LEFT) { cyclePage(-1); return true; }
        if (event.key() == KEY_RIGHT) { cyclePage(1); return true; }
        if ((event.modifiers() & MOD_SHIFT) != 0 && event.key() == KEY_D && page == PAGE_GRAPH) {
            ctx.duplicateSelectedNodes();
            return true;
        }
        if (isAlt(event) && event.key() == KEY_S) {
            ClientNetworkSender.scanArea(ctx.pos());
            return true;
        }
        if (isAlt(event) && event.key() == KEY_N) {
            createCanvas();
            return true;
        }
        if (page == PAGE_GRAPH && event.key() == KEY_F) {
            canvasWidget.fitView();
            return true;
        }
        if (page == PAGE_GRAPH && event.key() == KEY_DELETE) {
            if (ctx.selectedTextId != null) { ctx.deleteText(ctx.selectedTextId); return true; }
            if (ctx.selectedFrameId != null) { ctx.deleteFrame(ctx.selectedFrameId); return true; }
            if (ctx.selectedNodeId != null || !ctx.selectedNodeIds.isEmpty()) { ctx.deleteSelectedNode(); return true; }
        }
        return true;
    }

    private static boolean isAlt(KeyEvent event) {
        return (event.modifiers() & MOD_ALT) != 0;
    }

    private static String trim(Font font, String s, int maxWidth) {
        if (s == null) return "";
        if (font.width(s) <= maxWidth) return s;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (font.width(b.toString() + s.charAt(i) + "…") > maxWidth) break;
            b.append(s.charAt(i));
        }
        return b + "…";
    }

    @Override
    public void onClose() {
        if (ctx.isDirty()) saveGraph();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<ScannedBlockData> debugScan() {
        return ctx.scan();
    }
}
