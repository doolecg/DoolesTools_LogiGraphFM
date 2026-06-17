# Changelog

All notable changes to **Doole's Tools** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Scheduled auto-scan** — Logistics Computers can rescan on `scan.autoScanIntervalTicks` without a
  player opening the GUI.
- **Redstone alerts** — Logistics Computers emit redstone when `scan.redstoneAlertOnError` is enabled
  and any ERROR-level warning is active.
- **Per-link throughput stats** — Easy Factory returns server-owned moved counts per link; the computer
  syncs rolling samples to node details and the stats page.
- **Configurable wire traversal depth** — `scan.maxWireTraversalSteps` replaces the fixed wired traversal
  cap for large grids.
- **Shareable graph blueprints** — graph import/export uses compact base64 graph JSON on the clipboard,
  still validated by the server on save.
- **Filter item picker** — filter nodes expose a searchable 3x3 ghost item grid with recents while keeping
  JEI/REI/EMI panels off the modal.
- **Mod-aware scan providers** — optional AE2, Mekanism, and Create readers augment standard capability
  scans when those mods are present.
- **CC:Tweaked peripheral** — Logistics Computers expose scan, warning, storage, power, and network data
  through optional reflection-based peripheral hooks.
- **Multi-computer mesh** — computers can link peers and merge saved peer scan snapshots read-only.
- **Throughput planner** — pure stats-page analysis estimates bottlenecks/starvation from route capacity
  and machine progress snapshots.

### Fixed / Optimised

- **GraphCanvasWidget.nodeMap()** was rebuilding a `HashMap` on every hit-test call (link click,
  port hover, etc.), ignoring the existing revision-gated cache. Now returns `cachedNodesById`
  directly after calling `ensureModel()` — zero extra allocations on frames where the graph hasn't changed.
- **EasyFactoryManager DFS** — `outboundLinks()` previously scanned the entire link list for every
  routing node visited during a route-through pass (O(links * nodes) per tick). Now builds one
  `Map<nodeId, List<links>>` index at the start of each tick and reuses it for all DFS calls.
- **EasyFactoryManager dead code** — removed `singleAcceptingSlot` and `isInputLikeInsertionSlot`,
  neither of which was called anywhere in the codebase.
- **NetworkPowerCalculator** — logs a `debug` line when the wired traversal hits `MAX_COMPONENT_STEPS`
  so large wire grids are visible in logs without flooding them at normal scale.

## [0.5.0] — 2026-06-16

The big one: a powered device network, device upgrades, optional item routing, and a
performance/UX pass. Everything still honours the two contracts — the scanner/graph path stays
strictly read-only, and every world-mutating action is server-authoritative and config-gated.

### Added

- **Power network** — a logical FE network the Logistics Computer draws over connected
  infrastructure: **Network Wire**, **Network Relay** (extends wireless reach, hop-limited),
  **Network Modem** and **Wireless Router** endpoints, and a fuel-burning **Network Generator**.
  The computer tallies devices/relays/routers each tick and only runs graph automation when the
  network has enough power.
- **Device upgrades** — Speed, Stack, Range, and Efficiency upgrade cards (max 4 per type). Install
  with **Shift + right-click** using a card; remove one with the **Network Screwdriver**. Counts are
  shown on the device's naming screen.
- **Numeric identities** — devices and networks get globally-unique four-digit IDs
  (`NETWORK#0001`), allocated by per-world saved data; networks can be named and access-gated
  (shared / private / whitelist).
- **Easy Factory item routing** — optional, **off by default** and server-gated, moves items along
  the links you draw. All transfers are two-phase (simulate then commit) so a partial insert never
  voids items.
- **Power dashboard** — supply/demand history graph in the computer terminal.

### Changed

- **Performance — networking:** standalone relays and wireless routers now register in a persistent
  per-dimension node index on load and drop out on removal (AE2/RS/Powah-style), so power
  recalculation no longer scans every block entity in a chunk radius. Power history switched to an
  `ArrayDeque` ring buffer (O(1) eviction).
- **Performance — graph canvas:** link geometry (projected node lookup + baked Bezier polylines) is
  cached per graph revision instead of recomputed every frame, and `DUTheme.line` now coalesces
  pixel runs into a few `fill` rectangles instead of one quad per pixel. Large graphs pan smoothly.
- **UX:** the terminal subtitle now sits directly under the **DOOLE'S TOOLS** wordmark; the device
  naming screen no longer overlaps its buttons and spells out how to install/remove upgrades; the
  Network Generator screen was relaid out so the progress bars no longer run through the fuel slot.
- **Label Gun:** holograms now also show the names you give network devices, not just block labels.

### Removed

