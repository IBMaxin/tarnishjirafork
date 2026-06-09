# Offline Test Plan

This plan catalogs what offline tests exist and what still needs to be built. The goal is to catch broken data, broken startup loaders, bad config drift, profile/rights mistakes, and client launch prerequisites before doing manual in-game smoke testing.

→ **Single source of truth for new work:** [improvement-plan.md](improvement-plan.md)
→ **Game scope:** [game-scope.md](game-scope.md)
→ **Knowledge bank:** [knowledge-bank.md](knowledge-bank.md)
→ **Prompt pack:** [../prompts/README.md](../prompts/README.md) — 35 audit prompts
→ **Workflows:** [workflows/README.md](workflows/README.md)

## What Offline Tests Can Prove

Offline tests can prove:

- The server and client compile.
- Core JSON files parse.
- Server startup loaders can read cache/data/plugin definitions.
- Seeded profiles have expected rights.
- Local client cache files are present.
- Config values are sane for local development.
- Cross-reference data is probably usable, such as stores pointing at known item ids.

Offline tests cannot fully prove:

- Login works end to end.
- A player can move, teleport, eat, fight, trade, or use a shop.
- Minigames complete correctly.
- Interfaces render correctly.
- Runtime permissions behave correctly after packets arrive from the client.

## Current Baseline (2026-06-08)

**17 test files, 106 tests, 0 failures, 9% JaCoCo coverage on 1,192 source files.**
**Plus E2E test-automation module:** 3 E2E tests (gated behind `-Pe2e`), 3 test source files (GameClient, BotPlayer, E2ETest, TeleportE2ETest).

Existing tests:

| Test File | What It Covers | Status |
|-----------|---------------|--------|
| `RequiredDataFilesTest` | Cache files, def JSONs, skill data, profile list present | ✅ |
| `DataJsonParseTest` | Every `.json` under `data/` parses without error | ✅ |
| `ParserSmokeTest` | Core parsers (ItemDef, NpcDef, Store, CombatProjectile, etc.) load without error | ✅ |
| `CrossReferenceTest` | Store items, NPC spawns, NPC drops cross-reference valid IDs | ✅ |
| `NpcFileLoaderTest` | `NpcDropFileLoader` and `NpcSpawnFileLoader` smoke test | ✅ |
| `NpcSpawnSaveTest` | Spawn file round-trip (write then read) | ✅ |
| `ProfileRightsTest` | Profile rights validation (migrated to JUnit 5) | ✅ |
| `ReconciliationTest` | Gap map for items/NPCs/drops/spawns between old and new systems | ✅ |
| `AraxxorDataTest` | Araxxor-specific data integrity (updated for per-file JSONs) | ✅ |
| `HitTest` | `modifyDamage()`, `setAs()`, `isAccurate()`, `setAccurate()` | ✅ |
| `CombatHitTest` | `copyAndModify()`, getter values, multi-hit construction | ✅ |
| `PlayerRightTest` | `isAdministrator()`, `isDonator()`, enum from-id lookup | ✅ |
| `CommandTest` | Command equals, name hashing, precedence ordering | ✅ |
| `PluginDiscoveryTest` | `PluginManager.load("plugin")` loads ≥100 plugins | ✅ |
| `PluginContextDispatchTest` | 31 event-type routing tests, full subtype coverage | ✅ |
| `CommandExtensionErrorLoggingTest` | Command error logging (basic) | ⚠️ Needs expansion |
| `SlayerTest` | Slayer shop slot guard + totalPoints accumulation | ✅ |

Server boot logs show:
- `Startup service finished`
- `Loaded: 133 plugins`
- `Server built successfully`
- `Loaded 3436 NPC spawns` and `Loaded 1780 NPC drop tables` (per-file system)

---

## What Still Needs Tests

For the detailed implementation plan, see **[improvement-plan.md](improvement-plan.md)** §Testing Phases. Summary:

### High Priority (Phase F — Feature Integrity Tests)

| Test | What It Catches |
|------|----------------|
| `FeatureShopIntegrityTest` | Invalid shop items/prices, dead references, missing currencies |
| `DonatorFeatureIntegrityTest` | Donator shop/reward wiring breakage |
| `NpcSpawnFeatureIntegrityTest` | Broken spawn positions, orphaned NPC ids |
| `NpcDropFeatureIntegrityTest` | Orphaned drop items, invalid drop types |
| `CommandFeatureIntegrityTest` | Missing expected commands, registration drift |
| `CommandAccessIntegrityTest` | Dangerous commands in wrong rank plugins |
| `TeleportFeatureIntegrityTest` | Bad teleport destinations, broken unlock checks |

### Medium Priority (Phase G — Live Hardening)

| Test | What It Catches |
|------|----------------|
| `LogEvent` forum decoupling test | Logs suppressed when `LOG_PLAYER=false` |
| `DEV_COMMANDS_ENABLED` gating | Staff commands gated behind config flag |
| `PlayerRight` staff-as-donator matrix | Correct bonus thresholds for each rank |
| Boss entrance slayer checks | Missing slayer gates on bosses |

### Lower Priority

| Test | What It Catches |
|------|----------------|
| `LocalSettingsTest` | Config drift (port, integration flags, limits) |
| `ProfileJsonShapeTest` | Profile file structure validity |
| Client cache prerequisite test | Missing client cache files before launch |
| Client configuration test | Update-server mode defaults |
| `AchievementFeatureIntegrityTest` | Invalid achievement keys/amounts |
| `CollectionLogFeatureIntegrityTest` | Collection log dead references |
| `MysteryBoxFeatureIntegrityTest` | Mystery box reward validity |

---

## Data Systems Note

The monolithic JSON files listed in older versions of this plan (`npc_drops.json`, `npc_spawns.json`) have been **deleted**. The active system uses per-file directories:
- `data/def/npc-drops-json/{npcId}.json` — 1,778 files
- `data/def/npc-spawns-json/{npcId}.json` — 926 files

Tests referencing those monolithic files have been updated accordingly.
