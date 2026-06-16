# LogiGraph Factory OS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand Doole's Tools LogiGraph into a server-authoritative factory operating system with Soul resources, Soul fluids, tanks, bundled conduits, scanner exclusions, overclocking, Easy Factory routing, graph import/export, and wall monitors while preserving the read-only scanner contract.

**Architecture:** Keep the existing scanner/read-only graph path intact and add world-mutating systems as separate server-side managers gated by server config and validated payload handlers. Build in vertical slices so each milestone compiles, preserves old graph saves, and can be tested independently before the next subsystem is added.

**Tech Stack:** NeoForge 26.1.2.76, Minecraft 26.1.2, Java 25, Gradle 9.1.0 wrapper, NeoForge DeferredRegister, ValueInput/ValueOutput persistence, Codec/StreamCodec data models, NeoForge item/fluid/energy capabilities.

---

## Ground Rules
- Run all commands from repo root on branch `dev` unless a replacement branch is created later.
- Primary correctness gate after each milestone: `./gradlew.bat build`.
- Use `./gradlew.bat runClientData` only when datagen providers exist and generated resources are intentionally updated.
- Do not add item/fluid/energy movement to `LogisticsScanner`; scanner changes are limited to reading, categorizing, and excluding infrastructure noise.
- Every client action that mutates world/configured state must become a server-bound payload handled and validated in `ServerPayloadHandlers`.
- Every persisted graph/schema change must update the relevant `CODEC`; use `optionalFieldOf` only for old save compatibility.
- Do not copy code or assets from EnderIO, Super Factory Manager, CC:Tweaked, or Minecraft internals beyond normal API use.

## Milestone Map
1. Configuration and datagen scaffolding.
2. Soul resource items and storage blocks.
3. Soul fluids, buckets, Crystalised Soul Flux, and Soul Waste Block.
4. Fragment Refiner, Soul Flux Transducer, and Soul Compressor.
5. Soul Tank and Pressurised Soul Tank.
6. Conduit bundle data model and scanner blacklist.
7. Fluid, item, and energy conduit transport.
8. Filters, conduit wrench, and conduit UI.
9. Graph import/export.
10. Overclocking data model, UI, and server manager.
11. Easy Factory route rules, UI, and server manager.
12. LogiGraph Wall Monitor multiblock.
13. Final assets, warnings, compatibility, and release verification.

## File Structure Target

### Config
- Create: `src/main/java/net/doole/doolestools/config/ModServerConfig.java` for server config spec and accessors.
- Modify: `src/main/java/net/doole/doolestools/DoolesTools.java` to register config during mod construction.

### Registry
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlocks.java` for all new blocks.
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java` for resource items, buckets, conduits, filters, and block items.
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlockEntities.java` for machines, tanks, conduit bundle, and wall monitor block entities.
- Modify: `src/main/java/net/doole/doolestools/registry/ModMenus.java` for machine, tank, conduit, overclocking, Easy Factory, and wall monitor menus as needed.
- Create: `src/main/java/net/doole/doolestools/registry/ModFluids.java` for Soul Flux and Soul Waste source/flowing fluids.
- Create: `src/main/java/net/doole/doolestools/registry/ModDataComponents.java` for tank contents, filters, and conduit item data.

### Blocks and Items
- Create: `src/main/java/net/doole/doolestools/block/CrystalisedSoulFluxBlock.java`.
- Create: `src/main/java/net/doole/doolestools/block/SoulWasteBlock.java`.
- Create: `src/main/java/net/doole/doolestools/block/SoulMachineBlock.java` for shared horizontal machine behaviour.
- Create: `src/main/java/net/doole/doolestools/block/SoulTankBlock.java`.
- Create: `src/main/java/net/doole/doolestools/block/ConduitBundleBlock.java`.
- Create: `src/main/java/net/doole/doolestools/block/LogiGraphWallMonitorBlock.java`.
- Create: `src/main/java/net/doole/doolestools/block/ScannerHiddenBlock.java` marker interface.
- Create: `src/main/java/net/doole/doolestools/item/ConduitItem.java`.
- Create: `src/main/java/net/doole/doolestools/item/ConduitWrenchItem.java`.
- Create: `src/main/java/net/doole/doolestools/item/FilterItem.java`.

### Block Entities
- Create: `src/main/java/net/doole/doolestools/blockentity/FragmentRefinerBlockEntity.java`.
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulFluxTransducerBlockEntity.java`.
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulCompressorBlockEntity.java`.
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulTankBlockEntity.java`.
- Create: `src/main/java/net/doole/doolestools/blockentity/ConduitBundleBlockEntity.java`.
- Create: `src/main/java/net/doole/doolestools/blockentity/LogiGraphWallMonitorBlockEntity.java`.

