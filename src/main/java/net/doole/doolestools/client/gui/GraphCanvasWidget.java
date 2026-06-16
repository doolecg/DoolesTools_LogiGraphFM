package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.PortDirection;
import net.doole.doolestools.logistics.PortDiscovery;
import net.doole.doolestools.logistics.data.GraphFrameData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.GraphTextData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The centre flowgraph canvas: grid, links, draggable node cards, with pan + zoom. */
public class GraphCanvasWidget {

    public record PortHit(GraphNodeData node, GraphPortData port) {}
    public record LinkEndpointHit(GraphLinkData link) {}
    public record LinkSideHit(GraphLinkData link) {}

    /** A link with its Bezier polyline baked once per graph revision (px/py include both endpoints). */
    private record CachedLink(GraphLinkData link, int sx, int sy, int tx, int ty, int mx, int my, int[] px, int[] py) {}

    private static final int SIDE_PILL_W = 48;
    private static final int SIDE_PILL_H = 12;

    private final EditorContext ctx;
    public int x, y, w, h;

    // Projected geometry cache. Link curves only move when the graph changes, so we bake them per
    // revision and reuse them every frame; pan/zoom is just the matrix and viewport culling on top.
    private int cachedRevision = Integer.MIN_VALUE;
    private List<GraphNodeData> cachedNodes = List.of();
    private Map<String, GraphNodeData> cachedNodesById = Map.of();
    private List<CachedLink> cachedLinks = List.of();
    private java.util.Set<String> cachedLinkedNodeIds = java.util.Set.of();

