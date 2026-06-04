# Item Definitions — Remove Old Parser, Go Per-File Only

> **Status:** Draft
> **Date:** 2025-06-04
> **Goal:** Remove `item_definitions.json` (145K lines) from the boot sequence. Make `items-json/` (27K files) the sole source of item data.

## Current State

Two systems load every boot:

1. `ItemDefinition.createParser().run()` — reads `item_definitions.json` (145K lines, 17,461 items)
2. `ItemDefLoader.load()` — reads `items-json/` (27K files), merges on top

Both write to the same `ItemDefinition.DEFINITIONS[]` array. The old parser runs first, then per-file enriches.

## The Problem

The old parser has **custom fields** that `items-json/` doesn't have:

| Field | Count in old file | In items-json? |
|-------|------------------|----------------|
| `street-value` | 7,300 items | ❌ No |
| `destroyable` / `destroy-message` | 795 + 932 items | ❌ No |
| `block-animation` | 209 items | ❌ No |
| `attack-animations` | 3 items | ❌ No |
| `stand-animation` / `walk-animation` / `run-animation` | ~50 items | ❌ No |

These are server-specific customizations (custom street prices, destroy messages, custom animations) that don't exist in the OSRS wiki data.

If we just remove the old parser, we lose all of these.

## Proposed Approach

Create a small **item overrides file** (`item_overrides.json`) with just the custom fields. Then ItemDefLoader reads it after loading from `items-json/`.

```
Boot sequence (new):
1. ItemDefLoader.load()          → reads items-json/ (27K files, wiki data)
2. ItemOverridesLoader.load()    → reads item_overrides.json (custom fields)
```

No more 145K-line file. Two clean reads instead of one monster.

## Step-by-Step Plan

### Step 1: Write a Python script to extract custom fields from item_definitions.json

Script: `scripts/data-migration/extract_item_overrides.py`

Reads `item_definitions.json`, extracts only the custom fields that `items-json/` doesn't have:
- `street-value`
- `destroyable`, `destroy-message`
- `block-animation`
- `attack-animations`
- `stand-animation`, `walk-animation`, `run-animation`

Writes `item_overrides.json` — a small file (probably <1MB) with just the overrides.

Supports `--dry-run` flag.

### Step 2: Write `ItemOverridesLoader.kt`

New Kotlin loader in `org.jire.tarnishps.defs`:

```kotlin
object ItemOverridesLoader {
    fun load() {
        // reads item_overrides.json
        // for each entry, enriches ItemDefinition.DEFINITIONS[id] with custom fields
    }
}
```

Handles:
- `street-value` → `definition.street_value = value`
- `destroyable` → `definition.destroyable = true`
- `destroy-message` → `definition.destroyMessage = message`
- `block-animation` → `definition.block_animation = value`
- `attack-animations` → `definition.attack_animations = map`
- `stand-animation` → `definition.stand_animation = value`
- `walk-animation` → `definition.walk_animation = value`
- `run-animation` → `definition.run_animation = value`

### Step 3: Update ItemDefLoader.kt

Remove the `onEnd()` call chain. Make ItemDefLoader self-sufficient:

- Add `DEFINITIONS = new ItemDefinition[Config.ITEM_DEFINITION_LIMIT]` at the start
- Remove the call from `ItemDefinition.createParser().onEnd()`

### Step 4: Update Starter.java

Replace:
```java
ItemDefinition.createParser().run();
```

With:
```java
ItemDefLoader.load();
ItemOverridesLoader.INSTANCE.load();
```

### Step 5: Write parity test

`ItemLoaderParityTest.java` — compare old parser output vs new loader output:
- Same item IDs
- Same field values for a sample of items
- Verify custom fields (street-value, destroyable, etc.) are preserved

### Step 6: Update tests

- Update `ParserSmokeTest.java` — swap item parser test to use new loaders
- Update `ReconciliationTest.java` — no longer needs old file comparison
- Update `RequiredDataFilesTest.java` — remove `item_definitions.json` from required list

### Step 7: Manual smoke test

Boot server, log in as Zezima, verify:
- Items exist in-game
- Custom street values work
- Destroy messages work
- Custom animations work (if testable)

## Files That Will Change

| File | Action |
|------|--------|
| `scripts/data-migration/extract_item_overrides.py` | Created |
| `game-server/data/def/item/item_overrides.json` | Created (by script) |
| `game-server/src/main/kotlin/.../ItemOverridesLoader.kt` | Created |
| `game-server/src/main/kotlin/.../ItemDefLoader.kt` | Modified — add array init, make self-sufficient |
| `game-server/src/main/java/.../ItemDefinition.java` | Modified — remove createParser() call, or keep for reference |
| `game-server/src/main/java/.../Starter.java` | Modified — swap parser for loaders |
| `game-server/src/test/.../ItemLoaderParityTest.java` | Created |
| `game-server/src/test/.../ParserSmokeTest.java` | Modified |
| `game-server/src/test/.../ReconciliationTest.java` | Modified |
| `game-server/src/test/.../RequiredDataFilesTest.java` | Modified |

## Callers of createParser() (8 total)

| Caller | Type | Action |
|--------|------|--------|
| `Starter.java:83` | Boot | **Swap** to new loaders |
| `NpcLoadersParityTest.java:37` | Test | Update to use ItemDefLoader |
| `ParserSmokeTest.java:66` | Test | Update to use ItemDefLoader |
| `NpcDropsParser.java:27` | Wiki tool | Leave as-is (standalone utility) |
| `BonusParser.java:119` | Wiki tool | Leave as-is (standalone utility) |
| `ItemDBdefUpdate.java:12` | Utility | Leave as-is (standalone utility) |
| `EquipmentDefinitionParser.java:28` | Old parser | Leave as-is (reference) |
| `NpcDropTable.java:150` | Runtime | **Update** — needs item data at runtime |

Strategy: Keep `createParser()` as a legacy method (still works if called). Swap only the boot sequence and NpcDropTable. Wiki tools and old utilities can be updated later.

## Risks

- **Custom field coverage:** Need to verify the extraction script captures ALL custom fields. The parity test will catch gaps.
- **ItemDefLoader array init:** Currently assumes DEFINITIONS[] is already allocated. Need to add initialization.
- **NpcDropTable.java:** Calls `createParser().run()` at runtime. Need to update to use ItemDefLoader instead.

## Verification

1. `./gradlew :game-server:test` — all tests pass
2. `./gradlew :game-server:run` — server boots
3. Log in as Zezima — items work
4. Check a known custom item (e.g., twisted bow with street-value) — verify override applied
