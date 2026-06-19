package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared palette and primitive draw helpers for the LogiGraph terminal GUI. Colours match the
 * player-provided concept (see {@code docs/UI_CONCEPT.md}). All values are ARGB.
 */
public final class DUTheme {
    private DUTheme() {}

    // Frame / casing
    public static final int BEZEL = 0xFF3a3f3a;
    public static final int BEZEL_DARK = 0xFF23271f;
    public static final int BEZEL_LIGHT = 0xFF555b4f;
    public static final int SCREW = 0xFF1a1d18;
    public static final int SCREW_HI = 0xFF6b7263;

    // Screen / panels
    public static final int SCREEN = 0xFF0b0f0a;
    public static final int PANEL = 0xFF11160f;
    public static final int PANEL_ALT = 0xFF0e120c;
    public static final int PANEL_BORDER = 0xFF2c3a2a;
    public static final int PANEL_HEADER = 0xFF16331b;
    public static final int GRID_DOT = 0x33304a30;

    // Text
    public static final int TEXT = 0xFFd8e0d0;
    public static final int TEXT_DIM = 0xFF8a9484;
    public static final int TEXT_GREEN = 0xFF5fcf5f;
    public static final int TEXT_GREEN_DIM = 0xFF3f8f43;

    // Status
    public static final int OK = 0xFF46c83c;
    public static final int WARN = 0xFFe0a92e;
    public static final int ERROR = 0xFFd8443a;
    public static final int SELECTED = 0xFF3fd2e0;
    public static final int PROGRESS_BLUE = 0xFF3f7fe0;
    public static final int PROGRESS_ORANGE = 0xFFe08a2e;
    public static final int DISABLED = 0xFF6a7066;

