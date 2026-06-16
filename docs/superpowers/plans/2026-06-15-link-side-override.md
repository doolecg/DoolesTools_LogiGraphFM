# Link Side Override Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-link side override dropdown so Easy Factory item routes can force a Minecraft/NeoForge inventory side while preserving material/fuel/output port semantics.

**Architecture:** Store an optional side override on each `GraphLinkData`. Render a small midpoint selector for hovered/selected links in `GraphCanvasWidget`, mutate the graph through `LogisticsGraph`, and have `EasyFactoryManager` apply the override to item route side selection.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing graph codec/network sync.

---

### Task 1: Data Model

**Files:**
- Modify: `src/main/java/net/doole/doolestools/logistics/data/GraphLinkData.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/LogisticsGraph.java`
- Modify: `src/main/java/net/doole/doolestools/network/ServerPayloadHandlers.java`

- [ ] Add `sideOverride` string to `GraphLinkData`, defaulting to `auto` through `optionalFieldOf`.
- [ ] Update constructors and link replacement sites to preserve or set `auto`.
- [ ] Add `withSideOverride(String)` on `GraphLinkData`.
- [ ] Add `LogisticsGraph.setLinkSideOverride(...)` to update a link immutably.
- [ ] Update server-side graph sanitization to clamp/normalize side override values.

### Task 2: UI Midpoint Selector

**Files:**
- Modify: `src/main/java/net/doole/doolestools/client/gui/GraphCanvasWidget.java`
- Modify: `src/main/java/net/doole/doolestools/client/EditorContext.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`

- [ ] Render a compact pill at the midpoint of hovered or selected links.
- [ ] Cycle values in order: `auto`, `up`, `down`, `north`, `south`, `east`, `west`.
- [ ] Add hit detection for the pill.
- [ ] On click, call `LogisticsGraph.setLinkSideOverride(...)` and save through existing graph mutation flow.

### Task 3: Easy Factory Side Override

**Files:**
- Modify: `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`

- [ ] Parse `GraphLinkData.sideOverride()` into a nullable `Direction`.
- [ ] Use override direction for item source/target side when not `auto`.
- [ ] Keep port roles strict: `material_in` material slots only, `fuel_in` fuel-side slots only, `item_out` output-only extraction.
- [ ] If a manual side cannot satisfy the route, do not fallback to another side.

### Task 4: Verification

**Files:**
- No code files.

- [ ] Run `./gradlew.bat compileJava` and confirm `BUILD SUCCESSFUL`.
- [ ] Run `./gradlew.bat build` and confirm `BUILD SUCCESSFUL`.
- [ ] Inspect `git diff` for unintended files.
