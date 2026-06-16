# Filter Item Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let players click a filter ghost-grid slot and choose an item/block from a searchable in-game registry list.

**Architecture:** Add a client-only modal overlay inside `LogisticsComputerScreen`. The picker builds a cached list from the item registry, filters by display name or registry ID, and writes the selected registry ID through the existing `FilterSettings` notes serialization.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing `GuiGraphicsExtractor`, `EditBox`, `ItemIcons`, and Gradle build gate.

---

## Tasks

- [x] Add picker state and cached item-list model to `LogisticsComputerScreen`.
- [x] Open the picker when a filter ghost slot is clicked instead of requiring the carried stack.
- [x] Render searchable picker overlay with item icons, names, registry IDs, and a clear action.
- [x] Wire search typing, escape close, mouse wheel scrolling, and row selection.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
