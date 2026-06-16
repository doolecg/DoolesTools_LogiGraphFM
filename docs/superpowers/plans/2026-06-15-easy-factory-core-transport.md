# Easy Factory Core Transport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore Easy Factory as an always-on Logistics Computer transport system for item, fluid, and energy graph links, controlled by server settings.

**Architecture:** Add server config limits, add a focused `EasyFactoryManager` that resolves graph links to loaded block positions and moves resources through NeoForge capabilities, then call it from the Logistics Computer tick. Keep removed soul, overclock, conduit, custom machine, custom fluid, ingot, and block content absent.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, NeoForge transfer capabilities, Gradle wrapper.

---

### Task 1: Server Settings

**Files:**
- Modify: `src/main/java/net/doole/doolestools/config/ModServerConfig.java`

- [ ] **Step 1: Add Easy Factory config fields**

Replace `ModServerConfig` with a server config containing these fields and defaults:

```java
package net.doole.doolestools.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative settings for LogiGraph systems. */
public final class ModServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_EASY_FACTORY_TRANSPORT;
    public static final ModConfigSpec.IntValue EASY_FACTORY_TICK_INTERVAL;
    public static final ModConfigSpec.IntValue MAX_EASY_FACTORY_ROUTES_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_ITEMS_MOVED_PER_ROUTE;
    public static final ModConfigSpec.IntValue MAX_FLUID_MOVED_PER_ROUTE;
    public static final ModConfigSpec.IntValue MAX_ENERGY_MOVED_PER_ROUTE;
    public static final ModConfigSpec.BooleanValue ENABLE_ITEM_ROUTES;
    public static final ModConfigSpec.BooleanValue ENABLE_FLUID_ROUTES;
    public static final ModConfigSpec.BooleanValue ENABLE_ENERGY_ROUTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("easyFactory");
        ENABLE_EASY_FACTORY_TRANSPORT = builder.define("enableEasyFactoryTransport", true);
        EASY_FACTORY_TICK_INTERVAL = builder.defineInRange("easyFactoryTickInterval", 20, 1, Integer.MAX_VALUE);
        MAX_EASY_FACTORY_ROUTES_PER_TICK = builder.defineInRange("maxEasyFactoryRoutesPerTick", 128, 1, Integer.MAX_VALUE);
        MAX_ITEMS_MOVED_PER_ROUTE = builder.defineInRange("maxItemsMovedPerRoute", 64, 1, 64);
        MAX_FLUID_MOVED_PER_ROUTE = builder.defineInRange("maxFluidMovedPerRoute", 1000, 1, Integer.MAX_VALUE);
        MAX_ENERGY_MOVED_PER_ROUTE = builder.defineInRange("maxEnergyMovedPerRoute", 1024, 1, Integer.MAX_VALUE);
        ENABLE_ITEM_ROUTES = builder.define("enableItemRoutes", true);
        ENABLE_FLUID_ROUTES = builder.define("enableFluidRoutes", true);
        ENABLE_ENERGY_ROUTES = builder.define("enableEnergyRoutes", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ModServerConfig() {}
}
```

- [ ] **Step 2: Run compile check**

Run: `./gradlew.bat compileJava`

Expected: either passes or reports only missing Easy Factory manager references in later tasks.

### Task 2: Easy Factory Manager

**Files:**
- Create: `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`

- [ ] **Step 1: Add manager skeleton and route loop**

Create `EasyFactoryManager` with `tick(LogisticsGraphData graph, ServerLevel level)` that exits when transport is disabled, route limit is reached, graph is empty, endpoint nodes are missing, positions are invalid, chunks are unloaded, or the link type is disabled.

