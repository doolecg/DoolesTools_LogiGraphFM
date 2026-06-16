package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/** A single warning attached to a scanned block / node. */
public record WarningData(Severity severity, String message) {

    public enum Severity {
        INFO, WARNING, ERROR;

        public static final Codec<Severity> CODEC = Codec.STRING.xmap(Severity::fromString, Severity::serialize);

        public String serialize() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Severity fromString(String s) {
            if (s == null) return INFO;
            try {
                return valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return INFO;
            }
        }
    }

    public static final Codec<WarningData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Severity.CODEC.fieldOf("severity").forGetter(WarningData::severity),
            Codec.STRING.fieldOf("message").forGetter(WarningData::message)
    ).apply(inst, WarningData::new));

    public static WarningData info(String msg) {
        return new WarningData(Severity.INFO, msg);
    }

    public static WarningData warning(String msg) {
        return new WarningData(Severity.WARNING, msg);
    }

    public static WarningData error(String msg) {
        return new WarningData(Severity.ERROR, msg);
    }
}
