package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A labelled annotation frame drawn behind the nodes on the flowgraph canvas. Frames are purely
 * organisational: they group/label regions of the graph and carry no logistics meaning.
 */
public record GraphFrameData(String frameId,
                             String label,
                             int x,
                             int y,
                             int width,
                             int height) {

    public static final int DEFAULT_WIDTH = 180;
    public static final int DEFAULT_HEIGHT = 132;

    public static final Codec<GraphFrameData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("frameId").forGetter(GraphFrameData::frameId),
            Codec.STRING.fieldOf("label").forGetter(GraphFrameData::label),
            Codec.INT.fieldOf("x").forGetter(GraphFrameData::x),
            Codec.INT.fieldOf("y").forGetter(GraphFrameData::y),
            Codec.INT.fieldOf("width").forGetter(GraphFrameData::width),
            Codec.INT.fieldOf("height").forGetter(GraphFrameData::height)
    ).apply(inst, GraphFrameData::new));

    public GraphFrameData withPosition(int newX, int newY) {
        return new GraphFrameData(frameId, label, newX, newY, width, height);
    }

    public GraphFrameData withLabel(String newLabel) {
        return new GraphFrameData(frameId, newLabel, x, y, width, height);
    }

    public GraphFrameData withSize(int newW, int newH) {
        return new GraphFrameData(frameId, label, x, y, newW, newH);
    }
}
