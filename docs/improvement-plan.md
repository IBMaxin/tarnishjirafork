# Tarnish PS Fork — Improvement Plan

> **Status:** Active — migration done, tooling remains.
> **Context:** Personal fork, playable, manual in-game testing after changes.
> **Environment:** Windows, IntelliJ IDEA, JDK 21 (server) / JDK 11 (client), Gradle.
> **Accounts:** `Zezima` (OWNER), `Oak` (ADMIN).

## Completed Work (Summary)

Phase 0 test armor and the core migration (Steps 1–6) are all done:

- **Phase 0.1–0.5:** Five test classes in place — `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `NpcLoadersParityTest`, `ParserSmokeTest`. The dual-system cross-reference tests caught real orphaned entities.
- **Step 1 (Reconciliation):** Gap map produced for items, NPCs, drops, spawns.
- **Step 2 (Split):** Python scripts split monolithic `npc_drops.json` (1,778 files) and `npc_spawns.json` (926 files) into per-file directories.
- **Step 3 (Loaders):** `NpcDropFileLoader.kt` and `NpcSpawnFileLoader.kt` written in `org.jire.tarnishps.defs`.
- **Step 4 (Parity):** `NpcLoadersParityTest` confirms old parser and new loader output match.
- **Step 5 (Swap):** `Starter.java` now uses `NpcSpawnFileLoader` and `NpcDropFileLoader` instead of the old `NpcSpawnParser`/`NpcDropParser`. Server boots, mobs spawn, drops work — verified in-game.
- **Step 6 (Cleanup):** All remaining production references to old parsers removed, monolithic JSONs and parser classes deleted, tests updated. See Tier 2.1 below.

---

## Tier 1 — Modernisation (Completed)

| Item | Status | Details |
|------|--------|---------|
| Gradle build cache | Done | `org.gradle.caching=true` added to `gradle.properties` |
| GitHub Actions CI | Done | `.github/workflows/ci.yml` — JDK 21, gradle cache, `./gradlew check`, test artifact upload |
| Clean repo | Done | `build/` and `.gradle/` deleted from filesystem; `.gitignore` already covered them |
| Version catalog | Done | `gradle/libs.versions.toml` — 55 version keys, 64 library entries unifying server & client |
| Server → catalog | Done | `game-server/build.gradle.kts` rewritten to use `libs.*` references |
| Client → catalog | Done | `game-client/build.gradle.kts` rewritten to use `libs.*` references |
| jsr305 fix | Done | Added explicit `compileOnly(libs.jsr305)` to client (guava bump dropped transitive `@Nonnull`) |

**Build verified:** Server compiles, client compiles, 51/51 tests run (3 pre-existing NpcLoadersParityTest failures).

## Tier 2.1 — Monolithic JSON Cleanup (Completed)

| Item | Status | Details |
|------|--------|---------|
| Production rewire | Done | `NpcDropTable.main()`, `DeveloperCommandPlugin`, `NpcDropsParser` swapped to `NpcDropFileLoader`/`NpcSpawnFileLoader` with SLF4J logging |
| Admin spawnnpc | Done | Rewrote to write per-file JSON at `data/def/npc-spawns-json/{id}.json` via Gson + try-with-resources |
| Delete old files | Done | `npc_drops.json` (1.9 MB), `npc_spawns.json` (515 KB), `NpcDropParser.java`, `NpcSpawnParser.java` removed |
| Delete parity test | Done | `NpcLoadersParityTest.java` removed (parity confirmed) |
| Update tests | Done | `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `AraxxorDataTest` updated to use per-file JSONs |
| New tests | Done | `NpcFileLoaderTest` (loader smoke test), `NpcSpawnSaveTest` (spawn file round-trip) |

**Build verified:** All tests pass, server boots with `Loaded 3436 NPC spawns` and `Loaded 1780 NPC drop tables`.

## What's Deferred (Longer Term)

Remaining tiers:

- **Tier 2.2 (High-impact):** Refactor PluginContext event dispatch, migrate all tests to JUnit 5 only
- **Tier 3 (Structural):** Extract secrets from settings.toml, prune unused deps (JGroups, PF4J, Sentry), upgrade client to JDK 21
- **Tier 4 (Deep):** Player refactor, networking integration tests, hot-reload plugins

---

## Testing Phases

Builds on existing data-integrity tests (11 files, 0.92% coverage of 1,192 sources).
Goal: add behavioral coverage for plugin system, combat primitives, and utilities without World/Player refactoring.

### Phase A — Infrastructure (Completed)

| # | Task | Status |
|---|------|--------|
| A1 | Wire Mockito 5.x into `game-server/build.gradle.kts` test dependencies | Done |
| A2 | Add JaCoCo plugin for coverage visibility | Done |
| A3 | Migrate `ProfileRightsTest` from JUnit 4 → JUnit 5 | Done |

### Phase B — Pure Unit Tests (no Player/World dependency) (Completed)

| # | Task | Status |
|---|------|--------|
| B1 | `HitTest` — `modifyDamage()`, `setAs()`, `isAccurate()`, `setAccurate()` | Done |
| B2 | `CombatHitTest` — `copyAndModify()`, getter values, multi-hit construction | Done |
| B3 | `PlayerRightTest` — `isAdministrator()`, `isDonator()` enum from-id lookup | Done |
| B4 | `CommandTest` — equals, name hashing, precedence ordering | Done |

### Phase C — Plugin System Tests (classpath-sensitive)

| # | Task | Status |
|---|------|--------|
| C1 | `PluginDiscoveryTest` — `PluginManager.load("plugin")`, assert ≥100 loaded | Pending |
| C2 | `PluginContextDispatchTest` — verify each event type routes to correct handler | Pending |
| C3 | Expand `CommandExtensionErrorLoggingTest` — command lookup, exception handling | Pending |

### Phase D — Slayer Bug Fix

| # | Task | Status |
|---|------|--------|
| D1 | Fix `totalPoints` never incremented — add `totalPoints += rewardPoints` at `Slayer.java:176` | Pending |
| D2 | Add `totalPoints` accumulation test to `SlayerTest` | Pending |

### Phase E — PluginContext Dispatch Refactor

| # | Task | Status |
|---|------|--------|
| E1 | Replace 13-branch `instanceof` chain with `Map<Class, BiFunction>` dispatch | Pending |
| E2 | Verify with existing + new PluginContext tests | Pending |

---

## Step 6: Name Index + Tooling

Generate a `name_index.json` from all per-file directories:

```json
{
  "twisted bow": 20997,
  "zulrah": 2042,
  "dragon scimitar": 4587
}
```

Add a `find-item` script:

```bash
find-item "twisted"   → items-json/20997.json (Twisted Bow, cost: 1,100,000,000)
```

This makes the per-file system discoverable — you don't need to memorize IDs.