### Logistics Data and Managers
- Create: `src/main/java/net/doole/doolestools/logistics/tags/ModBlockTags.java`.
- Modify: `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java` for scanner blacklist and tank scan support.
- Modify: `src/main/java/net/doole/doolestools/logistics/WarningGenerator.java` for overclocking and Easy Factory warnings.
- Create: `src/main/java/net/doole/doolestools/logistics/data/GraphOverclockData.java`.
- Create: `src/main/java/net/doole/doolestools/logistics/data/GraphRouteRuleData.java`.
- Modify: `src/main/java/net/doole/doolestools/logistics/data/LogisticsGraphData.java` to attach overclock data compatibly.
- Modify: `src/main/java/net/doole/doolestools/logistics/data/GraphLinkData.java` to attach route rule data compatibly.
- Create: `src/main/java/net/doole/doolestools/logistics/overclock/OverclockManager.java`.
- Create: `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`.

### Conduits
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitType.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitTier.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitSideMode.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitRedstoneMode.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitLayerData.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitNetworkManager.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/ItemConduitNetwork.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/FluidConduitNetwork.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/EnergyConduitNetwork.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/filter/ItemFilterData.java`.
- Create: `src/main/java/net/doole/doolestools/conduit/filter/FluidFilterData.java`.

### Menus, Screens, and Client
- Create: `src/main/java/net/doole/doolestools/menu/SoulMachineMenu.java`.
- Create: `src/main/java/net/doole/doolestools/menu/SoulTankMenu.java`.
- Create: `src/main/java/net/doole/doolestools/menu/ConduitConfigMenu.java`.
- Create: `src/main/java/net/doole/doolestools/client/screen/SoulMachineScreen.java`.
- Create: `src/main/java/net/doole/doolestools/client/screen/SoulTankScreen.java`.
- Create: `src/main/java/net/doole/doolestools/client/screen/ConduitConfigScreen.java`.
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java` for Overclocking, Easy Factory, and import/export UI.
- Modify: `src/main/java/net/doole/doolestools/client/ClientSetup.java` for new screens/renderers.
- Create: `src/main/java/net/doole/doolestools/client/render/SoulTankRenderer.java`.
- Create: `src/main/java/net/doole/doolestools/client/render/ConduitBundleRenderer.java`.
- Create: `src/main/java/net/doole/doolestools/client/render/LogiGraphWallMonitorRenderer.java`.

### Networking
- Create payload records under `src/main/java/net/doole/doolestools/network/payload/` for tank config, conduit config, graph import, graph export request, overclock update, route rule update, wall monitor link/mode, and machine config.
- Modify: `src/main/java/net/doole/doolestools/network/ModNetworking.java` to register each payload.
- Modify: `src/main/java/net/doole/doolestools/network/ServerPayloadHandlers.java` to validate each server-bound action.
- Modify: `src/main/java/net/doole/doolestools/client/ClientPayloadHandlers.java` for server responses and UI sync.
- Modify: `src/main/java/net/doole/doolestools/network/ModStreamCodecs.java` when reusable Codec-derived stream codecs are needed.

### Resources
- Modify: `src/main/resources/assets/doolestools/lang/en_us.json`.
- Add assets, blockstates, models, recipes, loot tables, and tags listed in `docs/LOGIGRAPH_ASSET_CREATION_AND_IMPLEMENTATION_PROMPT.md`.

---

## Milestone 1: Configuration and Datagen Scaffolding

**Files:**
- Create: `src/main/java/net/doole/doolestools/config/ModServerConfig.java`
- Modify: `src/main/java/net/doole/doolestools/DoolesTools.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlocks.java`

- [x] **Step 1: Add server config values**

Create `ModServerConfig` with explicit config entries for feature gates, resource drops, tank capacities, conduit rates, Easy Factory budgets, overclock limits, and Crystalised Soul Flux damage. Use names exactly from the prompt where possible and keep default values equal to the brief.

- [x] **Step 2: Register server config**

