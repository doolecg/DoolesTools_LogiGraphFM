# Filter Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace text-only filter editing with structured ghost-item controls, channel/priority/limit/tick/routing controls, and matching transport behavior.

**Architecture:** Store filter settings in `GraphNodeData.notes` using a compact key/value format to avoid a graph codec migration. Add shared parsing helpers, render/edit the structured fields in the existing right-side graph panel, and make `EasyFactoryManager` consume the same parsed model.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing graph codecs and Gradle build gate.

---

## Tasks

- [x] Add a shared `FilterSettings` parser/serializer for notes.
- [x] Replace visible text rule editing with structured filter controls on selected filter nodes.
- [x] Add 3x3 ghost item slot editing from the carried cursor stack.
- [x] Apply exact ghost-item matching, mode, channel, priority, limit, tick speed, and round-robin behavior in `EasyFactoryManager`.
- [x] Update filter details text.
- [x] Run `./gradlew.bat compileJava` and `./gradlew.bat build`.
- [ ] Commit and push.
