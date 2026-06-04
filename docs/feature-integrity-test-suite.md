# Feature Integrity Test Suite Plan

This plan describes an offline test suite for the server's feature surface. The goal is to catch broken content wiring before logging into the client.

Offline tests should answer questions like:

- Does the data parse?
- Do feature definitions point to real items/NPCs/shops?
- Are important commands registered under the expected access level?
- Are custom systems wired to valid ids and currencies?
- Would startup silently skip required migrated directories?

Offline tests cannot prove that interfaces render correctly or that the whole gameplay loop feels right. Those remain in-game smoke tests.

## Phase 1: Required Feature Data

### Test: Required Data Directories

Candidate file:

```text
game-server/src/test/java/com/osroyale/FeatureRequiredDataTest.java
```

Assertions:

- `data/def/npc-spawns-json/` exists.
- `data/def/npc-drops-json/` exists.
- `data/def/items-json/` exists.
- `data/def/monsters-json/` exists.
- `data/def/store/stores.json` exists.
- Required directories contain more than a conservative minimum number of `.json` files.

Why:

- The active startup path now depends on per-file NPC spawn/drop directories.
- Current loaders warn and return if a directory is missing, which can hide bad deploys.

### Test: Every Feature JSON Parses

Candidate file:

```text
game-server/src/test/java/com/osroyale/FeatureJsonParseTest.java
```

Assertions:

- All `.json` files under `data/def/store/`, `data/def/npc-spawns-json/`, `data/def/npc-drops-json/`, `data/def/items-json/`, and `data/def/monsters-json/` parse.
- Failure messages include the exact path.

Why:

- Most custom content is data-driven; malformed JSON should fail before boot testing.

## Phase 2: Store and Currency Integrity

### Test: Shops Reference Existing Items

Candidate file:

```text
game-server/src/test/java/com/osroyale/FeatureShopIntegrityTest.java
```

Assertions:

- Every shop item id exists in item definitions.
- Item amounts are positive.
- Buy/sell prices are non-negative where present.
- Shop names are unique.
- Currency names are recognized by the store system.

Priority shops to assert by exact name:

- `Donator Store`
- `Ironman Donator Store`
- `Tarnish Vote Store`
- `Prestige Rewards Store`
- `The LMS Store`
- `The Pest Control Store`
- `Stardust Store`
- Blood money shops

Why:

- Shops are one of the easiest places to create economy-breaking dead references.

### Test: Donator Shops Are Wired

Candidate file:

```text
game-server/src/test/java/com/osroyale/DonatorFeatureIntegrityTest.java
```

Assertions:

- `Donator Store` exists.
- `Ironman Donator Store` exists.
- Both use `DONATOR_POINTS`.
- Donator ranks have ascending money thresholds.
- Donator rank helper methods return expected thresholds/bonuses for controlled test players where practical.

Why:

- Donator content is business-critical and has several moving parts: rank, points, shop, zone, title, and command access.

## Phase 3: NPC Spawn and Drop Integrity

### Test: Per-File Spawn References

Candidate file:

```text
game-server/src/test/java/com/osroyale/NpcSpawnFeatureIntegrityTest.java
```

Assertions:

- Every per-file spawn entry has a positive NPC id.
- Every NPC id resolves either directly or through `oldtonew.txt`.
- Position `x`, `y`, and `height` are within sane ranges.
- `facing` maps to `Direction`.
- `radius` is non-negative.
- `instance` is present or defaults safely.
- File name and contained ids are consistent, or exceptions are explicitly documented.

Known regression guard:

- Assert the Al Kharid warrior migration is intentional:
  - either `3103` exists as old id data,
  - or `3292` exists as the converted spawn/drop target,
  - or a documented allowlist explains why it was removed.

Why:

- This catches the class of issue found in review: old ids disappearing during migration.

### Test: Per-File Drop References

Candidate file:

```text
game-server/src/test/java/com/osroyale/NpcDropFeatureIntegrityTest.java
```

Assertions:

- Every drop file has a valid NPC id.
- Every dropped item id exists.
- Minimum and maximum quantities are positive.
- `minimum <= maximum`.
- Drop chance type is one of:
  - `ALWAYS`
  - `COMMON`
  - `UNCOMMON`
  - `RARE`
  - `VERY_RARE`
- Roll data, when present, has the expected shape.

Why:

- Drop table errors affect PvM, collection log, drop viewer, and economy all at once.

### Test: Drop Viewer Coverage

Candidate file:

```text
game-server/src/test/java/com/osroyale/DropViewerFeatureIntegrityTest.java
```

Assertions:

- Known bosses have drop tables or explicit no-drop documentation.
- Known low-level NPCs used for smoke testing have drop tables.
- Drop viewer can search/display without null table failures for a small known set.

Suggested known set:

- Vorkath
- Zulrah
- Rock crab
- Al Kharid warrior
- Barrows brothers
- Wintertodt reward source, if represented in drop data

Why:

- The user-facing drop viewer should not be the first place missing drops are discovered.

## Phase 4: Command and Permission Integrity

### Test: Command Registration

Candidate file:

```text
game-server/src/test/java/com/osroyale/CommandFeatureIntegrityTest.java
```

Assertions:

- Player commands include:
  - `home`
  - `players`
  - `staff`
  - `drops`
  - `vote`
  - `donate`
  - `trivia` / answer commands where applicable
- Donator commands include:
  - `yell`
  - `donatorzone`
  - `superdonatorzone`
