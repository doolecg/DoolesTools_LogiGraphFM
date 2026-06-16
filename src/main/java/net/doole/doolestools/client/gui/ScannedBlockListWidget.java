package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Left panel listing network-visible devices with icon, name, distance, and a status light. */
public class ScannedBlockListWidget {

    public static final int ROW_H = 22;

    private final EditorContext ctx;
    public int x, y, w, h;
    private int scroll = 0;

    public ScannedBlockListWidget(EditorContext ctx, int x, int y, int w, int h) {
        this.ctx = ctx;
        this.x = x; this.y = y; this.w = w; this.h = h;
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
        List<ScannedBlockData> list = ctx.filteredScan();
        g.enableScissor(x, y, x + w, y + h);
        int row = 0;
        for (int i = scroll; i < list.size(); i++) {
            int ry = y + (row++) * ROW_H;
            if (ry > y + h) break;
            ScannedBlockData s = list.get(i);
            boolean selected = s.id().equals(ctx.selectedScannedId);

            g.fill(x, ry, x + w, ry + ROW_H - 1, selected ? 0xFF14301f : (row % 2 == 0 ? DUTheme.PANEL_ALT : DUTheme.PANEL));
            if (selected) DUTheme.outline(g, x, ry, w, ROW_H - 1, DUTheme.SELECTED);

            // Real item icon (falls back to a category-coloured square).
            ItemIcons.render(g, s.registryId(), x + 3, ry + 3, ItemIcons.SIZE, iconColor(s));

            g.text(font, trim(font, s.blockName(), w - 56), x + 23, ry + 4, DUTheme.TEXT, false);
            g.text(font, Math.round(s.distance()) + "m", x + 23, ry + 13, DUTheme.TEXT_DIM, false);

            DUTheme.dot(g, x + w - 12, ry + 8, ctx.statusColorFor(s));
        }
        g.disableScissor();

        if (list.isEmpty()) {
            g.centeredText(font, "NO DEVICES", x + w / 2, y + 8, DUTheme.TEXT_DIM);
            g.centeredText(font, "SCAN NETWORK", x + w / 2, y + 20, DUTheme.TEXT_DIM);
        }
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
