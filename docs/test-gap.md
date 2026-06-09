# Test Coverage Gap Analysis

> **Generated:** 2026-06-08
> **Source:** Audit of 24 test files (114 @Test methods) against ~1,100+ source files + ~130 plugin files.
> **Estimated coverage:** < 5%

---

## Current Coverage Profile

| Metric | Value |
|--------|-------|
| Test files | 24 (20 server + 4 test-automation) |
| @Test methods | 114 |
| Source files (game-server) | ~1,100+ Java |
| Plugin files | ~130+ Java/Kotlin |
| Packages with any tests | ~10 of ~100+ |

### What IS Tested Well

| Area | Test Files | Strength |
|------|-----------|----------|
| **Data integrity** (JSON parseability, cross-references, file counts) | 6 files | Strong — catches corrupted/missing data files |
| **Plugin event dispatch routing** (14 event types) | `PluginContextDispatchTest` | Good — verifies routing logic |
| **Combat hit value objects** | `HitTest`, `CombatHitTest` | Adequate — value object behavior |
| **Player rights lookup** | `PlayerRightTest`, `ProfileRightsTest` | Good — covers lookup, admin/donator checks |
| **Command identity** | `CommandTest` | Adequate — equals/hashCode |
| **Slayer bounds checking** | `SlayerTest` | Minimal — store slot bounds only |
| **Shop data integrity** | `FeatureShopIntegrityTest`, `DonatorFeatureIntegrityTest` | Good — validates all 36 shops |
| **Parser smoke tests** (10 parsers) | `ParserSmokeTest`, `NpcFileLoaderTest` | Good — parsers load without errors |
| **NPC spawn save/load** | `NpcSpawnSaveTest` | Adequate — JSON format round-trip |

---

## Top 5 Critical Gaps

### #1 — Combat Formulas & Strategies

**Package:** `game.world.entity.combat.*` (~40 files)
**Coverage:** Only `Hit`/`CombatHit` value objects tested.
**Untested:**
- Accuracy calculations (melee, ranged, magic)
- Max hit formulas
- Damage strategies (melee, ranged, magic, special attacks)
- Weapon special attack logic
- Prayer effects on combat
- Combat effects (poison, venom, freeze, etc.)
- Projectile definitions and behavior

**Risk:** **CRITICAL** — Silent gameplay-breaking damage errors. Wrong accuracy, broken special attacks, incorrect prayer interactions.

---

### #2 — Player Entity State Management

**Package:** `game.world.entity.mob.player.*` (~40 files)
**Coverage:** Only `PlayerRight` tested.
**Untested:**
- Player serialization/deserialization (persist package)
- Inventory, bank, equipment container operations
- Trading and dueling logic
- Appearance and animations
- Relations (friends, ignores)
- Skill management and level-up
- Movement and teleportation
- Private messaging

**Risk:** **CRITICAL** — Data loss on save/load, item duplication or deletion, trading exploits.

---

### #3 — NPC System & Boss AI

**Package:** `game.world.entity.mob.npc.*` (~60 files)
**Coverage:** Data files validated (cross-references, parseability). Zero behavioral tests.
**Untested:**
- 18 boss strategy packages (Cerberus, Zulrah, Kraken, Vorkath, etc.)
- Godwars NPC AI and room mechanics
- NPC combat strategies and attack patterns
- Drop distribution logic (NpcDropManager)
- NPC aggression and respawn
- Pet following and interaction

**Risk:** **CRITICAL** — Bosses may be unkillable or trivial. Drop tables may distribute incorrectly.

---

### #4 — Network Packet Handling

**Packages:**
- `net.packet.in.*` (29 files) — incoming packet handlers
- `net.packet.out.*` (83 files) — outgoing packet builders
- `net.codec.*` (8 files) — login/game codec

**Coverage:** Zero tests across all 120+ files.
**Untested:**
- All 29 incoming packet handlers (movement, commands, interactions, etc.)
- All 83 outgoing packet builders
- Login handshake and RSA authentication
- Game packet encoding/decoding
- Session lifecycle management

**Risk:** **CRITICAL** — Desyncs, crashes, security exploits from malformed packets. Login/auth bugs prevent connection entirely.

