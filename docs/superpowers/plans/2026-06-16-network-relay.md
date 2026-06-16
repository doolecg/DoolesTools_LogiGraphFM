# Network Relay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add standalone loaded-chunk-only Network Relay blocks that extend wireless reach.

**Architecture:** Implement relays as independent block entities with identity, network assignment, and upgrade counters. Add bounded loaded-chunk relay reachability helpers to scanner and power calculation without force-loading chunks or changing graph payload schemas.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing Gradle/JUnit setup.

---

## Files

- Create: `src/main/java/net/doole/doolestools/block/NetworkRelayBlock.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/NetworkRelayBlockEntity.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlockEntities.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModCreativeTabs.java`
- Modify: `src/main/java/net/doole/doolestools/config/ModServerConfig.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/NetworkPowerCalculator.java`
- Add resources for `network_relay` under `src/main/resources/assets/doolestools/` and `src/main/resources/data/doolestools/`.

## Tasks

- [x] Add relay block/entity/item registry entries and creative-tab entry.
- [x] Add relay block and block entity with stable ID, network ID, display name, and upgrade counters.
- [x] Add lang, blockstate, model, item, loot table, and recipe resources.
- [x] Add relay power cost and max traversal config values.
- [x] Add bounded loaded-chunk relay reachability to `LogisticsScanner`.
- [x] Add reachable relay counting and relay power demand to `NetworkPowerCalculator`.
- [x] Run `./gradlew.bat test`.
- [x] Run `./gradlew.bat compileJava`.
- [x] Run `./gradlew.bat build`.
- [x] Review `git diff --stat` and update this plan's checkboxes.
