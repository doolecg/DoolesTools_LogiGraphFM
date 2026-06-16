# Network Numeric Identities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give routers/endpoints globally unique four-digit visible IDs, default computer networks to `NETWORK#0001`, and make endpoint network selection a dropdown-style list.

**Architecture:** Add a world-saved allocator for monotonic numeric IDs. Keep internal network IDs stable for saved endpoint assignments, while deriving default display names from allocated numbers. Update endpoint UI to open a network list instead of cycling a button.

**Tech Stack:** Java 25, NeoForge Value I/O saved data/block entity persistence, existing custom GUI widgets, Gradle wrapper.

---

### Task 1: Numeric Allocator

**Files:**
- Create: `src/main/java/net/doole/doolestools/world/NetworkIdentitySavedData.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/LogisticsComputerBlockEntity.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/NetworkEndpointBlockEntity.java`

- [ ] Add saved data that allocates monotonic `int` IDs and formats four-digit values with `String.format(Locale.ROOT, "%04d", value)`.
- [ ] Assign computer network numbers lazily from `ServerLevel` and default network name to `NETWORK#0001` style.
- [ ] Assign endpoint/router numbers lazily from `ServerLevel` and expose `numericDeviceId()` / `formattedDeviceId()`.
- [ ] Preserve existing `networkId` strings for assignment compatibility.

### Task 2: Endpoint Assignment UI

**Files:**
- Modify: `src/main/java/net/doole/doolestools/client/screen/NetworkEndpointNameScreen.java`

- [ ] Replace cycle-only network button with a dropdown/list panel shown when the network button is clicked.
- [ ] Show network display names and edit/view status in the list.
- [ ] Save selected network internal ID.
- [ ] On Clear, empty nickname and focus the nickname box with cursor at the end.

### Task 3: Validation

**Files:**
- Modify/add tests only where pure logic is available.

- [ ] Run `./gradlew.bat compileJava`.
- [ ] Run `./gradlew.bat build`.
- [ ] Report changed files and verification output.
