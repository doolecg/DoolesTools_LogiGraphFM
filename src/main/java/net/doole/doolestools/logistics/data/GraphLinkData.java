package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.logistics.LinkType;

/** A player-declared directed link between two graph nodes. */
public record GraphLinkData(String linkId,
                            String sourceNodeId,
                            String sourcePortId,
                            String targetNodeId,
                            String targetPortId,
                            String label,
                            LinkType type,
                            String sideOverride) {

    public static final Codec<GraphLinkData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("linkId").forGetter(GraphLinkData::linkId),
            Codec.STRING.fieldOf("sourceNodeId").forGetter(GraphLinkData::sourceNodeId),
            Codec.STRING.optionalFieldOf("sourcePortId", "out").forGetter(GraphLinkData::sourcePortId),
            Codec.STRING.fieldOf("targetNodeId").forGetter(GraphLinkData::targetNodeId),
            Codec.STRING.optionalFieldOf("targetPortId", "in").forGetter(GraphLinkData::targetPortId),
            Codec.STRING.fieldOf("label").forGetter(GraphLinkData::label),
            LinkType.CODEC.fieldOf("type").forGetter(GraphLinkData::type),
            Codec.STRING.optionalFieldOf("sideOverride", "auto").forGetter(GraphLinkData::sideOverride)
    ).apply(inst, GraphLinkData::new));

    public GraphLinkData(String linkId, String sourceNodeId, String sourcePortId,
                         String targetNodeId, String targetPortId, String label, LinkType type) {
        this(linkId, sourceNodeId, sourcePortId, targetNodeId, targetPortId, label, type, "auto");
    }

    public GraphLinkData(String linkId, String sourceNodeId, String targetNodeId, String label, LinkType type) {
        this(linkId, sourceNodeId, "out", targetNodeId, "in", label, type);
    }

    public GraphLinkData withSideOverride(String side) {
        return new GraphLinkData(linkId, sourceNodeId, sourcePortId, targetNodeId, targetPortId, label, type, side == null ? "auto" : side);
    }
}
