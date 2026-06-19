package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.LogisticsGraph;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;

/**
 * Renders the filter/channel/routing node editor in the right-hand panel of the graph page.
 * Also handles click events inside that area. All writes go through the EditorContext.
 */
public final class FilterNodeEditorPanel {

    private final EditorContext ctx;
    private final Font font;

    private int rightX, rightW, contentY;
    private IntConsumer openFilterPicker;

    public FilterNodeEditorPanel(EditorContext ctx, Font font) {
        this.ctx = ctx;
        this.font = font;
    }

    public void updateLayout(int rightX, int rightW, int contentY, IntConsumer openFilterPicker) {
        this.rightX = rightX;
        this.rightW = rightW;
        this.contentY = contentY;
        this.openFilterPicker = openFilterPicker;
    }

    /** Renders the editor for the selected node. Returns the y coordinate below the editor content. */
    public int render(GuiGraphics g) {
        GraphNodeData node = ctx.selectedNode();
        if (node == null) return contentY + 30;
        return switch (node.type()) {
            case FILTER  -> renderFilterEditor(g, node);
            case CHANNEL -> renderChannelEditor(g, node);
            case SPLITTER -> renderRoutingInfo(g, "SPLITTER", "Copies items to all outputs.", "All outputs receive a copy.");
            case COMBINE -> renderRoutingInfo(g, "COMBINE", "Merges inputs into one output.", "First available output wins.");
            default -> contentY + 30;
        };
    }

    /** Returns true if the click was consumed. */
    public boolean handleClick(double mx, double my) {
        GraphNodeData node = ctx.selectedNode();
        if (node == null) return false;
        if (node.type() == NodeType.CHANNEL) {
            int cxr = rightX + 4, cyr = contentY + 53;
            if (mx >= cxr && mx <= rightX + rightW - 4 && my >= cyr && my <= cyr + 10)
                return updateFilter(node, FilterSettings.parse(node.notes()).nextChannel());
            return false;
        }
        if (node.type() != NodeType.FILTER) return false;

        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4, y = contentY + 41;
        if (my >= y && my <= y + 12) {
            if (mx >= x && mx < x + 39)   return updateFilter(node, settings.withMode(FilterSettings.Mode.PASS_ALL));
            if (mx >= x + 42 && mx < x + 81)  return updateFilter(node, settings.withMode(FilterSettings.Mode.WHITELIST));
            if (mx >= x + 84 && mx < x + 123) return updateFilter(node, settings.withMode(FilterSettings.Mode.BLACKLIST));
        }
        y += 16;
        int slot = 15, gap = 2;
        for (int i = 0; i < FilterSettings.GHOST_SLOTS; i++) {
            int gx = x + (i % 3) * (slot + gap);
            int gy = y + (i / 3) * (slot + gap);
            if (mx >= gx && mx <= gx + slot && my >= gy && my <= gy + slot) {
                openFilterPicker.accept(i);
                return true;
            }
        }
        int controlX = x + 58;
        int controlW = rightX + rightW - controlX - 4;
        String hit = filterControlHit(mx, my, controlX, y, controlW);
        if (hit == null) return false;
        return switch (hit) {
            case "Channel"  -> updateFilter(node, settings.nextChannel());
            case "Priority" -> updateFilter(node, settings.nextPriority());
            case "Limit"    -> updateFilter(node, settings.nextLimit());
            case "Tick"     -> updateFilter(node, settings.nextTickSpeed());
            case "Route"    -> updateFilter(node, settings.nextRouting());
            default -> false;
        };
    }

    // --- Render helpers ---

    private int renderFilterEditor(GuiGraphics g, GraphNodeData node) {
        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4, y = contentY + 30;
        int slot = 15, gap = 2;
        g.drawString(font, "FILTER", x, y, DUTheme.TEXT_GREEN, false); y += 11;

        String[] modes = {"PASS", "ALLOW", "BLOCK"};
        for (int i = 0; i < modes.length; i++) {
            int bx = x + i * 42;
            boolean on = (i == 0 && settings.mode() == FilterSettings.Mode.PASS_ALL)
                      || (i == 1 && settings.mode() == FilterSettings.Mode.WHITELIST)
                      || (i == 2 && settings.mode() == FilterSettings.Mode.BLACKLIST);
            DUTheme.box(g, bx, y, 39, 12, on ? 0xFF14303a : DUTheme.PANEL_ALT, on ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            g.drawString(font, modes[i], bx + 4, y + 3, on ? DUTheme.SELECTED : DUTheme.TEXT_DIM, false);
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
        cy = filterControl(g, controlX, cy, "Limit",    String.valueOf(settings.limit()));
        cy = filterControl(g, controlX, cy, "Tick",     settings.tickSpeed() + "t");
        cy = filterControl(g, controlX, cy, "Route",    settings.routing() == FilterSettings.Routing.ROUND_ROBIN ? "Round" : "First");
        return Math.max(y + 54, cy) + 4;
    }

    private int renderChannelEditor(GuiGraphics g, GraphNodeData node) {
        FilterSettings settings = FilterSettings.parse(node.notes());
        int x = rightX + 4, y = contentY + 30;
        g.drawString(font, "CHANNEL", x, y, DUTheme.TEXT_GREEN, false);
        g.drawString(font, "Routes onto outputs of this channel", x, y + 11, DUTheme.TEXT_DIM, false);
        filterChannelControl(g, x, y + 23, settings.channel());
        return y + 39;
    }

    private int renderRoutingInfo(GuiGraphics g, String title, String line1, String line2) {
        int x = rightX + 4, y = contentY + 30;
        g.drawString(font, title, x, y, DUTheme.TEXT_GREEN, false);
        g.drawString(font, line1, x, y + 11, DUTheme.TEXT_DIM, false);
        g.drawString(font, line2, x, y + 21, DUTheme.TEXT_DIM, false);
        return y + 35;
    }

    private int filterControl(GuiGraphics g, int x, int y, String key, String value) {
        DUTheme.box(g, x, y, rightX + rightW - x - 4, 10, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.drawString(font, key,   x + 3,  y + 2, DUTheme.TEXT_DIM, false);
        g.drawString(font, value, x + 56, y + 2, DUTheme.TEXT, false);
        return y + 12;
    }

    private int filterChannelControl(GuiGraphics g, int x, int y, String channel) {
        DUTheme.box(g, x, y, rightX + rightW - x - 4, 10, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.drawString(font, "Channel", x + 3, y + 2, DUTheme.TEXT_DIM, false);
        int color = FilterSettings.channelColor(channel);
        if (color == 0) {
            g.drawString(font, "None", x + 56, y + 2, DUTheme.TEXT_DIM, false);
        } else {
            g.fill(x + 56, y + 2, x + 66, y + 9, 0xFF0B0F0A);
            g.fill(x + 57, y + 3, x + 65, y + 8, color);
        }
        return y + 12;
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
}
