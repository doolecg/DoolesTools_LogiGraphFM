package net.doole.doolestools.client;

/** Client-only display preferences, toggled from the Settings page. Not persisted server-side. */
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

    public static void addRecentLabel(String label) {
        if (label == null || label.isBlank()) return;
        recentLabels.remove(label);
        recentLabels.add(0, label);
        while (recentLabels.size() > MAX_RECENT_LABELS) recentLabels.remove(recentLabels.size() - 1);
    }
}
