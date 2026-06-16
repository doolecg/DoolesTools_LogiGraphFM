# LogiGraph Future Fixes And Refinement

## Current Direction

LogiGraph is now a networked factory manager built around visible routers, modems, network cable, a powered computer, Easy Factory routing, and a fuel generator. The next work should focus on making the system reliable and understandable in survival play before adding larger new systems.

## Immediate Bug-Fix Priorities

- Verify filter routing in-game with a saved graph: source storage -> filter -> target storage.
- Make filter configuration obvious in the UI, including whether graph changes must be saved before server transport uses them.
- Verify modem/router renaming for both standalone endpoint blocks and wire-hosted endpoints.
- Verify endpoint names update in The Network after scan/sync refresh.
- Verify generator UI shift-click fuel insertion, manual fuel slot insertion, burn progress, and FE extraction by a nearby computer.
- Verify underpowered networks pause Easy Factory transport and recover when generator energy is available.

## Milestone 1: Filter Reliability

Goal: filters should feel deterministic and easy to debug.

Tasks:
- Add a visible filter status line showing pass/block examples for the selected filter rule.
- Add a small help panel for filter syntax: `iron`, `minecraft:iron_ingot`, `blacklist:`, `limit=16`, `channel=ore`, `rr`.
- Add warnings when a filter has no inbound item route or no outbound item route.
- Consider server-side diagnostics for skipped routes, guarded behind a config flag.
- Keep filter logic side-effect-free except actual permitted transport.

Acceptance:
- A player can tell why a filter is not moving items without reading code.
- Bad filter syntax does not break all transport.

## Milestone 2: Endpoint Naming Polish

Goal: every endpoint should be nameable and have a sensible default.

Tasks:
- Add a server-confirmed rename response or immediate client refresh for endpoint name screens.
- Show the attached block name, endpoint kind, and position in the rename UI.
- Add a “Reset to attached block name” action separate from blank custom names.
- Confirm names are stable after world reload.
- Confirm duplicate endpoint names produce stable unique IDs or a clear warning.

Acceptance:
- Shift-right-click always opens the rename UI for standalone and wire-hosted endpoints.
- The Network shows the expected name after the next sync/scan.

## Milestone 3: Generator UI Refinement

Goal: the generator should feel like a complete machine.

Tasks:
- Add clearer empty/fuel/full status messages.
- Add tooltip text for energy and burn bars.
- Add a comparator output or visual blockstate if desired.
- Add recipe progression balancing after survival testing.
- Consider side-specific item insertion only after the current single-slot behavior is confirmed.

Acceptance:
- Fuel can be inserted by hand, through the UI, and through item capabilities.
- Energy can be consumed by the network without manual intervention.

## Milestone 4: In-Game Test Matrix

Run these scenarios before larger features:

- Fresh world: craft computer, cable, modem, router, generator.
- Place generator adjacent to computer, insert coal, confirm FE supply rises.
- Attach modem/router to chest via cable, confirm The Network shows chest by default block name.
- Rename modem/router, rescan/sync, confirm custom name persists.
- Build source chest -> filter -> target chest graph, save, confirm matching items move only.
- Test blacklist filter, amount limit, and round-robin route order.
- Break wire-hosted endpoint and confirm endpoint item drops without destroying cable.
- Dedicated server run: confirm client-only screens/classes do not classload on server.

## Later Refinement

- Add better endpoint part hit detection if players still struggle to break only the router/modem plate.
- Add graph-side validation warnings for invalid Easy Factory routes.
- Add more explicit power usage categories and top consumers.
- Add datagen for recipes/models/loot to reduce hand-written resource drift.
- Add GameTests or unit-testable pure helpers for filter parsing and route eligibility.

## Constraints To Preserve

- Do not reintroduce removed overclock/soul/conduit/custom machine content.
- Keep LogiGraph diagnostics safe: unreadable blocks should not abort scans.
- Easy Factory may transport items/fluids/energy, but it must respect slot/side semantics.
- Optional mod API references are allowed internally, but avoid player-facing dependency name noise unless it is an integration setting or explicit compatibility page.
