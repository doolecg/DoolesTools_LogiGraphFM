package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.logistics.PortDirection;
import net.doole.doolestools.logistics.PortKind;

/** A typed socket on a graph node. Links run from OUT ports to IN ports. */
public record GraphPortData(String portId,
                            PortDirection direction,
                            PortKind kind,
                            String label) {

    public static final Codec<GraphPortData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("portId").forGetter(GraphPortData::portId),
            PortDirection.CODEC.fieldOf("direction").forGetter(GraphPortData::direction),
            PortKind.CODEC.fieldOf("kind").forGetter(GraphPortData::kind),
            Codec.STRING.fieldOf("label").forGetter(GraphPortData::label)
    ).apply(inst, GraphPortData::new));
}
