# Power Dashboard History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static power page with a Factorio-style production/consumption history dashboard.

**Architecture:** Keep power calculation server-authoritative. Sample recent supply/demand values in the Logistics Computer block entity, sync them through the existing computer-state payload, and render a graph plus current bars on the client power page.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing `NetworkPowerData`, `ComputerStatePayload`, `GuiGraphicsExtractor`, and Gradle build gate.

---

## Tasks

- [x] Add runtime power history buffers to `LogisticsComputerBlockEntity`.
- [x] Sync power history through `ComputerStatePayload` and `EditorContext`.
- [x] Add stream codec support for integer lists.
- [x] Rework the Power page into a production/consumption graph with current supply/demand bars and top consumers.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
