package net.doole.doolestools.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side preferences for LogiGraph. Persisted to config/doolestools-client.toml. */
public final class ModClientConfig {
    public static final ModConfigSpec SPEC;

    // --- Display ---
    public static final ModConfigSpec.BooleanValue SHOW_GRID;
    public static final ModConfigSpec.BooleanValue ANIMATE;
    public static final ModConfigSpec.BooleanValue AUTO_REFRESH;
    public static final ModConfigSpec.BooleanValue SHOW_ITEM_ICONS;
    public static final ModConfigSpec.DoubleValue UI_SCALE;

    // --- Graph canvas ---
    public static final ModConfigSpec.IntValue GRAPH_NODE_SNAP_GRID;
    public static final ModConfigSpec.BooleanValue SHOW_NODE_TYPE_ICONS;
    public static final ModConfigSpec.BooleanValue HIGHLIGHT_WARNING_NODES;

    // --- Scan ---
    public static final ModConfigSpec.IntValue DEFAULT_SCAN_RADIUS;

    // --- Stats page ---
    public static final ModConfigSpec.BooleanValue POWER_GRAPH_AREA_STYLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("display");
        SHOW_GRID = builder.comment("Show snap grid on the flowgraph canvas").define("showGrid", true);
        ANIMATE = builder.comment("Animate link flow on the flowgraph canvas").define("animate", true);
        AUTO_REFRESH = builder.comment("Automatically re-scan the network every 40 ticks while the GUI is open").define("autoRefresh", true);
        SHOW_ITEM_ICONS = builder.comment("Render item icons inside node cards").define("showItemIcons", true);
        UI_SCALE = builder.comment("GUI scale factor (0.75 / 1.0 / 1.25 / 1.5)").defineInRange("uiScale", 1.0, 0.5, 2.0);
        builder.pop();

        builder.push("graph");
        GRAPH_NODE_SNAP_GRID = builder.comment("Canvas drag snap grid size in pixels").defineInRange("nodeSnapGrid", 8, 1, 64);
        SHOW_NODE_TYPE_ICONS = builder.comment("Show type icon badge on graph nodes").define("showNodeTypeIcons", true);
        HIGHLIGHT_WARNING_NODES = builder.comment("Tint nodes that have active warnings").define("highlightWarningNodes", true);
        builder.pop();

        builder.push("scan");
        DEFAULT_SCAN_RADIUS = builder.comment("Default radius (blocks) used when Scan Network is pressed").defineInRange("defaultScanRadius", 16, 4, 64);
        builder.pop();

        builder.push("stats");
        POWER_GRAPH_AREA_STYLE = builder.comment("Render the power history graph as a filled area (true) or a line (false)").define("powerGraphAreaStyle", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ModClientConfig() {}
}
