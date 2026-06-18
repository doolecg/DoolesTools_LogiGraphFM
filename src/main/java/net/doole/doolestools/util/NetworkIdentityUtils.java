package net.doole.doolestools.util;

import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class NetworkIdentityUtils {
    private NetworkIdentityUtils() {}

    /** Strip control characters and trim to 48 code points. */
    public static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = value.trim();
        int i = 0;
        while (i < trimmed.length() && out.length() < 48) {
            int cp = trimmed.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isISOControl(cp)) out.appendCodePoint(cp);
        }
        return out.toString();
    }

    /** Convert a display name to a lowercase alphanumeric slug, max 48 chars. */
    public static String slug(String value) {
        String cleaned = sanitize(value).toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        boolean lastUnderscore = false;
        for (int i = 0; i < cleaned.length() && out.length() < 48; i++) {
            char c = cleaned.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (ok) {
                out.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore && !out.isEmpty()) {
                out.append('_');
                lastUnderscore = true;
            }
        }
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == '_') out.setLength(len - 1);
        return out.isEmpty() ? "endpoint" : out.toString();
    }

    /** Normalise a network ID to lowercase alphanumeric + underscore/hyphen, max 64 chars. */
    public static String sanitizeNetworkId(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String cleaned = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < cleaned.length() && out.length() < 64; i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') out.append(c);
        }
        return out.toString();
    }

    /**
     * Scans loaded chunks within 32 blocks of {@code center} for Logistics Computers.
     * Returns the shared network ID if exactly one unique network is found, otherwise "".
     */
    public static String inferNearbyNetwork(ServerLevel level, BlockPos center) {
        String found = "";
        int matches = 0;
        int radius = 32;
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof LogisticsComputerBlockEntity computer)) continue;
                    if (center.distSqr(computer.getBlockPos()) > (long) radius * radius) continue;
                    String id = computer.networkId();
                    if (id.isBlank()) continue;
                    if (!id.equals(found)) {
                        found = id;
                        matches++;
                        if (matches > 1) return "";
                    }
                }
            }
        }
        return matches == 1 ? found : "";
    }
}
