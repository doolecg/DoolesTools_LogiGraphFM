# LogiGraph Wiki

LogiGraph is a powered logistics network. A Logistics Computer sees blocks through connected endpoints,
not by sweeping every loose block around it. Build the network first, then scan it.

---

## Table Of Contents

1. [Getting Started](#getting-started)
2. [How Discovery Works](#how-discovery-works)
3. [Network Devices](#network-devices)
4. [Computer UI](#computer-ui)
5. [Power And Batteries](#power-and-batteries)
6. [Easy Factory Routing](#easy-factory-routing)
7. [Filters And Routing Nodes](#filters-and-routing-nodes)
8. [Throughput And Planner](#throughput-and-planner)
9. [Multi-Computer Mesh](#multi-computer-mesh)
10. [Config Reference](#config-reference)
11. [Compatibility](#compatibility)
12. [FAQ](#faq)

---

## Getting Started

1. Place a **Logistics Computer**.
2. Attach a **Network Modem**, **Wireless Router**, **Wireless Dongle**, or wire-hosted endpoint to each
   machine/storage block you want to manage.
3. Wire modems back to the computer, or keep wireless endpoints in computer/relay range.
4. Add **Network Generators** and **Network Batteries** if automation should run under real power.
5. Open the computer and press **Scan Network**.
6. Add scanned devices to the graph, draw item/fluid/energy links, and press **Save**.

If a machine does not appear, check its endpoint first. Range alone is not enough.

---

## How Discovery Works

The scanner starts from the computer's network id and looks for reachable network endpoints in loaded
chunks. It only reads the block attached to those endpoints.

Reachable sources:

- **Wired modem:** attached to a block and connected back to the computer through wire.
- **Wire-hosted endpoint:** a wire segment configured as a socket/router and attached to a block.
- **Wireless router/dongle:** attached to a block and in wireless range of the computer or a relay.
- **Relay path:** wireless endpoints can be discovered through reachable relays.
- **Peer mesh:** linked computers contribute their latest saved scan results.

Network ids filter visibility. Blank endpoint ids are unclaimed and visible to nearby computers. Assigned
endpoint ids must match the computer network.

The scanner skips unloaded chunks and catches per-block failures. Unknown but connected block entities may
show as `UNKNOWN_MACHINE` if no useful inventory/capability/provider data is available.

---

## Network Devices

| Device | Purpose |
|---|---|
| Logistics Computer | Controller, graph editor, scan store, power dashboard, automation host. |
| Network Wire | Wired backbone between computer, modems, batteries, generators, and endpoints. |
| Network Modem | Wired endpoint attached to a target block face. |
| Wireless Router | Wireless endpoint attached to a target block. |
| Wireless Dongle | Compact wireless endpoint variant. |
| Network Relay | Extends wireless reach across relay hops. |
| Network Generator | Burns fuel to produce FE for the network. |
| Network Battery | Buffers FE and smooths surplus/deficit. |
| Logistics Monitor | Single-block external display. |
| LogiGraph Wall Monitor | Multi-block external display. |

Sneak + right-click endpoints or relays to rename them, assign a network id, and view/install upgrades.

---

## Computer UI

### Graph Page

- **Scan Network:** rebuilds the connected-device list.
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

## Power And Batteries

The computer calculates demand from infrastructure, visible devices, active routes, and batteries. Supply
comes from generators, batteries, and compatible FE providers on the network.

Power state affects routing:

- Full satisfaction: routes run normally.
- Partial satisfaction: route budget throttles down.
- No usable power: automation pauses.

Batteries charge from surplus and discharge during deficits using transactional energy handlers.

---

## Easy Factory Routing

Easy Factory runs on the server from the saved graph. It supports item, fluid, and energy routes when the
corresponding config gates are enabled.

For each route tick:

1. The computer calculates power satisfaction.
2. Easy Factory builds a link index and walks graph routes.
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

Live throughput comes from actual server transfers. `EasyFactoryManager.tickWithCounts(...)` returns moved
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
| `easyFactory.enableEasyFactoryTransport` | `true` | Master automation gate. |
| `easyFactory.enableItemRoutes` | `true` | Enables item transfers. |
| `easyFactory.enableFluidRoutes` | `true` | Enables fluid transfers. |
| `easyFactory.enableEnergyRoutes` | `true` | Enables energy transfers. |
| `easyFactory.easyFactoryTickInterval` | `20` | Ticks between route passes. |
| `easyFactory.maxEasyFactoryRoutesPerTick` | `16` | Base route budget. |
| `easyFactory.maxItemsMovedPerRoute` | `16` | Base item move size. |
| `easyFactory.maxFluidMovedPerRoute` | `1000` | Base fluid move size. |
| `easyFactory.maxEnergyMovedPerRoute` | `1024` | Base FE move size. |
| `scan.scanRadius` | `16` | Loaded endpoint search radius. |
| `scan.autoScanIntervalTicks` | `0` | Scheduled scan interval; `0` disables it. |
| `scan.maxWireTraversalSteps` | `256` | Wired traversal cap. |
| `scan.redstoneAlertOnError` | `true` | Computer emits redstone strength 15 while ERROR warnings exist. |
| `networkPower.wirelessBaseRange` | `32` | Base wireless range. |
| `networkPower.wirelessMaxRange` | `128` | Wireless range cap. |
| `networkPower.batteryMaxIoPerTick` | `20000` | Battery charge/discharge cap. |

---

## Compatibility

- **AE2 / Mekanism / Create:** optional scan providers add data when those mods are loaded.
- **CC:Tweaked:** Logistics Computers expose a `logistics_computer` peripheral when present.
- **JEI / REI / EMI:** recipe panels are kept away from the computer GUI and filter picker.
- **Hidden pipes/cables:** use `doolestools:scanner_blacklist` or `ScannerHiddenBlock`.

---

## FAQ

**Why does Scan Network show nothing?** Connect a target block to a reachable endpoint first. Loose blocks
inside radius are not listed.

**Can it move resources?** Yes. The saved graph drives server-side item/fluid/energy transfers when config
allows the route type and the network has usable power.

**Does it load chunks?** No. Unloaded chunks are skipped.

**Do devices need network ids?** Blank ids are unclaimed and visible to nearby computers. Assigned ids must
match the computer network.

**Why do planner estimates differ from live throughput?** Throughput is actual server history. Planner
output is a snapshot estimate.