- Manager commands include:
  - `broadcast`
- Admin commands include:
  - `item`
  - `spawnnpc`
  - `tele`
  - `bank`
- Owner commands include:
  - `setrank`
  - `giveitem`
  - `ban`
  - `resetplayer`

Why:

- Command plugins are a major feature interface and easy to regress while refactoring.

### Test: Command Access Floors

Candidate file:

```text
game-server/src/test/java/com/osroyale/CommandAccessIntegrityTest.java
```

Assertions:

- Player command plugin is accessible to normal players.
- Donator command plugin requires donor access.
- Helper/mod/admin/manager/developer/owner plugins require their intended rank floors.
- Dangerous commands do not appear in lower-rank plugins.

Dangerous commands to guard:

- `item`
- `giveitem`
- `spawnnpc`
- `setrank`
- `ban`
- `ipban`
- `resetplayer`
- `broadcast`
- `saveworld`

Why:

- Permission drift can wreck economy and account safety.

## Phase 5: Broadcast and Social Integrity

### Test: Yell Filter

Candidate file:

```text
game-server/src/test/java/com/osroyale/YellFeatureIntegrityTest.java
```

Assertions:

- Yell rejects formatting injection strings like `<img=`, `<col=`, and `.com`.
- Non-donators cannot yell.
- Donators/helpers/staff can yell when not muted/jailed and yell setting is enabled.

Why:

- Yell is public chat; formatting injection and permission mistakes are visible immediately.

### Test: Broadcast Sources Compile/Wire

Candidate file:

```text
game-server/src/test/java/com/osroyale/BroadcastFeatureIntegrityTest.java
```

Assertions:

- `World.sendBroadcast(...)` can be called in a test context without null failures.
- Manager broadcast command exists.
- Rare drop, mystery box, clue scroll, prestige, shooting star, and Well of Goodwill broadcast source classes compile and expose expected entry points.

Why:

- Broadcasts are a cross-cutting feature used by many systems.

## Phase 6: Progression System Integrity

### Test: Achievement References

Candidate file:

```text
game-server/src/test/java/com/osroyale/AchievementFeatureIntegrityTest.java
```

Assertions:

- Achievement list keys are unique.
- Every achievement has positive required amount where applicable.
- Rewards are non-negative.
- Achievement categories are valid.

Why:

- Achievements are touched by combat, minigames, skilling, tutorial, and miscellaneous systems.

### Test: Collection Log References

Candidate file:

```text
game-server/src/test/java/com/osroyale/CollectionLogFeatureIntegrityTest.java
```

Assertions:

- Collection log item ids exist.
- Collection log NPC ids exist or are documented virtual sources.
- Boss collection entries have a known display NPC/item id.

Why:

- Collection log is a key long-term progression system and depends heavily on valid ids.

### Test: Mystery Box Rewards

Candidate file:

```text
game-server/src/test/java/com/osroyale/MysteryBoxFeatureIntegrityTest.java
```

Assertions:

- Every mystery box reward item exists.
- Reward amounts are positive.
- Broadcast-worthy reward definitions do not point to invalid ids.

Why:

- Mystery boxes are high-visibility economy content.

## Phase 7: Teleport and Zone Integrity

### Test: Teleports

Candidate file:

```text
game-server/src/test/java/com/osroyale/TeleportFeatureIntegrityTest.java
```

Assertions:

- Every `Teleport` enum destination has a non-null position.
- Positions are within sane coordinate ranges.
- Required item ids in teleport unlock/cost arrays exist.
- Key destinations exist:
  - Home
  - Vorkath
  - Zulrah
  - Donator zones
  - Boss teleports

Why:

- Bad teleports can strand players, skip content gates, or break testing routes.

## Phase 8: Manual In-Game Smoke Checklist

Some checks should remain manual after offline tests pass:

1. Log in as a normal player and confirm no staff commands work.
2. Log in as Oak/Admin and confirm admin commands work but owner-only commands do not.
3. Open Donator Store and Ironman Donator Store on eligible accounts.
4. Use `::yell` as donor and verify public formatting.
5. Use manager `::broadcast` on a test account.
6. Kill rock crabs and confirm drops.
7. Kill Zulrah and Vorkath and confirm drops/collection log where applicable.
8. Open drop viewer and search common NPCs/items.
9. Prestige a test skill on a disposable profile and confirm broadcast/store points.
10. Restart server and confirm profile persistence.

## Suggested Implementation Order

1. `FeatureRequiredDataTest`
2. `FeatureShopIntegrityTest`
3. `NpcSpawnFeatureIntegrityTest`
4. `NpcDropFeatureIntegrityTest`
5. `CommandFeatureIntegrityTest`
6. `CommandAccessIntegrityTest`
7. `DonatorFeatureIntegrityTest`
8. `TeleportFeatureIntegrityTest`
9. Achievement, collection log, and mystery box integrity tests
10. Optional broadcast/yell tests once test-player helpers exist

## Normal Verification Command

```powershell
.\gradlew.bat :game-server:test
```

## Done Criteria

The feature integrity suite is useful when:

- It runs offline with `:game-server:test`.
- It fails loudly when required migrated data directories are missing.
- It catches invalid shop items, NPC drops, NPC spawns, and teleport item references.
- It protects donor shops, donor commands, broadcast commands, and dangerous staff commands.
- It documents intentional removals through allowlists instead of silently accepting missing content.
