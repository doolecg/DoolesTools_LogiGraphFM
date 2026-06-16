# Network Identity Permissions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give computers and endpoints stable network/device identities, assign endpoints to computer networks, and enforce multiplayer edit permissions server-side.

**Architecture:** Persist network identity and access settings on each Logistics Computer, and persist endpoint network assignment on standalone and wire-hosted endpoints. Extend existing computer-state sync and endpoint-rename payloads instead of adding a separate management UI stack in this slice.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, Value I/O persistence, CustomPacketPayload networking, existing Gradle build gate.

---

## Tasks

- [x] Add computer network identity, owner, access mode, whitelist persistence, and edit permission helper.
- [x] Add endpoint assigned network persistence and stable unique device IDs.
- [x] Filter scans so endpoints only appear on their assigned/current computer network.
- [x] Extend computer state sync with network settings and edit capability.
- [x] Extend endpoint identity payload to save nickname and network assignment.
- [x] Add basic computer settings controls for access mode and network assignment visibility.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
