package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** A small right-click context menu rendered on top of the canvas. */
public class CanvasContextMenu {

    /** A single menu row. {@code color} tints the label (e.g. red for destructive actions). */
    public record Item(String label, Runnable action, int color) {
        public Item(String label, Runnable action) {
            this(label, action, DUTheme.TEXT);
        }
    }

    private static final int ROW_H = 13;
    private static final int PAD = 4;

    private final List<Item> items;
    private int x, y, w, h;

    public CanvasContextMenu(int anchorX, int anchorY, List<Item> items, Font font, int maxX, int maxY) {
        this.items = items;
        int widest = 0;
        for (Item it : items) widest = Math.max(widest, font.width(it.label()));
        this.w = widest + PAD * 2 + 4;
        this.h = items.size() * ROW_H + 2;
        // Keep the menu fully on-screen.
        this.x = Math.min(anchorX, maxX - w);
        this.y = Math.min(anchorY, maxY - h);
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        DUTheme.box(g, x, y, w, h, 0xF0121712, DUTheme.SELECTED);
        for (int i = 0; i < items.size(); i++) {
            int ry = y + 1 + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H;
            if (hover) g.fill(x + 1, ry, x + w - 1, ry + ROW_H, 0xFF1b2a1c);
            Item it = items.get(i);
            g.drawString(font, it.label(), x + PAD + 2, ry + 3, hover ? DUTheme.SELECTED : it.color(), false);
        }
    }

    /** Runs the action under the cursor if any. Returns true if a row was clicked. */
    public boolean click(double mx, double my) {
        if (!contains(mx, my)) return false;
        int idx = (int) ((my - y - 1) / ROW_H);
        if (idx >= 0 && idx < items.size()) {
            Runnable action = items.get(idx).action();
            if (action != null) action.run();
            return true;
        }
        return false;
    }
}
