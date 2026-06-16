# Power System Virtual Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add AE2-style virtual network power demand, display it in Power Usage, and pause transport when demand exceeds virtual supply.

**Architecture:** Keep power read-only/diagnostic for this milestone. The server calculates one `NetworkPowerData` snapshot from the computer, visible network devices, wired components, and active graph routes, then syncs that snapshot to the client. Easy Factory transport checks the same snapshot before moving items, fluids, or energy.

**Tech Stack:** Java 25, NeoForge 26.1.x, existing Codec-derived stream codecs, existing Logistics Computer payload sync.

---

## Files

- Create: `src/main/java/net/doole/doolestools/logistics/data/NetworkPowerData.java`
- Create: `src/main/java/net/doole/doolestools/logistics/NetworkPowerCalculator.java`
- Modify: `src/main/java/net/doole/doolestools/config/ModServerConfig.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/LogisticsComputerBlockEntity.java`
- Modify: `src/main/java/net/doole/doolestools/network/payload/ComputerStatePayload.java`
- Modify: `src/main/java/net/doole/doolestools/network/ModStreamCodecs.java`
- Modify: `src/main/java/net/doole/doolestools/network/ServerPayloadHandlers.java`
- Modify: `src/main/java/net/doole/doolestools/client/ClientPayloadHandlers.java`
- Modify: `src/main/java/net/doole/doolestools/client/EditorContext.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`

## Tasks

- [x] Add centi-FE/t server config values for virtual supply and AE2-style costs.
- [x] Add `NetworkPowerData` with Codec support for server-client sync.
- [x] Add `NetworkPowerCalculator` that counts computer, endpoints, wires, visible devices, and active routes.
- [x] Sync power data through `ComputerStatePayload`.
- [x] Store synced power data in `EditorContext`.
- [x] Replace the Statistics page title/content with Power Usage diagnostics.
- [x] Gate Easy Factory transport when the network is underpowered.
- [x] Fix router/modem same-block wire placement inference for AE2-like cable parts.
- [x] Verify with `./gradlew.bat compileJava`.
- [x] Verify with `./gradlew.bat build`.
