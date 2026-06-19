package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Data provider for {@link GraphCanvasWidget} when driven by non-logistics-graph data
 * (e.g. the Network Switchboard). Supply node positions, edge connections, and a node renderer.
 * All coordinates are in logical (unscaled) canvas space; the widget handles zoom/pan.
 */
public interface CanvasAdapter {

    record CanvasNode(String id, int x, int y, int w, int h) {}

    record CanvasEdge(String edgeId, String fromId, String toId, String label, int color, boolean selected) {}

    List<CanvasNode> nodes();
    List<CanvasEdge> edges();

    void onNodeMoved(String id, int logicalX, int logicalY);

    /** Draw the node card. Called inside the widget's zoom/pan matrix, so coordinates are logical. */
    void renderNode(GuiGraphics g, Font font, CanvasNode node, boolean selected);

    default int portInX(CanvasNode node) { return node.x() - 1; }
    default int portInY(CanvasNode node) { return node.y() + node.h() / 2; }
    default int portOutX(CanvasNode node) { return node.x() + node.w(); }
    default int portOutY(CanvasNode node) { return node.y() + node.h() / 2; }

    /** Returns the id of the node currently being used as a link-drag source, or "" if none. */
    default String dragSourceId() { return ""; }

    /** True if this node should be rendered as selected/highlighted. */
    default boolean isSelected(String id) { return false; }
}
