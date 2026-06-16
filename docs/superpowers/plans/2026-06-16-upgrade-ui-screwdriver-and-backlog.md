# Upgrade UI, Screwdriver, And Backlog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show installed upgrade cards in network device UI, add a screwdriver to remove them, and fold in the remaining verified backlog items from the current stabilization pass.

**Architecture:** Keep upgrade storage as existing integer counts on endpoint, wire-hosted endpoint, and relay block entities. Add removal methods that decrement one installed upgrade and return the matching card item. Extend the existing endpoint/relay identity screen to show upgrade rows, and add a screwdriver item that removes upgrades server-side with shift-right-click/device interaction.

**Tech Stack:** Java 25, NeoForge block/item registration, existing GUI widgets, block entity Value I/O, Gradle wrapper.

---

### Stage 1: Backlog Tracking And Existing Stabilization

**Files:**
- Existing modified files from lag, fuel routing, numeric identity, and dropdown work.

- [ ] Keep fuel slot routing fix: legacy `in` routes remain generic while `material_in` and `fuel_in` are strict.
- [ ] Keep graph/network lag reductions: loaded block-entity iteration, power cache, and low-detail graph rendering.
- [ ] Keep numeric network/router IDs and dropdown selector.
- [ ] Keep clear-button focus behavior.
- [ ] Run `./gradlew.bat build` after later stages to verify all changes together.

### Stage 2: Upgrade Removal API

**Files:**
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/NetworkEndpointBlockEntity.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/NetworkWireBlockEntity.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/NetworkRelayBlockEntity.java`

- [ ] Add a `network_screwdriver` item to item registration and creative tab.
- [ ] Add helpers mapping upgrade type strings to card items.
- [ ] Add `removeUpgrade(String type)` methods to direct endpoints, wire-hosted endpoints, and relays.
- [ ] Add `installedUpgradeTypes()` methods for display/removal ordering.

### Stage 3: Screwdriver Interaction

**Files:**
- Modify: `src/main/java/net/doole/doolestools/block/NetworkEndpointBlock.java`
- Modify: `src/main/java/net/doole/doolestools/block/NetworkWireBlock.java`
- Modify: `src/main/java/net/doole/doolestools/block/NetworkRelayBlock.java`

- [ ] When using the screwdriver on a device, remove one installed upgrade in deterministic order: efficiency, range, stack, speed.
- [ ] Return the removed card to the player inventory or drop it if inventory is full.
- [ ] Send concise feedback if no upgrades are installed.

### Stage 4: UI Reflection

**Files:**
- Modify: `src/main/java/net/doole/doolestools/client/screen/NetworkEndpointNameScreen.java`
- Modify screen open bridge/callers as needed.

- [ ] Pass upgrade counts into the screen when opening endpoint/wire/relay assignment UI.
- [ ] Render rows for Speed, Stack, Range, Efficiency with `count / 4` or `n/a` for unsupported relay stack upgrades.
- [ ] Keep network dropdown and nickname fields working with the taller panel.

### Stage 5: Assets And Verification

**Files:**
- Add: `src/main/resources/assets/doolestools/models/item/network_screwdriver.json`
- Modify: `src/main/resources/assets/doolestools/lang/en_us.json` if present, otherwise create it.

- [ ] Add item model using an existing vanilla texture to avoid missing-model warnings.
- [ ] Add display name for Network Screwdriver.
- [ ] Run `./gradlew.bat compileJava`.
- [ ] Run `./gradlew.bat build`.