In `DoolesTools`, register the server config in the constructor before gameplay systems reference it.

- [x] **Step 3: Add registry placeholders only where compilation requires them**

Do not register incomplete gameplay blocks in this milestone. Add only config and helper classes that compile independently.

- [x] **Step 4: Verify**

Run: `./gradlew.bat build`

Expected: build succeeds and generated metadata still expands from `src/main/templates/META-INF/neoforge.mods.toml`.

- [ ] **Step 5: Commit**

Commit message: `chore: add factory os server config scaffolding`

---

## Milestone 2: Soul Resource Items and Storage Blocks

**Files:**
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModCreativeTabs.java`
- Modify: `src/main/resources/assets/doolestools/lang/en_us.json`
- Add resources for item/block models, blockstates, recipes, loot tables, and tags for raw soul, ingot, nugget, condensed block, and soul block.

- [x] **Step 1: Register items**

Add `raw_soul_fragment`, `soul_ingot`, and `soul_nugget` as basic items.

- [x] **Step 2: Register blocks and block items**

Add `condensed_raw_soul_fragment` and `soul_block` as storage blocks with matching block items.

- [x] **Step 3: Add creative tab entries**

Place resources near existing Doole's Tools items in `ModCreativeTabs`.

- [x] **Step 4: Add resources**

Add lang keys, generated-style item models, cube block models, blockstates, recipes, loot tables, and common item/block tags listed in the brief.

- [x] **Step 5: Verify**

Run: `./gradlew.bat build`

Expected: resource JSON loads and all registry objects compile.

- [ ] **Step 6: Commit**

Commit message: `feat: add soul resource progression basics`

---

## Milestone 3: Soul Fluids and Special Blocks

**Files:**
- Create: `src/main/java/net/doole/doolestools/registry/ModFluids.java`
- Create: `src/main/java/net/doole/doolestools/block/CrystalisedSoulFluxBlock.java`
- Create: `src/main/java/net/doole/doolestools/block/SoulWasteBlock.java`
- Modify: `src/main/java/net/doole/doolestools/DoolesTools.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- Modify: `src/main/java/net/doole/doolestools/registry/ModCreativeTabs.java`
- Add resources for fluid textures, bucket models, special block models, tags, loot tables, and translations.

- [ ] **Step 1: Register fluid types and fluids**

Add source and flowing variants for `soul_flux` and `soul_waste`, with bucket items `soul_flux_bucket` and `soul_waste_bucket`.

- [ ] **Step 2: Implement Crystalised Soul Flux**

Use ice-like friction, a configurable damage amount, damage interval tracking that does not spam every tick, optional light level from config, and self drop.

- [ ] **Step 3: Implement Soul Waste Block**

Use slime-like bounce/fall behaviour and soft/sticky movement feel without copying external source code.

- [ ] **Step 4: Add fluid and block resources**

Add all textures/models/blockstates/tags/lang required for Soul Flux, Soul Waste, Crystalised Soul Flux, and Soul Waste Block.

- [ ] **Step 5: Verify**

Run: `./gradlew.bat build`

Expected: fluids, buckets, and blocks compile and register.

- [ ] **Step 6: Commit**

Commit message: `feat: add soul fluids and special soul blocks`

---

## Milestone 4: Soul Machines

**Files:**
- Create: `src/main/java/net/doole/doolestools/block/SoulMachineBlock.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/FragmentRefinerBlockEntity.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulFluxTransducerBlockEntity.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulCompressorBlockEntity.java`
- Create: `src/main/java/net/doole/doolestools/menu/SoulMachineMenu.java`
- Create: `src/main/java/net/doole/doolestools/client/screen/SoulMachineScreen.java`
- Modify: registry, networking, scanner, lang, recipes, loot tables, models, and textures.

- [ ] **Step 1: Add shared machine base behaviour**

Implement a horizontal machine block with active/inactive state, menu opening, and comparator support where useful.

- [ ] **Step 2: Add Fragment Refiner**

Implement `Raw Soul Fragment -> Soul Ingot + 250 mB Soul Waste`, internal item slot, internal Soul Waste tank, ValueInput/ValueOutput persistence, and capability exposure.

- [ ] **Step 3: Add Soul Flux Transducer**

Implement `1 Soul Ingot -> 500 mB Soul Flux`, 100 tick processing, 8000 mB internal tank, persistence, and capability exposure.

