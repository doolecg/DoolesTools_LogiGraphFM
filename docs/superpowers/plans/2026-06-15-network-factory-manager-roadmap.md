# Network Factory Manager Roadmap

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert LogiGraph into a networked factory manager with routers, modems, wires, desktop UI, power, advanced filters, and compatibility-focused transport.

**Architecture:** Build the feature as independent milestones. Network identity and visibility come first, then UI shell, wired connectivity, power, generator, routing filters, graph UX, and compatibility passes. Each milestone must compile and pass `./gradlew.bat build` before the next begins.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, NeoForge resource/energy capabilities, Minecraft `Container`/`WorldlyContainer`, existing graph codecs and networking.

---

## Milestone 1: Network Core

**Goal:** Replace scan-first behavior with server-owned network-visible devices.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- `src/main/java/net/doole/doolestools/registry/ModItems.java`
- `src/main/java/net/doole/doolestools/registry/ModBlockEntities.java`
- `src/main/java/net/doole/doolestools/block/NetworkRouterBlock.java`
- `src/main/java/net/doole/doolestools/block/NetworkModemBlock.java`
- `src/main/java/net/doole/doolestools/blockentity/NetworkRouterBlockEntity.java`
- `src/main/java/net/doole/doolestools/blockentity/NetworkModemBlockEntity.java`
- `src/main/java/net/doole/doolestools/network/NetworkDeviceSavedData.java`
- `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java`
- `src/main/java/net/doole/doolestools/logistics/data/ScannedBlockData.java`

Tasks:
- [ ] Add Wireless Router block and item.
- [ ] Add Modem block and item.
- [ ] Store router/modem ID and name in block entities.
- [ ] Add shift-right-click naming/ID flow, initially using a simple server-owned label behavior.
- [ ] Add network device discovery from routers/modems attached to computers and machines.
- [ ] Rename UI language from Scanned Blocks to The Network.
- [ ] Keep existing scan snapshots but treat them as network device snapshots.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- A router attached to a computer makes a network exist.
- A router attached to a chest/machine makes that block visible in The Network.
- Unattached routers show offline/invalid state.

## Milestone 2: Desktop Shell UI

**Goal:** Convert the computer screen into a desktop shell while keeping the current graph editor intact.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- `src/main/java/net/doole/doolestools/client/gui/TerminalButton.java`
- `src/main/java/net/doole/doolestools/client/gui/DUTheme.java`
- `src/main/resources/assets/doolestools/lang/en_us.json`

Tasks:
- [ ] Add persistent desktop frame, start button, tray, time, and connection indicator.
- [ ] Replace centered bottom buttons with start-menu entries.
- [ ] Add apps: The Network, LogiGraph Factory Manager, Power Usage, Settings.
- [ ] Keep graph editor layout and behavior inside Factory Manager.
- [ ] Verify desktop renders at current supported GUI sizes.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- Existing graph editor is reachable through the start menu.
- The Network page replaces Scanned Blocks naming.
- System tray shows network connected/disconnected state.

## Milestone 3: Wired Network

**Goal:** Add wires and modem path discovery.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/block/NetworkWireBlock.java`
- `src/main/java/net/doole/doolestools/blockentity/NetworkWireBlockEntity.java`
- `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- `src/main/java/net/doole/doolestools/registry/ModBlockEntities.java`
- `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Resource files under `src/main/resources/assets/doolestools/`

Tasks:
- [ ] Add Wire Bundle block and item.
- [ ] Store up to four wire lanes per block entity.
- [ ] Store lane side connections and color/channel.
- [ ] Add simple placement behavior that adds a lane when bundle capacity remains.
- [ ] Add network graph traversal from computer modem through wire lanes to machine modems.
- [ ] Add basic wire model/texture placeholders.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- A wired modem path connects a machine to the computer network.
- Four lanes can be represented in one wire block position internally.
- Network traversal is bounded and cannot scan infinitely.

## Milestone 4: Power System

**Goal:** Networks consume power and expose power diagnostics.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/config/ModServerConfig.java`
- `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`
- `src/main/java/net/doole/doolestools/logistics/data/NetworkPowerData.java`
- `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- `src/main/java/net/doole/doolestools/network/payload/ComputerStatePayload.java`

Tasks:
- [ ] Add configurable FE/t costs: computer, router, modem, wire segment, visible device, item/fluid/energy route.
- [ ] Add network power calculation.
- [ ] Add underpowered state: visibility remains, transport pauses/slows.
- [ ] Add Power Usage page.
- [ ] Add power-based overclocking settings.
- [ ] Verify with `./gradlew.bat build`.

Suggested defaults:
- Base computer: 4 FE/t.
- Wireless router: 2 FE/t.
- Modem: 1 FE/t.
- Wire lane segment: 0.05 FE/t, rounded at network total.
- Visible device: 0.5 FE/t.
- Active item route: 1 FE/t.
- Active fluid route: 2 FE/t.
- Active energy route: 1 FE/t.
- Max visible devices per computer: 64.
- Max wireless router range: 32 blocks.
- Max wired cable distance: 256 segments.
- Max wire lanes per block: 4.

Acceptance:
- Power page shows supply, demand, deficit, and top consumers.
- Underpowered networks do not overperform.

## Milestone 5: Generator

**Goal:** Add a basic burnable fuel generator.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/block/NetworkGeneratorBlock.java`
- `src/main/java/net/doole/doolestools/blockentity/NetworkGeneratorBlockEntity.java`
- `src/main/java/net/doole/doolestools/registry/ModBlocks.java`
- `src/main/java/net/doole/doolestools/registry/ModBlockEntities.java`
- `src/main/java/net/doole/doolestools/registry/ModItems.java`
- Resource/data files for blockstate, model, item, loot table, recipe.

