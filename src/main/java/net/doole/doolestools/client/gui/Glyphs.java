package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tiny pixel glyphs drawn with {@code fill}, used for buttons and page tabs (no texture dependency).
 * Each glyph is drawn within roughly a 9x9 box anchored at (x, y).
 */
public final class Glyphs {
    private Glyphs() {}

    public interface Drawer { void draw(GuiGraphicsExtractor g, int x, int y, int color); }

    /** Three rows of "list" lines with leading dots. */
    public static void list(GuiGraphicsExtractor g, int x, int y, int c) {
        for (int i = 0; i < 3; i++) {
            int ry = y + i * 3;
            g.fill(x, ry, x + 1, ry + 1, c);
            g.fill(x + 3, ry, x + 9, ry + 1, c);
        }
    }

    /** Node-graph / share: two source dots linked to one. */
    public static void graph(GuiGraphicsExtractor g, int x, int y, int c) {
        DUTheme.line(g, x + 1, y + 1, x + 7, y + 4, c);
        DUTheme.line(g, x + 1, y + 7, x + 7, y + 4, c);
        dot(g, x, y, c);
        dot(g, x, y + 6, c);
        dot(g, x + 6, y + 3, c);
    }

    /** Bar chart: three bars of increasing height. */
    public static void chart(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y + 6, x + 2, y + 9, c);
        g.fill(x + 3, y + 3, x + 5, y + 9, c);
        g.fill(x + 6, y, x + 8, y + 9, c);
    }

    /** Gear: cross arms + hollow centre. */
    public static void gear(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x + 3, y, x + 6, y + 9, c);
        g.fill(x, y + 3, x + 9, y + 6, c);
        g.fill(x + 3, y + 3, x + 6, y + 6, DUTheme.PANEL);
    }

    /** Radar sweep: an L axis plus a diagonal sweep line and a blip. */
    public static void radar(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y, x + 1, y + 9, c);
        g.fill(x, y + 8, x + 9, y + 9, c);
        DUTheme.line(g, x, y + 8, x + 7, y + 1, c);
        g.fill(x + 6, y + 2, x + 8, y + 4, c);
    }

    /** Circular refresh arrow (approximate "C" with an arrowhead). */
    public static void refresh(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x + 2, y, x + 7, y + 1, c);
        g.fill(x + 1, y + 1, x + 2, y + 8, c);
        g.fill(x + 2, y + 8, x + 7, y + 9, c);
        g.fill(x + 7, y + 5, x + 8, y + 9, c);
        // arrowhead at top-right
        g.fill(x + 6, y, x + 9, y + 1, c);
        g.fill(x + 7, y - 1, x + 8, y + 3, c);
    }

    public static void plus(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x + 3, y, x + 6, y + 9, c);
        g.fill(x, y + 3, x + 9, y + 6, c);
    }

    public static void trash(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y + 1, x + 9, y + 2, c);   // lid
        g.fill(x + 3, y - 1, x + 6, y, c);   // handle
        g.fill(x + 1, y + 2, x + 8, y + 9, c); // bin
        g.fill(x + 3, y + 3, x + 4, y + 8, DUTheme.PANEL);
        g.fill(x + 5, y + 3, x + 6, y + 8, DUTheme.PANEL);
    }

    /** Link / chain. */
    public static void link(GuiGraphicsExtractor g, int x, int y, int c) {
        DUTheme.outline(g, x, y + 2, 5, 5, c);
        DUTheme.outline(g, x + 4, y + 2, 5, 5, c);
    }

    /** Auto-arrange: a small left-to-right tree. */
    public static void arrange(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y + 3, x + 3, y + 6, c);
        g.fill(x + 6, y, x + 9, y + 3, c);
        g.fill(x + 6, y + 6, x + 9, y + 9, c);
        DUTheme.line(g, x + 3, y + 4, x + 6, y + 1, c);
        DUTheme.line(g, x + 3, y + 4, x + 6, y + 7, c);
    }

    public static void frame(GuiGraphicsExtractor g, int x, int y, int c) {
        DUTheme.outline(g, x, y, 9, 9, c);
        g.fill(x, y, x + 9, y + 2, c);
    }

    /** Fit-to-view corner brackets. */
    public static void fit(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y, x + 3, y + 1, c); g.fill(x, y, x + 1, y + 3, c);
        g.fill(x + 6, y, x + 9, y + 1, c); g.fill(x + 8, y, x + 9, y + 3, c);
        g.fill(x, y + 8, x + 3, y + 9, c); g.fill(x, y + 6, x + 1, y + 9, c);
        g.fill(x + 6, y + 8, x + 9, y + 9, c); g.fill(x + 8, y + 6, x + 9, y + 9, c);
    }

    /** Warning triangle with exclamation. */
    public static void warning(GuiGraphicsExtractor g, int x, int y, int c) {
        GuiSprites.draw(g, GuiSprites.WARNING, x - 2, y - 2, 13);
    }

    private static void dot(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y + 1, x + 3, y + 4, c);
    }
}
