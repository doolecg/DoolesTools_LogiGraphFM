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
}
