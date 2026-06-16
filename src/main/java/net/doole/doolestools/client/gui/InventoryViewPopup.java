package net.doole.doolestools.client.gui;

import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Overlay popup showing all items in an inventory with sort, direction toggle, and totals. */
public class InventoryViewPopup {

    public enum SortMode { AMOUNT, NAME, MOD }

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int ROW_H       = 15;
    private static final int VISIBLE_ROWS = 14;
    private static final int HEADER_H    = 22;
    private static final int SORT_H      = 20;
    private static final int FOOTER_H    = 16;
    private static final int POP_W       = 360;
    private static final int POP_H       = HEADER_H + SORT_H + 2 + VISIBLE_ROWS * ROW_H + 2 + FOOTER_H;

    // Sort-button geometry — three mode buttons + one direction button
    private static final int MODE_BTN_W = 56;
    private static final int DIR_BTN_W  = 22;
    private static final int BTN_H      = 13;
    private static final int BTN_GAP    = 4;

    // Column x-offsets inside the popup (relative to bx)
    private static final int COL_ICON   = 4;
    private static final int COL_NAME   = 22;
    private static final int COL_MOD    = 208; // dim mod tag
    private static final int COL_COUNT  = 290; // right-anchored

    private final String blockName;
    private final List<ItemEntry> items;
    private SortMode sortMode = SortMode.AMOUNT;
    private boolean ascending = false; // AMOUNT default: highest first

    // Layout state (set each render, used for hit-testing)
    private int bx, by;
    private int listY;
    private int closeBtnX, closeBtnY;
    private final int[] modeBtnX = new int[3];
    private int modeBtnY;
    private int dirBtnX, dirBtnY;

    public InventoryViewPopup(String blockName, InventorySummary inv) {
        this.blockName = blockName;
        this.items = new ArrayList<>(inv.topStacks());
        applySort();
    }

    // ── Sorting ──────────────────────────────────────────────────────────────

    private void applySort() {
        Comparator<ItemEntry> cmp = switch (sortMode) {
            case AMOUNT -> Comparator.comparingInt(ItemEntry::count);
            case NAME   -> Comparator.comparing(e -> e.displayName().toLowerCase());
            case MOD    -> Comparator.comparing(e -> modOf(e.registryId()).toLowerCase());
        };
        if (!ascending) cmp = cmp.reversed();
        items.sort(cmp);
        scroll = 0;
    }

    /** Switch to a mode. Clicking the already-active mode flips the direction. */
    private void clickMode(SortMode clicked) {
        if (sortMode == clicked) {
            ascending = !ascending;
        } else {
            sortMode = clicked;
            // Natural defaults: AMOUNT → descending (most first); NAME/MOD → ascending (A→Z)
            ascending = (clicked != SortMode.AMOUNT);
        }
        applySort();
    }

    private static String modOf(String id) {
        int c = id.indexOf(':');
        return c < 0 ? id : id.substring(0, c);
    }

    // ── Scroll ───────────────────────────────────────────────────────────────

    private int scroll = 0;

    private int maxScroll() { return Math.max(0, items.size() - VISIBLE_ROWS); }

    // ── Render ───────────────────────────────────────────────────────────────

    public void render(GuiGraphicsExtractor g, Font font, int guiW, int guiH) {
        bx = (guiW - POP_W) / 2;
        by = (guiH - POP_H) / 2;

        g.fill(0, 0, guiW, guiH, 0xAA000000);

        DUTheme.box(g, bx, by, POP_W, POP_H, DUTheme.SCREEN, DUTheme.PANEL_BORDER);

        renderHeader(g, font);
        renderSortBar(g, font);
        renderItems(g, font);
        renderFooter(g, font);
    }

