package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.GraphCanvasWidget;
import net.doole.doolestools.client.gui.GuiSprites;
import net.doole.doolestools.client.gui.TerminalButton;
import net.doole.doolestools.logistics.WarningGenerator;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.doole.doolestools.menu.LogisticsMonitorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only monitor screen. Shares the full-size terminal frame of the Logistics Computer and reuses
 * the same flowgraph canvas widget so nodes render identically; it just never edits the graph.
 */
public class LogisticsMonitorScreen extends AbstractContainerScreen<LogisticsMonitorMenu> {

    private static final int MIN_GUI_W = 420;
    private static final int MIN_GUI_H = 246;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] MODE_NAMES = {"FLOWGRAPH", "WARNINGS", "STORAGE SUMMARY"};

    private boolean linked;
    private BlockPos computerPos = BlockPos.ZERO;
    private int mode;
    private List<ScannedBlockData> scan = List.of();
    private final Map<String, ScannedBlockData> scanById = new HashMap<>();

    private EditorContext ctx;
    private GraphCanvasWidget canvasWidget;
    private boolean panning;
    private double lastMouseX, lastMouseY;

    private int guiW = MIN_GUI_W, guiH = MIN_GUI_H;
    private int contentX, contentY, contentW, contentH;

    public LogisticsMonitorScreen(LogisticsMonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = MIN_GUI_W;
        this.imageHeight = MIN_GUI_H;
    }

    public void applyState(boolean linked, BlockPos computerPos, int mode,
                           LogisticsGraphData graph, List<ScannedBlockData> scan) {
        this.linked = linked;
        this.computerPos = computerPos;
        this.mode = mode;
        this.scan = scan;
        this.scanById.clear();
        for (ScannedBlockData s : scan) scanById.put(s.id(), s);
        if (ctx == null) ctx = new EditorContext(menu.getPos());
        ctx.setState(scan, graph, 0L, net.doole.doolestools.logistics.data.NetworkPowerData.EMPTY);
        if (canvasWidget != null) canvasWidget.fitView();
    }

    public BlockPos monitorPos() {
        return menu.getPos();
    }

    @Override
    protected void init() {
        super.init();
        this.guiW = Math.max(MIN_GUI_W, this.width - 24);
        this.guiH = Math.max(MIN_GUI_H, this.height - 24);
        this.leftPos = (this.width - guiW) / 2;
        this.topPos = (this.height - guiH) / 2;
        int lx = leftPos, ty = topPos;

        contentX = lx + 10;
        contentY = ty + 44;
        contentW = guiW - 20;
        contentH = guiH - 44 - 30;

        if (ctx == null) ctx = new EditorContext(menu.getPos());
        canvasWidget = new GraphCanvasWidget(ctx, contentX, contentY, contentW, contentH);
        canvasWidget.fitView();

        addRenderableWidget(new TerminalButton(lx + guiW - 26, ty + 12, 16, 16, Component.literal("X"), this::onClose)
                .spriteOnly(GuiSprites.CLOSE));
        addRenderableWidget(new TerminalButton(lx + 10, ty + guiH - 26, 110, 14,
                Component.literal("CYCLE MODE"), this::cycleMode).sprite(GuiSprites.CANVAS).accent(DUTheme.PROGRESS_BLUE));
        ClientNetworkSender.requestMonitorSync(menu.getPos());
    }

    private void cycleMode() {
        ClientNetworkSender.setMonitorMode(menu.getPos(), (mode + 1) % MODE_NAMES.length);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int lx = leftPos, ty = topPos;

        DUTheme.bezel(g, lx, ty, guiW, guiH);
        DUTheme.screw(g, lx + 7, ty + 7);
        DUTheme.screw(g, lx + guiW - 7, ty + 7);
        DUTheme.screw(g, lx + 7, ty + guiH - 7);
        DUTheme.screw(g, lx + guiW - 7, ty + guiH - 7);
        DUTheme.box(g, lx + 5, ty + 5, guiW - 10, guiH - 10, DUTheme.SCREEN, DUTheme.PANEL_BORDER);

        renderHeader(g, lx, ty);

        // Content panel.
        DUTheme.box(g, contentX, contentY - 12, contentW, contentH + 12, DUTheme.PANEL, DUTheme.PANEL_BORDER);
        g.fill(contentX + 1, contentY - 11, contentX + contentW - 1, contentY - 1, DUTheme.PANEL_HEADER);
        g.drawString(font, "MODE: " + MODE_NAMES[Math.floorMod(mode, MODE_NAMES.length)], contentX + 4, contentY - 10, DUTheme.TEXT_GREEN, false);

        if (!linked) {
            g.drawCenteredString(font, "NO COMPUTER LINKED", lx + guiW / 2, ty + guiH / 2 - 8, DUTheme.WARN);
            g.drawCenteredString(font, "Place near a Logistics Computer", lx + guiW / 2, ty + guiH / 2 + 4, DUTheme.TEXT_DIM);
        } else {
            switch (Math.floorMod(mode, MODE_NAMES.length)) {
                case 0 -> { if (canvasWidget != null) canvasWidget.render(g, font, -1000, -1000); }
                case 1 -> renderWarnings(g, contentX + 6, contentY + 6, contentW - 12);
                default -> renderStorage(g, contentX + 6, contentY + 6, contentW - 12);
            }
        }

        // Footer: linked computer coords.
        String link = linked
                ? "Linked Computer: X " + computerPos.getX() + " Y " + computerPos.getY() + " Z " + computerPos.getZ()
                : "Linked Computer: none";
        link = trimToWidth(link, Math.max(80, guiW - 144));
        g.drawString(font, link, lx + guiW - 12 - font.width(link), ty + guiH - 22, DUTheme.TEXT_DIM, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        if (maxWidth <= font.width(ellipsis)) return font.plainSubstrByWidth(text, maxWidth);
        return font.plainSubstrByWidth(text, maxWidth - font.width(ellipsis)) + ellipsis;
    }

    private void renderHeader(GuiGraphics g, int lx, int ty) {
        DUTheme.box(g, lx + 10, ty + 8, guiW - 20, 28, DUTheme.PANEL, DUTheme.PANEL_BORDER);
        g.fill(lx + 14, ty + 12, lx + 34, ty + 32, DUTheme.PROGRESS_BLUE);
        DUTheme.outline(g, lx + 14, ty + 12, 20, 20, 0xFF0a2a3a);
        g.drawString(font, "D", lx + 21, ty + 18, 0xFF06140a, false);

        g.drawString(font, "DOOLE'S UTILS ", lx + 40, ty + 13, DUTheme.TEXT_GREEN, false);
        int w1 = font.width("DOOLE'S UTILS ");
        g.drawString(font, "// LOGIGRAPH", lx + 40 + w1, ty + 13, DUTheme.TEXT_DIM, false);
        g.drawString(font, "LOGISTICS MONITOR", lx + 40, ty + 24, DUTheme.TEXT_DIM, false);

        String status = linked ? "MIRRORING" : "NO LINK";
        int statusColor = linked ? DUTheme.OK : DUTheme.WARN;
        int sx = lx + guiW - 112;
        DUTheme.dot(g, sx - 8, ty + 13, statusColor);
        g.drawString(font, status, sx, ty + 12, statusColor, false);
        g.drawString(font, LocalTime.now().format(CLOCK), sx, ty + 24, DUTheme.TEXT_DIM, false);
    }

    private void renderWarnings(GuiGraphics g, int x, int y, int w) {
        List<WarningData> graphWarnings = WarningGenerator.forGraph(ctx != null ? ctx.graph() : LogisticsGraphData.EMPTY, scanById);
        int cy = y;
        boolean any = false;
        int bottom = contentY + contentH - 6;
        for (ScannedBlockData s : scan) {
            for (WarningData wd : s.warnings()) {
                if (wd.severity() == WarningData.Severity.INFO) continue;
                g.drawString(font, "- " + s.blockName() + ": " + wd.message(), x, cy, color(wd), false);
                cy += 11;
                any = true;
                if (cy > bottom) break;
            }
            if (cy > bottom) break;
        }
        for (WarningData wd : graphWarnings) {
            if (cy > bottom) break;
            g.drawString(font, "- " + wd.message(), x, cy, color(wd), false);
            cy += 11;
            any = true;
        }
        if (!any) {
            g.drawString(font, "No active warnings.", x, y, DUTheme.OK, false);
        }
    }

    private void renderStorage(GuiGraphics g, int x, int y, int w) {
        int cy = y;
        boolean any = false;
        int bottom = contentY + contentH - 6;
        int barX = x + Math.min(180, w / 2);
        for (ScannedBlockData s : scan) {
            if (!s.inventory().hasData()) continue;
            int pct = s.inventory().fillPercent();
            g.drawString(font, clip(s.blockName(), 22), x, cy, DUTheme.TEXT, false);
            DUTheme.progress(g, barX, cy, 100, 8, pct / 100f,
                    pct >= 100 ? DUTheme.ERROR : pct >= 85 ? DUTheme.WARN : DUTheme.OK);
            g.drawString(font, pct + "%", barX + 106, cy, DUTheme.TEXT_DIM, false);
            cy += 12;
            any = true;
            if (cy > bottom) break;
        }
        if (!any) {
            g.drawString(font, "No storage data. Scan from the computer.", x, y, DUTheme.TEXT_DIM, false);
        }
    }

    // --- read-only pan/zoom for the flowgraph view ---

    private boolean overCanvas(double mx, double my) {
        return mode == 0 && linked && canvasWidget != null && canvasWidget.contains(mx, my);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overCanvas(mouseX, mouseY)) {
            panning = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (panning && ctx != null) {
            ctx.panX += (mouseX - lastMouseX);
            ctx.panY += (mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overCanvas(mx, my)) {
            float old = ctx.zoom;
            ctx.zoom = Math.max(0.4f, Math.min(2.0f, ctx.zoom + (float) sy * 0.1f));
            ctx.panX -= (mx - canvasWidget.x - ctx.panX) * (ctx.zoom / old - 1f);
            ctx.panY -= (my - canvasWidget.y - ctx.panY) * (ctx.zoom / old - 1f);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private static int color(WarningData w) {
        return switch (w.severity()) {
            case ERROR -> DUTheme.ERROR;
            case WARNING -> DUTheme.WARN;
            default -> DUTheme.TEXT_DIM;
        };
    }

    private static String clip(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) : s;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
