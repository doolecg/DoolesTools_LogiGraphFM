package net.doole.doolestools.integration;

import net.doole.doolestools.DoolesTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for mod-specific storage providers.
 * Providers register once at startup (during init), then reads happen on the server thread.
 * The list itself is only written during mod load so no locking is needed for reads.
 */
public final class ModProviderRegistry {
    private ModProviderRegistry() {}

    // Written only during FMLCommonSetupEvent / init, read-only after that
    private static final List<ModStorageProvider> PROVIDERS = new ArrayList<>();

    /** Register a provider. Call only during mod initialisation, not at scan time. */
    public static void register(ModStorageProvider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * Try every registered provider in registration order and return the first result.
     * Returns null if no provider claims the block or all reads failed.
     */
    @Nullable
    public static ModStorageProvider.ModStorageResult tryRead(ServerLevel level, BlockEntity be) {
        for (ModStorageProvider provider : PROVIDERS) {
            try {
                if (!provider.canHandle(be)) continue;
                ModStorageProvider.ModStorageResult result = provider.read(level, be);
                if (result != null) return result;
            } catch (Exception e) {
                // A broken provider should never kill the whole scan - just move on
                DoolesTools.LOGGER.debug("ModProviderRegistry: provider {} threw during read: {}",
                        provider.getClass().getSimpleName(), e.toString());
            }
        }
        return null;
    }

    /** Snapshot for tests / debug; not used at runtime. */
    public static List<ModStorageProvider> getProviders() {
        return Collections.unmodifiableList(PROVIDERS);
    }
}
