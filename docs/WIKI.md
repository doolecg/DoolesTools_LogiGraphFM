# LogiGraph Wiki

LogiGraph is a powered logistics network. A Logistics Computer sees blocks through connected endpoints,
not by sweeping every loose block around it. Build the network first, then scan it.

> **Beta testing build:** current Switchboard, LFM routing, scan providers, and UI polish are in active
> testing. Use backup worlds and expect behavior to change while issues are found.

---

## Table Of Contents

1. [Getting Started](#getting-started)
2. [How Discovery Works](#how-discovery-works)
3. [Network Devices](#network-devices)
4. [Computer UI](#computer-ui)
5. [Network Switchboard](#network-switchboard)
6. [Power And Batteries](#power-and-batteries)
7. [Logi Factory Manager](#logi-factory-manager)
8. [Filters And Routing Nodes](#filters-and-routing-nodes)
9. [Throughput And Planner](#throughput-and-planner)
10. [Multi-Computer Mesh](#multi-computer-mesh)
11. [Config Reference](#config-reference)
12. [Compatibility](#compatibility)
13. [FAQ](#faq)

---

## Getting Started

1. Place a **Logistics Computer**.
2. For wired machines, attach a **Socket** to the machine/storage block and connect it with **Network Wire**.
3. For wireless machines, place one **Wireless Router** for the network and attach **Wireless Dongles** to
   the machine/storage blocks you want to manage.
4. Add **Extenders** when dongles are too far from the router.
5. Endpoints auto-join the only nearby computer network when possible. For quick manual assignment,
   shift-right-click a computer with a **Linking Card**, then shift-right-click the target endpoint.
6. Add **Network Generators** and **Network Batteries** if automation should run under real power.
7. Open the computer and press **Scan Network**.
8. Add scanned devices to the graph, draw item/fluid/energy links, and press **Save**.

If a machine does not appear, check its endpoint first. Range alone is not enough.

---

## How Discovery Works

The scanner starts from the computer's network id and looks for reachable network endpoints in loaded
chunks. It only reads the block attached to those endpoints.

Reachable sources:

- **Socket:** wired adapter attached to a block and connected back to the computer through Network Wire.
- **Wireless Dongle:** wireless adapter attached to a block and in range of the network router or an extender.
- **Router:** one-per-network wireless base. It anchors the wireless network but does not add an attached block as a scanned device.
- **Extender path:** wireless dongles can be discovered through reachable extenders.
- **Peer mesh:** linked computers contribute their latest saved scan results.

Network ids filter visibility. New routers, dongles, and sockets copy the one nearby loaded computer
network when the match is unambiguous. If more than one network is nearby, they stay blank until assigned
with a Linking Card or the naming screen. Blank endpoint ids stay isolated and do not leak into nearby computers.

The scanner skips unloaded chunks and catches per-block failures. Unknown but connected block entities may
show as `UNKNOWN_MACHINE` if no useful inventory/capability/provider data is available.

---

## Network Devices

| Device | Purpose |
|---|---|
| Logistics Computer | Controller, graph editor, scan store, power dashboard, automation host. |
| Network Wire | Wired backbone between the computer, sockets, batteries, generators, and wired devices. |
| Socket | Wired adapter attached to a target block face. |
| Wireless Router | One-per-network wireless base/anchor. Routers are infrastructure, not machine adapters. |
| Wireless Dongle | Wireless adapter attached to a target block face. |
| Extender | Extends wireless reach from the router toward farther dongles. |
| Network Generator | Burns fuel to produce FE for the network. |
| Network Battery | Buffers FE and smooths surplus/deficit. |
| Network Switchboard | Connects networks together with LFM permissions and priorities. |
| Linking Card | Copies a computer network id and applies it to routers, dongles, and sockets. |
| Logistics Monitor | Single-block external display. |
| LogiGraph Wall Monitor | Multi-block external display. |

Sneak + right-click sockets, dongles, routers, or extenders to rename them, assign a network id, and view/install upgrades.
Use the Linking Card for faster assignment: shift-right-click a Logistics Computer to copy, then shift-right-click an endpoint to paste.

---

## Computer UI

### Graph Page

- **Scan Network:** rebuilds the connected-device list.
- **Network tabs:** when Switchboards expose more networks, vertical tabs group scanned blocks by source network.
- **Node labels:** graph nodes use the device/block name plus id as the title and show the source network in
  small text at the bottom-right of the node card.
- **Refresh Data:** asks the server for current state without forcing a scan.
- **Save:** sends the graph to the server; automation uses saved graph data.
- **Tool palette:** add selected devices, filters, splitters, combiners, channels, frames, and text labels.

### Node Details

The right panel shows block contents, FE/fluid data, machine progress, warnings, connected links, and live
per-link throughput when the server has samples.

### Settings

- Display preferences are client-local.
- Graph export/import uses compact base64 strings on the clipboard.
- Network name, access mode, and whitelist are server-owned.

---

## Network Switchboard

The Network Switchboard is the explicit bridge between networks. Place it in loaded chunks, open it, then
use the LogiGraph-style canvas to arrange known networks as draggable nodes. Middle-drag pans, mouse wheel
zooms, and links between nodes store item/fluid/energy permissions plus priority.

Switchboard links are bidirectional. A Logistics Computer on any connected network can scan every network
reachable through loaded Switchboards. The computer UI groups those results by source network, and LFM only
routes across network boundaries when the Switchboard graph allows the resource type.

---

## Power And Batteries

The computer calculates demand from infrastructure, visible devices, active routes, and batteries. Supply
comes from generators, batteries, and compatible FE providers on the network.

Power state affects routing:

- Full satisfaction: routes run normally.
- Partial satisfaction: route budget throttles down.
- No usable power: automation pauses.

Batteries charge from surplus and discharge during deficits using transactional energy handlers.

---

## Logi Factory Manager

Logi Factory Manager, or **LFM**, runs on the server from the saved graph. It supports item, fluid, and
energy routes when the corresponding config gates are enabled.

For each route tick:

1. The computer calculates power satisfaction.
2. LFM builds a link index and walks graph routes.
3. Transfers simulate extract/insert in a transaction.
4. Successful transfers commit and report moved counts per link.
5. Clients receive active route ids and throughput samples.

Side overrides and machine port roles matter. A source output port and target input port should match the
resource type you want to move.

---

## Filters And Routing Nodes

- **Filter:** matches configured item ids/tags/channels. Has normal and reject outputs.
- **Splitter:** distributes across outputs.
- **Combine:** merges multiple inputs into one route path.
- **Channel:** stamps a channel so downstream filters can branch.

Click a Filter node's 3x3 ghost grid to open the searchable item picker. The picker remembers recent items
and keeps JEI/REI/EMI panels outside the modal.

---

## Throughput And Planner

Live throughput comes from actual server transfers. `LogiFactoryManager.tickWithCounts(...)` returns moved
amounts per link; the computer stores rolling samples and syncs them in `ComputerStatePayload`.

The planner is an estimate. It compares configured route capacity against scan-derived machine progress and
storage state to flag likely bottlenecks or starved sources.

---

## Multi-Computer Mesh

Computers can link peers by network id. A scan merges the local reachable endpoint results with each peer's
latest saved scan. Mesh links do not force-load chunks and do not copy or mutate the peer graph.

---

## Config Reference

Current defaults are from `ModServerConfig`, not old README prose.

| Key | Default | Description |
|---|---:|---|
| `lfm.enableLfmTransport` | `true` | Master automation gate. |
| `lfm.enableItemRoutes` | `true` | Enables item transfers. |
| `lfm.enableFluidRoutes` | `true` | Enables fluid transfers. |
| `lfm.enableEnergyRoutes` | `true` | Enables energy transfers. |
| `lfm.lfmTickInterval` | `20` | Ticks between route passes. |
| `lfm.maxLfmRoutesPerTick` | `16` | Base route budget. |
| `lfm.maxItemsMovedPerRoute` | `16` | Base item move size. |
| `lfm.maxFluidMovedPerRoute` | `1000` | Base fluid move size. |
| `lfm.maxEnergyMovedPerRoute` | `1024` | Base FE move size. |
| `scan.scanRadius` | `16` | Loaded device search radius around the computer and extenders. |
| `scan.autoScanIntervalTicks` | `0` | Scheduled scan interval; `0` disables it. |
| `scan.maxWireTraversalSteps` | `256` | Wired traversal cap. |
| `scan.redstoneAlertOnError` | `true` | Computer emits redstone strength 15 while ERROR warnings exist. |
| `networkPower.wirelessBaseRange` | `32` | Base wireless range. |
| `networkPower.wirelessMaxRange` | `128` | Wireless range cap. |
| `networkPower.batteryMaxIoPerTick` | `20000` | Battery charge/discharge cap. |

---

## Compatibility

- **AE2 / Refined Storage / Storage Drawers / Mekanism / Create:** optional scan providers add data when those mods are loaded.
- **CC:Tweaked:** Logistics Computers expose a `logistics_computer` peripheral when present.
- **JEI / REI / EMI:** recipe panels are kept away from the computer GUI and filter picker.
- **Hidden pipes/cables:** use `doolestools:scanner_blacklist` or `ScannerHiddenBlock`.

---

## FAQ

**Why does Scan Network show nothing?** Connect a target block to a reachable Socket or Wireless Dongle
first. Loose blocks inside radius are not listed, and routers do not add machines by themselves.

**Can it move resources?** Yes. The saved graph drives server-side item/fluid/energy transfers when config
allows the route type and the network has usable power.

**Does it load chunks?** No. Unloaded chunks are skipped.

**Do devices need network ids?** Yes. Assign sockets, dongles, routers, and extenders to the intended
network. They auto-join only when exactly one nearby computer network is found; otherwise use a Linking Card
or naming screen. Blank ids stay isolated and do not make devices visible to nearby computers.

**Why do planner estimates differ from live throughput?** Throughput is actual server history. Planner
output is a snapshot estimate.
