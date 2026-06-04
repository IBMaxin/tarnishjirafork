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

## What's Deferred

All original Phases 1–5 are deferred until after the migration is complete:

- **Phase 1 (CI):** Would be noisy with data failures — wait until one system
- **Phase 2 (DevEx):** Gradle perf, templates, JRebel — nice but not blocking
- **Phase 3 (Refactoring):** Event merge, Player extraction, combat formulas — big code changes, do after data is clean
- **Phase 4 (Docs):** Add migration-specific docs instead
- **Phase 5 (Long-term):** Integration tests, benchmarks — future

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
