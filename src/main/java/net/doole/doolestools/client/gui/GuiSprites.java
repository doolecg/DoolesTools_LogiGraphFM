package net.doole.doolestools.client.gui;

import net.doole.doolestools.DoolesTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Small GUI texture helpers for the concept-art-inspired icon set. */
public final class GuiSprites {
    private GuiSprites() {}

    public static final ResourceLocation LOGO = gui("logo");
    public static final ResourceLocation PAGE_SCANNED = gui("page_scanned");
    public static final ResourceLocation PAGE_GRAPH = gui("page_graph");
    public static final ResourceLocation PAGE_STATS = gui("page_stats");
    public static final ResourceLocation PAGE_SETTINGS = gui("page_settings");
    public static final ResourceLocation LIST = gui("list");
    public static final ResourceLocation CANVAS = gui("canvas");
    public static final ResourceLocation STATS = gui("stats");
    public static final ResourceLocation FILTER = gui("filter");
    public static final ResourceLocation CLEAR = gui("clear");
    public static final ResourceLocation WARNING = gui("warning");
    public static final ResourceLocation SETTINGS = gui("settings");
    public static final ResourceLocation REFRESH = gui("refresh");
    public static final ResourceLocation RADAR = gui("radar");
    public static final ResourceLocation HELP = gui("help");
    public static final ResourceLocation CLOSE = gui("close");
    public static final ResourceLocation STATUS_OK = gui("status_ok");
    public static final ResourceLocation STATUS_WARN = gui("status_warn");
    public static final ResourceLocation STATUS_ERROR = gui("status_error");
    public static final ResourceLocation DETAIL_FRAME = gui("detail_frame");

    public static ResourceLocation gui(String name) {
        return DoolesTools.id("textures/gui/" + name + ".png");
    }

    public static void draw(GuiGraphics g, ResourceLocation texture, int x, int y, int size) {
        draw(g, texture, x, y, size, size);
    }

    public static void draw(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        g.blit(texture, x, y, 0.0f, 0.0f, w, h, w, h);
    }
}
