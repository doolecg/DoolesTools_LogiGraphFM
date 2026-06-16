# Wireless Range And Speed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce wireless router range and make wireless upgrades affect transport speed, stack size, and power demand.

**Architecture:** Add a pure `WirelessNetworkPolicy` helper for deterministic upgrade math and cover it with unit tests. Wire the helper into server scan, power, and Easy Factory route-budget logic while keeping graph/network payload schemas unchanged.

**Tech Stack:** Java 25, Gradle 9.1, NeoForge 26.1.x, JUnit Jupiter for pure helper tests.

---

## Files

- Modify: `build.gradle` to add JUnit Jupiter and enable `test`.
- Modify: `src/main/java/net/doole/doolestools/config/ModServerConfig.java` for wireless range/power config.
- Create: `src/main/java/net/doole/doolestools/logistics/WirelessNetworkPolicy.java` for pure range/speed/limit/surcharge math.
- Create: `src/test/java/net/doole/doolestools/logistics/WirelessNetworkPolicyTest.java` for policy tests.
- Modify: `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java` to enforce wireless range.
- Modify: `src/main/java/net/doole/doolestools/logistics/NetworkPowerCalculator.java` to count in-range wireless routers and add wireless route demand.
- Modify: `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java` to use wireless policy math.

## Tasks

- [x] Add JUnit test configuration.
- [x] Add failing tests for wireless policy range, item limit, route budget, and surcharge math.
- [x] Run `./gradlew.bat test` and verify tests fail because `WirelessNetworkPolicy` does not exist.
- [x] Implement `WirelessNetworkPolicy` plus config values.
- [x] Run `./gradlew.bat test` and verify policy tests pass.
- [x] Wire range checks into `LogisticsScanner` for standalone and wire-hosted routers.
- [x] Wire wireless route surcharge/counting into `NetworkPowerCalculator`.
- [x] Wire policy speed/stack math into `EasyFactoryManager`.
- [x] Run `./gradlew.bat compileJava`.
- [x] Run `./gradlew.bat build`.
- [x] Review `git diff --stat` and update this plan's checkboxes.
