# Filter Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make selected filter nodes easier to debug by showing route counts, configuration status, and actionable warnings.

**Architecture:** Add a small pure diagnostics helper for `FilterSettings` and graph links, then render its result in `NodeDetailsPanel`. Keep this client-side/read-only and avoid graph codec changes.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing graph data records and Gradle build gate.

---

## Tasks

- [x] Add a focused filter diagnostics helper under `logistics`.
- [x] Render inbound/outbound item route counts and selected rule status in `NodeDetailsPanel`.
- [x] Render warnings for missing inbound item routes, missing outbound item routes, and empty whitelist ghost grids.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