Tasks:
- [ ] Add generator block/item/entity.
- [ ] Add item fuel slot using Minecraft burnability rules where available.
- [ ] Expose NeoForge energy capability.
- [ ] Add configurable FE per burn tick.
- [ ] Add recipe and loot table.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- Solid burnable fuels generate FE.
- Generator can power a network through capability connection.

## Milestone 6: Advanced Filter Routing

**Goal:** Make filter nodes powerful enough for item logistics.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/logistics/data/GraphNodeData.java`
- `src/main/java/net/doole/doolestools/logistics/data/FilterRuleData.java`
- `src/main/java/net/doole/doolestools/logistics/easyfactory/EasyFactoryManager.java`
- `src/main/java/net/doole/doolestools/client/gui/NodeDetailsPanel.java`
- `src/main/java/net/doole/doolestools/client/gui/CanvasContextMenu.java`

Tasks:
- [ ] Add filter rule data codec.
- [ ] Add whitelist/blacklist mode.
- [ ] Add item type matching.
- [ ] Add per-route amount limits.
- [ ] Add round robin state.
- [ ] Add color channel field.
- [ ] Add right-click Create Filter menu item.
- [ ] Apply filters in Easy Factory route selection.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- Filter nodes can route specific items only.
- Round robin distributes across compatible output links.
- Amount limits are respected.

## Milestone 7: Graph UX

**Goal:** Improve graph interaction without hurting render performance.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/client/gui/GraphCanvasWidget.java`
- `src/main/java/net/doole/doolestools/client/EditorContext.java`
- `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- `src/main/java/net/doole/doolestools/logistics/LogisticsGraph.java`

Tasks:
- [ ] Add multi-connect from selected nodes to one target node.
- [ ] Skip incompatible or duplicate links.
- [ ] Replace straight dotted links with capped Bezier-like lines.
- [ ] Keep mostly solid line with subtle animated dotted highlight.
- [ ] Cap render samples per line to avoid lag.
- [ ] Verify with `./gradlew.bat build`.

Acceptance:
- Selecting multiple nodes and dragging a link creates all valid links.
- Link rendering looks smoother and remains performant with many links.

## Milestone 8: Compatibility Pass

**Goal:** Rebuild compatibility around safe capabilities and soft integrations.

**Files likely touched:**
- `src/main/java/net/doole/doolestools/logistics/LogisticsScanner.java`
- `src/main/java/net/doole/doolestools/logistics/ModMachineProgress.java`
- `src/main/java/net/doole/doolestools/logistics/PortDiscovery.java`
- `src/main/java/net/doole/doolestools/integration/IntegrationHooks.java`
- `src/main/java/net/doole/doolestools/integration/computercraft/ComputerCraftIntegration.java`

Tasks:
- [ ] Audit vanilla and NeoForge capability reads.
- [ ] Add safe capability-based visibility for AE2/Refined Storage/Create/Mekanism where exposed.
- [ ] Keep unknown mod blocks from aborting scans.
- [ ] Add CC:Tweaked soft peripheral API for network status and device list.
- [ ] Do not hard-depend on CC:Tweaked.
- [ ] Verify without optional mods using `./gradlew.bat build`.

Acceptance:
- Optional integrations do not classload-crash when absent.
- Network devices from common mods appear when they expose standard capabilities.
- CC:Tweaked support is soft and isolated.