- [ ] **Step 4: Add Soul Compressor**

Implement custom machine recipes for `1000 mB Soul Flux -> Crystalised Soul Flux` and `1000 mB Soul Waste -> Soul Waste Block`.

- [ ] **Step 5: Add menus and screen**

Expose progress, input/output slots, tank contents, and disabled-by-config messages.

- [ ] **Step 6: Add scanner support**

Ensure the three machines scan as machine nodes with inventory/fluid summaries and progress status.

- [ ] **Step 7: Verify**

Run: `./gradlew.bat build`

Expected: machines compile, menus register, scanner remains read-only.

- [ ] **Step 8: Commit**

Commit message: `feat: add soul processing machines`

---

## Milestone 5: Soul Tanks

**Files:**
- Create: `src/main/java/net/doole/doolestools/block/SoulTankBlock.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/SoulTankBlockEntity.java`
- Create: `src/main/java/net/doole/doolestools/menu/SoulTankMenu.java`
- Create: `src/main/java/net/doole/doolestools/client/screen/SoulTankScreen.java`
- Create: `src/main/java/net/doole/doolestools/client/render/SoulTankRenderer.java`
- Create payloads for tank config under `network/payload`.
- Modify: `ServerPayloadHandlers`, `ModNetworking`, registries, scanner, lang, resources.

- [ ] **Step 1: Define tank side and redstone modes**

Add enums with codecs for `DISABLED`, `INPUT`, `OUTPUT`, `INPUT_OUTPUT` and `IGNORED`, `ACTIVE_WITH_SIGNAL`, `ACTIVE_WITHOUT_SIGNAL`, `NEVER`.

- [ ] **Step 2: Implement tank block entity**

Store fluid contents, capacity, side modes, redstone mode, auto push/pull flags, and locked fluid. Persist with ValueInput/ValueOutput.

- [ ] **Step 3: Implement capabilities and container interaction**

Expose NeoForge fluid capability per side according to side mode. Support bucket/container fill and drain through the UI.

- [ ] **Step 4: Preserve contents on break**

Use item data components to store tank fluid content and restore it when placed.

- [ ] **Step 5: Add in-world rendering and comparator output**

Render fill level and fluid colour. Comparator output maps 0-15 to fill percentage.

- [ ] **Step 6: Add wrench interactions**

Right-click opens config; sneak-wrench cycles clicked side mode.

- [ ] **Step 7: Add scanner support**

Tanks scan as fluid storage nodes and are never excluded by conduit blacklist logic.

- [ ] **Step 8: Verify**

Run: `./gradlew.bat build`

Expected: tank registry, menu, renderer, payloads, persistence, and scanner support compile.

- [ ] **Step 9: Commit**

Commit message: `feat: add configurable soul tanks`

---

## Milestone 6: Conduit Bundle Model and Scanner Blacklist

**Files:**
- Create: `src/main/java/net/doole/doolestools/block/ScannerHiddenBlock.java`
- Create: `src/main/java/net/doole/doolestools/block/ConduitBundleBlock.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/ConduitBundleBlockEntity.java`
- Create conduit data enums/classes under `src/main/java/net/doole/doolestools/conduit/`.
- Create: `src/main/java/net/doole/doolestools/logistics/tags/ModBlockTags.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java`
- Add: `src/main/resources/data/doolestools/tags/block/scanner_blacklist.json`

- [ ] **Step 1: Add marker interface**

Create `ScannerHiddenBlock` as a marker for infrastructure blocks that should not become graph nodes.

- [ ] **Step 2: Add conduit bundle block and block entity**

Register `conduit_bundle` as the technical in-world block and implement persistence for empty layer maps.

- [ ] **Step 3: Add conduit layer data model**

Add type, tier, side mode, redstone mode, dye channel, priority, forced disconnect, and filter references with codecs.

- [ ] **Step 4: Implement scanner blacklist**

In `LogisticsScanner.readBlock`, read the `BlockState` before capability probing and return null if it is tagged `doolestools:scanner_blacklist`, implements `ScannerHiddenBlock`, or has registry id `doolestools:conduit_bundle`.

- [ ] **Step 5: Add resources**

Add conduit bundle blockstate/model/loot behavior and scanner blacklist tag with `doolestools:conduit_bundle`.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: conduit bundle compiles, scanner excludes it, normal inventories and tanks remain scannable.

- [ ] **Step 7: Commit**

