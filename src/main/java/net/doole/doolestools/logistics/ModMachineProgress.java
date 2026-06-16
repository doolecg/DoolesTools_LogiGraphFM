package net.doole.doolestools.logistics;

import net.doole.doolestools.DoolesTools;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Best-effort, read-only progress/active probe for modded machines that don't expose vanilla recipe
 * data.
 *
 * <p>It reads the machine's blockstate "active"-style flag for an instantaneous running signal (so the
 * node doesn't flicker between Standby/Working the way scan-to-scan deltas do) and reflects common
 * progress getters for a real recipe bar. Everything is guarded and cached per class; on any failure
 * the machine simply falls back to delta-based activity. Never mutates, never throws, never inserts.</p>
 */
public final class ModMachineProgress {
    private ModMachineProgress() {}

    /** A probe result. {@code percent < 0} means no numeric recipe progress was found. */
    public record Reading(boolean active, int percent, long remainingTicks) {}

    /** Boolean blockstate properties that mean "this machine is currently doing work". */
    private static final Set<String> ACTIVE_PROPS = Set.of(
            "active", "lit", "running", "working", "burning", "busy",
            "processing", "cooking", "smelting", "operating");

    /** {currentProgress, maxProgress} no-arg getter name pairs, most specific first. */
    private static final String[][] PROGRESS_PAIRS = {
            {"getOperatingTicks", "getTicksRequired"},
            {"getProgress", "getMaxProgress"},
            {"getCookProgress", "getCookProgressMax"},
            {"getBurnProgress", "getBurnProgressMax"},
            {"getCraftingProgress", "getMaxCraftingProgress"},
            {"getRecipeProgress", "getRecipeDuration"},
    };

    /** {currentProgress, maxProgress} field-name pairs, used when no getter pair is found. */
    private static final String[][] PROGRESS_FIELDS = {
            {"operatingTicks", "ticksRequired"},
            {"progress", "maxProgress"},
            {"cookTime", "cookTimeTotal"},
            {"burnTime", "totalBurnTime"},
    };

    /** Single no-arg getters returning a 0..1 (or 0..100) progress fraction. */
    private static final String[] PROGRESS_FRACTION_METHODS = {
            "getScaledProgress", "getProgressFraction", "getCraftingProgression", "getPercentDone",
    };

    private static final String[] ACTIVE_METHODS = {"getActive", "isActive", "isRunning", "isWorking"};

    /** Logs unrecognised machine members once per class. Default on; disable with -Ddoolestools.probeDebug=false. */
    public static boolean DEBUG = Boolean.parseBoolean(System.getProperty("doolestools.probeDebug", "true"));

