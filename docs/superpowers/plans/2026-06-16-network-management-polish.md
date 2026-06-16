# Network Management Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add server-known network assignment, whitelist editing, and clearer endpoint/relay status.

**Architecture:** Add request/response payloads for known networks and reuse the endpoint identity screen for routers, modems, wire-hosted endpoints, and relays. Keep permission storage unchanged and server-authoritative.

**Tech Stack:** Java 25, Minecraft 26.1.2, NeoForge 26.1.x, existing custom payload networking.

---

## Files

- Create: `src/main/java/net/doole/doolestools/network/payload/RequestKnownNetworksPayload.java`
- Create: `src/main/java/net/doole/doolestools/network/payload/KnownNetworksPayload.java`
- Modify: `src/main/java/net/doole/doolestools/network/ModNetworking.java`
- Modify: `src/main/java/net/doole/doolestools/network/ServerPayloadHandlers.java`
- Modify: `src/main/java/net/doole/doolestools/client/ClientPayloadHandlers.java`
- Modify: `src/main/java/net/doole/doolestools/client/ClientKnownNetworks.java`
- Modify: `src/main/java/net/doole/doolestools/client/ClientNetworkSender.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/NetworkEndpointNameScreen.java`
- Modify: `src/main/java/net/doole/doolestools/client/screen/LogisticsComputerScreen.java`
- Modify: `src/main/java/net/doole/doolestools/block/NetworkRelayBlock.java`
- Modify: `src/main/java/net/doole/doolestools/blockentity/NetworkRelayBlockEntity.java`

## Tasks

- [x] Add known-network request/response payload records and register them.
- [x] Add server known-network gathering and send payloads during request/computer sync.
- [x] Update client network cache to store server-fed entries with editability.
- [x] Update endpoint identity screen to request/refresh server networks and show status.
- [x] Allow relay identity/network assignment through the same payload and screen.
- [x] Add compact whitelist editing controls to computer settings.
- [x] Run `./gradlew.bat test`.
- [x] Run `./gradlew.bat compileJava`.
- [x] Run `./gradlew.bat build`.
- [x] Review `git diff --stat` and update this plan's checkboxes.
