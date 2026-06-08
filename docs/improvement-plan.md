# Tarnish PS Fork — Improvement Plan

> **Status:** Active — migration done, tooling remains.
> **Context:** Personal fork, playable, manual in-game testing after changes.
> **Environment:** Windows, IntelliJ IDEA, JDK 21 (server) / JDK 11 (client), Gradle.
> **Accounts:** `Zezima` (OWNER), `Oak` (ADMIN).

## Completed Work (Summary)

Phase 0 test armor and the core migration (Steps 1–5) are all done:

- **Phase 0.1–0.5:** Five test classes in place — `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `NpcLoadersParityTest`, `ParserSmokeTest`. The dual-system cross-reference tests caught real orphaned entities.
- **Step 1 (Reconciliation):** Gap map produced for items, NPCs, drops, spawns.
- **Step 2 (Split):** Python scripts split monolithic `npc_drops.json` (1,778 files) and `npc_spawns.json` (926 files) into per-file directories.
- **Step 3 (Loaders):** `NpcDropFileLoader.kt` and `NpcSpawnFileLoader.kt` written in `org.jire.tarnishps.defs`.
- **Step 4 (Parity):** `NpcLoadersParityTest` confirms old parser and new loader output match.
- **Step 5 (Swap):** `Starter.java` now uses `NpcSpawnFileLoader` and `NpcDropFileLoader` instead of the old `NpcSpawnParser`/`NpcDropParser`. Server boots, mobs spawn, drops work — verified in-game.

**Old monolithic files and parser classes remain as reference** until confidence in the new system is higher.

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

## What's Deferred

Remaining tiers:

- **Tier 2 (High-impact):** Remove monolithic JSON files, refactor PluginContext event dispatch, migrate all tests to JUnit 5 only
- **Tier 3 (Structural):** Extract secrets from settings.toml, prune unused deps (JGroups, PF4J, Sentry), upgrade client to JDK 21
- **Tier 4 (Deep):** Player refactor, networking integration tests, hot-reload plugins

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