    private void renderHeader(GuiGraphicsExtractor g, Font font) {
        g.fill(bx + 1, by + 1, bx + POP_W - 1, by + HEADER_H, DUTheme.PANEL_HEADER);
        DUTheme.glowText(g, font, "INVENTORY", bx + 6, by + 4, DUTheme.TEXT_GREEN);
        g.text(font, trim(font, blockName, POP_W - 60), bx + 6, by + 13, DUTheme.TEXT_DIM, false);

        closeBtnX = bx + POP_W - 20;
        closeBtnY = by + 5;
        DUTheme.box(g, closeBtnX, closeBtnY, 14, 11, DUTheme.PANEL, DUTheme.ERROR);
        g.centeredText(font, "X", closeBtnX + 7, closeBtnY + 2, DUTheme.ERROR);
    }

    private void renderSortBar(GuiGraphicsExtractor g, Font font) {
        modeBtnY = by + HEADER_H + 4;
        int sx = bx + 6;

        // "Sort by:" label
        g.text(font, "Sort by:", sx, modeBtnY + 3, DUTheme.TEXT_DIM, false);
        int labelW = font.width("Sort by:") + 6;

        // Three mode buttons
        String[] labels = { "AMOUNT", "NAME", "MOD" };
        SortMode[] modes = { SortMode.AMOUNT, SortMode.NAME, SortMode.MOD };
        for (int i = 0; i < 3; i++) {
            modeBtnX[i] = sx + labelW + i * (MODE_BTN_W + BTN_GAP);
            boolean active = sortMode == modes[i];
            String label = active ? labels[i] + " " + (ascending ? "▲" : "▼") : labels[i];
            DUTheme.box(g, modeBtnX[i], modeBtnY, MODE_BTN_W, BTN_H,
                    active ? 0xFF0d1f13 : DUTheme.PANEL,
                    active ? DUTheme.TEXT_GREEN : DUTheme.PANEL_BORDER);
            g.centeredText(font, label, modeBtnX[i] + MODE_BTN_W / 2, modeBtnY + 3,
                    active ? DUTheme.TEXT_GREEN : DUTheme.TEXT_DIM);
        }

        // Direction toggle button (▲ / ▼) — mirrors current direction, lets user flip without
        // having to re-click the active mode button
        dirBtnX = sx + labelW + 3 * (MODE_BTN_W + BTN_GAP);
        dirBtnY = modeBtnY;
        DUTheme.box(g, dirBtnX, dirBtnY, DIR_BTN_W, BTN_H, DUTheme.PANEL, DUTheme.TEXT_GREEN);
        g.centeredText(font, ascending ? "▲" : "▼", dirBtnX + DIR_BTN_W / 2, dirBtnY + 3,
                DUTheme.TEXT_GREEN);
    }

