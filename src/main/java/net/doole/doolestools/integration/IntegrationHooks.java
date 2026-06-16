package net.doole.doolestools.integration;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.integration.computercraft.ComputerCraftIntegration;
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
        // Optional compatibility stays silent by design: the scanner reads any mod's blocks through
        // vanilla Container / NeoForge capabilities, so no per-mod code is needed to see them, and we
        // never surface other mod names in LogiGraph. Mods that want their pipes/cables hidden from
        // scans opt out via the doolestools:scanner_blacklist tag or the ScannerHiddenBlock interface.
    }

    public static boolean isLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }
}
