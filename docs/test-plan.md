# Offline Test TODO Plan

This plan is for tests we can realistically run without logging into the game client. The goal is to catch broken data, broken startup loaders, bad config drift, profile/rights mistakes, and client launch prerequisites before doing manual in-game smoke testing.

→ **Project map:** `AGENTS.md`
→ **Game scope:** [game-scope.md](game-scope.md)
→ **Docs index:** [README.md](README.md)
→ **Prompt pack:** [../prompts/README.md](../prompts/README.md) — 24 audit prompts
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

## Current Baseline

Already present:

- `game-server/src/test/java/com/osroyale/ProfileRightsTest.java`
- `game-server` has JUnit 4.13.2 test dependency.
- `.\gradlew.bat :game-server:test` has passed before.
- Server boot logs previously showed:
  - `Startup service finished`
  - `Loaded: 133 plugins`
  - `Server built successfully`
- Server cache exists under `game-server/data/cache`.
- Local client cache exists under `%USERPROFILE%\.tarnish\cache`.

## Priority 0: Keep Basic Build/Test Green

These are command-level checks and should be the first gate.

### TODO: Server Compile Check

Command:

```powershell
.\gradlew.bat :game-server:classes
```

Proves:

- Server Java/Kotlin/plugin source still compiles.
- Generated or migrated code has not broken the main server artifact.

### TODO: Client Compile Check

Command:

```powershell
.\gradlew.bat :game-client:classes
```

Proves:

- Client source still compiles with Java 11.
- Local-cache client changes still compile.

### TODO: Current Unit Test Gate

Command:

```powershell
.\gradlew.bat :game-server:test
```

Proves:

- Current profile-right expectations are still true.
- Any new offline tests added under `game-server/src/test/java` pass.

## Priority 1: Data Parse Tests

The server has roughly 30k JSON files under `game-server/data`, mostly item and monster definitions. A basic parse test gives excellent value.

### TODO: Add `DataJsonParseTest`

Candidate file:

```text
game-server/src/test/java/com/osroyale/DataJsonParseTest.java
```

Test behavior:

- Walk `data`.
- Parse every `.json` file with Gson/JsonParser.
- Fail with the exact relative path when a file is malformed.
- Optionally skip empty known-placeholder files if any are found.

Proves:

- No malformed JSON exists in server data.
- Manual edits to profiles, stores, drops, spawns, and definitions did not corrupt JSON.

Notes:

- This should be simple and fast enough for normal test runs.
- Because there are many JSON files, failure messages should include only the broken file path and parser error.

### TODO: Add Important File Presence Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/RequiredDataFilesTest.java
```

Required files:

- `data/cache/main_file_cache.dat`
- `data/cache/main_file_cache.idx0`
- `data/cache/main_file_cache.idx1`
- `data/cache/main_file_cache.idx2`
- `data/cache/main_file_cache.idx3`
- `data/cache/main_file_cache.idx4`
- `data/cache/main_file_cache.idx5`
- `data/def/item/item_definitions.json`
- `data/def/npc/npc_definitions.json`
- `data/def/npc/npc_spawns.json`
- `data/def/npc/npc_drops.json`
- `data/def/store/stores.json`
- `data/def/combat/projectile_definitions.json`
- `data/io/message_sizes.json`
- `data/content/skills/agility.json`
- `data/profile/world_profile_list.json`

Proves:

- The server has the minimum files needed for startup and basic content loading.

## Priority 2: Server Loader Smoke Tests

These tests should exercise the actual parsers where possible instead of only checking raw JSON.

### TODO: Add Parser Smoke Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/ParserSmokeTest.java
```

Useful parser calls:

- `ItemDefinition.createParser().run()`
- `NpcDefinition.createParser().run()`
- `new CombatProjectileParser().run()`
- `new NpcSpawnParser().run()`
- `new NpcDropParser().run()`
- `new NpcForceChatParser().run()`
- `new StoreParser().run()`
- `new GlobalObjectParser().run()`
- `new PacketSizeParser().run()`
- `ObjectExamines.loadObjectExamines()`

Proves:

- Core server data can be loaded through the same parser classes used at startup.
- JSON may parse structurally and still fail when converted into game objects; this catches that second layer.

