package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.FilterDiagnostics;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Right panel rendering details for the selected node (text + sections). */
public class NodeDetailsPanel {

    private static final int SHOW_ALL_THRESHOLD = 5;

    private final EditorContext ctx;
    private final Runnable showAllCallback;
    public int x, y, w, h;

    // Hit-test state for "SHOW ALL" button — updated each render pass
    private int showAllY = -1;
    private boolean showAllVisible = false;

    public NodeDetailsPanel(EditorContext ctx, int x, int y, int w, int h, Runnable showAllCallback) {
        this.ctx = ctx;
        this.showAllCallback = showAllCallback;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    /** Returns true if a click in design-space coords was consumed. */
    public boolean mouseClicked(double mx, double my) {
        if (showAllVisible && showAllY >= 0
                && mx >= x && mx <= x + w
                && my >= showAllY && my <= showAllY + 11) {
            if (showAllCallback != null) showAllCallback.run();
            return true;
        }
        return false;
    }

    public void render(GuiGraphicsExtractor g, Font font, int nameFieldBottom) {
        // Clip everything to the panel so long detail lists never spill over the buttons / other panels.
        g.enableScissor(x, nameFieldBottom, x + w, y + h);
        try {
            renderBody(g, font, nameFieldBottom);
        } finally {
            g.disableScissor();
        }
    }

    private void renderBody(GuiGraphicsExtractor g, Font font, int nameFieldBottom) {
        showAllVisible = false;
        showAllY = -1;

        GraphNodeData node = ctx.selectedNode();
        if (node == null) {
            g.text(font, "No node selected.", x, y, DUTheme.TEXT_DIM, false);
            g.text(font, "Add a scanned block to", x, y + 14, DUTheme.TEXT_DIM, false);
            g.text(font, "the canvas, then click it.", x, y + 24, DUTheme.TEXT_DIM, false);
            return;
        }
        ScannedBlockData s = ctx.scannedFor(node);
        int cy = nameFieldBottom + 4;

        cy = kv(g, font, "Type", node.type().name(), cy);
        if (node.type() == NodeType.FILTER) {
            FilterSettings filter = FilterSettings.parse(node.notes());
            FilterDiagnostics diagnostics = FilterDiagnostics.inspect(ctx.graph(), node, filter);
            cy = kv(g, font, "Mode", FilterSettings.modeLabel(filter.mode()), cy);
            cy = kv(g, font, "Matches", diagnostics.ghostItems() + " ghost items", cy);
            cy = kv(g, font, "Limit", filter.limit() + " items", cy);
            cy = kv(g, font, "Channel", filter.channel(), cy);
            cy = kv(g, font, "Priority", String.valueOf(filter.priority()), cy);
            cy = kv(g, font, "Tick", filter.tickSpeed() + " ticks", cy);
            cy = kv(g, font, "Routing", filter.routing() == FilterSettings.Routing.ROUND_ROBIN ? "Round robin" : "First valid", cy);
            cy = kv(g, font, "Routes", diagnostics.inboundItemRoutes() + " in / " + diagnostics.outboundItemRoutes() + " out", cy);
            cy = kv(g, font, "Saved", ctx.isDirty() ? "Save graph to apply" : "Server graph current", cy);
            if (!diagnostics.warnings().isEmpty()) {
                g.text(font, "FILTER WARNINGS", x, cy, DUTheme.WARN, false);
                cy += 11;
                for (String warning : diagnostics.warnings()) {
                    g.text(font, trim(font, warning, w), x, cy, DUTheme.WARN, false);
                    cy += 10;
                }
            }
            g.text(font, "3x3 ghost grid controls item matches.", x, cy, DUTheme.TEXT_DIM, false);
            cy += 10;
            g.text(font, "Priority 0 ignores priority.", x, cy, DUTheme.TEXT_DIM, false);
            cy += 14;
        }
        if (s != null) {
            cy = kv(g, font, "Block", s.blockName(), cy);
            cy = kv(g, font, "Pos", s.pos().getX() + ", " + s.pos().getY() + ", " + s.pos().getZ(), cy);
            cy = kv(g, font, "Status", s.hasWarnings() ? "Attention" : "Online", cy);
        } else if (node.type() != NodeType.FILTER) {
            cy = kv(g, font, "Status", "Block missing / unloaded", cy);
        }

        cy += 4;
        if (s != null && s.inventory().hasData()) {
            DUTheme.glowText(g, font, "INVENTORY", x, cy, DUTheme.TEXT_GREEN);
            cy += 11;
            List<ItemEntry> top = s.inventory().topStacks();
            int preview = Math.min(top.size(), SHOW_ALL_THRESHOLD);
            for (int i = 0; i < preview; i++) {
                ItemEntry e = top.get(i);
                ItemIcons.render(g, e.registryId(), x, cy, ItemIcons.SIZE, DUTheme.PANEL_ALT);
                g.text(font, trim(font, e.displayName(), w - 44), x + 20, cy + 4, DUTheme.TEXT, false);
                String count = String.valueOf(e.count());
                g.text(font, count, x + w - font.width(count), cy + 4, DUTheme.TEXT_DIM, false);
                cy += 18;
            }
            if (top.size() > SHOW_ALL_THRESHOLD) {
                showAllY = cy;
                showAllVisible = true;
                String label = "[ SHOW ALL " + top.size() + " ITEMS ]";
                DUTheme.box(g, x, cy, w, 11, DUTheme.PANEL_ALT, DUTheme.TEXT_GREEN);
                g.centeredText(font, label, x + w / 2, cy + 2, DUTheme.TEXT_GREEN);
                cy += 13;
            }
            g.text(font, s.inventory().usedSlots() + " / " + s.inventory().totalSlots() + " slots ("
                    + s.inventory().fillPercent() + "%)", x, cy, DUTheme.TEXT_DIM, false);
            cy += 12;
        }

        if (s != null && s.furnace().hasData()) {
            var f = s.furnace();
            g.text(font, "FURNACE", x, cy, DUTheme.TEXT_GREEN, false);
            cy += 11;
            // Recipe row with real item icons: input -> result.
            if (f.hasRecipe() || !f.inputId().isEmpty()) {
                g.text(font, "Recipe", x, cy + 4, DUTheme.TEXT_DIM, false);
                int ix = x + 46;
                ItemIcons.render(g, f.inputId(), ix, cy, ItemIcons.SIZE, DUTheme.PANEL_ALT);
                ix += 18;
                g.text(font, "→", ix, cy + 4, DUTheme.TEXT_DIM, false);
                ix += font.width("→") + 4;
                ItemIcons.render(g, f.resultId(), ix, cy, ItemIcons.SIZE, DUTheme.PANEL_ALT);
                cy += 18;
            }
            cy = kv(g, font, "Fuel", orDash(f.fuelItem()), cy);
            cy = kv(g, font, "State", f.status(), cy);
            if (f.cookTotal() > 0) {
                long elapsed = DUTheme.clientGameTime() - s.lastScannedGameTime();
                int pct = f.predictedPercent(elapsed);
                g.text(font, "Cooking", x, cy, DUTheme.TEXT_DIM, false);
                DUTheme.progress(g, x + 46, cy, w - 78, 7, pct / 100f, DUTheme.PROGRESS_ORANGE);
                g.text(font, pct + "%", x + w - font.width(pct + "%"), cy, DUTheme.TEXT_DIM, false);
                cy += 11;
                if (f.isCooking()) {
                    cy = kv(g, font, "Time", DUTheme.formatTicks(f.predictedRemainingTicks(elapsed)) + " left", cy);
                }
            }
        }

        if (s != null && s.progress().present() && !s.furnace().hasData()) {
            var p = s.progress();
            g.text(font, "MACHINE", x, cy, DUTheme.TEXT_GREEN, false);
            cy += 11;
            cy = kv(g, font, "State", p.status(), cy);
            g.text(font, trim(font, p.label(), 44), x, cy, DUTheme.TEXT_DIM, false);
            DUTheme.progress(g, x + 46, cy, w - 78, 7, p.percent() / 100f,
                    p.error() ? DUTheme.ERROR : p.active() ? DUTheme.OK : DUTheme.WARN);
            g.text(font, p.percent() + "%", x + w - font.width(p.percent() + "%"), cy, DUTheme.TEXT_DIM, false);
            cy += 11;
            if (p.hasTimer()) {
                cy = kv(g, font, "Timer", "~" + DUTheme.formatTicks((int) Math.max(0L, Math.min(Integer.MAX_VALUE, p.remainingTicks()))) + " left", cy);
            }
        }

        if (s != null && s.energy().hasData()) {
            g.text(font, "ENERGY", x, cy, DUTheme.TEXT_GREEN, false);
            cy += 11;
            cy = kv(g, font, "Stored", s.energy().stored() + " / " + s.energy().capacity(), cy);
        }

        // Connections.
        cy += 2;
        g.text(font, "CONNECTED TO", x, cy, DUTheme.TEXT_GREEN, false);
        cy += 11;
        boolean any = false;
        for (GraphLinkData l : ctx.graph().activeCanvas().links()) {
            String other = null;
            if (l.sourceNodeId().equals(node.nodeId())) {
                GraphNodeData tn = ctx.graph().findNode(l.targetNodeId());
                if (tn != null) other = "→ " + tn.displayName();
            } else if (l.targetNodeId().equals(node.nodeId())) {
                GraphNodeData sn = ctx.graph().findNode(l.sourceNodeId());
                if (sn != null) other = "← " + sn.displayName();
            }
            if (other != null) {
                g.text(font, trim(font, other, w), x, cy, DUTheme.TEXT, false);
                cy += 10;
                any = true;
            }
        }
        if (!any) {
            g.text(font, "None", x, cy, DUTheme.TEXT_DIM, false);
            cy += 10;
        }

        // Warnings.
        cy += 2;
        g.text(font, "WARNINGS", x, cy, DUTheme.ERROR, false);
        cy += 11;
        if (s != null && s.hasWarnings()) {
            for (WarningData wd : s.warnings()) {
                g.text(font, trim(font, wd.message(), w), x, cy, color(wd), false);
                cy += 10;
            }
        } else {
            g.text(font, "None", x, cy, DUTheme.TEXT_DIM, false);
        }
    }

    private int kv(GuiGraphicsExtractor g, Font font, String key, String value, int cy) {
        g.text(font, key, x, cy, DUTheme.TEXT_DIM, false);
        int vx = x + 46;
        g.text(font, trim(font, value, w - 48), vx, cy, DUTheme.TEXT, false);
        return cy + 11;
    }

    private static int color(WarningData w) {
        return switch (w.severity()) {
            case ERROR -> DUTheme.ERROR;
            case WARNING -> DUTheme.WARN;
            default -> DUTheme.TEXT_DIM;
        };
    }

    private static String orDash(String s) {
        return s == null || s.isEmpty() ? "—" : s;
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
}
