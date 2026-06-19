package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.LogisticsGraph;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Modal overlay for picking a filter item from the full item registry. */
public final class FilterItemPickerPopup {

    private static final int RECENT_LIMIT = 24;
    private static final List<String> RECENTS = new ArrayList<>();

    private final EditorContext ctx;
    private final Font font;

    private int guiW, guiH, contentY;

    private boolean open;
    private int slot = -1;
    private int scroll;
    private String search = "";
    private boolean recentTab;
    private List<PickerEntry> allEntries;

    public FilterItemPickerPopup(EditorContext ctx, Font font) {
        this.ctx = ctx;
        this.font = font;
    }

    public void updateLayout(int guiW, int guiH, int contentY) {
        this.guiW = guiW;
        this.guiH = guiH;
        this.contentY = contentY;
    }

    public boolean isOpen() { return open; }

    public void open(int slot) {
        this.open = true;
        this.slot = slot;
        this.scroll = 0;
        this.search = "";
        this.recentTab = !RECENTS.isEmpty();
    }

    public void close() {
        open = false;
        slot = -1;
        scroll = 0;
        search = "";
        recentTab = false;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (!open) return;
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int x = (guiW - w) / 2;
        int y = contentY + 10;

        DUTheme.box(g, x - 3, y - 3, w + 6, h + 6, 0xF01b3320, DUTheme.SELECTED);
        DUTheme.box(g, x, y, w, h, 0xFF111b14, DUTheme.SELECTED);
        g.drawString(font, "CHOOSE FILTER ITEM", x + 8, y + 8, DUTheme.TEXT_GREEN, false);
        String slotText = slot >= 0 ? "Slot " + (slot + 1) : "Slot";
        g.drawString(font, slotText, x + w - 8 - font.width(slotText), y + 8, DUTheme.TEXT_DIM, false);

        int tabY = y + 22;
        pickerTab(g, x + 8,  tabY, 44, "ALL",    !recentTab);
        pickerTab(g, x + 56, tabY, 58, "RECENT",  recentTab);

        int searchY = y + 39;
        DUTheme.box(g, x + 8, searchY, w - 16, 16, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        String searchDisplay = recentTab ? "Recent picks" : search.isBlank() ? "Search item/block name or id..." : search;
        g.drawString(font, trim(font, searchDisplay, w - 26), x + 12, searchY + 5, search.isBlank() || recentTab ? DUTheme.TEXT_DIM : DUTheme.TEXT, false);
        if (!recentTab && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = x + 12 + font.width(trim(font, search, w - 34));
            g.fill(cursorX, searchY + 4, cursorX + 1, searchY + 13, DUTheme.SELECTED);
        }

        int listY = y + 61;
        int listH = h - 85;
        int rowH = 20;
        List<PickerEntry> entries = filteredEntries();
        int visibleRows = Math.max(1, listH / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - visibleRows)));
        g.enableScissor(x + 8, listY, x + w - 8, listY + listH);
        for (int row = 0; row < visibleRows && scroll + row < entries.size(); row++) {
            PickerEntry entry = entries.get(scroll + row);
            int ry = listY + row * rowH;
            boolean hover = mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= ry && mouseY < ry + rowH;
            DUTheme.box(g, x + 8, ry, w - 16, rowH - 1, hover ? 0xFF1f4a32 : (row % 2 == 0 ? 0xFF152018 : 0xFF101710), hover ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            ItemIcons.render(g, entry.registryId(), x + 10, ry + 2, ItemIcons.SIZE, DUTheme.PANEL_ALT);
            g.drawString(font, trim(font, entry.name(), w - 54), x + 30, ry + 3,  hover ? DUTheme.SELECTED : DUTheme.TEXT, false);
            g.drawString(font, trim(font, entry.registryId(), w - 54), x + 30, ry + 12, DUTheme.TEXT_DIM, false);
        }
        g.disableScissor();
        if (entries.isEmpty()) {
            g.drawCenteredString(font, recentTab ? "No recent items yet" : "No matching items", x + w / 2, listY + listH / 2, DUTheme.TEXT_DIM);
        }

        int footerY = y + h - 18;
        DUTheme.box(g, x + 8, footerY, 58, 12, DUTheme.PANEL_ALT, DUTheme.WARN);
        g.drawCenteredString(font, "CLEAR", x + 37, footerY + 3, DUTheme.WARN);
        g.drawString(font, "Esc closes", x + 74, footerY + 3, DUTheme.TEXT_DIM, false);
    }