Risk:

- Some parsers may swallow errors and only log them. If so, we may need small, focused assertions after parser runs.

### TODO: Add Definition Loader Tests

Candidate file:

```text
game-server/src/test/java/com/osroyale/DefinitionLoaderTest.java
```

Useful loader calls:

- `org.jire.tarnishps.defs.ItemDefLoader.load()`
- `org.jire.tarnishps.defs.MonsterDefLoader.load()`

Proves:

- The newer item/monster JSON directories are readable.
- Definition ids and file names can be loaded into the newer Kotlin definition maps.

Potential assertions:

- Loaded item definitions count is greater than zero.
- Loaded monster definitions count is greater than zero.
- A few common ids exist, such as shrimp, coins, and low-level NPCs, if the loader exposes lookup maps.

## Priority 3: Cross-Reference Data Tests

These tests catch content definitions that point to missing or invalid ids.

### TODO: Add Store Item Reference Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/StoreDataTest.java
```

Test behavior:

- Load known item ids from item definitions.
- Load `data/def/store/stores.json`.
- Assert every store item id exists in item definitions.
- Assert item amounts/prices are not negative.

Proves:

- Shops do not contain impossible item ids or obviously bad quantities.

### TODO: Add NPC Spawn Reference Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/NpcSpawnDataTest.java
```

Test behavior:

- Load known NPC ids.
- Load `data/def/npc/npc_spawns.json`.
- Assert each spawn references a known NPC id.
- Assert positions have sane x/y/height values.

Proves:

- NPC spawn data references definitions the server knows about.

### TODO: Add NPC Drop Reference Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/NpcDropDataTest.java
```

Test behavior:

- Load known NPC ids and item ids.
- Load `data/def/npc/npc_drops.json`.
- Assert each drop table references known NPC ids.
- Assert dropped item ids exist.
- Assert drop amounts and weights are positive where required.

Proves:

- Drop tables are not full of dead references.

## Priority 4: Profile and Account Safety Tests

The current `ProfileRightsTest` is a good start. We can expand it carefully.

### TODO: Expand Profile Rights Coverage

Candidate file:

```text
game-server/src/test/java/com/osroyale/ProfileRightsTest.java
```

Add checks:

- Every profile in `world_profile_list.json` has a corresponding save file or is intentionally exempt.
- Every profile save with elevated rights has a matching world profile rank.
- No account except explicit allowed owners has `OWNER`.
- `Oak` remains `ADMINISTRATOR`.
- `Zezima` remains `OWNER`.

Proves:

- Staff account edits do not drift between the save file and world profile list.
- Accidental owner grants are caught.

### TODO: Add Profile JSON Shape Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/ProfileJsonShapeTest.java
```

Test behavior:

- Walk `data/profile/save/*.json`, excluding subdirectories like trading post data.
- Assert each profile has:
  - `username`
  - `password`
  - `player-rights`
  - position fields, if consistently present
  - inventory/equipment/bank fields, if consistently present
- Assert `player-rights` maps to `PlayerRight.valueOf`.

Proves:

- Profile files are minimally loadable.
- Rights values are valid enum names.

## Priority 5: Local Config and Secret Safety Tests

The local `settings.toml` currently has external-service credentials/tokens present, while website integration and highscores are disabled. We can test the safe local posture.

