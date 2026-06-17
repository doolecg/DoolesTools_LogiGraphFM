# Doole's Tools - LogiGraph

LogiGraph is a powered logistics network and factory graph for NeoForge. A Logistics Computer does not
magically list every nearby machine anymore: blocks show up when they are connected to the computer's
network through a modem, router, dongle, or wire-hosted endpoint. Once connected, the computer can scan
their inventories/capabilities, place them on a flowgraph, warn about problems, and route items, fluids,
or energy when server config allows it.

Minecraft `26.1.2` | NeoForge `26.1.2.76` | Java `25`

---

## Contents

- [Quick Start](#quick-start)
- [How Scanning Works Now](#how-scanning-works-now)
- [Blocks And Items](#blocks-and-items)
- [Computer UI](#computer-ui)
- [Power Network](#power-network)
- [Easy Factory Routing](#easy-factory-routing)
- [Wiki](#wiki)
- [Compatibility](#compatibility)
- [Building From Source](#building-from-source)
- [Project Layout](#project-layout)
- [FAQ](#faq)

---

## Quick Start

1. Place a **Logistics Computer**.
2. Connect machines/storage to it:
   - Wired: run **Network Wire** from the computer to a **Network Modem** attached to the target block.
   - Wireless: place a **Wireless Router** or **Wireless Dongle** on the target block within range.
   - Extended wireless: place **Network Relays** between the computer and far wireless endpoints.
3. Open endpoint/relay naming screens with sneak + right-click if you want names, network assignment, or upgrades.
4. Open the Logistics Computer and press **Scan Network**. Connected reachable devices appear in the left panel.
5. Add scanned devices to the graph, draw typed links between ports, then press **Save**.
6. Add Filter / Splitter / Combine / Channel nodes if you want routing logic.
7. Use the Stats page for power, battery, throughput, and planner information.
8. Place a **Logistics Monitor** or **LogiGraph Wall Monitor** if you want an external display.

---

## How Scanning Works Now

The scanner discovers network endpoints, not arbitrary loose blocks. A chest, furnace, tank, machine, or
storage controller must be attached to a reachable endpoint before it appears in the computer.

What counts as reachable:

- A **Network Modem** attached to the target and wired back to the computer.
- A wire-hosted endpoint/socket attached to the target and connected through the wire graph.
- A **Wireless Router** or **Wireless Dongle** attached to the target and inside wireless range.
- A wireless endpoint reachable through one or more **Network Relays**.
- A peer computer's latest saved scan, if the two computers are linked in the mesh.

Network assignment matters. Devices with a blank network id are treated as unclaimed and can appear on
nearby computers. Devices assigned to a network only appear for matching computers.

Scans still stay safe: unreadable blocks are skipped or marked unknown, unloaded chunks are skipped, and
the scanner does not force-load chunks. Automation is separate from scanning and only happens through the
server tick/routing code.

---

## Blocks And Items

| Block / Item | What it does |
|---|---|
| **Logistics Computer** | Network controller, scanner, graph editor, power dashboard, automation engine. |
| **Logistics Monitor** | Single-block external display linked to a nearby computer. |
| **LogiGraph Wall Monitor** | Multi-block in-world display; adjacent tiles combine into one larger screen. |
| **Network Wire** | Wired network backbone; can also host endpoint/router data depending on placement. |
| **Network Modem** | Wired endpoint attached to a machine/storage face. |
| **Wireless Router** | Wireless endpoint attached to a block. |
| **Wireless Dongle** | Compact wireless endpoint variant. |
| **Network Relay** | Extends wireless reach across relay hops. |
| **Network Generator** | Burns fuel and supplies FE to the network. |
| **Network Battery** | 4 MFE buffer; charges from surplus and discharges during deficits. |
| **Network Screwdriver** | Removes one installed upgrade card and returns it. |
| **Speed Card** | Increases route budget contribution. |
| **Stack Card** | Increases item move size. |
| **Range Card** | Extends wireless reach. |
| **Efficiency Card** | Reduces network/wireless cost. |
| **Label Gun** | Stores and stamps labels; while held, shows block labels and network device names as holograms. |

Upgrade cards install with shift + right-click on endpoints/relays. The Network Screwdriver removes one
installed card with shift + right-click.

---

## Computer UI

### Network / Graph Page

- **Scan Network** refreshes reachable connected devices.
- **Refresh Data** requests the latest server state without forcing a new scan.
- Double-click a scanned device or press **Add Selected** to place it on the graph.
- Drag compatible OUT ports to IN ports to create links.
- Right-click empty canvas, nodes, frames, or labels for context actions.
- The right panel shows inventory, energy, machine progress, warnings, and live per-link throughput.

### Filter Picker

Filter nodes have a 3x3 ghost item grid. Click a grid slot to open a searchable item picker with recent
items. The computer screen keeps JEI/REI/EMI panels outside the modal so recipe overlays do not cover it.

### Stats Page

- Current power supply/demand and satisfaction.
- Battery capacity and stored FE.
- Power history across NOW / 30M / 1H / 12H / 1D / ALL views.
- Route/device counts and warning counts.
- Throughput planner summary for bottlenecks and starved links.

### Settings Page

- Client-only display settings: grid, animations, icons, auto-refresh, UI scale.
- Graph export/import as compact base64 clipboard blueprints.
- Network name/access controls and whitelist entries.

---

## Power Network

The computer calculates network demand from connected infrastructure and graph routes. Supply comes from
Network Generators, Network Batteries, and compatible FE providers exposed to the network.

If power is full, routing runs at full budget. If power is partial, routing throttles. If no usable power
is available, automation pauses until power returns.

Important tuning lives in server config:

- `networkPower.*` for device costs, wireless ranges, relay traversal, generator output, and battery cost.
- `scan.maxWireTraversalSteps` for very large wired grids.
- `scan.autoScanIntervalTicks` for scheduled server-side scans.

---

## Easy Factory Routing

Easy Factory reads the saved graph and moves resources through the connected blocks. Current server config
defaults enable the master Easy Factory switch and item/fluid/energy route types, but server config is the
source of truth.

Routing behavior:

- Item, fluid, and energy links are handled server-side.
- Moves are transactional: extract/insert simulate and commit together so resources are not voided.
- Filter / Splitter / Combine / Channel nodes shape item paths.
- Side overrides and machine port roles affect where items can be extracted/inserted.
- Speed/stack/range/efficiency upgrades change budget, move size, range, and cost.
- Per-link moved counts are recorded by the server and synced to clients as throughput stats.

---

## Wiki

### Connecting A Machine

For a wired machine:

1. Place the machine/storage block.
2. Place a Network Modem on the face that should connect to the network.
3. Run Network Wire from the modem back to the Logistics Computer.
4. Scan the network from the computer.

For a wireless machine:

1. Place a Wireless Router or Wireless Dongle on the machine/storage block.
2. Keep it within computer range or relay range.
3. Assign it to the desired network if needed.
4. Scan the network from the computer.

### Network Names And Access

Computers allocate stable internal network ids and display names like `NETWORK#0001`. Endpoint and relay
naming screens can assign a device to a network, rename it, and show upgrade counts. Blank device network
ids are unclaimed and can be discovered by nearby computers.

### Multi-Computer Mesh

Computers can link peer computers. On scan, a computer merges its own reachable devices with the peer's
latest saved scan results. This expands coverage without force-loading chunks or editing the peer graph.

### Graph Blueprints

Use **Settings -> Export Graph** to copy a base64 blueprint string. Use **Settings -> Import Graph** to
paste it into another world. Imported graphs still go through server-side graph sanitization.

### Admin Config

| Key | Default | Notes |
|---|---:|---|
| `easyFactory.enableEasyFactoryTransport` | `true` | Master automation switch. |
| `easyFactory.enableItemRoutes` | `true` | Enables item links. |
| `easyFactory.enableFluidRoutes` | `true` | Enables fluid links. |
| `easyFactory.enableEnergyRoutes` | `true` | Enables energy links. |
| `easyFactory.easyFactoryTickInterval` | `20` | Route tick interval. |
| `easyFactory.maxEasyFactoryRoutesPerTick` | `16` | Base route budget per tick. |
| `scan.scanRadius` | `16` | Endpoint search radius around computer/relays. |
| `scan.autoScanIntervalTicks` | `0` | `0` disables scheduled scans. `1200` is about one minute. |
| `scan.maxWireTraversalSteps` | `256` | Raise for very large wired networks. |
| `scan.redstoneAlertOnError` | `true` | Computer outputs redstone strength 15 while ERROR warnings exist. |

### CC:Tweaked Peripheral

When CC:Tweaked is present, a Logistics Computer exposes peripheral type `logistics_computer` with:
`getStatus`, `getPower`, `getDevices`, `getWarnings`, `getStorageSummary`, `scan`, and `getNetworkId`.

### What The Scanner Reads From Connected Blocks

- Vanilla containers: chests, barrels, hoppers, droppers, dispensers.
- Furnaces, blast furnaces, smokers: slots, recipe state, cook progress, fuel state.
- NeoForge item/fluid/energy capabilities.
- Modded machine progress through guarded reflection.
- Optional provider data for AE2, Mekanism, and Create when loaded.

Unknown block entities appear as `UNKNOWN_MACHINE` if they are connected but do not expose useful data.

### Warnings

Warnings are generated from scan and graph state: empty/full storage, missing furnace input/fuel, full
outputs, not-progressing machines, unlinked nodes, sources with no output, sinks with no input, and nearly
full link targets.

---

## Compatibility

- **JEI/REI/EMI:** panels are kept outside the computer GUI so they do not cover the editor or picker.
- **AE2 / Mekanism / Create:** optional scan providers add extra storage/tank/machine data when present.
- **CC:Tweaked:** optional peripheral hooks load only when CC:Tweaked is installed.
- **Hiding blocks:** add them to `doolestools:scanner_blacklist` or implement `ScannerHiddenBlock`.

---

## Building From Source

```powershell
.\gradlew.bat build       # compile + package -> build/libs/doolestools-<version>.jar
.\gradlew.bat runClient   # dev client
.\gradlew.bat runServer   # dedicated dev server (--nogui)
.\gradlew.bat test        # JUnit 5 unit suite
```

There is no lint/CI workflow in this checkout. `build` is the main correctness gate. The unit tests under
`src/test/java` cover pure logic that does not need a live `Level`.

---

## Project Layout

```text
net.doole.doolestools
  registry/     blocks, items, block entities, menus, creative tabs
  block/        computer, monitor, endpoint, wire, power blocks
  blockentity/  computer state, endpoint state, batteries, generators, monitors
  config/       server/client config flags and tuning
  logistics/    scanner, graph ops, warnings, port discovery, power, routing, planner, data records
  network/      payload records, codecs, validated server handlers
  client/       screens, widgets, editor state, renderers, client preferences
  integration/  optional mod-aware scan providers and CC:Tweaked hooks
  world/        saved labels and stable network/device identities
```

---

## FAQ

**Why does Scan Network not show my chest/machine?** It must be attached to a reachable modem, router,
dongle, or wire-hosted endpoint. Loose nearby blocks are not listed just because they are inside radius.

**Can it move items/fluids/energy?** Yes, when the server config allows Easy Factory and the relevant
route type. The graph must be saved before server automation uses it.

**Does it load chunks?** No. Unloaded chunks are skipped.

**How do I share a layout?** Use Settings -> Export Graph, then import the base64 string elsewhere.

**Why do planner estimates differ from throughput?** Throughput is actual server transfer history.
Planner output is an estimate from graph capacity and current scan snapshots.

**How do I remove upgrades?** Shift + right-click the endpoint or relay with the Network Screwdriver.