    /** Returns true if the event was consumed. */
    public boolean handleClick(double mx, double my) {
        if (!open) return false;
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int x = (guiW - w) / 2;
        int y = contentY + 10;
        if (mx < x || mx > x + w || my < y || my > y + h) { close(); return true; }

        int tabY = y + 22;
        if (my >= tabY && my <= tabY + 13) {
            if (mx >= x + 8 && mx <= x + 52)  { recentTab = false; scroll = 0; return true; }
            if (mx >= x + 56 && mx <= x + 114) { recentTab = true;  scroll = 0; return true; }
        }
        int footerY = y + h - 18;
        if (mx >= x + 8 && mx <= x + 66 && my >= footerY && my <= footerY + 12) {
            commitSlot(""); return true;
        }
        int listY = y + 61, listH = h - 85, rowH = 20;
        if (mx >= x + 8 && mx <= x + w - 8 && my >= listY && my < listY + listH) {
            int index = scroll + (int) ((my - listY) / rowH);
            List<PickerEntry> entries = filteredEntries();
            if (index >= 0 && index < entries.size()) commitSlot(entries.get(index).registryId());
            return true;
        }
        return true;
    }

    /** Returns true if the event was consumed. */
    public boolean handleKey(int key, int modifiers) {
        if (!open) return false;
        if (key == 256) { close(); return true; }
        if (key == 259) {
            if (!search.isEmpty()) { search = search.substring(0, search.length() - 1); scroll = 0; }
            return true;
        }
        if (recentTab) return true;
        String typed = keyText(key, modifiers);
        if (!typed.isEmpty() && search.length() < 64) { search += typed; scroll = 0; }
        return true;
    }

    public void handleScroll(double delta) {
        if (!open) return;
        int w = Math.min(360, guiW - 40);
        int h = Math.min(250, guiH - 70);
        int visibleRows = Math.max(1, (h - 85) / 20);
        int max = Math.max(0, filteredEntries().size() - visibleRows);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta) * 3));
    }

    // --- Private ---

    private void pickerTab(GuiGraphics g, int x, int y, int w, String label, boolean selected) {
        DUTheme.box(g, x, y, w, 13, selected ? 0xFF1f4a32 : DUTheme.PANEL_ALT, selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
        g.drawCenteredString(font, label, x + w / 2, y + 3, selected ? DUTheme.SELECTED : DUTheme.TEXT_DIM);
    }

    private void commitSlot(String registryId) {
        GraphNodeData node = ctx.selectedNode();
        if (node != null && node.type() == NodeType.FILTER && slot >= 0) {
            FilterSettings settings = FilterSettings.parse(node.notes()).withItem(slot, registryId);
            ctx.setGraph(LogisticsGraph.updateNode(ctx.graph(), node.withNotes(settings.serialize())));
            addRecent(registryId);
        }
        close();
    }

    private static void addRecent(String registryId) {
        if (registryId == null || registryId.isBlank()) return;
        RECENTS.remove(registryId);
        RECENTS.add(0, registryId);
        while (RECENTS.size() > RECENT_LIMIT) RECENTS.remove(RECENTS.size() - 1);
    }

    private List<PickerEntry> filteredEntries() {
        if (recentTab) {
            List<PickerEntry> recent = new ArrayList<>();
            for (String id : RECENTS) {
                PickerEntry e = find(id);
                if (e != null) recent.add(e);
            }
            return recent;
        }
        String query = search.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return allEntries();
        List<PickerEntry> matches = new ArrayList<>();
        for (PickerEntry e : allEntries()) if (e.searchText().contains(query)) matches.add(e);
        return matches;
    }

    private PickerEntry find(String registryId) {
        for (PickerEntry e : allEntries()) if (e.registryId().equals(registryId)) return e;
        return null;
    }

    private List<PickerEntry> allEntries() {
        if (allEntries == null) {
            List<PickerEntry> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) continue;
                String rid = id.toString();
                String name = new ItemStack(item).getHoverName().getString();
                list.add(new PickerEntry(rid, name, (name + " " + rid).toLowerCase(Locale.ROOT)));
            }
            list.sort(Comparator.comparing(PickerEntry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(PickerEntry::registryId));
            allEntries = List.copyOf(list);
        }
        return allEntries;
    }

    private static String keyText(int key, int modifiers) {
        boolean shift = (modifiers & 1) != 0;
        if (key >= 65 && key <= 90) return String.valueOf((char) (shift ? key : key + 32));
        if (key >= 48 && key <= 57) return String.valueOf((char) key);
        return switch (key) {
            case 32 -> " "; case 45 -> "-"; case 46 -> ".";
            case 47 -> "/"; case 59 -> ":"; case 95 -> "_";
            default -> "";
        };
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 3 && font.width(text + "...") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private record PickerEntry(String registryId, String name, String searchText) {}
}