```java
package net.doole.doolestools.logistics.easyfactory;

import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.LinkType;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class EasyFactoryManager {
    private EasyFactoryManager() {}

    public static void tick(LogisticsGraphData graph, ServerLevel level) {
        if (!ModServerConfig.ENABLE_EASY_FACTORY_TRANSPORT.get() || graph.isEmpty()) return;
        int processed = 0;
        for (GraphLinkData link : graph.activeCanvas().links()) {
            if (processed >= ModServerConfig.MAX_EASY_FACTORY_ROUTES_PER_TICK.get()) break;
            GraphNodeData sourceNode = graph.findNode(link.sourceNodeId());
            GraphNodeData targetNode = graph.findNode(link.targetNodeId());
            BlockPos sourcePos = targetPosOf(sourceNode);
            BlockPos targetPos = targetPosOf(targetNode);
            if (sourcePos == null || targetPos == null) continue;
            if (!level.hasChunkAt(sourcePos) || !level.hasChunkAt(targetPos)) continue;
            if (transfer(link.type(), level, sourcePos, targetPos)) processed++;
        }
    }

    private static BlockPos targetPosOf(GraphNodeData node) {
        if (node == null || node.scannedBlockId() == null || !node.scannedBlockId().startsWith("blk_")) return null;
        try {
            return BlockPos.of(Long.parseLong(node.scannedBlockId().substring(4)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean transfer(LinkType type, ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        return switch (type) {
            case ITEMS -> ModServerConfig.ENABLE_ITEM_ROUTES.get() && transferItems(level, sourcePos, targetPos);
            case FLUIDS -> ModServerConfig.ENABLE_FLUID_ROUTES.get() && transferFluids(level, sourcePos, targetPos);
            case ENERGY -> ModServerConfig.ENABLE_ENERGY_ROUTES.get() && transferEnergy(level, sourcePos, targetPos);
        };
    }
}
```

- [ ] **Step 2: Add capability lookup helpers**

Add helper methods in the same class:

```java
    private static ResourceHandler<ItemResource> itemHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return Capabilities.Item.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static ResourceHandler<FluidResource> fluidHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return Capabilities.Fluid.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static EnergyHandler energyHandler(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return Capabilities.Energy.BLOCK.getCapability(level, pos, state, be, side);
    }

    private static Direction sideFrom(BlockPos source, BlockPos target) {
        int dx = Integer.compare(target.getX(), source.getX());
        int dy = Integer.compare(target.getY(), source.getY());
        int dz = Integer.compare(target.getZ(), source.getZ());
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) return null;
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dy > 0) return Direction.UP;
        if (dy < 0) return Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
```

- [ ] **Step 3: Add item transfer implementation**

Add `transferItems` that tries adjacent-side handlers first and then null-side handlers, simulates extraction/insertion, then commits a bounded transfer.

```java
    private static boolean transferItems(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        Direction sourceSide = sideFrom(sourcePos, targetPos);
        Direction targetSide = sourceSide == null ? null : sourceSide.getOpposite();
        ResourceHandler<ItemResource> source = itemHandler(level, sourcePos, sourceSide);
        ResourceHandler<ItemResource> target = itemHandler(level, targetPos, targetSide);
        if (source == null) source = itemHandler(level, sourcePos, null);
        if (target == null) target = itemHandler(level, targetPos, null);
        if (source == null || target == null) return false;

        int max = ModServerConfig.MAX_ITEMS_MOVED_PER_ROUTE.get();
        for (int slot = 0; slot < source.size(); slot++) {
            ItemResource resource = source.getResource(slot);
            long available = source.getAmount(slot);
            if (resource.isEmpty() || available <= 0) continue;
            int amount = (int) Math.min(max, available);
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = source.extract(resource, amount, tx);
                if (extracted <= 0) continue;
                long inserted = target.insert(resource, extracted, tx);
                if (inserted <= 0) continue;
                if (inserted < extracted) continue;
                tx.commit();
                return true;
            }
        }
        return false;
    }
```

- [ ] **Step 4: Add fluid transfer implementation**

Add `transferFluids` using the same transaction pattern and `maxFluidMovedPerRoute`.

```java
    private static boolean transferFluids(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        Direction sourceSide = sideFrom(sourcePos, targetPos);
        Direction targetSide = sourceSide == null ? null : sourceSide.getOpposite();
        ResourceHandler<FluidResource> source = fluidHandler(level, sourcePos, sourceSide);
        ResourceHandler<FluidResource> target = fluidHandler(level, targetPos, targetSide);
        if (source == null) source = fluidHandler(level, sourcePos, null);
        if (target == null) target = fluidHandler(level, targetPos, null);
        if (source == null || target == null) return false;

        long max = ModServerConfig.MAX_FLUID_MOVED_PER_ROUTE.get();
        for (int tank = 0; tank < source.size(); tank++) {
            FluidResource resource = source.getResource(tank);
            long available = source.getAmount(tank);
            if (resource.isEmpty() || available <= 0) continue;
            long amount = Math.min(max, available);
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = source.extract(resource, amount, tx);
                if (extracted <= 0) continue;
                long inserted = target.insert(resource, extracted, tx);
                if (inserted <= 0) continue;
                if (inserted < extracted) continue;
                tx.commit();
                return true;
            }
        }
        return false;
    }
```

