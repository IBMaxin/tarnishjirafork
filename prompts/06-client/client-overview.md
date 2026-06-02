# Client-Side Structural Overview

**Goal:** Map the client architecture — entry point, key classes, packet flow, and trust boundary with the server.

**Docs:** `AGENTS.md`, `code_index.json`, `prompts/00-cross-cutting/client-server-boundary.md`, `prompts/06-client/client-security.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-client/src/main/java/com/osroyale/Client.java` | 787KB monolith — 27,000+ lines, the entire client |
| `game-client/src/main/java/com/osroyale/BufferedConnection.java` | Raw TCP socket (no TLS) |
| `game-client/src/main/java/com/osroyale/AccountManager.java` | Dead code — entire file commented out |
| `game-client/src/main/java/com/osroyale/Configuration.java` | Client config constants |
| `game-client/src/main/java/net/runelite/client/` | RuneLite client integration — 400+ files |
| `game-client/src/main/java/com/osroyale/Js5.java` | JS5 cache protocol client |
| `game-client/src/main/java/com/osroyale/Game.java` | Game applet/entry point |
| `game-client/src/main/java/com/osroyale/Signlink.java` | Signlink — native platform integration |

---

## Architecture

```
Client
├── Entry: Game.java (applet) → Client.java (27K+ lines)
├── Network: BufferedConnection.java (raw TCP, circular buffer)
├── Cache: Js5.java + Signlink.java
├── Rendering: Software rasterizer (no GPU)
├── Audio: Signlink MIDI/audio
└── Input: Keyboard + Mouse listeners in Client.java

Trust boundary:
  Client                     Server
  ──────                     ──────
  Sends raw bytes    ──→    Parses opcode + payload
  Claims item IDs    ──→    Must validate against definitions
  Claims amounts     ──→    Must validate amount > 0
  Claims position    ──→    Must verify clipping/walkability
  Claims actions     ──→    Must verify preconditions
```

---

## Steps

### 1. Size the client

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Client.java size
wc -l game-client/src/main/java/com/osroyale/Client.java

# Total client source files
find game-client/src/main/java/ -name "*.java" | wc -l

# Client size breakdown by package
find game-client/src/main/java/ -name "*.java" | \
  sed 's|/[^/]*$||' | sort | uniq -c | sort -rn | head -15
```

### 2. Map packet opcodes

```bash
# Find packet opcode constants in client
grep -n "PACKET_SIZES\|packetSizes\|opcode\|OPCODE\|OPCODE_" \
  game-client/src/main/java/com/osroyale/Client.java | head -30

# Count distinct packet handlers
grep -c "case " game-client/src/main/java/com/osroyale/Client.java
```

### 3. Find server connection details

```bash
# How does the client connect? What IP/port?
grep -n "socket\|connect\|43594\|server\|address\|host" \
  game-client/src/main/java/com/osroyale/Client.java | head -20

# BufferedConnection setup
cat game-client/src/main/java/com/osroyale/BufferedConnection.java
```

### 4. Check RuneLite integration

```bash
# RuneLite client files — modern OSRS client framework integrated with 317 base
find game-client/src/main/java/net/runelite/ -type f -name "*.java" | head -20

# What RuneLite plugins are included?
ls game-client/src/main/java/net/runelite/client/plugins/ | head -20
```

### 5. Check dead code

```bash
# AccountManager.java — entirely commented out
head -5 game-client/src/main/java/com/osroyale/AccountManager.java
# Output shows: /* package com.osroyale; ... entire file is block comment */

# Any other dead client code?
grep -rn "deprecated\|TODO\|FIXME\|HACK\|XXX" \
  game-client/src/main/java/com/osroyale/Client.java | head -20
```

### 6. Check client config

```bash
# What's configurable in the client?
cat game-client/src/main/java/com/osroyale/Configuration.java
```

---

## Key Facts

| Fact | Value |
|------|-------|
| Client type | 317 deob (pre-OSRS, 2006 era) |
| RuneLite integration | Yes — modern plugin framework grafted onto 317 base |
| Rendering | Software rasterizer, no GPU acceleration |
| Networking | Raw TCP, no TLS, RSA at application layer only |
| Cache format | JS5 (Jagex Store 5) |
| JDK version | JDK 11 |
| Main class size | ~27,000 lines (787KB) in single Client.java |
| Total client files | ~1,200 Java files |
| AccountManager | Dead code — fully commented out |

---

## Architecture Notes

### BufferedConnection — the network layer

- 5KB circular byte buffer for outgoing data
- Separate writer thread (priority 3)
- 30-second socket timeout
- Nagle's algorithm disabled (`setTcpNoDelay(true)`)
- No TLS/SSL — encryption at RSA/ISAAC level only
- Traffic class: `0b101_110_00` (DSCP EF + ECN capable)

### Client.java — the monolith

All rendering, input handling, packet parse/send, game logic, UI, and model loading lives in a single 27K-line file. This is standard for 317-era RSPS clients — they were decompiled from the original RuneScape gamepack and maintained as monoliths.

### RuneLite Integration

The client includes RuneLite (`net.runelite.client.*`) — a modern OSRS third-party client framework. This provides:
- Plugin system (Discord rich presence, loot tracker, XP tracker, etc.)
- HTTP API clients (Grand Exchange, hiscores, item data, XP tracking)
- WebSocket client for real-time data
- Modern config system
- External plugin loading

### AccountManager (DEAD)

`AccountManager.java` is 162 lines, **entirely inside a `/* */` block comment**. This was the old client-side account storage (local profile management) that has been removed. Account management is now handled by the RuneLite integration or server-side only.

---

## Client Impact

This is the client overview — no changes proposed. Use this as reference for understanding what the client CAN and CANNOT do when writing server-side code.

---

## Verify

- [ ] Understand that Client.java is 27K lines in a single file
- [ ] Understand that AccountManager.java is dead code
- [ ] Understand that BufferedConnection has no TLS
- [ ] Understand that RuneLite provides HTTP/WS clients that could be attack vectors
- [ ] Know that ALL validation must happen server-side — the client is untrusted
