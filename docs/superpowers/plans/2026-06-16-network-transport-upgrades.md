# Network Transport Upgrades Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore reliable item transport across network graph routes and add basic router/modem upgrade-card installation.

**Architecture:** Keep routing server-authoritative inside `EasyFactoryManager`; endpoint upgrade counts are persisted on endpoint block entities and wire-hosted endpoint data. Upgrade cards are simple registered items with recipes/lang entries and small helper methods for applying capped limits.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing Value I/O persistence, existing Gradle build gate.

---

## Tasks

- [ ] Fix non-adjacent item transfer so saved network graph links use unsided handlers unless a side override is explicit.
- [ ] Add upgrade-card items, recipes, lang entries, and creative-tab inclusion.
- [ ] Persist upgrade counts on standalone and wire-hosted endpoints.
- [ ] Allow shift-right-click card installation on standalone endpoint blocks and wire-hosted endpoints.
- [ ] Apply stack/speed upgrade counts to item amount and route budget caps.
- [ ] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