- [ ] **Step 5: Add energy transfer implementation**

Add `transferEnergy` using the project’s `EnergyHandler` API. If compile errors show different method names, inspect NeoForge API usage in dependencies and adjust only this method.

```java
    private static boolean transferEnergy(ServerLevel level, BlockPos sourcePos, BlockPos targetPos) {
        Direction sourceSide = sideFrom(sourcePos, targetPos);
        Direction targetSide = sourceSide == null ? null : sourceSide.getOpposite();
        EnergyHandler source = energyHandler(level, sourcePos, sourceSide);
        EnergyHandler target = energyHandler(level, targetPos, targetSide);
        if (source == null) source = energyHandler(level, sourcePos, null);
        if (target == null) target = energyHandler(level, targetPos, null);
        if (source == null || target == null) return false;

        long amount = Math.min(ModServerConfig.MAX_ENERGY_MOVED_PER_ROUTE.get(), source.getEnergyStored());
        if (amount <= 0) return false;
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = source.extract(amount, tx);
            if (extracted <= 0) return false;
            long inserted = target.insert(extracted, tx);
            if (inserted <= 0) return false;
            if (inserted < extracted) return false;
            tx.commit();
            return true;
        }
    }
```

- [ ] **Step 6: Run compile check**

Run: `./gradlew.bat compileJava`

Expected: compile either passes or identifies exact capability API method names to adjust in `EasyFactoryManager`.

### Task 3: Wire Computer Tick

**Files:**
- Modify: `src/main/java/net/doole/doolestools/blockentity/LogisticsComputerBlockEntity.java`

- [ ] **Step 1: Import manager and config**

Add imports:

```java
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.easyfactory.EasyFactoryManager;
```

- [ ] **Step 2: Call Easy Factory on configured interval**

Replace `serverTick` with:

```java
    public void serverTick(ServerLevel level) {
        if (!ModServerConfig.ENABLE_EASY_FACTORY_TRANSPORT.get() || graph == LogisticsGraphData.EMPTY) return;
        int interval = ModServerConfig.EASY_FACTORY_TICK_INTERVAL.get();
        if (level.getGameTime() % interval != 0L) return;
        EasyFactoryManager.tick(graph, level);
    }
```

- [ ] **Step 3: Run compile check**

Run: `./gradlew.bat compileJava`

Expected: compile passes or reports manager API errors from Task 2.

### Task 4: Settings UI Status

**Files:**
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`

- [ ] **Step 1: Render Easy Factory settings section**

Update `renderSettingsPage` to include a required Easy Factory status/settings section. Because server config values may not be synchronized to the client, display the default core behavior and the exact config keys the server can change.

```java
        int efY = contentY + 28 + 5 * 22;
        g.text(font, "Easy Factory: ON by default", leftX + 12, efY, DUTheme.OK, false);
        g.text(font, "Server settings: enableEasyFactoryTransport, route toggles, tick interval, item/fluid/energy limits.",
                leftX + 12, efY + 12, DUTheme.TEXT_DIM, false);
```

- [ ] **Step 2: Run compile check**

Run: `./gradlew.bat compileJava`

Expected: compile passes.

### Task 5: Final Verification

**Files:**
- Verify only.

- [ ] **Step 1: Search for removed content**

Run targeted searches for removed systems:

```powershell
rg -i "soul|overclock|conduit|fragment_refiner|crystalised|pressurised" src/main/java src/main/resources
```

Expected: no matches except allowed documentation/spec files outside `src/` are irrelevant. If any `src/` matches appear, remove only stale references that are not part of Easy Factory transport.

- [ ] **Step 2: Run correctness gate**

Run: `./gradlew.bat build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Report outcome**

Report changed areas and the build result. Do not claim completion unless `./gradlew.bat build` succeeded.