    /** Filled rectangle with a 1px border. */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        outline(g, x, y, w, h, border);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /**
     * Vanilla-style recessed inventory slot well, using the vanilla slot colours. Pass the slot's
     * pixel position minus 1 ({@code slotX - 1, slotY - 1}); the well is 18x18 to frame a 16x16 slot.
     */
    public static void slotWell(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF12160F);
        g.fill(x, y, x + 18, y + 1, 0xFF000000);
        g.fill(x, y, x + 1, y + 18, 0xFF000000);
        g.fill(x + 17, y, x + 18, y + 18, 0xFF2E382C);
        g.fill(x, y + 17, x + 18, y + 18, 0xFF2E382C);
    }

    /** Chunky bezel with bevel highlight/shadow. */
    public static void bezel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BEZEL);
        g.fill(x, y, x + w, y + 2, BEZEL_LIGHT);
        g.fill(x, y, x + 2, y + h, BEZEL_LIGHT);
        g.fill(x, y + h - 2, x + w, y + h, BEZEL_DARK);
        g.fill(x + w - 2, y, x + w, y + h, BEZEL_DARK);
    }

    public static void screw(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 3, cy - 3, cx + 3, cy + 3, SCREW);
        g.fill(cx - 3, cy - 3, cx + 3, cy - 2, SCREW_HI);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, SCREW_HI);
    }

    /** Status indicator dot. */
    public static void dot(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 1, x + 5, y + 4, color);
        g.fill(x + 1, y, x + 4, y + 5, color);
    }

    /** Horizontal progress bar with fraction 0..1. */
    public static void progress(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        frac = Math.max(0f, Math.min(1f, frac));
        g.fill(x, y, x + w, y + h, 0xFF06090a);
        outline(g, x, y, w, h, PANEL_BORDER);
        int fillW = Math.round((w - 2) * frac);
        if (fillW > 0) {
            g.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, color);
        }
    }

    /**
     * Draws a 1px line as a handful of axis-aligned {@code fill} rectangles rather than one quad per
     * pixel (GuiGraphics has no native line primitive). For the near-horizontal/near-vertical
     * segments that make up the graph's Bezier links this collapses dozens of draws into a couple,
     * which is the difference between a smooth canvas and a stutter once many links are on screen.
     */
    public static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (y1 == y2) {
            g.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, color);
            return;
        }
        if (x1 == x2) {
            g.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
            return;
        }
        int dx = x2 - x1, dy = y2 - y1;
        if (Math.abs(dx) >= Math.abs(dy)) {
            // Shallow line: walk x, emit one horizontal run per row.
            int step = dx > 0 ? 1 : -1;
            double slope = (double) dy / dx;
            int runStart = x1, curY = y1, x = x1;
            while (true) {
                int y = (int) Math.round(y1 + slope * (x - x1));
                if (y != curY) {
                    g.fill(Math.min(runStart, x - step), curY, Math.max(runStart, x - step) + 1, curY + 1, color);
                    runStart = x;
                    curY = y;
                }
                if (x == x2) {
                    g.fill(Math.min(runStart, x), curY, Math.max(runStart, x) + 1, curY + 1, color);
                    break;
                }
                x += step;
            }
        } else {
            // Steep line: walk y, emit one vertical run per column.
            int step = dy > 0 ? 1 : -1;
            double slope = (double) dx / dy;
            int runStart = y1, curX = x1, y = y1;
            while (true) {
                int x = (int) Math.round(x1 + slope * (y - y1));
                if (x != curX) {
                    g.fill(curX, Math.min(runStart, y - step), curX + 1, Math.max(runStart, y - step) + 1, color);
                    runStart = y;
                    curX = x;
                }
                if (y == y2) {
                    g.fill(curX, Math.min(runStart, y), curX + 1, Math.max(runStart, y) + 1, color);
                    break;
                }
                y += step;
            }
        }
    }

    /**
     * Marching-ants dashed rectangle border. {@code phase} animates the dash offset (e.g. derived
     * from {@code System.currentTimeMillis()}). {@code dash}/{@code gap} are in pixels.
     */
    public static void dashedRect(GuiGraphics g, int x, int y, int w, int h, int color, int dash, int gap, int phase) {
        int period = Math.max(1, dash + gap);
        int perimeter = 2 * (w + h);
        for (int d = 0; d < perimeter; d++) {
            if ((d + phase) % period >= dash) continue;
            int px, py;
            if (d < w) { px = x + d; py = y; }
            else if (d < w + h) { px = x + w; py = y + (d - w); }
            else if (d < 2 * w + h) { px = x + w - (d - w - h); py = y + h; }
            else { px = x; py = y + h - (d - 2 * w - h); }
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Current client world game time in ticks (0 if no level loaded). */
    public static long clientGameTime() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** Formats a tick count as m:ss (20 ticks = 1 second). */
    public static String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /**
     * Draws text with a subtle CRT-style glow: a faint translucent halo of the same colour behind a
     * crisp foreground. Cheap (4 offset passes), so reserve it for prominent labels.
     */
    public static void glowText(GuiGraphics g, net.minecraft.client.gui.Font font, String s, int x, int y, int color) {
        int glow = (color & 0x00FFFFFF) | 0x55000000;
        g.drawString(font, s, x + 1, y, glow, false);
        g.drawString(font, s, x - 1, y, glow, false);
        g.drawString(font, s, x, y + 1, glow, false);
        g.drawString(font, s, x, y - 1, glow, false);
        g.drawString(font, s, x, y, color, false);
    }

    public static int statusColor(String kind) {
        return switch (kind) {
            case "ok" -> OK;
            case "warn" -> WARN;
            case "error" -> ERROR;
            default -> DISABLED;
        };
    }

    /** Generic cubic Bezier point evaluation. */
    public static double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        double u = 1.0 - t;
        return u * u * u * p0 + 3.0 * u * u * t * p1 + 3.0 * u * t * t * p2 + t * t * t * p3;
    }

    /**
     * Distance from (px,py) to a cubic Bezier link with horizontal S-curve control points.
     * Control points are x1±bend/x2∓bend horizontally; y control points equal the endpoints
     * (produces the characteristic S-shaped link used in both canvas screens).
     */
    public static double distanceToCurve(double px, double py, int x1, int y1, int x2, int y2) {
        double best = Double.MAX_VALUE;
        double lastX = x1, lastY = y1;
        int bend = Math.max(24, Math.abs(x2 - x1) / 2);
        int c1x = x1 + bend, c2x = x2 - bend;
        for (int i = 1; i <= 18; i++) {
            double t = i / 18.0;
            double x = cubicBezier(t, x1, c1x, c2x, x2);
            double y = cubicBezier(t, y1, y1, y2, y2);
            double dx = x - lastX, dy = y - lastY;
            double len = dx * dx + dy * dy;
            double seg;
            if (len <= 0.0) {
                seg = Math.hypot(px - lastX, py - lastY);
            } else {
                double tt = Math.max(0.0, Math.min(1.0, ((px - lastX) * dx + (py - lastY) * dy) / len));
                seg = Math.hypot(px - (lastX + tt * dx), py - (lastY + tt * dy));
            }
            if (seg < best) best = seg;
            lastX = x;
            lastY = y;
        }
        return best;
    }

    /** Horizontal battery bar: positive-terminal bump on the left, interior fill left→right, 4 segment dividers. */
    public static void horizBattery(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        int bumpH = 6, bumpW = 3;
        int bumpY = y + (h - bumpH) / 2;
        g.fill(x - bumpW, bumpY, x, bumpY + bumpH, PANEL_BORDER);
        g.fill(x - bumpW + 1, bumpY + 1, x, bumpY + bumpH - 1, 0xFF1a221a);
        g.fill(x, y, x + w, y + h, 0xFF070b08);
        outline(g, x, y, w, h, PANEL_BORDER);
        int inX = x + 1, inY = y + 1, inW = w - 2, inH = h - 2;
        int fillW = Math.round(Math.max(0f, Math.min(1f, frac)) * inW);
        if (fillW > 0) g.fill(inX, inY, inX + fillW, inY + inH, color);
        for (int i = 1; i < 5; i++) {
            int sx = inX + i * inW / 5;
            g.fill(sx, inY, sx + 1, inY + inH, 0x55000000);
        }
    }

    /** Vertical battery bar: positive-terminal bump on top, interior fill bottom→top, 2 segment dividers. */
    public static void vertBattery(GuiGraphics g, int bx, int by, int bw, int bh, float frac, int color) {
        int bumpW = Math.max(2, bw / 2), bumpH = 2;
        int bumpX = bx + (bw - bumpW) / 2;
        g.fill(bumpX, by - bumpH, bumpX + bumpW, by, PANEL_BORDER);
        g.fill(bumpX + 1, by - bumpH + 1, bumpX + bumpW - 1, by, 0xFF1a221a);
        box(g, bx, by, bw, bh, 0xFF070b08, PANEL_BORDER);
        int inX = bx + 1, inY = by + 1, inW = bw - 2, inH = bh - 2;
        int fillH = Math.round(Math.max(0f, Math.min(1f, frac)) * inH);
        if (fillH > 0) g.fill(inX, inY + inH - fillH, inX + inW, inY + inH, color);
        for (int i = 1; i < 3; i++) {
            int sy = inY + i * inH / 3;
            g.fill(inX, sy, inX + inW, sy + 1, 0x55000000);
        }
    }

    /** Panel box with a coloured header strip and glow-text title. */
    public static void panelWithHeader(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, int h, String title) {
        box(g, x, y, w, h, PANEL, PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 11, PANEL_HEADER);
        glowText(g, font, title, x + 4, y + 2, TEXT_GREEN);
    }

    /** Full terminal frame: bezel, four corner screws, inner screen box. */
    public static void frame(GuiGraphics g, int x, int y, int w, int h) {
        bezel(g, x, y, w, h);
        screw(g, x + 8, y + 8);
        screw(g, x + w - 8, y + 8);
        screw(g, x + 8, y + h - 8);
        screw(g, x + w - 8, y + h - 8);
        box(g, x + 6, y + 6, w - 12, h - 12, SCREEN, PANEL_BORDER);
    }
}