---

### #5 — Skills (17 of 18)

**Package:** `content.skill.impl.*` (~80 files across 18 skill packages)
**Coverage:** Only Slayer has minimal bounds-checking tests.
**Untested skills:**
- Agility, Construction, Cooking, Crafting, Farming
- Firemaking, Fishing, Fletching, Herblore, Hunter
- Magic, Mining, Prayer, Runecrafting, Smithing
- Thieving, Woodcutting

**Risk:** **HIGH** — Each skill involves complex state machines, resource consumption, level-up logic, and item production. Bugs cause: items not created, wrong XP rates, skill progression breaking, infinite resource exploits.

---

## Secondary Gaps

### Plugin Click Handlers (~75+ files)

| Package | Files | Risk |
|---------|-------|------|
| `plugins/plugin/click/button/` | 37 | HIGH — all button interactions untested |
| `plugins/plugin/click/item/` | 25 | HIGH — all item click interactions untested |
| `plugins/plugin/click/object/` | 13 | HIGH — all object click interactions untested |
| `plugins/plugin/click/npc/` | 5 | HIGH — all NPC click interactions untested |
| `plugins/plugin/command/` | 8 | HIGH — only Command class tested, not implementations |

### Activities & Minigames (~40+ files)

| Activity | Files | Risk |
|----------|-------|------|
| Barrows | ~5 | HIGH |
| Fight Caves | ~5 | HIGH |
| Godwars | ~8 | HIGH |
| Zulrah | ~5 | HIGH |
| Wintertodt | ~15 | HIGH |
| Pest Control | ~5 | HIGH |
| Kraken | ~3 | HIGH |
| Duel Arena | ~3 | HIGH |
| Last Man Standing | ~8 | HIGH |
| Warrior Guild | ~3 | MEDIUM |
| Mage Arena | ~2 | MEDIUM |

### Game Engine Core

| Package | Files | Risk |
|---------|-------|------|
| `game/action/` (action system, policies) | ~20 | HIGH |
| `game/event/` (event bus, listeners) | ~15 | HIGH |
| `game/engine/` (game loop/tick) | 1 | HIGH |
| `game/task/` (task scheduling) | 25 | MEDIUM |
| `game/world/items/` (Item, containers) | ~15 | HIGH |
| `game/world/region/` | 4 | MEDIUM |
| `game/world/position/` | 3 | MEDIUM |
| `game/world/pathfinding/` | 2 | MEDIUM |

### Content Systems

| System | Files | Risk |
|--------|-------|------|
| Teleport destinations | 3 | HIGH |
| Trading post | 4 | HIGH |
| Clan channels | ~15 | MEDIUM |
| Achievement system | 5 | MEDIUM |
| Collection log | 7 | LOW |
| Mystery boxes | 5 | LOW |
| Presets | 2 | LOW |
| Donator system | 2 | MEDIUM |
| Dialogue system | 11 | MEDIUM |
| Item consumption | 2 | MEDIUM |
| Item-on-item/NPC/object/player handlers | ~17 | MEDIUM |

---

## Summary

| Category | Coverage |
|----------|----------|
| **Data integrity** | ✅ Strong (6 test files) |
| **Plugin dispatch routing** | ✅ Good (1 test file) |
| **Value objects** (Hit, PlayerRight, Command) | ✅ Adequate (4 test files) |
| **Parser smoke tests** | ✅ Good (2 test files) |
| **Shop/slayer data integrity** | ✅ Adequate (3 test files) |
| **Combat formulas & strategies** | ❌ Zero |
| **Player state management** | ❌ Zero |
| **NPC AI & boss behavior** | ❌ Zero |
| **Network packets** (120+ files) | ❌ Zero |
| **Skills** (17 of 18) | ❌ Zero |
| **Plugin click handlers** (75+ files) | ❌ Zero |
| **Activities & minigames** (40+ files) | ❌ Zero |
| **Game engine core** (action, event, task) | ❌ Zero |

The codebase has **strong data integrity testing** but **extremely weak behavioral testing**. The vast majority of game logic — combat, skills, activities, NPC AI, player management, networking, and plugin behavior — has zero test coverage.