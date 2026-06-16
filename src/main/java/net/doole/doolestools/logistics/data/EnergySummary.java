package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Read-only energy reading, if the block exposes an energy capability. */
public record EnergySummary(boolean present, long stored, long capacity) {

    public static final EnergySummary EMPTY = new EnergySummary(false, 0L, 0L);

    public static final Codec<EnergySummary> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("present").forGetter(EnergySummary::present),
            Codec.LONG.fieldOf("stored").forGetter(EnergySummary::stored),
            Codec.LONG.fieldOf("capacity").forGetter(EnergySummary::capacity)
    ).apply(inst, EnergySummary::new));

    public boolean hasData() {
        return present && capacity > 0;
    }

    public int fillPercent() {
        if (capacity <= 0) return 0;
        return (int) Math.round((stored * 100.0) / capacity);
    }
}
