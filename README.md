# Doole's Tools — LogiGraph

**LogiGraph** is a node-based logistics dashboard for NeoForge. Scan the blocks around you, lay them
out on a flowgraph, label them, and run a powered device network on top — with optional, server-gated
item routing when you want the graph to actually move things.

Minecraft `26.1.2` · NeoForge `26.1.2.76` · Java `25`

---

## Two halves, one tool

LogiGraph has a strict split that the whole codebase is built around:

- **Diagnostics (always read-only).** Scanning, the flowgraph, warnings, and the monitors never
  transport, insert, extract, mutate machines, or force-load chunks. Unreadable blocks are marked
  `UNKNOWN`, never crash the scan.
- **Automation (server-authoritative, opt-in).** The power network and Easy Factory item routing do
  move things, but only on the server, only when the relevant config flag allows it, and only after
  the server re-validates the request. UI toggles are advisory — the server decides.

---

## Contents

- [Quick Start](#quick-start)
- [Blocks and Items](#blocks-and-items)
- [The Editor](#the-editor)
- [The Power Network](#the-power-network)
- [Device Upgrades](#device-upgrades)
- [Easy Factory Routing](#easy-factory-routing)
- [What the Scanner Reads](#what-the-scanner-reads)
- [Warnings](#warnings)
- [Modded Machine Support](#modded-machine-support)
- [Mod Compatibility](#mod-compatibility)
- [Building from Source](#building-from-source)
- [Project Layout](#project-layout)
- [Roadmap](#roadmap)
- [FAQ](#faq)

---

## Quick Start

1. Place a **Logistics Computer** near the machines or storage you want to inspect.
2. Right-click it and press **Scan Area**. Every logistics-relevant block in range appears in the
   left panel.
3. Double-click a block — or select it and press **Add Selected** — to drop it on the flowgraph.
4. Drag nodes to arrange them. Drag an **OUT** socket onto a compatible **IN** socket to draw a link.
5. Select a node for full details on the right; warnings show along the bottom. Press **Save**.
6. Place a **Logistics Monitor** within 16 blocks and right-click it to mirror the computer as a
   read-only wall display.

---

## Blocks and Items

| Block / Item | What it does |
|---|---|
| **Logistics Computer** | Scans on demand, stores the latest scan + your flowgraph, hosts the editor, and runs the power network and (if enabled) routing. |
| **Logistics Monitor** | Read-only wall display; auto-links to the nearest computer within 16 blocks. Cycles Flowgraph / Warnings / Storage Summary. |
| **Label Gun** | Names blocks so scans read at a glance. Right-click in air to set the remembered name; sneak + right-click a block to stamp it. While held, nearby block labels **and** network device names render as see-through holograms. |
| **Network Wire** | Carries the wired side of the network between the computer and its endpoints. |
| **Network Modem** | A wired endpoint that attaches the network to a machine/storage face. |
| **Wireless Router** | A wireless endpoint; reaches the computer (or a relay) within range. |
| **Network Relay** | Extends wireless reach by hopping the signal further out (hop count is config-limited). |
| **Network Generator** | Burns fuel to feed FE into the network. |
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

Client-only settings (not synced): grid, animation, auto-refresh, real item icons.

---

## The Power Network

The Logistics Computer flood-fills its connected infrastructure each tick (server-side, read-only, no
force-loading) and tallies wires, endpoints, relays, and wireless routers to compute the network's
**demand**. Supply comes from FE pulled out of adjacent energy blocks (e.g. a Network Generator), or
a configurable virtual supply if no real source is attached.

Graph automation (Easy Factory, below) only runs on a tick when the network can pay that tick's
demand. Insufficient power simply pauses automation — nothing breaks.

Wireless math (base range, per-range-upgrade distance, max range, relay hop limit, per-device costs)
all lives under the `networkPower` section of the server config. Standalone relays and wireless
routers register in a persistent per-dimension index, so adding more of them doesn't make the
recalculation walk the world.

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

Optional, **off by default**, and gated behind a server config flag. When enabled, the computer moves
items along the item links you've drawn, in filter/priority order, up to a per-tick route budget.
Fluid and energy route types exist behind their own flags. Every move is two-phase (simulate, then
commit) through a shared transfer helper, so a partial insert can never void resources. Filter nodes
let you channel, round-robin, rate-limit, and whitelist what flows through.

---

## What the Scanner Reads

For each block entity in range, read-only:

- **Vanilla containers** — chests (incl. doubles), barrels, hoppers, droppers, dispensers.
- **Furnaces / blast furnaces / smokers** — slots, active recipe, real cook progress and remaining
  time from the block's own timers.
- **NeoForge capabilities** — item, fluid, and energy handlers (amounts, capacity, resource metadata
  only; never insert/extract).
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

LogiGraph reads other mods' blocks through vanilla `Container` and NeoForge capabilities, so most
modded storage and machines appear in scans with no per-mod code. There are no hard dependencies
beyond NeoForge and Minecraft.

- **Inventory overlays (JEI/REI/EMI):** the computer terminal keeps their panels off-screen so they
  don't draw over the editor.
- **CC:Tweaked:** optional peripheral hooks load only if the mod is present and fail safe if not.
- **Hiding your pipes/cables from scans:** add them to the `doolestools:scanner_blacklist` block tag
  or implement `ScannerHiddenBlock`.

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
                power calculator, network node index, easyfactory, data records
  network/      payloads + validated server-side handlers
  world/        per-world saved data (labels, numeric identities)
  client/       client-only: screens, GUI widgets, editor state, renderers
  integration/  soft mod detection (CC:Tweaked)
```

`CLAUDE.md` is the full engineering contract; `AGENTS.md` is the compact guardrails summary.

---

## Roadmap

Ideas, not promises — and clearly separate from what ships today:

- Mod-aware port providers (AE2, Mekanism, Create) and wider modded progress coverage.
- Multi-canvas graph polish, exportable blueprints, JEI/Jade hints.
- A CC:Tweaked peripheral surface for reading network/graph state.
- More routing filters and a throughput planner view.

---

## FAQ

**Does it move items?** Only if you enable Easy Factory routing in the server config (off by default).
Diagnostics and the flowgraph are always read-only.

**Will scanning lag my world or load chunks?** It scans on demand (or on auto-refresh) and never
force-loads chunks; unloaded chunks are skipped.

**I can't install upgrades.** Hold an upgrade card and **Shift + right-click** the network device.
Use the **Network Screwdriver** (also Shift + right-click) to take one back out.

**I named a network device but don't see it floating.** Hold the **Label Gun** — device names render
as holograms alongside block labels.

**A block shows as `UNKNOWN_MACHINE`.** It has a block entity but no vanilla container or standard
NeoForge capability; a mod-specific provider would be needed to read it.
