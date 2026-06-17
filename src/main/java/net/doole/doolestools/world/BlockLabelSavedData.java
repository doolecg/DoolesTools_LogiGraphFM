package net.doole.doolestools.world;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/** Server-owned persistent block labels shared by all players. */
public class BlockLabelSavedData extends SavedData {
    private static final String NAME = DoolesTools.MOD_ID + "_block_labels";
    private static final int MAX_LABEL = 48;
    public static final Codec<BlockLabelSavedData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(BlockLabelSavedData::new, data -> data.labels);
    public static final SavedDataType<BlockLabelSavedData> TYPE =
            new SavedDataType<>(DoolesTools.id(NAME), BlockLabelSavedData::new, CODEC);

    private final Map<String, String> labels = new HashMap<>();

    private BlockLabelSavedData(Map<String, String> labels) {
        this.labels.putAll(labels);
    }

    public BlockLabelSavedData() {}

    public static BlockLabelSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public String getLabel(Identifier dimension, BlockPos pos) {
        return labels.get(key(dimension, pos));
    }

    /** Collects labels in {@code dimension} within {@code radius} blocks of {@code center}. */
    public void gatherNearby(Identifier dimension, BlockPos center, int radius,
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

    public void setLabel(Identifier dimension, BlockPos pos, String label) {
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

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + ":" + pos.asLong();
    }
}
