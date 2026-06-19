package net.doole.doolestools.world;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Server-owned persistent block labels shared by all players. */
public class BlockLabelSavedData extends SavedData {
    private static final String NAME = DoolesTools.MOD_ID + "_block_labels";
    private static final int MAX_LABEL = 48;
    public static final Codec<BlockLabelSavedData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(BlockLabelSavedData::new, data -> data.labels);
    private static final SavedData.Factory<BlockLabelSavedData> FACTORY =
            new SavedData.Factory<>(BlockLabelSavedData::new, BlockLabelSavedData::load, null);

    private final Map<String, String> labels = new HashMap<>();

    private BlockLabelSavedData(Map<String, String> labels) {
        this.labels.putAll(labels);
    }

    public BlockLabelSavedData() {}

    public static BlockLabelSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static BlockLabelSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        Map<String, String> labels = new HashMap<>();
        CompoundTag values = tag.getCompound("labels");
        for (String key : values.getAllKeys()) {
            labels.put(key, values.getString(key));
        }
        return new BlockLabelSavedData(labels);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag values = new CompoundTag();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            values.putString(entry.getKey(), entry.getValue());
        }
        tag.put("labels", values);
        return tag;
    }

    public String getLabel(ResourceLocation dimension, BlockPos pos) {
        return labels.get(key(dimension, pos));
    }

    /** Collects labels in {@code dimension} within {@code radius} blocks of {@code center}. */
    public void gatherNearby(ResourceLocation dimension, BlockPos center, int radius,
                             java.util.List<BlockPos> outPositions, java.util.List<String> outLabels) {
        String dimPrefix = dimension + ":";
        long radiusSqr = (long) radius * radius;
        for (Map.Entry<String, String> e : labels.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith(dimPrefix)) continue;
            long packed;
            try {
                packed = Long.parseLong(key.substring(dimPrefix.length()));
            } catch (NumberFormatException ex) {
                continue;
            }
            BlockPos pos = BlockPos.of(packed);
            if (center.distSqr(pos) > radiusSqr) continue;
            outPositions.add(pos);
            outLabels.add(e.getValue());
        }
    }

    public void setLabel(ResourceLocation dimension, BlockPos pos, String label) {
        String cleaned = sanitize(label);
        String key = key(dimension, pos);
        if (cleaned.isBlank()) labels.remove(key);
        else labels.put(key, cleaned);
        setDirty();
    }

    public static String sanitize(String label) {
        if (label == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = label.trim();
        int i = 0;
        while (i < trimmed.length() && out.length() < MAX_LABEL) {
            int cp = trimmed.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isISOControl(cp)) out.appendCodePoint(cp);
        }
        return out.toString();
    }

    private static String key(ResourceLocation dimension, BlockPos pos) {
        return dimension + ":" + pos.asLong();
    }
}
