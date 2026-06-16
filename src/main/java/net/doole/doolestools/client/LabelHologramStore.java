package net.doole.doolestools.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache of nearby block labels, used to render the Label Gun holograms. Refreshed by the
 * server while the player holds the gun, and updated optimistically when the player applies a label.
 */
public final class LabelHologramStore {
    private LabelHologramStore() {}

    private static final Map<BlockPos, String> LABELS = new HashMap<>();

    public static synchronized void replaceAll(java.util.List<BlockPos> positions, java.util.List<String> labels) {
        LABELS.clear();
        int n = Math.min(positions.size(), labels.size());
        for (int i = 0; i < n; i++) {
            LABELS.put(positions.get(i).immutable(), labels.get(i));
        }
    }

    /** Optimistic local update so a freshly-applied label appears before the next server sync. */
    public static synchronized void put(BlockPos pos, String label) {
        if (label == null || label.isBlank()) LABELS.remove(pos);
        else LABELS.put(pos.immutable(), label);
    }

    public static synchronized Map<BlockPos, String> snapshot() {
        return new HashMap<>(LABELS);
    }

    public static synchronized void clear() {
        LABELS.clear();
    }
}
