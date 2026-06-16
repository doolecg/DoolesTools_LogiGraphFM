package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A single stacked item type and its total count within an inventory. */
public record ItemEntry(String displayName, String registryId, int count) {
    public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(ItemEntry::displayName),
            // Optional for backward compatibility with entries saved before item icons existed.
            Codec.STRING.optionalFieldOf("id", "").forGetter(ItemEntry::registryId),
            Codec.INT.fieldOf("count").forGetter(ItemEntry::count)
    ).apply(inst, ItemEntry::new));
}
