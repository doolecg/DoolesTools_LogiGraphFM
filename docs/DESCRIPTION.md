# LogiGraph - Platform Description

> **Beta testing build:** this version is intended for testing active LogiGraph/LFM/Switchboard work. Back up worlds before testing.

## Short

> Build a powered logistics network, connect your machines, and drive a live factory graph from it.
> Scan connected endpoints, watch power and throughput, then let Logi Factory Manager move resources along the graph.

---

## Full Description

**LogiGraph** is a network-first logistics and factory automation mod for NeoForge. Place a Logistics
Computer, connect wired machines with Sockets and Network Wire, or connect wireless machines with Dongles
through a network Router and optional Extenders. Connected blocks appear in a node-based editor where you
can inspect contents, machine progress, warnings, power demand, and live throughput.

Endpoint setup is connection-first but faster than typing ids: new endpoints auto-join the one nearby
computer network when unambiguous, and the Linking Card copies a computer network id to routers, dongles,
and sockets with shift-right-click.

The computer is not an old-style area scanner: loose nearby blocks do not appear just because they are in
range. A block must be attached to a reachable endpoint, or come from a linked peer computer's saved scan,
before it shows in the graph.

Logi Factory Manager, or LFM, uses the saved graph as a routing table. Item, fluid, and energy links can move resources
server-side when config allows them. Transfers are transactional, power-aware, and tracked per link so the
UI can show real moved-per-minute history.

---

## What It Does

- **Connected-device scanning** — scans machines/storage attached to reachable Sockets or Wireless Dongles.
- **Network graph editor** — drag devices onto a graph, connect typed ports, group with frames, and save layouts.
- **Power network** — wires, sockets, routers, extenders, dongles, generators, and batteries drive route budgets.
- **Logi Factory Manager automation** — item/fluid/energy routes move resources through saved graph links.
- **Network Switchboard** — connects networks together with per-resource permissions and routing priorities.
- **Linking Card** — copies/pastes network ids for quick router/dongle/socket setup.
- **Routing logic** — Filter, Splitter, Combine, and Channel nodes shape item paths.
- **Throughput stats** — server-owned moved counts are synced to node details and the stats dashboard.
- **Planner overlay** — estimates bottlenecks and starvation from route capacity and current machine progress.
- **Graph blueprints** — export/import compact base64 graph strings between worlds.
- **Network identity** — computers and devices have stable ids, names, access modes, and upgrade counts.
- **External displays** — single-block and wall monitors mirror computer state.
- **Integrations** — optional AE2, Refined Storage, Storage Drawers, Mekanism, Create scan providers and CC:Tweaked peripheral hooks.

---

## What It Does Not Do

- Show arbitrary nearby chests/machines unless they are connected to the network.
- Force-load unloaded chunks during scanning, power calculation, or peer mesh merging.
- Trust client UI data for automation; server config and server handlers decide what can mutate the world.

---

## Requirements

- Minecraft 26.1.2
- NeoForge 26.1.2.76+
- Java 25