Commit message: `feat: add conduit bundle scaffold and scanner blacklist`

---

## Milestone 7: Conduit Transport Networks

**Files:**
- Create: `src/main/java/net/doole/doolestools/conduit/ConduitNetworkManager.java`
- Create: `src/main/java/net/doole/doolestools/conduit/ItemConduitNetwork.java`
- Create: `src/main/java/net/doole/doolestools/conduit/FluidConduitNetwork.java`
- Create: `src/main/java/net/doole/doolestools/conduit/EnergyConduitNetwork.java`
- Create: `src/main/java/net/doole/doolestools/item/ConduitItem.java`
- Modify: `ConduitBundleBlockEntity`, registries, events, resources.

- [ ] **Step 1: Add conduit items**

Register item, advanced item, fluid, pressurised fluid, energy, and enhanced energy conduits. Placing a conduit creates or augments a conduit bundle.

- [ ] **Step 2: Add server-only network discovery**

Group connected conduit bundle layers by type/channel without crossing unloaded chunks and without force-loading.

- [ ] **Step 3: Implement fluid conduit transfer**

Use NeoForge fluid capabilities, simulation before commit, side modes, priorities, redstone modes, channel matching, and per-tick transfer budgets.

- [ ] **Step 4: Implement item conduit transfer**

Use item capabilities first and vanilla Container fallback only when safe. Respect side modes, priorities, channels, round-robin, self-feed prevention, and per-tick transfer budgets.

- [ ] **Step 5: Implement energy conduit transfer**

Use NeoForge energy capabilities, side modes, priorities, channels, redstone modes, and per-tick FE budgets.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: all conduit managers compile and only mutate state server-side.

- [ ] **Step 7: Commit**

Commit message: `feat: add server-side conduit transport networks`

---

## Milestone 8: Filters, Wrench, and Conduit Configuration UI

**Files:**
- Create: `src/main/java/net/doole/doolestools/conduit/filter/ItemFilterData.java`
- Create: `src/main/java/net/doole/doolestools/conduit/filter/FluidFilterData.java`
- Create: `src/main/java/net/doole/doolestools/item/ConduitWrenchItem.java`
- Create: `src/main/java/net/doole/doolestools/item/FilterItem.java`
- Create: `src/main/java/net/doole/doolestools/menu/ConduitConfigMenu.java`
- Create: `src/main/java/net/doole/doolestools/client/screen/ConduitConfigScreen.java`
- Create conduit config payloads under `network/payload`.
- Modify registries, networking, handlers, lang, models, textures.

- [ ] **Step 1: Add filter data components**

Store allow/deny mode and matched item/fluid entries on filter item stacks through data components.

- [ ] **Step 2: Apply filters in conduit networks**

Use item filters in item conduit routing and fluid filters in fluid conduit routing before simulation/commit.

- [ ] **Step 3: Add conduit wrench actions**

Right-click side cycles selected layer side mode; sneak-right-click toggles forced disconnect; right-click body opens config; configured remove action removes selected layer and drops the conduit item.

- [ ] **Step 4: Add conduit config screen**

Expose per-layer side mode, channel, priority, redstone mode, filter slot, and forced disconnect state with server-disabled messages.

- [ ] **Step 5: Validate config payloads**

Server handlers verify player range, target block entity type, feature gate, selected layer, allowed enum values, filter stack validity, and chunk loaded state.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: filters and wrench compile, config changes cannot bypass server validation.

- [ ] **Step 7: Commit**

Commit message: `feat: add conduit filters wrench and config ui`

---

## Milestone 9: Graph Import and Export

**Files:**
- Create: `src/main/java/net/doole/doolestools/logistics/data/GraphExportData.java`
- Create import/export payloads under `network/payload`.
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- Modify: `ServerPayloadHandlers`, `ModNetworking`, `ClientPayloadHandlers`.

- [ ] **Step 1: Add export wrapper data**

Create `GraphExportData` with `format`, `version`, `exportedAt`, and `graph`; serialize `graph` using `LogisticsGraphData.CODEC`.

- [ ] **Step 2: Add export UI**

Add Settings buttons for Save Graph, Export Graph, Import Graph, Duplicate Graph, and Reset Graph.

- [ ] **Step 3: Add import validation**

Server validates max nodes, max links, valid node IDs, valid link IDs, valid port references, coordinate bounds, route rules, and overclock entries before saving.

