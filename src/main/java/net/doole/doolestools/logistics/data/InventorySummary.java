package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Read-only summary of an item inventory. Slots/contents are counted, never mutated. */
public record InventorySummary(int usedSlots, int totalSlots, List<ItemEntry> topStacks) {

    public static final InventorySummary EMPTY = new InventorySummary(0, 0, List.of());

    public static final Codec<InventorySummary> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("used").forGetter(InventorySummary::usedSlots),
            Codec.INT.fieldOf("total").forGetter(InventorySummary::totalSlots),
            ItemEntry.CODEC.listOf().fieldOf("top").forGetter(InventorySummary::topStacks)
    ).apply(inst, InventorySummary::new));

    public boolean hasData() {
        return totalSlots > 0;
    }

    /** Fill percentage by slot occupancy (0-100). */
    public int fillPercent() {
        if (totalSlots <= 0) return 0;
        return Math.round((usedSlots * 100.0F) / totalSlots);
    }
}