    private static final Method[] NONE = new Method[0];
    private static final Field[] NO_FIELDS = new Field[0];
    /** Sentinel cached when a class has no usable active-getter, so we never re-scan it. */
    private static final Method ABSENT;
    static {
        try {
            ABSENT = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    private static final Map<Class<?>, Method[]> PROGRESS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> FRACTION_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> ACTIVE_CACHE = new ConcurrentHashMap<>();
    private static final Set<Class<?>> LOGGED = ConcurrentHashMap.newKeySet();

    /** Returns a reading, or {@code null} if the block exposes no recognisable progress/active signal. */
    public static Reading read(BlockState state, BlockEntity be) {
        if (be == null) return null;
        Boolean stateActive = blockStateActive(state);
        Boolean methodActive = reflectActive(be);

        int[] ticks = reflectProgress(be);          // {current, max} when available
        int percent = -1;
        long remaining = -1L;
        if (ticks != null && ticks[1] > 0) {
            percent = clampPercent(ticks[0], ticks[1]);
            remaining = Math.max(0L, ticks[1] - ticks[0]);
        } else {
            percent = reflectProgressFraction(be);  // -1 when none
        }

        boolean anyActive = stateActive != null || methodActive != null;
        if (!anyActive && percent < 0) {
            logCandidates(state, be);
            return null;
        }

        boolean active;
        if (stateActive != null) active = stateActive;
        else if (methodActive != null) active = methodActive;
        else active = percent > 0 && percent < 100;

        // Found an active flag but no numeric progress: still useful, but log so we can map this mod.
        if (percent < 0) logCandidates(state, be);

        return new Reading(active, percent, remaining);
    }

    private static Boolean blockStateActive(BlockState state) {
        try {
            for (Property<?> p : state.getProperties()) {
                if (p instanceof BooleanProperty bp && ACTIVE_PROPS.contains(p.getName())) {
                    return state.getValue(bp);
                }
            }
        } catch (RuntimeException ignored) {
            // A weird property implementation must never abort the scan.
        }
        return null;
    }

    private static int[] reflectProgress(BlockEntity be) {
        int[] viaMethods = reflectProgressMethods(be);
        return viaMethods != null ? viaMethods : reflectProgressFields(be);
    }

    private static int[] reflectProgressMethods(BlockEntity be) {
        Method[] pair = PROGRESS_CACHE.computeIfAbsent(be.getClass(), ModMachineProgress::findProgressMethods);
        if (pair == NONE) return null;
        try {
            Object cur = pair[0].invoke(be);
            Object max = pair[1].invoke(be);
            if (!(cur instanceof Number cn) || !(max instanceof Number mn)) return null;
            int maxI = mn.intValue();
            if (maxI <= 0) return null;
            return new int[] { Math.max(0, cn.intValue()), maxI };
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static int[] reflectProgressFields(BlockEntity be) {
        java.lang.reflect.Field[] pair = FIELD_CACHE.computeIfAbsent(be.getClass(), ModMachineProgress::findProgressFields);
        if (pair == NO_FIELDS) return null;
        try {
            if (!(pair[0].get(be) instanceof Number cn) || !(pair[1].get(be) instanceof Number mn)) return null;
            int maxI = mn.intValue();
            if (maxI <= 0) return null;
            return new int[] { Math.max(0, cn.intValue()), maxI };
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Reads a single fractional-progress getter (0..1 or 0..100), returning a 0-100 percent or -1. */
    private static int reflectProgressFraction(BlockEntity be) {
        Method m = FRACTION_CACHE.computeIfAbsent(be.getClass(), ModMachineProgress::findFractionMethod);
        if (m == ABSENT) return -1;
        try {
            if (!(m.invoke(be) instanceof Number n)) return -1;
            double v = n.doubleValue();
            if (Double.isNaN(v) || v < 0) return -1;
            double pct = v <= 1.0 ? v * 100.0 : v;        // accept either 0..1 or already-0..100
            return (int) Math.max(0L, Math.min(100L, Math.round(pct)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1;
        }
    }

    private static Method findFractionMethod(Class<?> cls) {
        for (String name : PROGRESS_FRACTION_METHODS) {
            try {
                Method m = cls.getMethod(name);
                Class<?> r = m.getReturnType();
                if (r == double.class || r == float.class || Number.class.isAssignableFrom(r)) {
                    trySetAccessible(m);
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
                // try next candidate
            }
        }
        return ABSENT;
    }

    private static Method[] findProgressMethods(Class<?> cls) {
        for (String[] names : PROGRESS_PAIRS) {
            Method cur = noArgNumber(cls, names[0]);
            Method max = noArgNumber(cls, names[1]);
            if (cur != null && max != null) return new Method[] { cur, max };
        }
        return NONE;
    }

    private static java.lang.reflect.Field[] findProgressFields(Class<?> cls) {
        for (String[] names : PROGRESS_FIELDS) {
            java.lang.reflect.Field cur = numberField(cls, names[0]);
            java.lang.reflect.Field max = numberField(cls, names[1]);
            if (cur != null && max != null) return new java.lang.reflect.Field[] { cur, max };
        }
        return NO_FIELDS;
    }

    private static java.lang.reflect.Field numberField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                Class<?> t = f.getType();
                if (t == int.class || t == long.class || t == short.class || t == double.class
                        || t == float.class || Number.class.isAssignableFrom(t)) {
                    trySetAccessible(f);
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
                // declared on a superclass, keep walking up
            }
        }
        return null;
    }

    private static Boolean reflectActive(BlockEntity be) {
        Method m = ACTIVE_CACHE.computeIfAbsent(be.getClass(), ModMachineProgress::findActiveMethod);
        if (m == ABSENT) return null;
        try {
            return m.invoke(be) instanceof Boolean b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Method findActiveMethod(Class<?> cls) {
        for (String name : ACTIVE_METHODS) {
            try {
                Method m = cls.getMethod(name);
                if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                    trySetAccessible(m);
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
                // try next candidate name
            }
        }
        return ABSENT;
    }

    private static Method noArgNumber(Class<?> cls, String name) {
        try {
            Method m = cls.getMethod(name);
            Class<?> r = m.getReturnType();
            if (r == int.class || r == long.class || r == short.class || r == double.class
                    || r == float.class || Number.class.isAssignableFrom(r)) {
                trySetAccessible(m);
                return m;
            }
        } catch (NoSuchMethodException ignored) {
            // absent on this class
        }
        return null;
    }

    /**
     * One-shot diagnostic: logs a modded machine's blockstate booleans plus its no-arg numeric/boolean
     * getters and numeric fields, so unrecognised progress APIs can be mapped without a debugger.
     * Only fires when {@link #DEBUG} is on, and at most once per block-entity class.
     */
    private static void logCandidates(BlockState state, BlockEntity be) {
        if (!DEBUG || !LOGGED.add(be.getClass())) return;
        List<String> bools = new ArrayList<>();
        for (Property<?> p : state.getProperties()) {
            if (p instanceof BooleanProperty) bools.add(p.getName());
        }
        List<String> getters = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        for (Class<?> c = be.getClass(); c != null && c != BlockEntity.class && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> r = m.getReturnType();
                if (r == int.class || r == long.class || r == short.class || r == double.class
                        || r == float.class || r == boolean.class) {
                    getters.add(r.getSimpleName() + " " + m.getName() + "()");
                }
            }
            for (Field f : c.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (t == int.class || t == long.class || t == short.class || t == double.class || t == float.class) {
                    fields.add(t.getSimpleName() + " " + f.getName());
                }
            }
        }
        DoolesTools.LOGGER.info("[probe] {} | activeProps={} | getters={} | fields={}",
                be.getClass().getName(), bools, getters, fields);
    }

    private static void trySetAccessible(java.lang.reflect.AccessibleObject o) {
        try {
            o.setAccessible(true);
        } catch (RuntimeException ignored) {
            // Module encapsulation can refuse; a public member is still invocable without it.
        }
    }

    private static int clampPercent(int cur, int max) {
        if (max <= 0) return 0;
        long pct = Math.round(cur * 100.0 / max);
        return (int) Math.max(0L, Math.min(100L, pct));
    }
}
