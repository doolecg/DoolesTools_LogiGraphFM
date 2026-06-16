package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bottom warning strip. Aggregates block + graph warnings into clickable segments. */
public class WarningBarWidget {

    /** A clickable warning segment. {@code nodeId}/{@code scannedId} may be null. */
    public record Entry(String text, int color, String nodeId, String scannedId) {}

    private final EditorContext ctx;
    public int x, y, w, h;
    private final List<int[]> hitBoxes = new ArrayList<>(); // {startX, endX, entryIndex}
    private List<Entry> entries = List.of();

    public WarningBarWidget(EditorContext ctx, int x, int y, int w, int h) {
        this.ctx = ctx;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public List<Entry> build() {
        List<Entry> out = new ArrayList<>();

        // Per-block warnings (only surface ones the player has on the graph, plus any errors).
        for (ScannedBlockData s : ctx.scan()) {
            if (!s.hasWarnings()) continue;
            String nodeId = nodeForScanned(s.id());
            for (WarningData wd : s.warnings()) {
                if (wd.severity() == WarningData.Severity.INFO && nodeId == null) continue;
                out.add(new Entry(s.blockName() + " " + wd.message(), color(wd.severity()), nodeId, s.id()));
            }
        }

        // Graph structural warnings.
        Set<String> hasOut = new HashSet<>();
        Set<String> hasIn = new HashSet<>();
        for (GraphLinkData l : ctx.graph().activeCanvas().links()) {
            hasOut.add(l.sourceNodeId());
            hasIn.add(l.targetNodeId());
        }
        for (GraphNodeData n : ctx.graph().activeCanvas().nodes()) {
            boolean linked = hasOut.contains(n.nodeId()) || hasIn.contains(n.nodeId());
            if (!linked) {
                out.add(new Entry(n.displayName() + " not linked", DUTheme.WARN, n.nodeId(), n.scannedBlockId()));
            }
        }
        return out;
    }

    private String nodeForScanned(String scannedId) {
        for (GraphNodeData n : ctx.graph().activeCanvas().nodes()) {
            if (n.scannedBlockId().equals(scannedId)) return n.nodeId();
        }
        return null;
    }

    public void render(GuiGraphicsExtractor g, Font font) {
        entries = build();
        hitBoxes.clear();

        int tx = x + 4, ty = y + (h - 9) / 2;
        if (!entries.isEmpty()) {
            GuiSprites.draw(g, GuiSprites.WARNING, tx - 1, ty - 2, 13);
        }

        int labelX = x + 16;
        g.text(font, "WARNINGS:", labelX, y + (h - 8) / 2, entries.isEmpty() ? DUTheme.OK : DUTheme.ERROR, false);
        int cursor = labelX + font.width("WARNINGS: ") + 4;
        int textY = y + (h - 8) / 2;

        if (entries.isEmpty()) {
            g.text(font, "All systems nominal.", cursor, textY, DUTheme.TEXT_DIM, false);
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            String seg = e.text();
            int segW = font.width(seg);
            if (cursor + segW > x + w - 8) {
                g.text(font, "…", cursor, textY, DUTheme.TEXT_DIM, false);
                break;
            }
            g.text(font, seg, cursor, textY, e.color(), false);
            hitBoxes.add(new int[]{cursor, cursor + segW, i});
            cursor += segW;
            if (i < entries.size() - 1) {
                g.text(font, " | ", cursor, textY, DUTheme.TEXT_DIM, false);
                cursor += font.width(" | ");
            }
        }
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Selects the node/scanned block associated with a clicked warning. */
    public boolean click(double mx, double my) {
        if (!contains(mx, my)) return false;
        for (int[] box : hitBoxes) {
            if (mx >= box[0] && mx < box[1]) {
                Entry e = entries.get(box[2]);
                if (e.nodeId() != null) ctx.selectedNodeId = e.nodeId();
                if (e.scannedId() != null) ctx.selectedScannedId = e.scannedId();
                return true;
            }
        }
        return false;
    }

    private static int color(WarningData.Severity sev) {
        return switch (sev) {
            case ERROR -> DUTheme.ERROR;
            case WARNING -> DUTheme.WARN;
            default -> DUTheme.TEXT_DIM;
        };
    }
}
