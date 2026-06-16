package net.doole.doolestools.logistics;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Player-assignable role of a graph node. */
public enum NodeType {
    SOURCE,
    STORAGE,
    MACHINE,
    BUFFER,
    SINK,
    FILTER,
    // Routing nodes — standalone relays that move items along the links you draw (no world block):
    SPLITTER,   // one input distributed round-robin across its outputs
    COMBINE,    // many inputs merged into one output
    CHANNEL,    // forwards onto the outputs carrying its selected colour channel
    UNKNOWN;

    public static final Codec<NodeType> CODEC = Codec.STRING.xmap(NodeType::fromString, NodeType::serialize);

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static NodeType fromString(String s) {
        if (s == null) return UNKNOWN;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
