package net.doole.doolestools.logistics;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public record FilterSettings(Mode mode,
                             List<String> items,
                             String channel,
                             int priority,
                             int limit,
                             int tickSpeed,
                             Routing routing,
                             List<String> legacyTokens) {
    public static final int GHOST_SLOTS = 9;
    public static final int[] LIMITS = {1, 2, 4, 8, 16, 32, 64};
    public static final int[] TICK_SPEEDS = {1, 5, 10, 20, 40};
    public static final String[] CHANNELS = {
            "none", "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    /** ARGB swatch colour per channel (index-aligned with CHANNELS); 0 for "none". */
    public static final int[] CHANNEL_COLORS = {
            0, 0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA,
            0xFF474F52, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA, 0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21
    };

    /** Maps a channel name to its dye colour (ARGB), or 0 for the "none" channel. */
    public static int channelColor(String channel) {
        String wanted = channel == null ? "none" : channel.toLowerCase(Locale.ROOT);
        for (int i = 0; i < CHANNELS.length && i < CHANNEL_COLORS.length; i++) {
            if (CHANNELS[i].equals(wanted)) return CHANNEL_COLORS[i];
        }
        return 0;
    }

    public enum Mode { PASS_ALL, WHITELIST, BLACKLIST }
    public enum Routing { FIRST_VALID, ROUND_ROBIN }

    public static FilterSettings empty() {
        return new FilterSettings(Mode.PASS_ALL, blankItems(), "none", 0, 16, 20, Routing.FIRST_VALID, List.of());
    }

    public static FilterSettings parse(String notes) {
        if (notes == null || notes.isBlank()) return empty();
        String raw = notes.trim();
        Mode mode = raw.startsWith("!") || raw.toLowerCase(Locale.ROOT).startsWith("blacklist:") ? Mode.BLACKLIST : Mode.WHITELIST;
        List<String> items = blankItems();
        String channel = "none";
        int priority = 0;
        int limit = 16;
        int tickSpeed = 20;
        Routing routing = Routing.FIRST_VALID;
        List<String> legacyTokens = new ArrayList<>();

        String cleaned = raw.replace("blacklist:", "").replace("whitelist:", "");
        if (cleaned.startsWith("!")) cleaned = cleaned.substring(1);
        for (String token : cleaned.split("[,;\\s]+")) {
            if (token.isBlank()) continue;
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.equals("mode=pass") || lower.equals("mode=pass_all")) mode = Mode.PASS_ALL;
            else if (lower.equals("mode=whitelist")) mode = Mode.WHITELIST;
            else if (lower.equals("mode=blacklist")) mode = Mode.BLACKLIST;
            else if (lower.startsWith("items=")) readItems(lower.substring(6), items);
            else if (lower.startsWith("channel=")) channel = normalizeChannel(lower.substring(8));
            else if (lower.startsWith("priority=")) priority = clampInt(lower.substring(9), 0, 99, 0);
            else if (lower.startsWith("limit=")) limit = nearest(lower.substring(6), LIMITS, 16);
            else if (lower.startsWith("tick=")) tickSpeed = nearest(lower.substring(5), TICK_SPEEDS, 20);
            else if (lower.equals("rr") || lower.equals("round_robin") || lower.equals("round-robin") || lower.equals("routing=round_robin")) routing = Routing.ROUND_ROBIN;
            else if (lower.equals("routing=first")) routing = Routing.FIRST_VALID;
            else legacyTokens.add(lower);
        }
        return new FilterSettings(mode, List.copyOf(items), channel, priority, limit, tickSpeed, routing, List.copyOf(legacyTokens));
    }

    public String serialize() {
        return "mode=" + modeName(mode)
                + " items=" + String.join("|", paddedItems())
                + " channel=" + normalizeChannel(channel)
                + " priority=" + Math.max(0, Math.min(99, priority))
                + " limit=" + nearest(limit, LIMITS)
                + " tick=" + nearest(tickSpeed, TICK_SPEEDS)
                + " routing=" + (routing == Routing.ROUND_ROBIN ? "round_robin" : "first");
    }

    public boolean allows(ItemResource resource) {
        if (mode == Mode.PASS_ALL) return true;
        String id = BuiltInRegistries.ITEM.getKey(resource.getItem()).toString().toLowerCase(Locale.ROOT);
        boolean hasItems = false;
        boolean matched = false;
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            hasItems = true;
            if (id.equals(item)) matched = true;
        }
        if (hasItems) return mode == Mode.WHITELIST ? matched : !matched;
        if (legacyTokens.isEmpty()) return mode != Mode.WHITELIST;
        String name = resource.getHoverName().getString().toLowerCase(Locale.ROOT);
        for (String token : legacyTokens) {
            if (id.contains(token) || name.contains(token)) {
                matched = true;
                break;
            }
        }
        return mode == Mode.WHITELIST ? matched : !matched;
    }

    public FilterSettings withMode(Mode value) {
        return new FilterSettings(value, items, channel, priority, limit, tickSpeed, routing, legacyTokens);
    }

    public FilterSettings withItem(int slot, String registryId) {
        List<String> copy = paddedItems();
        if (slot >= 0 && slot < GHOST_SLOTS) copy.set(slot, registryId == null ? "" : registryId.toLowerCase(Locale.ROOT));
        return new FilterSettings(mode, List.copyOf(copy), channel, priority, limit, tickSpeed, routing, legacyTokens);
    }

    public FilterSettings nextChannel() {
        int index = Arrays.asList(CHANNELS).indexOf(normalizeChannel(channel));
        return new FilterSettings(mode, items, CHANNELS[Math.floorMod(index + 1, CHANNELS.length)], priority, limit, tickSpeed, routing, legacyTokens);
    }

    public FilterSettings nextLimit() {
        return new FilterSettings(mode, items, channel, priority, next(limit, LIMITS), tickSpeed, routing, legacyTokens);
    }

    public FilterSettings nextTickSpeed() {
        return new FilterSettings(mode, items, channel, priority, limit, next(tickSpeed, TICK_SPEEDS), routing, legacyTokens);
    }

    public FilterSettings nextRouting() {
        return new FilterSettings(mode, items, channel, priority, limit, tickSpeed,
                routing == Routing.ROUND_ROBIN ? Routing.FIRST_VALID : Routing.ROUND_ROBIN, legacyTokens);
    }

    public FilterSettings nextPriority() {
        int next = priority >= 9 ? 0 : priority + 1;
        return new FilterSettings(mode, items, channel, next, limit, tickSpeed, routing, legacyTokens);
    }

    public boolean matchesChannel(String value) {
        String wanted = normalizeChannel(channel);
        return wanted.equals("none") || wanted.equals(normalizeChannel(value));
    }

    public List<String> paddedItems() {
        List<String> copy = blankItems();
        for (int i = 0; i < Math.min(GHOST_SLOTS, items.size()); i++) copy.set(i, items.get(i) == null ? "" : items.get(i));
        return copy;
    }

    public static String modeLabel(Mode mode) {
        return switch (mode) {
            case PASS_ALL -> "Pass All";
            case WHITELIST -> "Whitelist";
            case BLACKLIST -> "Blacklist";
        };
    }

    private static String modeName(Mode mode) {
        return switch (mode) {
            case PASS_ALL -> "pass_all";
            case WHITELIST -> "whitelist";
            case BLACKLIST -> "blacklist";
        };
    }

    private static void readItems(String raw, List<String> items) {
        String[] parts = raw.split("\\|");
        for (int i = 0; i < Math.min(GHOST_SLOTS, parts.length); i++) items.set(i, parts[i].trim());
    }

    private static List<String> blankItems() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < GHOST_SLOTS; i++) values.add("");
        return values;
    }

    private static String normalizeChannel(String value) {
        String cleaned = value == null || value.isBlank() ? "none" : value.toLowerCase(Locale.ROOT);
        for (String channel : CHANNELS) if (channel.equals(cleaned)) return cleaned;
        return "none";
    }

    private static int clampInt(String raw, int min, int max, int fallback) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(raw))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int nearest(String raw, int[] values, int fallback) {
        try { return nearest(Integer.parseInt(raw), values); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int nearest(int value, int[] values) {
        int best = values[0];
        for (int option : values) if (Math.abs(option - value) < Math.abs(best - value)) best = option;
        return best;
    }

    private static int next(int value, int[] values) {
        int current = nearest(value, values);
        for (int i = 0; i < values.length; i++) if (values[i] == current) return values[(i + 1) % values.length];
        return values[0];
    }
}
