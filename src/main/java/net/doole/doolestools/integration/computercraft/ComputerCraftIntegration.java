package net.doole.doolestools.integration.computercraft;

import net.neoforged.bus.api.IEventBus;

/**
 * Reserved entry point for optional computer-peripheral support.
 *
 * <p><b>MVP status:</b> stub only. The optional API is NOT a required dependency and none of its classes
 * are referenced here, so this loads safely even when that mod is absent (it is only invoked after a
 * positive {@code ModList} check in {@link net.doole.doolestools.integration.IntegrationHooks}).</p>
 *
 * <p>Soft peripheral surface intended once a compileOnly optional dependency is present:</p>
 * <pre>
 *   getStatus()           -- network online/powered/device counts
 *   getDevices()          -- network-visible device list
 *   getPower()            -- supply, demand, deficit
 *   getWarnings()         -- active warnings
 *   getStorageSummary()   -- aggregated storage fill data
 * </pre>
 */
public final class ComputerCraftIntegration {
    private ComputerCraftIntegration() {}

    public static void register(IEventBus modBus) {
        // Intentionally reflection-free for now: the mod must build and load without optional APIs on the
        // classpath. Add a compileOnly optional dependency before registering actual peripheral providers.
    }
}
