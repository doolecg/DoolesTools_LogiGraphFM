package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** The persisted flowgraph: nodes, links, and which monitors mirror it. */
public record LogisticsGraphData(String graphId,
                                 String graphName,
                                 List<GraphNodeData> nodes,
                                 List<GraphLinkData> links,
                                  long lastSavedTime,
                                  List<BlockPos> linkedMonitors,
                                  List<GraphFrameData> frames,
                                  List<GraphTextData> texts,
                                  String activeCanvasId,
                                   List<GraphCanvasData> canvases) {

    public static final LogisticsGraphData EMPTY =
            new LogisticsGraphData("default", "Untitled Graph", List.of(), List.of(), 0L, List.of(), List.of(), List.of(), "main", List.of());

    public static final Codec<LogisticsGraphData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("graphId").forGetter(LogisticsGraphData::graphId),
            Codec.STRING.fieldOf("graphName").forGetter(LogisticsGraphData::graphName),
            GraphNodeData.CODEC.listOf().fieldOf("nodes").forGetter(LogisticsGraphData::nodes),
            GraphLinkData.CODEC.listOf().fieldOf("links").forGetter(LogisticsGraphData::links),
            Codec.LONG.fieldOf("lastSavedTime").forGetter(LogisticsGraphData::lastSavedTime),
            BlockPos.CODEC.listOf().fieldOf("linkedMonitors").forGetter(LogisticsGraphData::linkedMonitors),
            // Optional for backward compatibility with graphs saved before frames/texts existed.
            GraphFrameData.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(LogisticsGraphData::frames),
            GraphTextData.CODEC.listOf().optionalFieldOf("texts", List.of()).forGetter(LogisticsGraphData::texts),
            Codec.STRING.optionalFieldOf("activeCanvasId", "main").forGetter(LogisticsGraphData::activeCanvasId),
            GraphCanvasData.CODEC.listOf().optionalFieldOf("canvases", List.of()).forGetter(LogisticsGraphData::canvases)
    ).apply(inst, LogisticsGraphData::new));

    public LogisticsGraphData(String graphId, String graphName, List<GraphNodeData> nodes,
                              List<GraphLinkData> links, long lastSavedTime, List<BlockPos> linkedMonitors,
                              List<GraphFrameData> frames, List<GraphTextData> texts) {
        this(graphId, graphName, nodes, links, lastSavedTime, linkedMonitors, frames, texts, "main", List.of());
    }

    /** Defensive copy with mutable lists, for editor-side use. */
    public LogisticsGraphData copyMutable() {
        return new LogisticsGraphData(graphId, graphName,
                new ArrayList<>(nodes), new ArrayList<>(links), lastSavedTime,
                new ArrayList<>(linkedMonitors), new ArrayList<>(frames), new ArrayList<>(texts),
                activeCanvasId, new ArrayList<>(canvases));
    }

    public List<GraphCanvasData> canvasesOrLegacy() {
        if (!canvases.isEmpty()) return canvases;
        return List.of(new GraphCanvasData("main", graphName, nodes, links, frames, texts));
    }

    public GraphCanvasData activeCanvas() {
        for (GraphCanvasData canvas : canvasesOrLegacy()) {
            if (canvas.canvasId().equals(activeCanvasId)) return canvas;
        }
        return canvasesOrLegacy().get(0);
    }

    public LogisticsGraphData withActiveCanvas(GraphCanvasData updated) {
        List<GraphCanvasData> source = canvasesOrLegacy();
        List<GraphCanvasData> out = new ArrayList<>();
        boolean replaced = false;
        for (GraphCanvasData canvas : source) {
            if (canvas.canvasId().equals(updated.canvasId())) {
                out.add(updated);
                replaced = true;
            } else out.add(canvas);
        }
        if (!replaced) out.add(updated);
        return new LogisticsGraphData(graphId, graphName, updated.nodes(), updated.links(), lastSavedTime,
                linkedMonitors, updated.frames(), updated.texts(), updated.canvasId(), out);
    }

    public GraphFrameData findFrame(String frameId) {
        for (GraphFrameData f : activeCanvas().frames()) {
            if (f.frameId().equals(frameId)) return f;
        }
        return null;
    }

    public GraphTextData findText(String textId) {
        for (GraphTextData t : activeCanvas().texts()) {
            if (t.textId().equals(textId)) return t;
        }
        return null;
    }

    public GraphNodeData findNode(String nodeId) {
        for (GraphNodeData n : activeCanvas().nodes()) {
            if (n.nodeId().equals(nodeId)) return n;
        }
        return null;
    }

    public boolean isEmpty() {
        GraphCanvasData active = activeCanvas();
        return active.nodes().isEmpty() && active.links().isEmpty();
    }
}
