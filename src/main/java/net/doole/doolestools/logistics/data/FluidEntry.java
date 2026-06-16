package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A single fluid tank reading. {@code fluidId} is the registry id. */
public record FluidEntry(String fluidName, String fluidId, long amount, long capacity) {
    public static final Codec<FluidEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("fluid").forGetter(FluidEntry::fluidName),
            // Optional so tank readings saved before the id was tracked keep loading.
            Codec.STRING.optionalFieldOf("id", "").forGetter(FluidEntry::fluidId),
            Codec.LONG.fieldOf("amount").forGetter(FluidEntry::amount),
            Codec.LONG.fieldOf("capacity").forGetter(FluidEntry::capacity)
    ).apply(inst, FluidEntry::new));

}
