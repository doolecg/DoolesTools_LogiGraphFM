package net.doole.doolestools.integration.create;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.integration.IntegrationHooks;
import net.doole.doolestools.integration.ModStorageProvider;
import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.FluidSummary;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Create Basins (and similar) by probing their input/output inventories via reflection.
 * Create's Vault and Depot blocks already expose everything through NeoForge item capability
 * so the base scanner handles those fine. The Basin is the tricky one - it has internal
 * SmartInventory slots that aren't always exposed fully through the standard capability.
 *
 * If Create isn't loaded this is completely inert.
 */
public final class CreateStorageProvider implements ModStorageProvider {

    private final boolean createLoaded = IntegrationHooks.isLoaded("create");

    // Resolved once on first use
    private volatile boolean setupDone = false;
    private volatile Class<?> basinBeClass = null;         // BasinBlockEntity
    private volatile Method getInputInventory = null;      // BasinBlockEntity.getInputInventory()
    private volatile Method getOutputInventory = null;     // BasinBlockEntity.getOutputInventory()

    @Override
    public boolean canHandle(BlockEntity be) {
        if (!createLoaded) return false;
        ensureSetup();
        // only claim basins - vaults and depots are handled by the standard capability path
        return basinBeClass != null && basinBeClass.isInstance(be);
    }

    @Override
    public @Nullable ModStorageResult read(ServerLevel level, BlockEntity be) {
        ensureSetup();
        if (basinBeClass == null) return null;
        if (!basinBeClass.isInstance(be)) return null;

        try {
            // merge input and output inventories into one combined picture
            Map<String, Integer> tally = new LinkedHashMap<>();
            Map<String, String> names = new LinkedHashMap<>();
            int used = 0;
            int total = 0;

            used += tallyInventory(be, getInputInventory, tally, names);
            used += tallyInventory(be, getOutputInventory, tally, names);
            total = tally.size(); // basins dont have a fixed slot count visible externally

            if (tally.isEmpty()) return null;

            // sort by count descending, capped at 500 stacks
            List<ItemEntry> top = tally.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(500)
                    .map(e -> new ItemEntry(names.get(e.getKey()), e.getKey(), e.getValue()))
                    .toList();

            InventorySummary inv = new InventorySummary(used, Math.max(used, total), top);
            // Basins are machines (they process ingredients), not plain storage
            return new ModStorageResult(inv, null, null, ScannedType.MACHINE);

        } catch (Exception e) {
            DoolesTools.LOGGER.debug("CreateProvider: basin read failed: {}", e.toString());
            return null;
        }
    }

    /**
     * Invoke an inventory-getter method on the basin and tally up its contents.
     * Returns the number of occupied slots found.
     */
    private static int tallyInventory(Object target, @Nullable Method getter,
                                      Map<String, Integer> tally, Map<String, String> names) {
        if (getter == null) return 0;
        try {
            Object inv = getter.invoke(target);
            if (inv == null) return 0;

            // The returned object should be some kind of IItemHandler or vanilla Container.
            // Try getSlots() / getStackInSlot(i) which is the IItemHandler pattern.
            Method getSlotsM = findMethod(inv.getClass(), "getSlots");
            Method getStackM = findMethodWithIntArg(inv.getClass(), "getStackInSlot");

            // Also try vanilla Container: getContainerSize() / getItem(i)
            if (getSlotsM == null) getSlotsM = findMethod(inv.getClass(), "getContainerSize");
            if (getStackM == null) getStackM = findMethodWithIntArg(inv.getClass(), "getItem");

            if (getSlotsM == null || getStackM == null) return 0;

            int size = ((Number) getSlotsM.invoke(inv)).intValue();
            int used = 0;
            for (int i = 0; i < size; i++) {
                Object stackObj = getStackM.invoke(inv, i);
                if (!(stackObj instanceof ItemStack stack) || stack.isEmpty()) continue;
                used++;
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                tally.merge(id, stack.getCount(), Integer::sum);
                names.putIfAbsent(id, stack.getHoverName().getString());
            }
            return used;

        } catch (Exception e) {
            // best effort - a broken inv read shouldnt kill the scanner
            return 0;
        }
    }

    /** Find a public no-arg method by name, walking up the class hierarchy. */
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

    /** Find a method that takes a single int argument, walking up the class hierarchy. */
    @Nullable
    private static Method findMethodWithIntArg(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1
                        && (m.getParameterTypes()[0] == int.class || m.getParameterTypes()[0] == Integer.class)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    /** Lazy one-time reflection setup. */
    private void ensureSetup() {
        if (setupDone) return;
        synchronized (this) {
            if (setupDone) return;
            try {
                // Create's Basin block entity class - might be in different sub-packages depending on version
                basinBeClass = tryLoadClass(
                        "com.simibubi.create.content.processing.basin.BasinBlockEntity",
                        "com.simibubi.create.content.contraptions.processing.basin.BasinBlockEntity"
                );
                if (basinBeClass != null) {
                    getInputInventory = findMethod(basinBeClass, "getInputInventory");
                    getOutputInventory = findMethod(basinBeClass, "getOutputInventory");
                    if (getInputInventory == null && getOutputInventory == null) {
                        // cant do anything useful - clear the class marker so canHandle returns false
                        DoolesTools.LOGGER.debug("CreateProvider: found BasinBlockEntity but couldn't find inventory getters");
                        basinBeClass = null;
                    }
                }
            } catch (Exception e) {
                DoolesTools.LOGGER.debug("CreateProvider: setup failed (ok if create isnt installed): {}", e.toString());
                basinBeClass = null;
            }
            setupDone = true;
        }
    }

    @Nullable
    private static Class<?> tryLoadClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }
}