    public GraphCanvasWidget(EditorContext ctx, int x, int y, int w, int h) {
        this.ctx = ctx;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public double toCanvasX(double mx) {
        return (mx - x - ctx.panX) / ctx.zoom;
    }

    public double toCanvasY(double my) {
        return (my - y - ctx.panY) / ctx.zoom;
    }

    public GraphNodeData nodeAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        List<GraphNodeData> nodes = ctx.graph().activeCanvas().nodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNodeData n = nodes.get(i);
            if (cx >= n.x() && cx <= n.x() + n.width() && cy >= n.y() && cy <= n.y() + n.height()) {
                return n;
            }
        }
        return null;
    }

    public static final int FRAME_TITLE_H = 13;

    /** Returns the frame whose title bar is under the cursor (used to drag/select/delete frames). */
    public GraphFrameData frameTitleAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        List<GraphFrameData> frames = ctx.graph().activeCanvas().frames();
        for (int i = frames.size() - 1; i >= 0; i--) {
            GraphFrameData f = frames.get(i);
            if (cx >= f.x() && cx <= f.x() + f.width() && cy >= f.y() && cy <= f.y() + FRAME_TITLE_H) {
                return f;
            }
        }
        return null;
    }

    public static final int FRAME_HANDLE = 8;

    /** Returns the frame whose bottom-right resize handle is under the cursor. */
    public GraphFrameData frameResizeHandleAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        List<GraphFrameData> frames = ctx.graph().activeCanvas().frames();
        for (int i = frames.size() - 1; i >= 0; i--) {
            GraphFrameData f = frames.get(i);
            int hx = f.x() + f.width();
            int hy = f.y() + f.height();
            if (cx >= hx - FRAME_HANDLE && cx <= hx + 2 && cy >= hy - FRAME_HANDLE && cy <= hy + 2) return f;
        }
        return null;
    }

    /** Node ids whose centre lies within the given frame (used to drag a frame and its contents together). */
    public java.util.List<String> nodeIdsInFrame(GraphFrameData f) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (GraphNodeData n : ctx.graph().activeCanvas().nodes()) {
            int cx = n.x() + n.width() / 2;
            int cy = n.y() + n.height() / 2;
            if (cx >= f.x() && cx <= f.x() + f.width() && cy >= f.y() && cy <= f.y() + f.height()) ids.add(n.nodeId());
        }
        return ids;
    }

    /** Returns the text label under the cursor (used to select/drag/edit/delete it). */
    public GraphTextData textAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        List<GraphTextData> texts = ctx.graph().activeCanvas().texts();
        for (int i = texts.size() - 1; i >= 0; i--) {
            GraphTextData t = texts.get(i);
            int tw = font.width(t.text());
            if (cx >= t.x() - 2 && cx <= t.x() + tw + 2 && cy >= t.y() - 2 && cy <= t.y() + 10) {
                return t;
            }
        }
        return null;
    }

    /** Node ids whose card overlaps the given screen-space rectangle (marquee selection). */
    public java.util.List<String> nodesInScreenRect(double sx1, double sy1, double sx2, double sy2) {
        double x1 = Math.min(sx1, sx2), x2 = Math.max(sx1, sx2);
        double y1 = Math.min(sy1, sy2), y2 = Math.max(sy1, sy2);
        double cx1 = toCanvasX(x1), cy1 = toCanvasY(y1);
        double cx2 = toCanvasX(x2), cy2 = toCanvasY(y2);
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (GraphNodeData n : ctx.graph().activeCanvas().nodes()) {
            boolean overlap = n.x() <= cx2 && n.x() + n.width() >= cx1
                    && n.y() <= cy2 && n.y() + n.height() >= cy1;
            if (overlap) ids.add(n.nodeId());
        }
        return ids;
    }

    public PortHit portAt(double mx, double my, PortDirection direction) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        List<GraphNodeData> nodes = ctx.graph().activeCanvas().nodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNodeData node = nodes.get(i);
            int index = 0;
            for (GraphPortData port : node.ports()) {
                if (port.direction() != direction) continue;
                int px = GraphNodeWidget.portX(node, direction);
                int py = GraphNodeWidget.portY(node, direction, index++);
                double dx = cx - px;
                double dy = cy - py;
                if (dx * dx + dy * dy <= 36) return new PortHit(node, port);
            }
        }
        return null;
    }

    public LinkEndpointHit linkTargetAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        Map<String, GraphNodeData> nodesById = nodeMap();
        for (GraphLinkData link : ctx.graph().activeCanvas().links()) {
            GraphNodeData target = nodesById.get(link.targetNodeId());
            if (target == null) continue;
            int[] endpoint = portPoint(target, link.targetPortId());
            if (endpoint == null) continue;
            double dx = cx - endpoint[0];
            double dy = cy - endpoint[1];
            if (dx * dx + dy * dy <= 49) return new LinkEndpointHit(link);
        }
        return null;
    }

    public GraphLinkData linkAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        GraphLinkData best = null;
        double bestDist = 64.0;
        Map<String, GraphNodeData> nodesById = nodeMap();
        for (GraphLinkData link : ctx.graph().activeCanvas().links()) {
            GraphNodeData source = nodesById.get(link.sourceNodeId());
            GraphNodeData target = nodesById.get(link.targetNodeId());
            if (source == null || target == null) continue;
            int[] sp = portPoint(source, link.sourcePortId());
            int[] tp = portPoint(target, link.targetPortId());
            if (sp == null || tp == null) continue;
            if (!lineBoundsIntersects(sp[0], sp[1], tp[0], tp[1], cx - 8, cy - 8, cx + 8, cy + 8)) continue;
            double dist = distanceToCurveSquared(cx, cy, sp[0], sp[1], tp[0], tp[1]);
            if (dist < bestDist) {
                bestDist = dist;
                best = link;
            }
        }
        return best;
    }

    public LinkSideHit linkSideAt(double mx, double my) {
        double cx = toCanvasX(mx);
        double cy = toCanvasY(my);
        List<GraphLinkData> links = ctx.graph().activeCanvas().links();
        Map<String, GraphNodeData> nodesById = nodeMap();
        for (int i = links.size() - 1; i >= 0; i--) {
            GraphLinkData link = links.get(i);
            int[] midpoint = linkMidpoint(link, nodesById);
            if (midpoint == null) continue;
            int px = midpoint[0] - SIDE_PILL_W / 2;
            int py = midpoint[1] - SIDE_PILL_H / 2;
            if (cx >= px && cx <= px + SIDE_PILL_W && cy >= py && cy <= py + SIDE_PILL_H) return new LinkSideHit(link);
        }
        return null;
    }

    private Map<String, GraphNodeData> nodeMap() {
        Map<String, GraphNodeData> nodesById = new HashMap<>();
        for (GraphNodeData node : ctx.graph().activeCanvas().nodes()) nodesById.put(node.nodeId(), node);
        return nodesById;
    }

    private static double distanceToCurveSquared(double px, double py, int x1, int y1, int x2, int y2) {
        double best = Double.MAX_VALUE;
        double lastX = x1;
        double lastY = y1;
        int samples = curveSamples(x1, y1, x2, y2);
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            double cx = curveX(x1, x2, t);
            double cy = curveY(y1, y2, t);
            best = Math.min(best, distanceToSegmentSquared(px, py, lastX, lastY, cx, cy));
            lastX = cx;
            lastY = cy;
        }
        return best;
    }

    private static double distanceToSegmentSquared(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = dx * dx + dy * dy;
        if (len <= 0.0001) return square(px - x1) + square(py - y1);
        double t = Math.max(0.0, Math.min(1.0, ((px - x1) * dx + (py - y1) * dy) / len));
        return square(px - (x1 + t * dx)) + square(py - (y1 + t * dy));
    }

    private static double square(double value) {
        return value * value;
    }

    public void fitView() {
        ctx.zoom = 1f;
        if (ctx.graph().activeCanvas().nodes().isEmpty()) {
            ctx.panX = 0;
            ctx.panY = 0;
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (GraphNodeData n : ctx.graph().activeCanvas().nodes()) {
            minX = Math.min(minX, n.x());
            minY = Math.min(minY, n.y());
            maxX = Math.max(maxX, n.x() + n.width());
            maxY = Math.max(maxY, n.y() + n.height());
        }
        int gw = maxX - minX, gh = maxY - minY;
        float zx = gw > 0 ? (w - 20f) / gw : 1f;
        float zy = gh > 0 ? (h - 20f) / gh : 1f;
        ctx.zoom = Math.max(0.4f, Math.min(1.5f, Math.min(zx, zy)));
        ctx.panX = (w - gw * ctx.zoom) / 2f - minX * ctx.zoom;
        ctx.panY = (h - gh * ctx.zoom) / 2f - minY * ctx.zoom;
    }

    public void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY) {
        DUTheme.box(g, x, y, w, h, DUTheme.SCREEN, DUTheme.PANEL_BORDER);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        var canvas = ctx.graph().activeCanvas();
        double minX = toCanvasX(x) - 96;
        double minY = toCanvasY(y) - 96;
        double maxX = toCanvasX(x + w) + 96;
        double maxY = toCanvasY(y + h) + 96;

        // Bounded grid following pan. Avoid nested per-dot loops; zoom/pan should stay cheap.
        if (net.doole.doolestools.client.ClientPrefs.showGrid) {
            int spacing = Math.max(12, Math.round(24 * ctx.zoom));
            int ox = ((int) ctx.panX % spacing + spacing) % spacing;
            int oy = ((int) ctx.panY % spacing + spacing) % spacing;
            int maxLines = 96;
            int count = 0;
            for (int gx = x + ox; gx < x + w && count++ < maxLines; gx += spacing) {
                g.fill(gx, y + 1, gx + 1, y + h - 1, 0x3632452f);
            }
            count = 0;
            for (int gy = y + oy; gy < y + h && count++ < maxLines; gy += spacing) {
                g.fill(x + 1, gy, x + w - 1, gy + 1, 0x3632452f);
            }
        }

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + ctx.panX, y + ctx.panY);
        pose.scale(ctx.zoom, ctx.zoom);
        ensureModel(canvas);
        Map<String, GraphNodeData> nodesById = cachedNodesById;
        // Viewport culling over cached geometry is cheap rect arithmetic; no per-frame projection.
        List<GraphNodeData> visibleNodes = new ArrayList<>();
        for (GraphNodeData n : cachedNodes) {
            if (rectIntersects(n.x(), n.y(), n.width(), n.height(), minX, minY, maxX, maxY)) visibleNodes.add(n);
        }
        List<CachedLink> visibleLinks = new ArrayList<>();
        for (CachedLink link : cachedLinks) {
            if (lineBoundsIntersects(link.sx(), link.sy(), link.tx(), link.ty(), minX, minY, maxX, maxY)) visibleLinks.add(link);
        }
        boolean lowDetail = ctx.zoom < 0.85f || visibleNodes.size() > 80 || visibleLinks.size() > 120;

        // Annotation frames render behind everything.
        for (GraphFrameData f : canvas.frames()) {
            if (!rectIntersects(f.x(), f.y(), f.width(), f.height(), minX, minY, maxX, maxY)) continue;
            boolean fsel = f.frameId().equals(ctx.selectedFrameId);
            g.fill(f.x(), f.y(), f.x() + f.width(), f.y() + f.height(), 0x14000000 | (DUTheme.SELECTED & 0x00FFFFFF));
            DUTheme.outline(g, f.x(), f.y(), f.width(), f.height(), fsel ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
            g.fill(f.x(), f.y(), f.x() + f.width(), f.y() + FRAME_TITLE_H, fsel ? 0xFF14303a : DUTheme.PANEL_HEADER);
            g.text(font, f.label(), f.x() + 4, f.y() + 3, fsel ? DUTheme.SELECTED : DUTheme.TEXT_GREEN, false);
            // Bottom-right resize grip.
            int hx = f.x() + f.width();
            int hy = f.y() + f.height();
            int grip = fsel ? DUTheme.SELECTED : DUTheme.PANEL_BORDER;
            g.fill(hx - FRAME_HANDLE, hy - 2, hx - 1, hy - 1, grip);
            g.fill(hx - 2, hy - FRAME_HANDLE, hx - 1, hy - 1, grip);
        }

        // Links first (under nodes).
        for (CachedLink link : visibleLinks) {
            drawArrowCached(g, link, DUTheme.OK, ctx.isRouteActive(link.link().linkId()), lowDetail);
        }

        // Connection (side) override pill only shows for the link under the cursor.
        GraphLinkData hoveredLink = linkAt(mouseX, mouseY);
        if (hoveredLink != null) {
            for (CachedLink link : visibleLinks) {
                if (link.link().linkId().equals(hoveredLink.linkId())) {
                    drawSideOverridePill(g, font, link.link(), link.mx(), link.my());
                    break;
                }
            }
        }

        if (ctx.isDraggingPortLink()) {
            GraphNodeData source = nodesById.get(ctx.draggingSourceNodeId);
            int[] sp = source == null ? null : portPoint(source, ctx.draggingSourcePortId);
            if (sp != null) drawArrow(g, sp[0], sp[1], (int) toCanvasX(ctx.dragMouseX), (int) toCanvasY(ctx.dragMouseY), DUTheme.SELECTED, true, 12, false);
        } else if (ctx.isDraggingLinkRetarget()) {
            GraphLinkData link = findLink(ctx.draggingLinkId);
            GraphNodeData source = link == null ? null : nodesById.get(link.sourceNodeId());
            int[] sp = source == null || link == null ? null : portPoint(source, link.sourcePortId());
            if (sp != null) drawArrow(g, sp[0], sp[1], (int) toCanvasX(ctx.dragMouseX), (int) toCanvasY(ctx.dragMouseY), DUTheme.WARN, true, 12, false);
        }

        // Nodes.
        for (GraphNodeData n : visibleNodes) {
            boolean selected = ctx.isNodeSelected(n.nodeId());
            boolean linkSource = ctx.linkMode && n.nodeId().equals(ctx.linkSourceId);
            if (lowDetail) GraphNodeWidget.renderLite(g, font, n, selected, linkSource);
            else GraphNodeWidget.render(g, font, n, ctx.scannedFor(n), selected, linkSource);
            drawNodePowerBadge(g, font, n);
        }

        // Free-floating text labels (on top, always readable).
        for (GraphTextData t : canvas.texts()) {
            if (!rectIntersects(t.x(), t.y(), font.width(t.text()) + 4, 12, minX, minY, maxX, maxY)) continue;
            boolean tsel = t.textId().equals(ctx.selectedTextId);
            int tw = font.width(t.text());
            if (tsel) {
                g.fill(t.x() - 2, t.y() - 2, t.x() + tw + 2, t.y() + 10, 0x223fd2e0);
                DUTheme.outline(g, t.x() - 2, t.y() - 2, tw + 4, 12, DUTheme.SELECTED);
            }
            g.text(font, t.text(), t.x(), t.y(), tsel ? DUTheme.SELECTED : DUTheme.TEXT, false);
        }

        pose.popMatrix();
        g.disableScissor();

        if (canvas.nodes().isEmpty()) {
            g.centeredText(font, "DROP SCANNED BLOCKS HERE", x + w / 2, y + h / 2 - 4, DUTheme.TEXT_DIM);
        }
    }

    private static boolean rectIntersects(double x, double y, double w, double h, double minX, double minY, double maxX, double maxY) {
        return x <= maxX && x + w >= minX && y <= maxY && y + h >= minY;
    }

    private static boolean lineBoundsIntersects(int x1, int y1, int x2, int y2, double minX, double minY, double maxX, double maxY) {
        double margin = Math.max(64, Math.abs(x2 - x1) * 0.5);
        double lx = Math.min(x1, x2) - margin;
        double ly = Math.min(y1, y2) - 64;
        double lw = Math.abs(x2 - x1) + margin * 2;
        double lh = Math.abs(y2 - y1) + 128;
        return rectIntersects(lx, ly, lw, lh, minX, minY, maxX, maxY);
    }

    /** Rebuilds the cached node lookup and baked link polylines when the graph revision changes. */
    /** Small badge under transport-participating nodes when the network is out of / low on power. */
    private void drawNodePowerBadge(GuiGraphicsExtractor g, Font font, GraphNodeData n) {
        if (!cachedLinkedNodeIds.contains(n.nodeId())) return;
        net.doole.doolestools.logistics.data.NetworkPowerData power = ctx.power();
        if (power == null) return;
        String text;
        int color;
        if (power.starved()) {
            text = "TRANSPORT OFF · NO POWER";
            color = DUTheme.ERROR;
        } else if (power.degraded()) {
            text = "SLOW · LOW POWER";
            color = DUTheme.WARN;
        } else {
            return;
        }
        int bw = font.width(text) + 6;
        int bx = n.x() + (n.width() - bw) / 2;
        int by = n.y() + n.height() + 2;
        g.fill(bx, by, bx + bw, by + 10, 0xE60a0d09);
        DUTheme.outline(g, bx, by, bw, 10, color);
        g.text(font, text, bx + 3, by + 1, color, false);
    }

    private void ensureModel(net.doole.doolestools.logistics.data.GraphCanvasData canvas) {
        if (ctx.graphRevision() == cachedRevision) return;
        cachedRevision = ctx.graphRevision();
        Map<String, GraphNodeData> map = new HashMap<>();
        for (GraphNodeData n : canvas.nodes()) map.put(n.nodeId(), n);
        cachedNodes = canvas.nodes();
        cachedNodesById = map;
        List<CachedLink> links = new ArrayList<>();
        for (GraphLinkData link : canvas.links()) {
            GraphNodeData s = map.get(link.sourceNodeId());
            GraphNodeData t = map.get(link.targetNodeId());
            if (s == null || t == null) continue;
            int[] sp = portPoint(s, link.sourcePortId());
            int[] tp = portPoint(t, link.targetPortId());
            if (sp == null || tp == null) continue;
            int samples = curveSamples(sp[0], sp[1], tp[0], tp[1]);
            int[] px = new int[samples + 1];
            int[] py = new int[samples + 1];
            px[0] = sp[0];
            py[0] = sp[1];
            for (int i = 1; i <= samples; i++) {
                double tt = i / (double) samples;
                px[i] = (int) Math.round(curveX(sp[0], tp[0], tt));
                py[i] = (int) Math.round(curveY(sp[1], tp[1], tt));
            }
            int mx = (int) Math.round(curveX(sp[0], tp[0], 0.5));
            int my = (int) Math.round(curveY(sp[1], tp[1], 0.5));
            links.add(new CachedLink(link, sp[0], sp[1], tp[0], tp[1], mx, my, px, py));
        }
        cachedLinks = links;
        java.util.Set<String> linked = new java.util.HashSet<>();
        for (CachedLink cl : links) {
            linked.add(cl.link().sourceNodeId());
            linked.add(cl.link().targetNodeId());
        }
        cachedLinkedNodeIds = linked;
    }

    /** Draws a link from its baked polyline: solid base, optional animated flow dots, arrowhead. */
    private void drawArrowCached(GuiGraphicsExtractor g, CachedLink link, int color, boolean active, boolean lowDetail) {
        int[] px = link.px();
        int[] py = link.py();
        int baseColor = (color & 0x00FFFFFF) | 0xCC000000;
        int highlightPhase = net.doole.doolestools.client.ClientPrefs.animate ? (int) ((System.currentTimeMillis() / 90L) % 6L) : 0;
        for (int i = 1; i < px.length; i++) {
            DUTheme.line(g, px[i - 1], py[i - 1], px[i], py[i], baseColor);
            if (active && Math.floorMod(i - highlightPhase, 6) == 0) g.fill(px[i] - 1, py[i] - 1, px[i] + 2, py[i] + 2, color);
        }
        if (lowDetail || px.length < 2) return;
        int n = px.length;
        double dx = px[n - 1] - px[n - 2];
        double dy = py[n - 1] - py[n - 2];
        double len = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
        double ux = dx / len;
        double uy = dy / len;
        g.fill(link.sx() - 1, link.sy() - 1, link.sx() + 2, link.sy() + 2, color);
        int nx = (int) Math.round(-uy * 2.0);
        int ny = (int) Math.round(ux * 2.0);
        int hx = (int) Math.round(link.tx() - ux * 6.0);
        int hy = (int) Math.round(link.ty() - uy * 6.0);
        DUTheme.line(g, hx + nx, hy + ny, link.tx(), link.ty(), color);
        DUTheme.line(g, hx - nx, hy - ny, link.tx(), link.ty(), color);
        g.fill(link.tx() - 2, link.ty() - 2, link.tx() + 2, link.ty() + 2, color);
    }

    /** Draws a live Bezier link (used for the in-progress drag link, which can't be cached). */
    private void drawArrow(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, boolean active, int maxSamples, boolean lowDetail) {
        int samples = Math.max(4, Math.min(maxSamples, curveSamples(x1, y1, x2, y2)));
        int baseColor = (color & 0x00FFFFFF) | 0xCC000000;
        int highlightPhase = net.doole.doolestools.client.ClientPrefs.animate ? (int) ((System.currentTimeMillis() / 90L) % 6L) : 0;
        int lastX = x1;
        int lastY = y1;
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            int px = (int) Math.round(curveX(x1, x2, t));
            int py = (int) Math.round(curveY(y1, y2, t));
            DUTheme.line(g, lastX, lastY, px, py, baseColor);
            if (active && Math.floorMod(i - highlightPhase, 6) == 0) g.fill(px - 1, py - 1, px + 2, py + 2, color);
            lastX = px;
            lastY = py;
        }

        if (lowDetail) return;

        double t1 = Math.max(0.0, 1.0 - 1.0 / samples);
        double dx = x2 - curveX(x1, x2, t1);
        double dy = y2 - curveY(y1, y2, t1);
        double len = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
        double ux = dx / len;
        double uy = dy / len;
        g.fill(x1 - 1, y1 - 1, x1 + 2, y1 + 2, color);
        int nx = (int) Math.round(-uy * 2.0);
        int ny = (int) Math.round(ux * 2.0);
        int hx = (int) Math.round(x2 - ux * 6.0);
        int hy = (int) Math.round(y2 - uy * 6.0);
        DUTheme.line(g, hx + nx, hy + ny, x2, y2, color);
        DUTheme.line(g, hx - nx, hy - ny, x2, y2, color);
        g.fill(x2 - 2, y2 - 2, x2 + 2, y2 + 2, color);
    }

    private static int curveSamples(int x1, int y1, int x2, int y2) {
        double len = Math.sqrt(square(x2 - x1) + square(y2 - y1));
        return Math.max(12, Math.min(36, (int) Math.ceil(len / 14.0)));
    }

    private static double curveX(int x1, int x2, double t) {
        double c1 = x1 + Math.max(36, Math.abs(x2 - x1) * 0.45);
        double c2 = x2 - Math.max(36, Math.abs(x2 - x1) * 0.45);
        double u = 1.0 - t;
        return u * u * u * x1 + 3 * u * u * t * c1 + 3 * u * t * t * c2 + t * t * t * x2;
    }

    private static double curveY(int y1, int y2, double t) {
        double u = 1.0 - t;
        return u * u * u * y1 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y2;
    }

    private void drawSideOverridePill(GuiGraphicsExtractor g, Font font, GraphLinkData link) {
        int[] midpoint = linkMidpoint(link);
        if (midpoint == null) return;
        drawSideOverridePill(g, font, link, midpoint[0], midpoint[1]);
    }

    private void drawSideOverridePill(GuiGraphicsExtractor g, Font font, GraphLinkData link, int midX, int midY) {
        String label = sideLabel(link.sideOverride());
        int px = midX - SIDE_PILL_W / 2;
        int py = midY - SIDE_PILL_H / 2;
        int border = "auto".equalsIgnoreCase(link.sideOverride()) ? DUTheme.PANEL_BORDER : DUTheme.SELECTED;
        DUTheme.box(g, px, py, SIDE_PILL_W, SIDE_PILL_H, 0xEE0b0f0a, border);
        g.text(font, label, px + 4, py + 2, "auto".equalsIgnoreCase(link.sideOverride()) ? DUTheme.TEXT_DIM : DUTheme.SELECTED, false);
    }

    private int[] linkMidpoint(GraphLinkData link) {
        return linkMidpoint(link, nodeMap());
    }

    private int[] linkMidpoint(GraphLinkData link, Map<String, GraphNodeData> nodesById) {
        GraphNodeData source = nodesById.get(link.sourceNodeId());
        GraphNodeData target = nodesById.get(link.targetNodeId());
        if (source == null || target == null) return null;
        int[] sp = portPoint(source, link.sourcePortId());
        int[] tp = portPoint(target, link.targetPortId());
        if (sp == null || tp == null) return null;
        return new int[] { (int) Math.round(curveX(sp[0], tp[0], 0.5)), (int) Math.round(curveY(sp[1], tp[1], 0.5)) };
    }

    private static String sideLabel(String sideOverride) {
        return switch (sideOverride == null ? "auto" : sideOverride.toLowerCase(java.util.Locale.ROOT)) {
            case "up" -> "Top";
            case "down" -> "Bottom";
            case "north" -> "Back";
            case "west" -> "Left";
            case "east" -> "Right";
            case "south" -> "Back";
            default -> "Auto";
        };
    }

    private int[] portPoint(GraphNodeData node, String portId) {
        int inIndex = 0;
        int outIndex = 0;
        for (GraphPortData port : node.ports()) {
            int index = port.direction() == PortDirection.IN ? inIndex++ : outIndex++;
            if (port.portId().equals(portId)) {
                return new int[] { GraphNodeWidget.portX(node, port.direction()), GraphNodeWidget.portY(node, port.direction(), index) };
            }
        }
        return null;
    }

    private GraphLinkData findLink(String linkId) {
        for (GraphLinkData link : ctx.graph().activeCanvas().links()) {
            if (link.linkId().equals(linkId)) return link;
        }
        return null;
    }
}
