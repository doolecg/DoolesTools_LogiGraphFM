package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** One named graph canvas/tab inside a Logistics Computer graph. */
public record GraphCanvasData(String canvasId,
                              String title,
                              List<GraphNodeData> nodes,
                              List<GraphLinkData> links,
                              List<GraphFrameData> frames,
                              List<GraphTextData> texts) {
    public static final Codec<GraphCanvasData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("canvasId").forGetter(GraphCanvasData::canvasId),
            Codec.STRING.fieldOf("title").forGetter(GraphCanvasData::title),
            GraphNodeData.CODEC.listOf().fieldOf("nodes").forGetter(GraphCanvasData::nodes),
            GraphLinkData.CODEC.listOf().fieldOf("links").forGetter(GraphCanvasData::links),
            GraphFrameData.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(GraphCanvasData::frames),
            GraphTextData.CODEC.listOf().optionalFieldOf("texts", List.of()).forGetter(GraphCanvasData::texts)
    ).apply(inst, GraphCanvasData::new));

    public GraphCanvasData copyMutable() {
        return new GraphCanvasData(canvasId, title, new ArrayList<>(nodes), new ArrayList<>(links),
                new ArrayList<>(frames), new ArrayList<>(texts));
    }
}