- Empty placeholder packages (`conduit`, `tank`, `transport`, `logistics/overclock`) and the stale
  documentation describing soul machines, conduits, tanks, and overclocking that were never
  implemented. Docs now describe only what ships.

## [0.3.0] — 2026-06-15

### Fixed

- JEI, REI, EMI and all other inventory overlay mods no longer render on top of the LogiGraph screen. `imageWidth`/`imageHeight` are set to `10000` in the constructor so overlay mods calculate no available margin and keep their panels off-screen. No external dependencies required.

## [0.2.0] — 2026-06-15

### Added

- **Inventory popup** — "SHOW ALL" button appears in the Node Details panel when an inventory holds more than 5 distinct item types. Opens a 360 px overlay listing every item with fixed columns (icon · name · mod · count). Dismissed by X, clicking outside, or Escape.
- **Inventory sort controls** — sort by Amount, Name, or Mod; click the active button or the standalone ▲/▼ button to flip ascending/descending (AE2-style). Amount defaults to descending, Name/Mod default to ascending.
- **Inventory totals footer** — shows type count, total item count, and number of full stacks of 64.

### Changed

- Complete package rename: `net.doole.doolesutils` → `net.doole.doolestools` across all Java source files, resource directories, and build config.
- README rewritten: emoji removed, prose tightened, formatting standardised.
- GUI header corrected from "DOOLE'S UTILS" to "DOOLE'S TOOLS".
- Scanner now sends up to 500 distinct item types per block (was 5) so the full inventory is available client-side without a follow-up request.

### Fixed

- `LogisticsMonitorBlockEntity` persistence: replaced fragile `hasLink` boolean + raw long pair with `storeNullable(BlockPos.CODEC)`. Forward-compatible with existing save data.
- `ModMenus` registry holders typed as `DeferredHolder` instead of raw `Supplier`.
- Network string payloads (`SetBlockLabelPayload`, `SetGunLabelPayload`) now cap label length at the Netty decode layer before server code runs.
- `LogisticsScanner.probeDirections()` no longer allocates a new `Direction[7]` on every capability query — extracted to a `static final` constant.

## [0.1.0] — 2026-06-14

Initial release. Ships the **LogiGraph** MVP: a read-only logistics diagnostics
and planning tool. No item/fluid/energy transport — observation and visualisation only.

### Added

- **Logistics Computer** block — scans an 8-block cube on demand and hosts a
  node-graph editor terminal. Manual-trigger and read-only; unreadable blocks are
  reported as `UNKNOWN` rather than crashing the scan.
- **Logistics Monitor** block — mirrors the state of a linked Logistics Computer
  on a separate screen.
- **Label Gun** item — assign and view custom labels on nearby blocks, surfaced
  as in-world holograms on the client.
- **Read-only scanner** (`LogisticsScanner`) covering inventory, fluid, energy,
  furnace/machine-progress summaries, with per-block try/catch guarding.
- **Node-graph editor** — programmatic terminal-style GUI with draggable nodes,
  links, ports, frames, and free text; design-space scaling with matched
  hit-testing. All edits route through `LogisticsGraph` so client and server
  apply identical rules.
- **Warnings** — pure, side-effect-free diagnostics generated from scan data
  (`WarningGenerator`) and displayed in the terminal warning bar.
- **Networking** — codec-driven C2S/S2C payloads (scan, clear, save graph,
  sync, labels, monitor mode). Every C2S handler validates the sender's open
  menu, target position, and chunk-loaded state, and sanitises/clamps all
  client-supplied graph data.
- **Persistence** — block-entity state via Value I/O; block labels stored in
  world saved data (`BlockLabelSavedData`).
- **CC:Tweaked soft integration** — optional peripheral hooks that degrade
  gracefully when the mod is absent.
- Creative tab grouping the mod's blocks and items.

### Technical

- Built for **Minecraft 26.1.2** on **NeoForge 26.1.2.76**, Java 25,
  Gradle 9.1, ModDevGradle 2.x.
- Hard client/server split: all rendering/GUI code is isolated under `client/`
  and gated by `Dist.CLIENT`; client classes are referenced from common code
  only inside lambda bodies, so they never load on a dedicated server.
- One Mojang `CODEC` per data record drives both persistence and networking.

[Unreleased]: https://github.com/doolecg/DoolesTools_LogiGraph/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/doolecg/DoolesTools_LogiGraph/compare/v0.3.0...v0.5.0
[0.3.0]: https://github.com/doolecg/DoolesTools_LogiGraph/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/doolecg/DoolesTools_LogiGraph/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/doolecg/DoolesTools_LogiGraph/releases/tag/v0.1.0

