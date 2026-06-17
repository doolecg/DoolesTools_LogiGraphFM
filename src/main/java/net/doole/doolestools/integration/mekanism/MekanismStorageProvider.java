package net.doole.doolestools.integration.mekanism;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.integration.IntegrationHooks;
import net.doole.doolestools.integration.ModStorageProvider;
import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.FluidEntry;
import net.doole.doolestools.logistics.data.FluidSummary;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads Mekanism chemical tanks (gas, infuse, pigment, slurry) via reflection.
 * Mekanism already exposes fluids and energy through NeoForge capabilities so the base scanner
 * handles those fine. The extra value here is the chemical API which has no NeoForge equivalent.
 *
 * All reflection calls are wrapped - if Mekanism changes its internals we just return null
 * and the scanner falls back to whatever capability data it already has.
 */
public final class MekanismStorageProvider implements ModStorageProvider {

    private final boolean mekanismLoaded = IntegrationHooks.isLoaded("mekanism");

    // Resolved once on first successful read. volatile so reads from other threads see it.
    private volatile boolean setupDone = false;
    private volatile Class<?> chemHandlerClass = null;
    private volatile Method getTanksMethod = null;  // IChemicalHandler.getChemicalTanks(Direction)

    @Override
    public boolean canHandle(BlockEntity be) {
        if (!mekanismLoaded) return false;
        // All Mekanism block entities are under the mekanism package
        return be.getClass().getName().startsWith("mekanism.");
    }

    @Override
    public @Nullable ModStorageResult read(ServerLevel level, BlockEntity be) {
        ensureSetup();
        if (chemHandlerClass == null || getTanksMethod == null) return null;
        if (!chemHandlerClass.isInstance(be)) return null;

        try {
            // null side = expose all tanks without a directional filter
            @SuppressWarnings("unchecked")
            List<?> tanks = (List<?>) getTanksMethod.invoke(be, (Object) null);
            if (tanks == null || tanks.isEmpty()) return null;

            List<FluidEntry> entries = new ArrayList<>();
            for (Object tank : tanks) {
                FluidEntry entry = readTank(tank);
                if (entry != null) entries.add(entry);
            }

            if (entries.isEmpty()) return null;

            // chemicals are surfaced as fluid-like entries; the GUI renders them fine
            // null inventory/energy tells the scanner: dont overwrite what you already found
            return new ModStorageResult(null, new FluidSummary(entries), null, ScannedType.MACHINE);

        } catch (Exception e) {
            DoolesTools.LOGGER.debug("MekanismProvider: read failed for {}: {}",
                    be.getClass().getSimpleName(), e.toString());
            return null;
        }
    }

    /**
     * Pull amount/capacity/name from a chemical tank object using best-effort reflection.
     * The concrete tank class is an internal detail so we walk up the hierarchy for each method.
     */
    @Nullable
    private static FluidEntry readTank(Object tank) {
        try {
            // getStack() → ChemicalStack-like object
            Method getStack = findMethod(tank.getClass(), "getStack");
            if (getStack == null) return null;
            Object stack = getStack.invoke(tank);
            if (stack == null) return null;

            // getAmount() or amount field on the stack
            long amount = 0L;
            Method getAmount = findMethod(stack.getClass(), "getAmount");
            if (getAmount != null) {
                amount = ((Number) getAmount.invoke(stack)).longValue();
            }
            if (amount <= 0) return null;

            // capacity from the tank itself
            long capacity = 0L;
            for (String capName : new String[]{"getCapacity", "getTankCapacity"}) {
                Method capMethod = findMethod(tank.getClass(), capName);
                if (capMethod != null) {
                    try { capacity = ((Number) capMethod.invoke(tank)).longValue(); break; }
                    catch (Exception ignored) {}
                }
            }

            // human-readable name from the chemical type
            String chemName = "Chemical";
            String chemId = "";
            try {
                Method getType = findMethod(stack.getClass(), "getType");
                if (getType != null) {
                    Object chemType = getType.invoke(stack);
                    if (chemType != null) {
                        // try several name-ish methods in order of preference
                        for (String nm : new String[]{"getTranslatedName", "getName", "toString"}) {
                            Method nm2 = findMethod(chemType.getClass(), nm);
                            if (nm2 != null) {
                                Object r = nm2.invoke(chemType);
                                if (r != null && !r.toString().isBlank()) { chemName = r.toString(); break; }
                            }
                        }
                        // registry id - might not exist on older versions
                        for (String rn : new String[]{"getRegistryName", "getId"}) {
                            Method rnM = findMethod(chemType.getClass(), rn);
                            if (rnM != null) {
                                try { Object r = rnM.invoke(chemType); if (r != null) { chemId = r.toString(); break; } }
                                catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            return new FluidEntry(chemName, chemId, amount, capacity);

        } catch (Exception e) {
            // one broken tank shouldn't stop the others
            return null;
        }
    }

    /** Walk up the class hierarchy looking for a no-arg public method with the given name. */
    @Nullable
    private static Method findMethod(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    /** Lazy one-time setup; safe to call from the server thread during a scan. */
    private void ensureSetup() {
        if (setupDone) return;
        synchronized (this) {
            if (setupDone) return;
            try {
                // IChemicalHandler is the top-level interface; the actual tanks list comes from it
                Class<?> cls = Class.forName("mekanism.api.chemical.IChemicalHandler");
                chemHandlerClass = cls;
                // getChemicalTanks takes a nullable Direction
                getTanksMethod = cls.getMethod("getChemicalTanks", Direction.class);
            } catch (Exception e) {
                DoolesTools.LOGGER.debug("MekanismProvider: IChemicalHandler not found, chemicals will be skipped: {}", e.toString());
                chemHandlerClass = null;
                getTanksMethod = null;
            }
            setupDone = true;
        }
    }
}