- [ ] **Step 4: Preserve compatibility**

Older graph JSON without route/overclock fields must load through existing optional codec defaults.

- [ ] **Step 5: Verify**

Run: `./gradlew.bat build`

Expected: graph export/import data compiles and validation runs server-side.

- [ ] **Step 6: Commit**

Commit message: `feat: add logigraph graph import export`

---

## Milestone 10: Overclocking

**Files:**
- Create: `src/main/java/net/doole/doolestools/logistics/data/GraphOverclockData.java`
- Create: `src/main/java/net/doole/doolestools/logistics/overclock/OverclockManager.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/data/LogisticsGraphData.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- Create overclock payloads under `network/payload`.
- Modify networking, handlers, monitor payloads, warnings, lang, GUI sprites.

- [ ] **Step 1: Add graph overclock data**

Store enabled state, node id, target position, tier, and last validation status in `GraphOverclockData`; attach to `LogisticsGraphData` with `optionalFieldOf` defaulting to an empty list.

- [ ] **Step 2: Add UI page**

Extend Logistics Computer pages from 4 to 5 with `PAGE_OVERCLOCKING`, tab icon, supported status, enabled toggle, tier selector, Soul Flux/t, multiplier, available Soul Flux, and warnings.

- [ ] **Step 3: Add server payloads**

Validate enable/disable and tier updates by player range, computer block entity, config gate, tier range, graph node existence, and loaded target chunk.

- [ ] **Step 4: Implement OverclockManager**

Tick server-side; validate config, loaded chunk, target machine exists, graph node resolves to scanned block, target is supported, and enough Soul Flux is available.

- [ ] **Step 5: Implement safe machine acceleration**

Support vanilla furnace, blast furnace, smoker, Fragment Refiner, Soul Flux Transducer, and Soul Compressor first. Do not tick arbitrary modded block entities unless `enableGenericMachineAcceleration` is true.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: old graph saves load, overclocking is config-gated and server-authoritative.

- [ ] **Step 7: Commit**

Commit message: `feat: add logigraph overclocking`

---

## Milestone 11: Easy Factory Mode

**Files:**
- Create: `src/main/java/net/doole/doolestools/logistics/data/GraphRouteRuleData.java`
- Create: `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`
- Modify: `src/main/java/net/doole/doolestools/logistics/data/GraphLinkData.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- Create route rule payloads under `network/payload`.
- Modify networking, handlers, warnings, labels, lang, GUI sprites.

- [ ] **Step 1: Add route rule data**

Attach `GraphRouteRuleData` to `GraphLinkData` with `optionalFieldOf`. Include enabled, allow/deny item filter, priority, max transfer, keep-stock, round-robin, and status fields.

- [ ] **Step 2: Add route editor UI**

Open route editor from graph link context menu. Show server enabled/disabled status, source, target, moved count, failed routes, warnings, filter, priority, max transfer, keep-stock, round-robin, and enabled toggle.

- [ ] **Step 3: Add server validation**

Server rejects route changes when Easy Factory Mode is disabled and validates player range, graph link existence, filter validity, transfer bounds, and loaded computer chunk.

- [ ] **Step 4: Implement EasyFactoryManager**

Tick server-side at configurable interval; resolve graph nodes to scanned positions; require loaded chunks; use item capabilities first; use safe Container fallback; enforce per-tick route and item budgets.

- [ ] **Step 5: Add warnings**

Surface blocked routes, missing inventories, full destinations, stale labels, duplicate labels, disabled server config, and unloaded target chunks.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: when disabled, graph links remain read-only annotations; when enabled, transfer is server-authoritative.

- [ ] **Step 7: Commit**

Commit message: `feat: add easy factory route automation`

---

## Milestone 12: LogiGraph Wall Monitor

**Files:**
- Create: `src/main/java/net/doole/doolestools/block/LogiGraphWallMonitorBlock.java`
- Create: `src/main/java/net/doole/doolestools/blockentity/LogiGraphWallMonitorBlockEntity.java`
- Create: `src/main/java/net/doole/doolestools/client/render/LogiGraphWallMonitorRenderer.java`
- Create wall monitor sync payloads under `network/payload`.
- Modify registries, networking, client setup, resources, lang, loot tables.

- [ ] **Step 1: Add wall monitor block and block entity**

Store controller position, width, height, tileX, tileY, valid flag, linked computer, and mode.

