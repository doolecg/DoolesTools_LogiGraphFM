package net.doole.doolestools.client;

import net.doole.doolestools.config.ModClientConfig;

/**
 * Client-only display preferences, toggled from the Settings page.
 * Values are backed by {@link ModClientConfig} and persist across sessions.
 * Call {@link #load()} after the client config loads; call {@link #save()} after any change.
 */
public final class ClientPrefs {
    private ClientPrefs() {}

    public static boolean showGrid = true;
    public static boolean animate = true;
    // On by default: generic machine activity is detected from scan-to-scan deltas, so the monitor
    // needs to keep re-sampling or every modded machine looks idle after a single manual scan.
    public static boolean autoRefresh = true;
    public static boolean showItemIcons = true;
    public static float uiScale = 1.0f;

    /** Most-recently used Label Gun names (newest first), for quick re-use. Client-local, session only. */
    public static final java.util.List<String> recentLabels = new java.util.ArrayList<>();
    public static final int MAX_RECENT_LABELS = 10;

    /** Populate in-memory fields from the persisted client config. Called on config load. */
    public static void load() {
        showGrid = ModClientConfig.SHOW_GRID.get();
        animate = ModClientConfig.ANIMATE.get();
        autoRefresh = ModClientConfig.AUTO_REFRESH.get();
        showItemIcons = ModClientConfig.SHOW_ITEM_ICONS.get();
        uiScale = (float) (double) ModClientConfig.UI_SCALE.get();
    }

    /** Write current in-memory fields back to the persisted client config. */
    public static void save() {
        ModClientConfig.SHOW_GRID.set(showGrid);
        ModClientConfig.ANIMATE.set(animate);
        ModClientConfig.AUTO_REFRESH.set(autoRefresh);
        ModClientConfig.SHOW_ITEM_ICONS.set(showItemIcons);
        ModClientConfig.UI_SCALE.set((double) uiScale);
        ModClientConfig.SPEC.save();
    }

    public static void addRecentLabel(String label) {
        if (label == null || label.isBlank()) return;
        recentLabels.remove(label);
        recentLabels.add(0, label);
        while (recentLabels.size() > MAX_RECENT_LABELS) recentLabels.remove(recentLabels.size() - 1);
    }
}
