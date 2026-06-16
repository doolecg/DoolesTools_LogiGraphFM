package net.doole.doolestools.logistics;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Direction of a graph node socket. */
public enum PortDirection {
    IN,
    OUT;

    public static final Codec<PortDirection> CODEC = Codec.STRING.xmap(PortDirection::fromString, PortDirection::serialize);

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PortDirection fromString(String s) {
        if (s == null) return IN;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return IN;
        }
    }
}
