package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Left panel listing network-visible devices with icon, name, distance, and a status light. */
public class ScannedBlockListWidget {

    public static final int ROW_H = 22;

    private final EditorContext ctx;
    public int x, y, w, h;
    private int scroll = 0;
    /** Scanned-block ids selected for a multi-drop drag (Ctrl/Shift click). */
    private final Set<String> multiSelectedIds = new LinkedHashSet<>();
    private int lastClickedRow = -1;
    private static final int TAB_W = 22;

    public ScannedBlockListWidget(EditorContext ctx, int x, int y, int w, int h) {
        this.ctx = ctx;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public Set<String> multiSelectedIds() {
        return multiSelectedIds;
    }

    public void clearMultiSelect() {
        multiSelectedIds.clear();
        lastClickedRow = -1;
    }

    /** Returns the row index under the cursor, or -1. */
    public int rowIndexAt(double mx, double my) {
        if (!contains(mx, my)) return -1;
        List<ScannedBlockData> list = ctx.filteredScan();
        int idx = (int) ((my - y) / ROW_H) + scroll;
        if (idx < 0 || idx >= list.size()) return -1;
        return idx;
    }

    /**
     * Handles modifier-aware clicks on the list. Ctrl toggles a row in/out of the multi-select set;
     * Shift range-selects from the last clicked row. Returns true if a multi-select gesture was handled
     * (so the caller should not start a normal drag); false for a plain click.
     */
    public boolean handleModifierClick(double mx, double my, boolean ctrl, boolean shift) {
        int idx = rowIndexAt(mx, my);
        if (idx < 0) return false;
        List<ScannedBlockData> list = ctx.filteredScan();
        if (ctrl) {
            String id = list.get(idx).id();
            if (!multiSelectedIds.remove(id)) multiSelectedIds.add(id);
            lastClickedRow = idx;
            return true;
        }
        if (shift && lastClickedRow >= 0) {
            int lo = Math.min(lastClickedRow, idx);
            int hi = Math.max(lastClickedRow, idx);
            multiSelectedIds.clear();
            for (int i = lo; i <= hi && i < list.size(); i++) multiSelectedIds.add(list.get(i).id());
            return true;
        }
        // Plain click: this row becomes the multi-select anchor and the sole member.
        lastClickedRow = idx;
        multiSelectedIds.clear();
        multiSelectedIds.add(list.get(idx).id());
        return false;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int maxScroll(int count) {
        int visible = h / ROW_H;
        return Math.max(0, count - visible);
    }

    public void scroll(double amount, int count) {
        scroll = Math.max(0, Math.min(maxScroll(count), scroll - (int) Math.signum(amount)));
    }

    /** Returns the scanned block under the cursor, or null. */
    public ScannedBlockData rowAt(double mx, double my) {
        if (!contains(mx, my)) return null;
        List<ScannedBlockData> list = ctx.filteredScan();
        int idx = (int) ((my - y) / ROW_H) + scroll;
        if (idx < 0 || idx >= list.size()) return null;
        return list.get(idx);
    }

    public void render(GuiGraphicsExtractor g, Font font) {
        renderNetworkTabs(g, font);
        List<ScannedBlockData> list = ctx.filteredScan();
        int listX = x + tabOffset();
        int listW = w - tabOffset();
        g.enableScissor(listX, y, x + w, y + h);
        int row = 0;
        for (int i = scroll; i < list.size(); i++) {
            int ry = y + (row++) * ROW_H;
            if (ry > y + h) break;
            ScannedBlockData s = list.get(i);
            boolean selected = s.id().equals(ctx.selectedScannedId);
            boolean multi = multiSelectedIds.contains(s.id());

            g.fill(listX, ry, x + w, ry + ROW_H - 1, (selected || multi) ? 0xFF14301f : (row % 2 == 0 ? DUTheme.PANEL_ALT : DUTheme.PANEL));
            if (selected || multi) DUTheme.outline(g, listX, ry, listW, ROW_H - 1, DUTheme.SELECTED);

            // Real item icon (falls back to a category-coloured square).
            ItemIcons.render(g, s.registryId(), listX + 3, ry + 3, ItemIcons.SIZE, iconColor(s));

            g.text(font, trim(font, s.blockName(), listW - 56), listX + 23, ry + 4, DUTheme.TEXT, false);
            g.text(font, Math.round(s.distance()) + "m", listX + 23, ry + 13, DUTheme.TEXT_DIM, false);

            DUTheme.dot(g, x + w - 12, ry + 8, ctx.statusColorFor(s));
        }
        g.disableScissor();

        if (list.isEmpty()) {
            g.centeredText(font, "NO DEVICES", listX + listW / 2, y + 8, DUTheme.TEXT_DIM);
            g.centeredText(font, "SCAN NETWORK", listX + listW / 2, y + 20, DUTheme.TEXT_DIM);
        }
    }

    public boolean handleNetworkTabClick(double mx, double my) {
        if (ctx.scanNetworkTabs().size() <= 1) return false;
        if (mx < x || mx >= x + TAB_W || my < y || my >= y + h) return false;
        int idx = (int) ((my - y) / 34);
        java.util.List<EditorContext.NetworkTab> tabs = ctx.scanNetworkTabs();
        if (idx < 0 || idx >= tabs.size()) return false;
        ctx.selectedScanNetworkId = tabs.get(idx).id();
        scroll = 0;
        clearMultiSelect();
        return true;
    }

    private void renderNetworkTabs(GuiGraphicsExtractor g, Font font) {
        java.util.List<EditorContext.NetworkTab> tabs = ctx.scanNetworkTabs();
        if (tabs.size() <= 1) return;
        for (int i = 0; i < tabs.size(); i++) {
            int ty = y + i * 34;
            if (ty >= y + h) break;
            EditorContext.NetworkTab tab = tabs.get(i);
            boolean selected = tab.id().equals(ctx.selectedScanNetworkId);
            int tabH = Math.min(y + h, ty + 32) - ty;
            g.fill(x, ty, x + TAB_W - 1, ty + tabH, selected ? DUTheme.SELECTED : DUTheme.PANEL_ALT);
            if (selected) g.fill(x + TAB_W - 2, ty, x + TAB_W - 1, ty + tabH, DUTheme.TEXT_GREEN);
            String label = tab.name() == null || tab.name().isBlank() ? "NET" : tab.name();
            String abbrev = label.length() >= 3
                    ? label.substring(0, 3).toUpperCase(java.util.Locale.ROOT)
                    : label.toUpperCase(java.util.Locale.ROOT);
            int labelColor = selected ? 0xFF001408 : DUTheme.TEXT_DIM;
            g.text(font, abbrev.substring(0, Math.min(1, abbrev.length())), x + 3, ty + 6, labelColor, false);
            if (abbrev.length() >= 2) g.text(font, abbrev.substring(1, 2), x + 3, ty + 14, labelColor, false);
            if (abbrev.length() >= 3) g.text(font, abbrev.substring(2, 3), x + 3, ty + 22, labelColor, false);
        }
    }

    private int tabOffset() {
        return ctx.scanNetworkTabs().size() > 1 ? TAB_W + 2 : 0;
    }

    private static int iconColor(ScannedBlockData s) {
        return switch (s.type()) {
            case STORAGE -> 0xFFb07a3a;
            case MACHINE -> 0xFF8a8f96;
            case TRANSPORT -> 0xFF6a7066;
            default -> 0xFF55405a;
        };
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
