package net.doole.doolestools.logistics;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Detected category of a scanned block, used for filtering and node-type defaults. */
public enum ScannedType {
    STORAGE,
    MACHINE,
    TRANSPORT,
    UNKNOWN_STORAGE,
    UNKNOWN_MACHINE,
    UNKNOWN;

    public static final Codec<ScannedType> CODEC = Codec.STRING.xmap(ScannedType::fromString, ScannedType::serialize);

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ScannedType fromString(String s) {
        if (s == null) return UNKNOWN;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public NodeType toDefaultNodeType() {
        return switch (this) {
            case STORAGE, UNKNOWN_STORAGE -> NodeType.STORAGE;
            case MACHINE, UNKNOWN_MACHINE -> NodeType.MACHINE;
            case TRANSPORT -> NodeType.BUFFER;
            default -> NodeType.UNKNOWN;
        };
    }
}
