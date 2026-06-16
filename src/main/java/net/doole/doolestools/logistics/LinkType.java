package net.doole.doolestools.logistics;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** What a player-declared link represents. Purely descriptive in the MVP. */
public enum LinkType {
    ITEMS,
    FLUIDS,
    ENERGY,
    MANUAL;

    public static final Codec<LinkType> CODEC = Codec.STRING.xmap(LinkType::fromString, LinkType::serialize);

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LinkType fromString(String s) {
        if (s == null) return MANUAL;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MANUAL;
        }
    }
}
