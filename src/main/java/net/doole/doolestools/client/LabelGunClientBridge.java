package net.doole.doolestools.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Reflection target for common item code so dedicated servers never directly link client screens. */
public final class LabelGunClientBridge {
    private LabelGunClientBridge() {}

    public static void open(String currentName) {
        net.doole.doolestools.client.screen.LabelGunScreen.open(currentName);
    }

    public static void applyLocal(BlockPos pos, String name) {
        LabelHologramStore.put(pos, name);
    }

    public static void openEndpointName(BlockPos pos, String title, String currentName, String currentId, int[] upgradeCounts) {
        net.doole.doolestools.client.screen.NetworkEndpointNameScreen.open(pos, title, currentName, currentId, upgradeCounts, null);
    }

    public static void openEndpointNameForFace(BlockPos pos, String title, String currentName, String currentId, int[] upgradeCounts, @Nullable Direction face) {
        net.doole.doolestools.client.screen.NetworkEndpointNameScreen.open(pos, title, currentName, currentId, upgradeCounts, face);
    }
}
