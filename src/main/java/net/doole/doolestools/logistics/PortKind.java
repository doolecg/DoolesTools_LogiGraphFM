package net.doole.doolestools.logistics;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Semantic type of a graph node socket. */
public enum PortKind {
    ITEM,
    FLUID,
    ENERGY,
    SIGNAL,
    NETWORK,
    GENERIC;

    public static final Codec<PortKind> CODEC = Codec.STRING.xmap(PortKind::fromString, PortKind::serialize);

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PortKind fromString(String s) {
        if (s == null) return GENERIC;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GENERIC;
        }
    }
}