    private void renderItems(GuiGraphicsExtractor g, Font font) {
        int divY = by + HEADER_H + SORT_H;
        g.fill(bx + 4, divY, bx + POP_W - 4, divY + 1, DUTheme.PANEL_BORDER);

        listY = divY + 2;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        boolean hasBar = items.size() > VISIBLE_ROWS;
        int listRight = bx + POP_W - (hasBar ? 8 : 2);
        g.enableScissor(bx + 2, listY, listRight, listY + VISIBLE_ROWS * ROW_H);
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) break;
            renderRow(g, font, items.get(idx), listY + i * ROW_H, idx % 2 == 0, listRight);
        }
        g.disableScissor();

        // Scrollbar
        if (hasBar) {
            int trackX = bx + POP_W - 6;
            int trackH = VISIBLE_ROWS * ROW_H;
            g.fill(trackX, listY, trackX + 4, listY + trackH, DUTheme.PANEL_BORDER);
            int thumbH = Math.max(10, trackH * VISIBLE_ROWS / items.size());
            int thumbY = maxScroll() > 0
                    ? listY + (int) ((trackH - thumbH) * (scroll / (float) maxScroll()))
                    : listY;
            g.fill(trackX + 1, thumbY, trackX + 3, thumbY + thumbH, DUTheme.TEXT_GREEN);
        }
    }

    private void renderRow(GuiGraphicsExtractor g, Font font, ItemEntry e, int ry, boolean alt, int listRight) {
        int rowRight = listRight - bx;
        if (alt) g.fill(bx + 2, ry, listRight, ry + ROW_H - 1, DUTheme.PANEL_ALT);

        ItemIcons.render(g, e.registryId(), bx + COL_ICON, ry + 1, ItemIcons.SIZE, DUTheme.PANEL_ALT);

        // Item name — trim to fit before the mod column
        int nameMaxW = COL_MOD - COL_NAME - 6;
        g.text(font, trim(font, e.displayName(), nameMaxW), bx + COL_NAME, ry + 4, DUTheme.TEXT, false);

        // Mod tag — always shown, dim
        String mod = modOf(e.registryId());
        int modMaxW = COL_COUNT - COL_MOD - 6;
        g.text(font, trim(font, mod, modMaxW), bx + COL_MOD, ry + 4, DUTheme.TEXT_DIM, false);

        // Count right-aligned, with full-stack annotation
        int stacks = e.count() / 64;
        String countStr = formatCount(e.count());
        String stackStr = stacks > 0 ? " ×" + stacks : ""; // ×N
        String full = countStr + stackStr;
        int cx = bx + rowRight - 6 - font.width(full);
        g.text(font, countStr, cx, ry + 4, DUTheme.TEXT, false);
        if (stacks > 0) {
            g.text(font, stackStr, cx + font.width(countStr), ry + 4, DUTheme.TEXT_DIM, false);
        }
    }

    private void renderFooter(GuiGraphicsExtractor g, Font font) {
        int footerY = by + POP_H - FOOTER_H;
        g.fill(bx + 1, footerY, bx + POP_W - 1, footerY + FOOTER_H - 1, DUTheme.PANEL_HEADER);
        g.fill(bx + 4, footerY, bx + POP_W - 4, footerY + 1, DUTheme.PANEL_BORDER);

        long total = items.stream().mapToLong(ItemEntry::count).sum();
        long fullStacks = total / 64;
        long rem = total % 64;

        String left = items.size() + " types  ·  " + formatCount(total) + " items";
        String right = fullStacks + " stacks of 64" + (rem > 0 ? " + " + rem : "");
        g.text(font, left,  bx + 6, footerY + 4, DUTheme.TEXT_DIM, false);
        g.text(font, right, bx + POP_W - 4 - font.width(right), footerY + 4, DUTheme.TEXT_DIM, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /**
     * Handle a design-space click.
     * Returns {@code false} if the popup should close, {@code true} if it consumed the event.
     */
    public boolean mouseClicked(double mx, double my) {
        if (!isInside(mx, my)) return false;

        // Close button
        if (mx >= closeBtnX && mx <= closeBtnX + 14 && my >= closeBtnY && my <= closeBtnY + 11) {
            return false;
        }

        // Direction toggle
        if (mx >= dirBtnX && mx <= dirBtnX + DIR_BTN_W && my >= dirBtnY && my <= dirBtnY + BTN_H) {
            ascending = !ascending;
            applySort();
            return true;
        }

        // Mode buttons
        SortMode[] modes = { SortMode.AMOUNT, SortMode.NAME, SortMode.MOD };
        for (int i = 0; i < 3; i++) {
            if (mx >= modeBtnX[i] && mx <= modeBtnX[i] + MODE_BTN_W
                    && my >= modeBtnY && my <= modeBtnY + BTN_H) {
                clickMode(modes[i]);
                return true;
            }
        }

        return true;
    }

    public void mouseScrolled(double delta) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta), maxScroll()));
    }

    public boolean isInside(double mx, double my) {
        return mx >= bx && mx <= bx + POP_W && my >= by && my <= by + POP_H;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String formatCount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private static String trim(Font font, String s, int maxWidth) {
        if (s == null) return "";
        if (font.width(s) <= maxWidth) return s;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (font.width(b + String.valueOf(c) + "…") > maxWidth) break;
            b.append(c);
        }
        return b + "…";
    }
}
