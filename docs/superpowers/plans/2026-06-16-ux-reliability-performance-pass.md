# UX Reliability Performance Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish filter node UX, improve the item picker, fix merged chest transport, and reduce graph lag with low-risk render optimizations.

**Architecture:** Keep persisted graph/filter data unchanged. Make client-only UI changes in the existing computer screen and graph renderers, and make the server transport fix in `EasyFactoryManager` by resolving merged vanilla chest containers before generic container wrapping.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing `GuiGraphicsExtractor`, NeoForge transfer handlers, and Gradle build gate.

---

## Tasks

- [x] Add filter node summary/action pills on filter nodes.
- [x] Brighten the filter item picker overlay.
- [x] Add a Recent tab to the filter item picker with session-capped recent selections.
- [x] Fix item transport against merged vanilla chests.
- [x] Optimize graph rendering with viewport culling, per-render node lookup, reduced side pills, and lower link detail under load.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
