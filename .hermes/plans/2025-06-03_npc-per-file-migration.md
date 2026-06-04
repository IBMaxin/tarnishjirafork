# NPC Drops & Spawns — Per-File Migration Plan

> **Status:** Migration complete. All 7 steps done. Old files kept as reference.
> **Date:** 2025-06-03

## Current State

Two Kotlin file loaders exist, compile clean, and pass parity tests:

| Loader | Path | Reads from |
|--------|------|-----------|
| `NpcDropFileLoader.kt` | `game-server/src/main/kotlin/org/jire/tarnishps/defs/` | `data/def/npc-drops-json/` |
| `NpcSpawnFileLoader.kt` | `game-server/src/main/kotlin/org/jire/tarnishps/defs/` | `data/def/npc-spawns-json/` |

**Done:**
- [x] Python split scripts written (`scripts/data-migration/split_npc_drops.py`, `split_npc_spawns.py`)
- [x] Both scripts support `--dry-run`
- [x] Split scripts executed — 1,778 drop files, 926 spawn files created
- [x] NpcSpawnFileLoader: `instance` field added and passed through
- [x] NpcDropFileLoader: `roll-data` support added, item ID remapping reordered to match old parser
- [x] Parity test (`NpcLoadersParityTest.java`) — 5/5 tests pass
- [x] Scripts organized under `scripts/data-migration/` and `scripts/data-cleanup/`

**Remaining:**
- [x] Swap loaders in Starter.java (lines 86-87)
- [x] Update ParserSmokeTest to test new loaders
- [x] Manual smoke test (boot server, log in, verify drops/spawns)
- [ ] Old monolithic files kept as reference — delete later when confident

## Bugs Found & Fixed

### NpcSpawnFileLoader.kt — missing `instance` field ✅ Fixed
Added `instance: Int = 0` to `SpawnEntry`, passed to `Npc()` constructor.

### NpcDropFileLoader.kt — missing `roll-data` support ✅ Fixed
Added `rollData: IntArray?` to `DropFile`, calls `table.setRollData()` when present.

### NpcDropFileLoader.kt — order of item ID remapping ✅ Fixed
Moved remapping (1436→7936, 1437→7937) after clue scroll checks to match old parser exactly.

## Steps Completed

### Step 1: Split scripts ✅
`scripts/data-migration/split_npc_drops.py` and `split_npc_spawns.py` — both with `--dry-run` support.

### Step 2: Fix loader bugs ✅
All three bugs fixed, compiles clean.

### Step 3: Run split scripts ✅
- `npc-drops-json/` — 1,778 files (783 monolithic entries expanded via 290 multi-ID entries)
- `npc-spawns-json/` — 926 files (3,446 entries grouped by NPC ID)

### Step 4: Parity test ✅
`NpcLoadersParityTest.java` — 5 tests, all pass:
- `dropTables_produceSameKeys` — same NPC IDs in both systems
- `dropTables_matchDropCountsPerNpc` — same drop tier counts per NPC
- `dropTables_matchItemIdsPerNpc` — same item IDs per NPC
- `spawns_monolithicAndPerFileHaveSameNpcIds` — same NPC ID sets
- `spawns_monolithicAndPerFileHaveSameTotalEntries` — same total entry count

### Step 5: Swap loaders ✅
`Starter.java` — swapped `new NpcSpawnParser().run()` / `new NpcDropParser().run()` → `NpcSpawnFileLoader.INSTANCE.load()` / `NpcDropFileLoader.INSTANCE.load()`. Added imports. Compiles clean, parity tests pass.

**Keep old files** — monolithic JSONs and parser classes stay as reference.

### Step 6: Update ParserSmokeTest ✅
`ParserSmokeTest.java` — tests 2 and 3 now use `NpcSpawnFileLoader.INSTANCE.load()` and `NpcDropFileLoader.INSTANCE.load()` instead of old parsers. Added floor-count assertion for drops (`NPC_DROPS.size() > 100`).

### Step 7: Manual smoke test ✅
Boot server, log in as Zezima, verify NPCs spawn and drops work. Tested: Vorkath (6 kills), Zulrah, low-level mobs, donor shop — all confirmed working.

## Files That Changed

| File | Action |
|------|--------|
| `scripts/data-migration/split_npc_drops.py` | Created |
| `scripts/data-migration/split_npc_spawns.py` | Created |
| `game-server/src/main/kotlin/.../NpcSpawnFileLoader.kt` | Fixed: `instance` field |
| `game-server/src/main/kotlin/.../NpcDropFileLoader.kt` | Fixed: `roll_data`, remap order |
| `game-server/data/def/npc-drops-json/*.json` | Created (1,778 files) |
| `game-server/data/def/npc-spawns-json/*.json` | Created (926 files) |
| `game-server/src/test/.../NpcLoadersParityTest.java` | Created (5 tests) |

## Risks

- **MobList.clear() bug:** `forEach(this::remove)` modifies list during iteration, doesn't fully clear. Doesn't affect production (only relevant for tests). Spawn parity test uses pure data comparison instead.
- **Roll-data:** 1 entry in entire dataset uses custom roll-data. Now handled.
- **Instance field:** 1 NPC uses instance=999999. Now handled.
