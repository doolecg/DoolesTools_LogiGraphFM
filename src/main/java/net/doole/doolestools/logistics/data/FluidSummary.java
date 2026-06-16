package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Read-only summary of fluid tanks, if the block exposes any. */
public record FluidSummary(List<FluidEntry> tanks) {

    public static final FluidSummary EMPTY = new FluidSummary(List.of());

    public static final Codec<FluidSummary> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            FluidEntry.CODEC.listOf().fieldOf("tanks").forGetter(FluidSummary::tanks)
    ).apply(inst, FluidSummary::new));

    public boolean hasData() {
        return !tanks.isEmpty();
    }
}
