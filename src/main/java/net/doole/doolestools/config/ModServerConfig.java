package net.doole.doolestools.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative settings for LogiGraph systems. */
public final class ModServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_LFM_TRANSPORT;
    public static final ModConfigSpec.IntValue LFM_TICK_INTERVAL;
    public static final ModConfigSpec.IntValue MAX_LFM_ROUTES_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_ITEMS_MOVED_PER_ROUTE;
    public static final ModConfigSpec.IntValue MAX_FLUID_MOVED_PER_ROUTE;
    public static final ModConfigSpec.IntValue MAX_ENERGY_MOVED_PER_ROUTE;
    public static final ModConfigSpec.BooleanValue ENABLE_ITEM_ROUTES;
    public static final ModConfigSpec.BooleanValue ENABLE_FLUID_ROUTES;
    public static final ModConfigSpec.BooleanValue ENABLE_ENERGY_ROUTES;
    public static final ModConfigSpec.IntValue COMPUTER_POWER_COST;
    public static final ModConfigSpec.IntValue WIRELESS_ROUTER_POWER_COST;
    public static final ModConfigSpec.IntValue MODEM_POWER_COST;
    public static final ModConfigSpec.IntValue WIRE_SEGMENT_POWER_COST;
    public static final ModConfigSpec.IntValue VISIBLE_DEVICE_POWER_COST;
    public static final ModConfigSpec.IntValue ITEM_ROUTE_POWER_COST;
    public static final ModConfigSpec.IntValue FLUID_ROUTE_POWER_COST;
    public static final ModConfigSpec.IntValue ENERGY_ROUTE_POWER_COST;
    public static final ModConfigSpec.IntValue WIRELESS_BASE_RANGE;
    public static final ModConfigSpec.IntValue WIRELESS_RANGE_UPGRADE_BLOCKS;
    public static final ModConfigSpec.IntValue WIRELESS_MAX_RANGE;
    public static final ModConfigSpec.IntValue WIRELESS_ROUTE_POWER_SURCHARGE;
    public static final ModConfigSpec.IntValue NETWORK_RELAY_POWER_COST;
    public static final ModConfigSpec.IntValue MAX_RELAY_TRAVERSAL;
    public static final ModConfigSpec.IntValue GENERATOR_FE_PER_BURN_TICK;
    public static final ModConfigSpec.IntValue BATTERY_POWER_COST;
    public static final ModConfigSpec.IntValue BATTERY_MAX_IO;
    public static final ModConfigSpec.IntValue SCAN_RADIUS;
    public static final ModConfigSpec.IntValue MAX_WIRE_TRAVERSAL_STEPS;
    public static final ModConfigSpec.IntValue AUTO_SCAN_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue REDSTONE_ALERT_ON_ERROR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("lfm");
        ENABLE_LFM_TRANSPORT = builder.define("enableLfmTransport", true);
        LFM_TICK_INTERVAL = builder.defineInRange("lfmTickInterval", 20, 1, Integer.MAX_VALUE);
        MAX_LFM_ROUTES_PER_TICK = builder.defineInRange("maxLfmRoutesPerTick", 16, 1, Integer.MAX_VALUE);
        MAX_ITEMS_MOVED_PER_ROUTE = builder.defineInRange("maxItemsMovedPerRoute", 4, 1, 64);
        MAX_FLUID_MOVED_PER_ROUTE = builder.defineInRange("maxFluidMovedPerRoute", 1000, 1, Integer.MAX_VALUE);
        MAX_ENERGY_MOVED_PER_ROUTE = builder.defineInRange("maxEnergyMovedPerRoute", 1024, 1, Integer.MAX_VALUE);
        ENABLE_ITEM_ROUTES = builder.define("enableItemRoutes", true);
        ENABLE_FLUID_ROUTES = builder.define("enableFluidRoutes", true);
        ENABLE_ENERGY_ROUTES = builder.define("enableEnergyRoutes", true);
        builder.pop();

        builder.push("networkPower");
        COMPUTER_POWER_COST = builder.defineInRange("computerPowerCostCentiFe", 400, 0, Integer.MAX_VALUE);
        WIRELESS_ROUTER_POWER_COST = builder.defineInRange("wirelessRouterPowerCostCentiFe", 200, 0, Integer.MAX_VALUE);
        MODEM_POWER_COST = builder.defineInRange("modemPowerCostCentiFe", 100, 0, Integer.MAX_VALUE);
        WIRE_SEGMENT_POWER_COST = builder.defineInRange("wireSegmentPowerCostCentiFe", 5, 0, Integer.MAX_VALUE);
        VISIBLE_DEVICE_POWER_COST = builder.defineInRange("visibleDevicePowerCostCentiFe", 50, 0, Integer.MAX_VALUE);
        ITEM_ROUTE_POWER_COST = builder.defineInRange("itemRoutePowerCostCentiFe", 100, 0, Integer.MAX_VALUE);
        FLUID_ROUTE_POWER_COST = builder.defineInRange("fluidRoutePowerCostCentiFe", 200, 0, Integer.MAX_VALUE);
        ENERGY_ROUTE_POWER_COST = builder.defineInRange("energyRoutePowerCostCentiFe", 100, 0, Integer.MAX_VALUE);
        WIRELESS_BASE_RANGE = builder.defineInRange("wirelessBaseRange", 32, 1, 128);
        WIRELESS_RANGE_UPGRADE_BLOCKS = builder.defineInRange("wirelessRangeUpgradeBlocks", 24, 0, 128);
        WIRELESS_MAX_RANGE = builder.defineInRange("wirelessMaxRange", 128, 1, 128);
        WIRELESS_ROUTE_POWER_SURCHARGE = builder.defineInRange("wirelessRoutePowerSurchargeCentiFe", 200, 0, Integer.MAX_VALUE);
        NETWORK_RELAY_POWER_COST = builder.defineInRange("networkRelayPowerCostCentiFe", 300, 0, Integer.MAX_VALUE);
        MAX_RELAY_TRAVERSAL = builder.defineInRange("maxRelayTraversal", 128, 1, 1024);
        GENERATOR_FE_PER_BURN_TICK = builder.defineInRange("generatorFePerBurnTick", 40, 1, Integer.MAX_VALUE);
        BATTERY_POWER_COST = builder.defineInRange("batteryPowerCostCentiFe", 25, 0, Integer.MAX_VALUE);
        BATTERY_MAX_IO = builder.defineInRange("batteryMaxIoPerTick", 20000, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("scan");
        SCAN_RADIUS = builder.defineInRange("scanRadius", 16, 4, 64);
        // raise this if you have a massive wired network and the traversal cap keeps firing
        MAX_WIRE_TRAVERSAL_STEPS = builder.defineInRange("maxWireTraversalSteps", 256, 64, 4096);
        AUTO_SCAN_INTERVAL_TICKS = builder.defineInRange("autoScanIntervalTicks", 0, 0, Integer.MAX_VALUE);
        REDSTONE_ALERT_ON_ERROR = builder.define("redstoneAlertOnError", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ModServerConfig() {}
}
