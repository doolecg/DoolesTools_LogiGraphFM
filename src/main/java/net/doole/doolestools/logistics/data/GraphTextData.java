package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A free-floating text label placed on the flowgraph canvas. Purely an annotation. */
public record GraphTextData(String textId, String text, int x, int y) {

    public static final Codec<GraphTextData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("textId").forGetter(GraphTextData::textId),
            Codec.STRING.fieldOf("text").forGetter(GraphTextData::text),
            Codec.INT.fieldOf("x").forGetter(GraphTextData::x),
            Codec.INT.fieldOf("y").forGetter(GraphTextData::y)
    ).apply(inst, GraphTextData::new));

    public GraphTextData withPosition(int newX, int newY) {
        return new GraphTextData(textId, text, newX, newY);
    }

    public GraphTextData withText(String newText) {
        return new GraphTextData(textId, newText, x, y);
    }
}
