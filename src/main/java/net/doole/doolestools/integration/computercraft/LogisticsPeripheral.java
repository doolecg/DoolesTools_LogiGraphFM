package net.doole.doolestools.integration.computercraft;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.logistics.WarningGenerator;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.doole.doolestools.logistics.data.NetworkPowerData;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CC:Tweaked peripheral wrapper for the Logistics Computer, built without any CC imports.
 * Everything goes through reflection so the class loads fine even when CC isnt on the classpath.
 *
 * peripheral type: "logistics_computer"
 *
 * methods exposed to lua:
 *   getStatus, getPower, getDevices, getWarnings, getStorageSummary, scan, getNetworkId
 */
public final class LogisticsPeripheral {

    // method names in the order we advertise them to CC
    private static final String[] METHOD_NAMES = {
        "getStatus",
        "getPower",
        "getDevices",
        "getWarnings",
        "getStorageSummary",
        "scan",
        "getNetworkId"
    };

    private final LogisticsComputerBlockEntity be;

    public LogisticsPeripheral(LogisticsComputerBlockEntity be) {
        this.be = be;
    }

    // --- the actual lua-callable logic, all returns are plain java collections ---

    /** { powered, hasErrors, scannedBlocks, activeRoutes } */
    private Map<String, Object> doGetStatus() {
        NetworkPowerData power = be.getNetworkPower();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("powered", power.powered());
        // compute hasErrors ourselves since we cant assume that helper method exists
        out.put("hasErrors", computeHasErrors());
        out.put("scannedBlocks", be.getLastScan().size());
        out.put("activeRoutes", be.getActiveRouteIds().size());
        return out;
    }

    /** true if any scanned block or graph warning is severity ERROR */
    private boolean computeHasErrors() {
        for (ScannedBlockData s : be.getLastScan()) {
            for (WarningData w : s.warnings()) {
                if (w.severity() == WarningData.Severity.ERROR) return true;
            }
        }
        Map<String, ScannedBlockData> byId = new HashMap<>();
        for (ScannedBlockData s : be.getLastScan()) byId.put(s.id(), s);
        for (WarningData w : WarningGenerator.forGraph(be.getGraph(), byId)) {
            if (w.severity() == WarningData.Severity.ERROR) return true;
        }
        return false;
    }

    /** { supply, demand, satisfied, wires, endpoints } */
    private Map<String, Object> doGetPower() {
        NetworkPowerData p = be.getNetworkPower();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("supply", p.supplyCentiFe());
        out.put("demand", p.demandCentiFe());
        out.put("satisfied", p.powered());
        out.put("wires", p.wireCount());
        out.put("endpoints", p.endpointCount());
        return out;
    }

