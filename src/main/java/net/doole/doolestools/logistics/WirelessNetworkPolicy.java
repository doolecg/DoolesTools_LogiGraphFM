package net.doole.doolestools.logistics;

public final class WirelessNetworkPolicy {
    private WirelessNetworkPolicy() {}

    public static int effectiveRange(int baseRange, int rangeUpgradeBlocks, int rangeUpgrades, int maxRange) {
        int cappedBase = Math.max(1, baseRange);
        int cappedMax = Math.max(1, maxRange);
        int upgrades = Math.max(0, rangeUpgrades);
        long range = (long) cappedBase + (long) Math.max(0, rangeUpgradeBlocks) * upgrades;
        return (int) Math.max(1, Math.min(cappedMax, range));
    }

    public static boolean inRange(int baseRange, int rangeUpgradeBlocks, int rangeUpgrades, int maxRange, long distanceSqr) {
        int range = effectiveRange(baseRange, rangeUpgradeBlocks, rangeUpgrades, maxRange);
        return distanceSqr <= (long) range * range;
    }

    /** Speed upgrades double the per-tick item limit (capped at 1 stack = 64). Stack upgrades add 64 beyond that. */
    public static int speedItemLimit(int base, int speedUpgrades, int stackUpgrades) {
        int cappedSpeed = Math.max(0, Math.min(4, speedUpgrades));
        int speedMax = Math.min(64, Math.max(1, base) << cappedSpeed);
        return speedMax + Math.max(0, stackUpgrades) * 64;
    }

    public static int routeBudgetBonus(int speedUpgrades, boolean wireless) {
        int perUpgrade = wireless ? 16 : 8;
        return Math.max(0, speedUpgrades) * perUpgrade;
    }

    public static int wirelessRouteSurcharge(int baseSurcharge, int efficiencyUpgrades) {
        int surcharge = Math.max(0, baseSurcharge);
        int reductionSteps = Math.max(0, Math.min(4, efficiencyUpgrades));
        return surcharge * (4 - reductionSteps) / 4;
    }

    /**
     * Throughput multiplier applied to wireless routes while it is raining/thundering. Thunder takes
     * precedence over rain. Penalties are percentages in [0,100]; a 20% rain penalty returns 0.8.
     */
    public static float weatherMultiplier(boolean raining, boolean thundering, int rainPenaltyPct, int thunderPenaltyPct) {
        if (thundering) return clamp01((100 - clampPct(thunderPenaltyPct)) / 100f);
        if (raining) return clamp01((100 - clampPct(rainPenaltyPct)) / 100f);
        return 1f;
    }

    /**
     * Wireless signal strength in [floor,1]: 1.0 at distance 0, falling linearly to
     * {@code minSignalPct/100} at the effective range edge and clamped to that floor beyond it.
     */
    public static float signalStrength(long distanceSqr, int effectiveRange, int minSignalPct) {
        int range = Math.max(1, effectiveRange);
        float floor = clamp01(clampPct(minSignalPct) / 100f);
        double dist = Math.sqrt(Math.max(0L, distanceSqr));
        double t = Math.min(1.0, dist / range);
        return clamp01((float) (1.0 - (1.0 - floor) * t));
    }

    /** Combined wireless throughput multiplier (signal strength × weather), clamped to [0,1]. */
    public static float wirelessThroughputMultiplier(float signalStrength, float weatherMultiplier) {
        return clamp01(signalStrength * weatherMultiplier);
    }

    private static int clampPct(int pct) {
        return Math.max(0, Math.min(100, pct));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