- [ ] **Step 2: Implement multiblock validation**

On placement, removal, neighbor update, and load, recalculate filled square/rectangle validity, same facing, no holes, no L-shapes, no disconnected shapes, and deterministic controller.

- [ ] **Step 3: Add sync and linking**

Sync controller state and linked computer state to clients. Validate link/mode payloads server-side.

- [ ] **Step 4: Add renderer**

Render one shared UI across the full rectangle, with each block rendering only its slice. Support Flowgraph, Warnings, Storage, Statistics, Overclocking, and Easy Factory display modes.

- [ ] **Step 5: Add invalid/unlinked states**

Invalid screens render `INVALID SCREEN`; unlinked screens render `NO LOGISTICS COMPUTER LINKED`.

- [ ] **Step 6: Verify**

Run: `./gradlew.bat build`

Expected: wall monitor compiles and dedicated-server classloading remains safe.

- [ ] **Step 7: Commit**

Commit message: `feat: add logigraph wall monitor multiblock`

---

## Milestone 13: Assets, Polish, and Final Verification

**Files:**
- All remaining assets/resources listed in `docs/LOGIGRAPH_ASSET_CREATION_AND_IMPLEMENTATION_PROMPT.md`.
- Modify: `README.md` if product docs need updated commands/features.
- Modify: `AGENTS.md` only if new build/test/codegen workflows are added.

- [ ] **Step 1: Complete asset checklist**

Ensure every listed texture, blockstate, model, recipe, loot table, tag, GUI texture, sprite, and translation key exists.

- [ ] **Step 2: Run datagen if providers were added**

Run: `./gradlew.bat runClientData`

Expected: generated resources update only intended generated files.

- [ ] **Step 3: Verify scanner contract**

Review `LogisticsScanner` and confirm it contains no insert/extract calls, no world mutation, no force-loading, and per-block guarded reads remain intact.

- [ ] **Step 4: Verify server authority**

Review all server-bound payloads and confirm disabled feature actions are rejected based on server config.

- [ ] **Step 5: Verify graph compatibility**

Confirm `LogisticsGraphData` and `GraphLinkData` new fields use optional defaults so old saves and old exports load.

- [ ] **Step 6: Final build**

Run: `./gradlew.bat build`

Expected: build succeeds.

- [ ] **Step 7: Manual smoke test**

Run: `./gradlew.bat runClient`

Expected: client launches; existing Logistics Computer and Monitor screens still open; new creative tab entries appear; no client-only class loads on server.

- [ ] **Step 8: Dedicated server smoke test**

Run: `./gradlew.bat runServer`

Expected: server starts with `--nogui`; no client-only classloading crash; config loads.

- [ ] **Step 9: Commit**

Commit message: `docs: finalize logigraph factory os implementation`

---

## Risk Register
- **Scope risk:** This is a multi-release feature set. Keep each milestone shippable and do not start the next milestone until build passes.
- **Scanner regression risk:** Conduits and Easy Factory move resources, but the scanner must remain pure read-only. Keep transport code outside `LogisticsScanner`.
- **Save compatibility risk:** Graph changes must use optional codec defaults. Block entity persistence changes must read missing fields safely.
- **Server authority risk:** UI toggles are advisory; server config and payload validation decide whether features can mutate state.
- **Performance risk:** Conduit and Easy Factory managers need transfer budgets, loaded-chunk checks, and cached network discovery.
- **Dedicated server risk:** Client renderers/screens must remain under `client/`; common networking may reference client handlers only through safe lambdas consistent with existing `ModNetworking` style.
- **Asset licensing risk:** All assets must be original Doole's Tools work, not copied or edited from reference mods.

## Final Acceptance Gate
- [ ] `./gradlew.bat build` passes.
- [ ] `./gradlew.bat runServer` starts without client classloading errors.
- [ ] `./gradlew.bat runClient` starts and existing LogiGraph screens still work.
- [ ] Conduit bundles and scanner-blacklisted blocks do not appear in LogiGraph scans.
- [ ] Tanks, machines, chests, furnaces, and normal inventories still appear in scans.
- [ ] Easy Factory Mode does not move items when server config is disabled.
- [ ] Overclocking does not run when server config is disabled.
- [ ] Old saved graph data loads with default route/overclock fields.
- [ ] All files listed in the asset creation checklist exist or are explicitly removed from scope in a follow-up decision.