### TODO: Add Local Settings Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/LocalSettingsTest.java
```

Test behavior:

- Parse `settings.toml`.
- Assert `server.server_port` is `43594`.
- Assert `server.website_integration` is `false` for local dev.
- Assert `services.highscores_enabled` is `false` for local dev.
- Assert `network.resource_leak_detection` is one of the accepted values.
- Assert `game.max_players`, `game.max_npcs`, and definition limits are positive.

Proves:

- Local tests do not accidentally depend on forum/highscore/database integrations.
- Important numeric config values remain sane.

Follow-up cleanup:

- Move real external tokens/passwords out of committed config if this repo is going to be shared.

## Priority 6: Client Local-Cache Launch Prerequisites

These tests check the client-side assumptions that made local dev playable.

### TODO: Add Client Cache Prerequisite Test

Candidate file:

```text
game-client/src/test/java/com/osroyale/ClientCachePrerequisiteTest.java
```

Test behavior:

- Check `%USERPROFILE%\.tarnish\cache`.
- Assert the cache dir exists.
- Assert required `main_file_cache.*` files exist.
- Assert `main_file_cache.dat` is non-empty.
- Assert idx files exist; allow `idx3` to be zero because this cache currently has an empty `idx3`.

Proves:

- A local client run has the cache files needed before launch.

Risk:

- This test is machine-specific. It may be better as a manual smoke command or a Gradle task excluded from CI.

### TODO: Add Client Config Test

Candidate file:

```text
game-client/src/test/java/com/osroyale/ClientConfigurationTest.java
```

Test behavior:

- Assert `Configuration.USE_UPDATE_SERVER` defaults to `false`.
- Assert setting `-Dtarnish.updateServer=true` can still enable update-server mode if that behavior remains intended.

Proves:

- The client defaults to local-cache dev mode.
- SwiftFUP mode remains explicitly opt-in.

## Priority 7: Plugin Discovery Test

The server uses ClassGraph in `PluginManager.load("plugin")` and previously loaded 133 plugins.

### TODO: Add Plugin Count Smoke Test

Candidate file:

```text
game-server/src/test/java/com/osroyale/PluginDiscoveryTest.java
```

Test behavior:

- Invoke `PluginManager.load("plugin")` in a fresh test JVM.
- Assert plugin count is at least a conservative floor, such as `100`.
- Prefer a floor instead of exact `133` to avoid churn when plugins are added or removed intentionally.

Proves:

- Plugin classes are discoverable from the test/runtime classpath.
- Plugin constructors and `onInit` methods do not immediately crash during offline loading.

Risk:

- Plugins may mutate static global registries and make repeated test runs order-sensitive.
- This test may need isolation or to run last.

## Priority 8: Full Server Boot Smoke Harness

This is the highest-value offline check, but it is more of an integration smoke test than a normal unit test.

### TODO: Add Manual Boot Smoke Script

Candidate file:

```text
scripts/server-boot-smoke.ps1
```

Behavior:

- Start `.\gradlew.bat :game-server:run`.
- Capture stdout/stderr to `build/server-boot-smoke.*.log`.
- Wait until the log contains `Server built successfully`.
- Assert the log contains `Startup service finished`.
- Assert the log contains `Loaded: 133 plugins` or at least `Loaded:`.
- Check `Test-NetConnection localhost -Port 43594`.
- Stop the spawned Java/Gradle process cleanly.

Proves:

- The server can boot from a clean command.
- The game port opens.
- Startup loader/plugin chain completes.

Risk:

- Process cleanup must be careful on Windows.
- This is slower and more brittle than unit tests, but it proves the most without a client.

## Suggested Implementation Order

1. Add `DataJsonParseTest`.
2. Add `RequiredDataFilesTest`.
3. Expand `ProfileRightsTest`.
4. Add `LocalSettingsTest`.
5. Add parser smoke tests for the safest parser classes.
6. Add cross-reference tests for stores, NPC spawns, and NPC drops.
7. Add client configuration/cache prerequisite tests if we are comfortable with machine-specific tests.
8. Add the PowerShell server boot smoke script.
9. Decide which checks belong in normal `:game-server:test` versus manual smoke only.

## Proposed Test Commands

Fast server gate:

```powershell
.\gradlew.bat :game-server:test
```

Compile everything:

```powershell
.\gradlew.bat :game-server:classes :game-client:classes
```

Manual server boot:

```powershell
.\gradlew.bat :game-server:run
```

Manual port check while server is running:

```powershell
Test-NetConnection localhost -Port 43594
```

Manual local client cache check:

```powershell
Get-ChildItem "$env:USERPROFILE\.tarnish\cache\main_file_cache.*"
```

## Definition of Done

Offline testing is in a strong place when:

- `:game-server:test` validates JSON parse health, required files, staff/profile rights, and config sanity.
- `:game-server:classes` and `:game-client:classes` both pass.
- A manual boot smoke script can prove server startup and port listening.
- The plan clearly marks client-login-only behavior as manual/integration territory.
