# Doole's Tools — LogiGraph

> Scan your base, wire it into a flowgraph, watch it breathe.
> Optional automation to move items — or just the map. Either way it's useful.

**LogiGraph** is a node-based logistics dashboard for NeoForge. Scan the blocks around you, lay them
out on a flowgraph, label them, monitor warnings, and run a powered device network on top. When the
server config allows it, Easy Factory routing can move items, fluids, and energy along the links you draw.

Minecraft `26.1.2` · NeoForge `26.1.2.76` · Java `25`

---

## Two halves, one tool

LogiGraph has a strict split that the whole codebase is built around:

- **Diagnostics (always read-only).** Scanning, the flowgraph, warnings, and the monitors never
  transport, insert, extract, mutate machines, or force-load chunks. Unreadable blocks are marked
  `UNKNOWN`, never crash the scan.
- **Automation (server-authoritative, config-gated).** Easy Factory routing, power buffering, and route
  transfers do move resources, but only on the server, only when the relevant config flag allows it,
  and only after the server re-validates the request. UI toggles are advisory — the server decides.

---

## Contents

- [Quick Start](#quick-start)
- [Blocks and Items](#blocks-and-items)
- [The Editor](#the-editor)
- [The Power Network](#the-power-network)
- [Device Upgrades](#device-upgrades)
- [Easy Factory Routing](#easy-factory-routing)
- [Feature Highlights](#feature-highlights)
- [What the Scanner Reads](#what-the-scanner-reads)
- [Warnings](#warnings)
- [Modded Machine Support](#modded-machine-support)
- [Mod Compatibility](#mod-compatibility)
- [Wiki](#wiki)
- [Building from Source](#building-from-source)
- [Project Layout](#project-layout)
- [FAQ](#faq)

---

## Quick Start

1. Place a **Logistics Computer** near the machines or storage you want to inspect.
2. Right-click it and press **Scan Area**. Every logistics-relevant block in range appears in the
   left panel.
3. Double-click a block — or select it and press **Add Selected** — to drop it on the flowgraph.
4. Drag nodes to arrange them. Drag an **OUT** socket onto a compatible **IN** socket to draw a link.
5. Add Filter / Splitter / Combine / Channel nodes if you want Easy Factory to route through logic.
6. Select a node for details and live throughput; warnings show along the bottom. Press **Save**.
7. Place a **Logistics Monitor** within 16 blocks and right-click it to mirror the computer as a
   read-only wall display.

---

## Blocks and Items

| Block / Item | What it does |
|---|---|
| **Logistics Computer** | Scans on demand, stores the latest scan + your flowgraph, hosts the editor, and runs the power network and (if enabled) routing. |
| **Logistics Monitor** | Read-only wall display; auto-links to the nearest computer within 16 blocks. Cycles Flowgraph / Warnings / Storage Summary. |
| **LogiGraph Wall Monitor** | Multi-block in-world display; adjacent tiles form a larger read-only wall screen. |
| **Label Gun** | Names blocks so scans read at a glance. Right-click in air to set the remembered name; sneak + right-click a block to stamp it. While held, nearby block labels **and** network device names render as see-through holograms. |
| **Network Wire** | Carries the wired side of the network between the computer and its endpoints. |
| **Network Modem** | A wired endpoint that attaches the network to a machine/storage face. |
| **Wireless Router** | A wireless endpoint; reaches the computer (or a relay) within range. |
| **Wireless Dongle** | Compact wireless endpoint variant for attaching machines to a network. |
| **Network Relay** | Extends wireless reach by hopping the signal further out (hop count is config-limited). |
| **Network Generator** | Burns fuel to feed FE into the network. |
| **Network Battery** | 4 MFE buffer; soaks up surplus FE and feeds deficits back into the network transactionally. |
| **Network Screwdriver** | Removes one installed upgrade card from a device. |
| **Upgrade Cards** | Speed, Stack, Range, Efficiency — installed into network devices (max 4 each). |

Sneak + right-click any network endpoint or relay to open its **naming screen** — set a nickname,
assign it to a network, and see its four-digit ID and installed upgrades.

---

## The Editor

A terminal-style interface: dark CRT styling, glowing green text, real item icons.

| Action | Input |
|---|---|
| Context menu | Right-click (node, label, frame, or empty canvas) |
| Add scanned block | Double-click in the list, or **Add Selected** |
| Select / marquee-select | Left-click / left-drag on empty canvas |
| Move node(s) | Drag any selected node |
| Draw a link | Drag an OUT socket onto a compatible IN socket |
| Retarget / remove a link | Drag the link's IN endpoint |
| Pan / Zoom | Middle-drag / scroll |
| Delete selection | Delete |
| Rename | Type in the name field while the node/frame/label is selected |

Each node exposes typed ports (machines read like a furnace — Input, Fuel/Power, Output — plus fluid
ports where relevant). Node cards show a status dot, the real item icon, contents, and a live recipe
progress bar for machines. Frames group nodes; free-floating text labels annotate. **Auto Arrange**
and **Fit View** are in the empty-canvas menu.

Filter nodes have a searchable 3x3 ghost item picker with recents. The computer screen deliberately
keeps JEI/REI/EMI side panels outside its bounds so recipe overlays do not cover the modal.

Client-only settings (not synced): grid, animation, auto-refresh, real item icons.

---

## The Power Network

The Logistics Computer flood-fills its connected infrastructure each tick (server-side, read-only, no
force-loading) and tallies wires, endpoints, relays, wireless routers, batteries, and route costs to
compute the network's **demand**. Supply comes from FE sources such as Network Generators and adjacent
energy providers. Wired Network Batteries smooth supply by charging from surplus and discharging into
deficit.

Graph automation (Easy Factory, below) only runs when the network has usable power. Full power runs at
full budget; partial power throttles throughput; zero usable power pauses automation without breaking
machines or links.

Wireless math (base range, per-range-upgrade distance, max range, relay hop limit, per-device costs)
all lives under the `networkPower` section of the server config. Standalone relays and wireless
routers register in a persistent per-dimension index, so adding more of them doesn't make the
recalculation walk the world. Very large wired grids can raise `scan.maxWireTraversalSteps`.

---

## Device Upgrades

Four card types, four max per type per device:

| Card | Effect |
|---|---|
| **Speed** | More routing throughput per tick for the attached device. |
| **Stack** | Larger per-route move size. |
| **Range** | Extends a wireless device's reach. |
| **Efficiency** | Reduces the device's power cost / wireless surcharge. |

**Install:** Shift + right-click the device with a card in hand. **Remove:** Shift + right-click with
the **Network Screwdriver** (the card pops back into your inventory). The device's naming screen shows
the current counts and these same instructions.

---

## Easy Factory Routing

Server-authoritative and gated behind `ModServerConfig`. Current defaults enable Easy Factory and
item/fluid/energy route types; server config is the source of truth. The computer moves resources along
links in filter/priority order, capped by route budget, power satisfaction, and upgrades. Every move is
two-phase (simulate, then commit) through NeoForge transactions, so a partial insert can never void
resources. Filter nodes let you channel, round-robin, rate-limit, and whitelist what flows through.

Each successful route reports moved amounts back to the computer. The editor shows per-link throughput
in node details, and the stats page includes a read-only throughput planner that flags bottlenecks and
starved links from the current scan snapshot.

---

## Feature Highlights

- **Scheduled auto-scan:** set `scan.autoScanIntervalTicks` above `0` to refresh computer data without
  a player opening the GUI.
- **Alert redstone output:** when `scan.redstoneAlertOnError` is true, the Logistics Computer emits redstone
  strength `15` while any ERROR-level warning is active.
- **Per-link throughput:** Easy Factory records server-side moved counts per link and syncs rolling samples
  to the client.
- **Configurable wire depth:** `scan.maxWireTraversalSteps` controls how far wired network traversal may walk.
- **Shareable graph blueprints:** Settings page export/import uses compact base64 graph JSON on the clipboard.
- **Filter item picker:** click the filter grid to open a searchable item modal with recents.
- **Mod-aware scan providers:** optional readers exist for AE2, Mekanism, and Create when those mods are loaded.
- **CC:Tweaked peripheral:** Logistics Computers expose scan, warning, power, storage, and network data.
- **Multi-computer mesh:** linked computers can merge saved scan snapshots for larger read-only coverage.
- **Throughput planner:** stats page estimates bottlenecks from route capacity and observed machine progress.

---

## What the Scanner Reads

For each block entity in range, read-only:

- **Vanilla containers** — chests (incl. doubles), barrels, hoppers, droppers, dispensers.
- **Furnaces / blast furnaces / smokers** — slots, active recipe, real cook progress and remaining
  time from the block's own timers.
- **NeoForge capabilities** — item, fluid, and energy handlers (amounts, capacity, resource metadata
  only; never insert/extract).
- **Mod-aware providers** — AE2 storage, Mekanism tanks/chemicals, and Create inventories where available.
- **Modded machine process** — see [Modded Machine Support](#modded-machine-support).

Blocks are categorised automatically. Anything with a block entity but no readable standard data shows
as `UNKNOWN_MACHINE` rather than being dropped. Conduits/infrastructure and anything in the
`doolestools:scanner_blacklist` tag (or implementing `ScannerHiddenBlock`) are excluded before any
probing.

---

## Warnings

Pure, side-free rules over the snapshot: storage (Empty / Nearly full / Full), furnace (No fuel /
No input / Output full / Not progressing), and graph structure (unlinked nodes, source with no
output, sink with no input, link target nearly full). A furnace with all three slots occupied is
normal operation, not an error.

---

## Modded Machine Support

Many mods don't expose recipe progress through vanilla APIs, so a best-effort read-only probe
(`ModMachineProgress`) surfaces real process state instead of defaulting to the energy buffer: it
reads a blockstate `active`/`running`/`working` flag (or reflects `getActive()`/`isActive()`), and
reflects common progress getters/fields into a real progress bar. Everything is guarded and
per-class cached; unknown mods fall back to scan-to-scan activity detection.

> **Diagnostic:** unmapped machines log one line per class to `logs/latest.log`:
> `[probe] <class> | activeProps=… | getters=… | fields=…`. Disable with
> `-DdoolesTools.probeDebug=false`.

---

## Mod Compatibility

LogiGraph reads other mods' blocks through vanilla `Container`, NeoForge capabilities, and optional
provider hooks. There are no hard dependencies beyond NeoForge and Minecraft.

- **Inventory overlays (JEI/REI/EMI):** the computer terminal keeps their panels off-screen so they
  don't draw over the editor or filter modal.
- **AE2 / Mekanism / Create:** optional scan providers load only when the target mod is present.
- **CC:Tweaked:** optional peripheral hooks load only if the mod is present and fail safe if not.
- **Hiding your pipes/cables from scans:** add them to the `doolestools:scanner_blacklist` block tag
  or implement `ScannerHiddenBlock`.

---

## Wiki

### Admin Config

Server config is authoritative. Useful keys:

| Key | Default | Notes |
|---|---:|---|
| `easyFactory.enableEasyFactoryTransport` | `true` | Master switch for automation. |
| `easyFactory.enableItemRoutes` / `enableFluidRoutes` / `enableEnergyRoutes` | `true` | Per-resource route gates. |
| `easyFactory.easyFactoryTickInterval` | `20` | Route tick interval; lower is more responsive and more expensive. |
| `scan.scanRadius` | `16` | Base scan radius; effective scan also covers configured wireless max range. |
| `scan.autoScanIntervalTicks` | `0` | `0` disables scheduled scans; `1200` is roughly one minute. |
| `scan.maxWireTraversalSteps` | `256` | Raise for huge wire grids if debug logs report traversal caps. |
| `scan.redstoneAlertOnError` | `true` | Computer emits redstone while ERROR warnings exist. |

### Graph Blueprints

Use **Settings → Export Graph** to copy a compact base64 string. Use **Settings → Import Graph** to paste
it into another world. Imported graphs still pass through server-side `SaveGraphPayload` sanitization, so
old or hostile strings cannot bypass graph limits.

### Filters

Add a **Filter** node from the tool palette, select it, then click its 3x3 ghost grid. The picker searches
all registered items by display name and registry id, remembers recent choices, and leaves JEI/REI/EMI
panels outside the modal.

### Multi-Computer Mesh

Computers can store peer computer links and merge those peers' latest saved scan snapshots during a new
scan. Mesh merging is read-only: it never scans unloaded chunks, force-loads peers, or edits the peer's
graph.

### CC:Tweaked Peripheral

When CC:Tweaked is installed, a Logistics Computer exposes peripheral type `logistics_computer` with:
`getStatus`, `getPower`, `getDevices`, `getWarnings`, `getStorageSummary`, `scan`, and `getNetworkId`.
All returned data is plain Lua-friendly tables derived from server-owned scan/power state.

---

## Building from Source

```powershell
.\gradlew.bat build       # compile + package → build/libs/doolestools-<version>.jar
.\gradlew.bat runClient   # dev client
.\gradlew.bat runServer   # dedicated dev server (--nogui)
.\gradlew.bat test        # JUnit 5 unit suite (pure logic only)
```

There's no lint/CI; `build` plus `test` are the correctness gates. The unit suite under
`src/test/java` covers pure logic that doesn't need a live `Level` (e.g. `WirelessNetworkPolicy`,
`EasyFactoryManager` helpers).

**Toolchain:** Java 25 · Gradle 9.1 (wrapper) · ModDevGradle 2.x · mod id `doolestools` · root package
`net.doole.doolestools`. Version pins live in `gradle.properties`. Mod metadata expands at build time
from `src/main/templates/META-INF/neoforge.mods.toml` — never hand-edit the generated copy.

---

## Project Layout

```
net.doole.doolestools
  registry/     blocks, items, block entities, menus, creative tabs
  block/        computer, monitor, and network blocks
  blockentity/  data stores (Value I/O persistence)
  config/       ModServerConfig feature flags + tuning
  menu/         container/slotless menus
  logistics/    scanner, graph ops, port discovery, warnings, machine probe,
                power calculator, network node index, easyfactory, throughput planner, data records
  network/      payloads + validated server-side handlers
  world/        per-world saved data (labels, numeric identities)
  client/       client-only: screens, GUI widgets, editor state, renderers
  integration/  optional CC:Tweaked and mod-aware scan providers
```

Repo-local agent guidance files may be ignored by git; use the checked-in README and source config as the public source of truth.

---

## FAQ

**Does it move items?** Easy Factory can move items, fluids, and energy when server config allows it.
Current defaults enable the route types, but diagnostics and the flowgraph are always read-only.

**Will scanning lag my world or load chunks?** It scans on demand or by `autoScanIntervalTicks`, and never
force-loads chunks; unloaded chunks are skipped.

**How do I share a factory layout?** Use Settings → Export Graph to copy a base64 blueprint string, then
Settings → Import Graph in the destination world.

**Why is throughput different from the planner?** Throughput is live server transfer history. The planner
is a read-only estimate from route config and machine progress snapshots.

**I can't install upgrades.** Hold an upgrade card and **Shift + right-click** the network device.
Use the **Network Screwdriver** (also Shift + right-click) to take one back out.

**I named a network device but don't see it floating.** Hold the **Label Gun** — device names render
as holograms alongside block labels.

**A block shows as `UNKNOWN_MACHINE`.** It has a block entity but no vanilla container or standard
NeoForge capability; a mod-specific provider would be needed to read it.
