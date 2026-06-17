package net.doole.doolestools.integration;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.integration.ae2.AE2StorageProvider;
import net.doole.doolestools.integration.computercraft.ComputerCraftIntegration;
import net.doole.doolestools.integration.create.CreateStorageProvider;
import net.doole.doolestools.integration.mekanism.MekanismStorageProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Soft-integration discovery. Optional integrations are detected by id and wired only if present, so
 * the mod never hard-crashes when an integration target is absent.
 */
public final class IntegrationHooks {
    private IntegrationHooks() {}

    public static void init(IEventBus modBus) {
        if (isLoaded("computercraft") || isLoaded("cc_tweaked")) {
            try {
                ComputerCraftIntegration.register(modBus);
            } catch (Throwable t) {
                // Defensive: a broken/old integration must never take the whole mod down.
                DoolesTools.LOGGER.warn("Optional integration init failed, continuing without it: {}", t.toString());
            }
        }
        // Register mod-specific storage providers. These run after the standard capability read and
        // fill in data that the NeoForge capability API doesn't cover (AE2 cells, Mekanism chemicals,
        // Create basins). Each provider guards itself with isLoaded() so these are all safe no-ops
        // when the target mod isn't installed.
        try { ModProviderRegistry.register(new AE2StorageProvider()); }
        catch (Throwable t) { DoolesTools.LOGGER.warn("AE2 provider registration failed: {}", t.toString()); }
        try { ModProviderRegistry.register(new MekanismStorageProvider()); }
        catch (Throwable t) { DoolesTools.LOGGER.warn("Mekanism provider registration failed: {}", t.toString()); }
        try { ModProviderRegistry.register(new CreateStorageProvider()); }
        catch (Throwable t) { DoolesTools.LOGGER.warn("Create provider registration failed: {}", t.toString()); }
    }

    public static boolean isLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }
}
