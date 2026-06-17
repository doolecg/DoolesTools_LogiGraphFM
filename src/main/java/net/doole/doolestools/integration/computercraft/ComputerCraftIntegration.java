package net.doole.doolestools.integration.computercraft;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.registry.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wires up the CC:Tweaked peripheral integration for LogisticsComputerBlockEntity.
 *
 * No CC classes are imported here — everything goes through reflection. The class is only
 * invoked when IntegrationHooks confirms CC is actually loaded, so Class.forName failures
 * would be unexpected, but we catch them anyway and carry on.
 *
 * How it works:
 *   1. Grab CC's IPeripheral BlockCapability instance via reflection
 *      (dan200.computercraft.api.capability.BlockEntityCapabilityIds doesnt exist in all versions,
 *       so we try the PeripheralCapability class which CC usually exposes for this)
 *   2. Listen on RegisterCapabilitiesEvent (a NeoForge type we CAN import)
 *   3. Call event.registerBlockEntity reflectively so the generic types dont bite us
 *      when CC isnt on the compile classpath
 */
public final class ComputerCraftIntegration {
    private ComputerCraftIntegration() {}

    public static void register(IEventBus modBus) {
        // grab CC's peripheral capability object now, while CC is confirmed loaded
        // we store it as Object so it doesnt cause linkage errors at class-load time
        Object peripheralCapability = findPeripheralCapability();
        if (peripheralCapability == null) {
            DoolesTools.LOGGER.warn("[doolestools] CC loaded but couldnt find IPeripheral capability — peripheral wont register");
            return;
        }

        DoolesTools.LOGGER.info("[doolestools] CC:Tweaked detected, registering logistics_computer peripheral");

        // register on the mod bus so it runs during RegisterCapabilitiesEvent
        modBus.addListener(RegisterCapabilitiesEvent.class, event ->
                registerPeripheralCapability(event, peripheralCapability));
    }

    /**
     * Try to find CC's BlockCapability<IPeripheral, Direction> static field.
     * CC:Tweaked exposes this in a few places depending on version, we try them all.
     */
    @SuppressWarnings("unchecked")
    private static Object findPeripheralCapability() {
        // CC 1.21+ usually puts it here
        String[] candidateClasses = {
            "dan200.computercraft.api.peripheral.PeripheralCapability",   // older CC api style
            "dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider", // sometimes here
            "dan200.computercraft.api.ComputerCraftAPI",                  // sometimes a static on this
        };

        // first, try the class that most versions of CC expose
        // the field is typically CAPABILITY or PERIPHERAL
        String[] candidateFields = { "CAPABILITY", "PERIPHERAL", "BLOCK" };

        for (String className : candidateClasses) {
            for (String fieldName : candidateFields) {
                Object found = tryGetStaticField(className, fieldName);
                if (found != null && isBlockCapability(found)) {
                    DoolesTools.LOGGER.debug("[doolestools] Found IPeripheral capability at {}.{}", className, fieldName);
                    return found;
                }
            }
        }

        // fallback: scan all static fields on PeripheralCapability for anything that looks like a BlockCapability
        Object found = scanForBlockCapability("dan200.computercraft.api.peripheral.PeripheralCapability");
        if (found != null) return found;

        // last ditch: try the neoforge capabilities helper CC might register
        found = scanForBlockCapability("dan200.computercraft.impl.PeripheralCapabilities");
        return found;
    }

    private static Object tryGetStaticField(String className, String fieldName) {
        try {
            Class<?> cls = Class.forName(className);
            Field f = cls.getField(fieldName);
            return f.get(null);
        } catch (Throwable t) {
            return null; // normal if that class/field doesnt exist in this CC version
        }
    }

    private static boolean isBlockCapability(Object obj) {
        if (obj == null) return false;
        // NeoForge BlockCapability class — we can check by class name to avoid importing it
        return obj.getClass().getName().equals("net.neoforged.neoforge.capabilities.BlockCapability")
               || obj.getClass().getSuperclass() != null &&
                  obj.getClass().getSuperclass().getName().equals("net.neoforged.neoforge.capabilities.BlockCapability");
    }

    /** Scan all static fields of a class looking for a BlockCapability instance */
    private static Object scanForBlockCapability(String className) {
        try {
            Class<?> cls = Class.forName(className);
            for (Field f : cls.getFields()) {
                try {
                    Object val = f.get(null);
                    if (isBlockCapability(val)) return val;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Actually register the peripheral factory on the event.
     * We call event.registerBlockEntity reflectively to dodge the generic type constraint
     * (BlockCapability<IPeripheral, Direction>) that would require importing CC classes.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerPeripheralCapability(RegisterCapabilitiesEvent event, Object peripheralCap) {
        try {
            // find the registerBlockEntity method — it has signature:
            //   <T, C> void registerBlockEntity(BlockCapability<T,C>, BlockEntityType<? extends E>, BiFunction<E,C,T>)
            // we pick the right overload by looking for the one that takes BlockCapability + BlockEntityType + something

            Method registerMethod = null;
            for (Method m : RegisterCapabilitiesEvent.class.getMethods()) {
                if (m.getName().equals("registerBlockEntity") && m.getParameterCount() == 3) {
                    registerMethod = m;
                    break;
                }
            }

            if (registerMethod == null) {
                DoolesTools.LOGGER.error("[doolestools] Couldnt find registerBlockEntity(cap, type, factory) on RegisterCapabilitiesEvent");
                return;
            }

            // the factory: (LogisticsComputerBlockEntity be, Direction side) -> proxy
            // Direction comes in as Object because of reflection, thats fine since we dont use it
            java.util.function.BiFunction<LogisticsComputerBlockEntity, Object, Object> factory =
                    (be, side) -> LogisticsPeripheral.buildProxy(be);

            registerMethod.invoke(event,
                    peripheralCap,
                    ModBlockEntities.LOGISTICS_COMPUTER.get(),
                    factory);

            DoolesTools.LOGGER.info("[doolestools] logistics_computer peripheral registered with CC");

        } catch (Throwable t) {
            DoolesTools.LOGGER.error("[doolestools] Failed to register CC peripheral capability: {}", t.toString());
        }
    }
}