    /** list of { name, type, pos, fillPct } for each scanned block */
    private List<Map<String, Object>> doGetDevices() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ScannedBlockData b : be.getLastScan()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", b.blockName());
            entry.put("type", b.type().name().toLowerCase());
            net.minecraft.core.BlockPos p = b.pos();
            entry.put("pos", p.getX() + "," + p.getY() + "," + p.getZ());
            // fill pct from inventory if it has data, otherwise 0
            int fillPct = 0;
            if (b.inventory() != null && b.inventory().hasData()) {
                fillPct = b.inventory().fillPercent();
            }
            entry.put("fillPct", fillPct);
            out.add(entry);
        }
        return out;
    }

    /** list of { severity, message } — graph warnings + per-block scan warnings */
    private List<Map<String, Object>> doGetWarnings() {
        Map<String, ScannedBlockData> byId = new HashMap<>();
        for (ScannedBlockData s : be.getLastScan()) byId.put(s.id(), s);

        // graph-level warnings first
        List<WarningData> all = new ArrayList<>(
                WarningGenerator.forGraph(be.getGraph(), byId));

        // then per-block scan warnings (already baked into ScannedBlockData during scan)
        for (ScannedBlockData s : be.getLastScan()) {
            all.addAll(s.warnings());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (WarningData w : all) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("severity", w.severity().name().toLowerCase());
            entry.put("message", w.message());
            out.add(entry);
        }
        return out;
    }

    /**
     * Top-50 items by count aggregated across all storage blocks.
     * returns list of { item, count }
     */
    private List<Map<String, Object>> doGetStorageSummary() {
        // aggregate counts per display name across all scanned blocks that have inventory data
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ScannedBlockData b : be.getLastScan()) {
            if (b.inventory() == null || !b.inventory().hasData()) continue;
            for (ItemEntry e : b.inventory().topStacks()) {
                totals.merge(e.displayName(), (long) e.count(), Long::sum);
            }
        }

        // sort desc by count, take top 50
        List<Map<String, Object>> out = new ArrayList<>();
        totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(50)
                .forEach(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("item", entry.getKey());
                    row.put("count", entry.getValue().intValue());
                    out.add(row);
                });
        return out;
    }

    /** trigger a scan server-side, return "ok" */
    private String doScan() {
        be.performScan();
        return "ok";
    }

    /** the network id string */
    private String doGetNetworkId() {
        return be.networkId();
    }

    // --- dispatch by method index ---

    private Object dispatch(int methodIndex, Object[] args) throws Exception {
        return switch (methodIndex) {
            case 0 -> doGetStatus();
            case 1 -> doGetPower();
            case 2 -> doGetDevices();
            case 3 -> doGetWarnings();
            case 4 -> doGetStorageSummary();
            case 5 -> doScan();
            case 6 -> doGetNetworkId();
            default -> throw new IllegalArgumentException("Unknown method index: " + methodIndex);
        };
    }

    // --- build a java.lang.reflect.Proxy that implements IPeripheral at runtime ---

    /**
     * Creates a proxy object implementing dan200.computercraft.api.peripheral.IPeripheral.
     * Returns null if CC isnt on the classpath (silently ignored by the caller).
     *
     * the IPeripheral interface methods we implement:
     *   String getType()
     *   String[] getMethodNames()
     *   Object callMethod(IComputerAccess, ILuaContext, int, Object[]) throws LuaException, InterruptedException
     *   void attach(IComputerAccess)   -- no-op
     *   void detach(IComputerAccess)   -- no-op
     *   boolean equals(IPeripheral)
     */
    public static Object buildProxy(LogisticsComputerBlockEntity be) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = LogisticsPeripheral.class.getClassLoader();

            Class<?> peripheralClass = Class.forName(
                    "dan200.computercraft.api.peripheral.IPeripheral", false, cl);

            LogisticsPeripheral impl = new LogisticsPeripheral(be);

            return Proxy.newProxyInstance(cl, new Class<?>[]{ peripheralClass },
                    new PeripheralHandler(impl));

        } catch (ClassNotFoundException e) {
            // cc not loaded, thats fine, no peripheral for you
            return null;
        } catch (Throwable t) {
            DoolesTools.LOGGER.warn("[doolestools] CC peripheral proxy failed to build: {}", t.toString());
            return null;
        }
    }

    /** InvocationHandler that dispatches IPeripheral method calls to our implementation. */
    private static final class PeripheralHandler implements InvocationHandler {

        private final LogisticsPeripheral impl;

        PeripheralHandler(LogisticsPeripheral impl) {
            this.impl = impl;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            return switch (name) {
                case "getType" -> "logistics_computer";

                case "getMethodNames" -> METHOD_NAMES.clone();

                case "callMethod" -> {
                    // signature: callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
                    // args[2] = method index (int boxed), args[3] = lua arg array (may be null)
                    int methodIndex = ((Number) args[2]).intValue();
                    Object[] luaArgs = (args[3] instanceof Object[] oa) ? oa : new Object[0];
                    try {
                        Object result = impl.dispatch(methodIndex, luaArgs);
                        // callMethod returns Object[] for multi-value lua returns
                        if (result == null) {
                            yield new Object[0];
                        }
                        yield new Object[]{ result };
                    } catch (Exception ex) {
                        // wrap in LuaException if possible, otherwise just rethrow
                        throw buildLuaException(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                    }
                }

                // attach/detach — dont need to track connections
                case "attach", "detach" -> null;

                // equals check by identity, not super helpful but required by the interface
                case "equals" -> (args != null && args.length > 0) && proxy == args[0];

                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "LogisticsPeripheral[logistics_computer]";

                // any other method CC might add in future versions, just return null
                default -> null;
            };
        }

        private static Throwable buildLuaException(String msg) {
            try {
                Class<?> luaExClass = Class.forName("dan200.computercraft.api.lua.LuaException");
                return (Throwable) luaExClass.getConstructor(String.class).newInstance(msg);
            } catch (Throwable t) {
                // cant build a LuaException, CC will probably wrap this anyway
                return new RuntimeException(msg);
            }
        }
    }
}
