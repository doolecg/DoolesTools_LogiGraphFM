package net.doole.doolestools.client.gui;

import net.doole.doolestools.DoolesTools;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Small GUI texture helpers for the concept-art-inspired icon set. */
public final class GuiSprites {
    private GuiSprites() {}

    public static final Identifier LOGO = gui("logo");
    public static final Identifier PAGE_SCANNED = gui("page_scanned");
    public static final Identifier PAGE_GRAPH = gui("page_graph");
    public static final Identifier PAGE_STATS = gui("page_stats");
    public static final Identifier PAGE_SETTINGS = gui("page_settings");
    public static final Identifier LIST = gui("list");
    public static final Identifier CANVAS = gui("canvas");
    public static final Identifier STATS = gui("stats");
    public static final Identifier FILTER = gui("filter");
    public static final Identifier CLEAR = gui("clear");
    public static final Identifier WARNING = gui("warning");
    public static final Identifier SETTINGS = gui("settings");
    public static final Identifier REFRESH = gui("refresh");
    public static final Identifier RADAR = gui("radar");
    public static final Identifier HELP = gui("help");
    public static final Identifier CLOSE = gui("close");
    public static final Identifier STATUS_OK = gui("status_ok");
    public static final Identifier STATUS_WARN = gui("status_warn");
    public static final Identifier STATUS_ERROR = gui("status_error");
    public static final Identifier DETAIL_FRAME = gui("detail_frame");

    public static Identifier gui(String name) {
        return DoolesTools.id("textures/gui/" + name + ".png");
    }

    public static void draw(GuiGraphicsExtractor g, Identifier texture, int x, int y, int size) {
        draw(g, texture, x, y, size, size);
    }

    public static void draw(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, w, h, w, h);
    }
}
