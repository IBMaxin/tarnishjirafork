# Tarnish PS Fork — Improvement Plan

> **Status:** Active — 106 tests, 0 failures, 9% coverage, 17 test files on 1,192 sources.
> **Last updated:** 2026-06-08
> **Context:** Personal fork, playable, manual in-game testing after changes.
> **Environment:** Windows, IntelliJ IDEA, JDK 21 (server) / JDK 11 (client), Gradle.
> **Accounts:** `Zezima` (OWNER), `Oak` (ADMIN).

---

## Completed Work (Summary)

Phase 0 test armor and the core migration (Steps 1–6) are all done:

- **Phase 0:** Six data-integrity test classes — `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `ParserSmokeTest`, `NpcFileLoaderTest`, `NpcSpawnSaveTest`.
- **Phase A–B:** Infrastructure + pure unit tests — Mockito 5.x, JaCoCo, JUnit 5 migration, `HitTest`, `CombatHitTest`, `PlayerRightTest`, `CommandTest`.
- **Phase C1–C2:** Plugin system tests — `PluginDiscoveryTest` (≥100 plugins loaded), `PluginContextDispatchTest` (31 event-type routing tests).
- **Step 1 (Reconciliation):** Gap map produced for items, NPCs, drops, spawns.
- **Step 2 (Split):** Python scripts split monolithic `npc_drops.json` (1,778 files) and `npc_spawns.json` (926 files) into per-file directories.
- **Step 3 (Loaders):** `NpcDropFileLoader.kt` and `NpcSpawnFileLoader.kt` written in `org.jire.tarnishps.defs`.
- **Step 4 (Parity):** Parity confirmed between old parser and new loader output; `NpcLoadersParityTest` deleted after confirmation.
- **Step 5 (Swap):** `Starter.java` now uses per-file loaders. Server boots, mobs spawn, drops work — verified in-game.
- **Step 6 (Cleanup):** All old monolithic JSONs and parser classes deleted. Tests updated. See Tier 2.1.
- **Slayer fixes:** Shop slot guard (`||` fix) and `totalPoints` accumulation fix applied; `SlayerTest` added.

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

## Tier 2.1 — Monolithic JSON Cleanup (Completed)

| Item | Status | Details |
|------|--------|---------|
| Production rewire | Done | `NpcDropTable.main()`, `DeveloperCommandPlugin`, `NpcDropsParser` swapped to `NpcDropFileLoader`/`NpcSpawnFileLoader` with SLF4J logging |
| Admin spawnnpc | Done | Rewrote to write per-file JSON at `data/def/npc-spawns-json/{id}.json` via Gson + try-with-resources |
| Delete old files | Done | `npc_drops.json`, `npc_spawns.json`, `NpcDropParser.java`, `NpcSpawnParser.java` removed |
| Delete parity test | Done | `NpcLoadersParityTest.java` removed (parity confirmed) |
| Update tests | Done | `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `AraxxorDataTest` updated to use per-file JSONs |
| New tests | Done | `NpcFileLoaderTest` (loader smoke test), `NpcSpawnSaveTest` (spawn file round-trip) |

**Build verified:** All 106 tests pass, server boots with `Loaded 3436 NPC spawns` and `Loaded 1780 NPC drop tables`.

---

## What's Deferred (Longer Term)

- **Tier 2.2 (High-impact):** Refactor PluginContext event dispatch, migrate all tests to JUnit 5 only
- **Tier 3 (Structural):** Extract secrets from settings.toml, prune unused deps (JGroups, PF4J, Sentry), upgrade client to JDK 21
- **Tier 4 (Deep):** Player refactor, networking integration tests, hot-reload plugins

---

## Testing Phases

Current baseline: 17 test files, 106 tests, 0 failures, 9% JaCoCo coverage on 1,192 sources.

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

### Phase C — Plugin System Tests

| # | Task | Status |
|---|------|--------|
| C1 | `PluginDiscoveryTest` — `PluginManager.load("plugin")`, assert ≥100 loaded | Done |
| C2 | `PluginContextDispatchTest` — verify each event type routes to correct handler (31 tests, full subtype coverage) | Done |
| C3 | Expand `CommandExtensionErrorLoggingTest` — command lookup, exception handling, duplicate detection, canAccess gating | Pending |

### Phase D — Slayer Bug Fix

| # | Task | Status |
|---|------|--------|
| D1 | Fix shop slot guard: `&&` → `||` in `Slayer.java` | Done |
| D2 | Fix `totalPoints` never incremented — add `totalPoints += rewardPoints` at `Slayer.java:176` | Done |
| D3 | Add `totalPoints` accumulation test to `SlayerTest` | Done |

### Phase E — PluginContext Dispatch Refactor (Deferred → Tier 2.2)

| # | Task | Status |
|---|------|--------|
| E1 | Replace 13-branch `instanceof` chain with `Map<Class, BiFunction>` dispatch | Pending |
| E2 | Verify with existing + new PluginContext tests | Pending |

---

## Phase F — Feature Integrity Tests (Next Priority)

These tests catch broken content wiring before logging into the client. Candidate files under `game-server/src/test/java/com/osroyale/`.

### F1: Required Feature Data

`FeatureRequiredDataTest`:
- `data/def/npc-spawns-json/` exists with ≥900 files
- `data/def/npc-drops-json/` exists with ≥1,700 files
- `data/def/items-json/` exists with ≥26,000 files
- `data/def/monsters-json/` exists with ≥3,200 files
- `data/def/store/stores.json` exists

### F2: Store and Currency Integrity

`FeatureShopIntegrityTest`:
- Every shop item id exists in item definitions
- Item amounts are positive
- Buy/sell prices are non-negative where present
- Shop names are unique
- Currency names are recognized by the store system
- Priority shops asserted by name: Donator Store, Ironman Donator Store, Tarnish Vote Store, Prestige Rewards Store, LMS Store, Pest Control Store, Stardust Store, blood money shops

`DonatorFeatureIntegrityTest`:
- Donator Store and Ironman Donator Store exist and use `DONATOR_POINTS`
- Donator ranks have ascending money thresholds
- Donator rank helpers return expected thresholds

### F3: NPC Spawn and Drop Integrity

`NpcSpawnFeatureIntegrityTest`:
- Every per-file spawn entry has a positive NPC id
- Every NPC id resolves (directly or through `oldtonew.txt`)
- Position x, y, height are within sane ranges
- `facing` maps to `Direction`, `radius` is non-negative
- Regression guard: Al Kharid warrior migration (`3103` or `3292` or documented allowlist)

`NpcDropFeatureIntegrityTest`:
- Every drop file has a valid NPC id
- Every dropped item id exists in item definitions
- Minimum and maximum quantities are positive, `minimum <= maximum`
- Drop chance type is one of: `ALWAYS`, `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`

`DropViewerFeatureIntegrityTest`:
- Known bosses (Vorkath, Zulrah, barrows brothers) have drop tables
- Drop viewer can search without null failures for known set

### F4: Command and Permission Integrity

`CommandFeatureIntegrityTest`:
- Player commands: `home`, `players`, `staff`, `drops`, `vote`, `donate`, `trivia`
- Donator commands: `yell`, `donatorzone`
- Manager commands: `broadcast`
- Admin commands: `item`, `spawnnpc`, `tele`, `bank`
- Owner commands: `setrank`, `giveitem`, `ban`, `resetplayer`

`CommandAccessIntegrityTest`:
- Player command plugin accessible to normal players
- Donator/Helper/Mod/Admin/Manager/Developer/Owner plugins require their rank floors
- Dangerous commands (`item`, `giveitem`, `spawnnpc`, `setrank`, `ban`, `resetplayer`, `broadcast`) do NOT appear in lower-rank plugins

### F5: Progression System Integrity

- `AchievementFeatureIntegrityTest` — unique keys, positive amounts, valid categories
- `CollectionLogFeatureIntegrityTest` — valid item/NPC ids, boss entries have display ids
- `MysteryBoxFeatureIntegrityTest` — every reward item exists, amounts positive

### F6: Teleport and Zone Integrity

`TeleportFeatureIntegrityTest`:
- Every Teleport enum destination has non-null position within sane coordinate ranges
- Required item ids for teleport unlocks exist
- Key destinations: Home, Vorkath, Zulrah, Donator zones, boss teleports

---

## Phase G — Live Hardening

### G1: LogEvent — Decouple from Forum Integration

File: `game-server/src/main/java/com/osroyale/game/event/impl/log/LogEvent.java`

- Change gate from `!Config.FORUM_INTEGRATION || !Config.LOG_PLAYER` to `!Config.LOG_PLAYER`
- Add comment: "Player event logs are independent of forum integration."
- Add unit test: `LogEvent` suppressed when `LOG_PLAYER=false`, proceeds when `LOG_PLAYER=true`

### G2: Config.DEV_COMMANDS Flag

Gate dev commands via a config flag:
- `Config.java` — add `DEV_COMMANDS_ENABLED` bound to `settings.toml` `server.dev_commands_enabled = false`
- `AdminCommandPlugin.java` — gate `canAccess` behind `DEV_COMMANDS_ENABLED`
- `OwnerCommandPlugin.java` — split: moderation commands always-on (`::ban`, `::unban`, `::ipmute`, `::save`, `::resetplayer`); gated commands behind flag (`::giveitem`, `::setrank`, `::kill`, etc.)
- See open questions on granularity

### G3: Remove Stray System.out.println

Grep `game-server/plugins/**` and `game-server/src/main/java/**` and replace any remaining `System.out.println` with `logger.debug` or remove.

### G4: PlayerRight Staff-as-Donator Spillover

File: `game-server/src/main/java/com/osroyale/game/world/entity/mob/player/PlayerRight.java`

- Confirm whether staff (Admin/Manager/Developer/Owner) intentionally count as donator tiers for drop-rate, presets, blood-money, and deposit-amount checks
- If unintentional: change `isExtreme`/`isElite`/`isKing` to drop the `isAdministrator` short-circuit
- Add unit tests asserting the matrix

### G5: Donation Claiming Config

File: `game-server/settings.toml` — `donations_enabled = false`. Confirm whether bonds are in-game rewards only and `::setCredits`/`::points` are the only mint paths.

### G6: Boss Entrance Gaps

File: `game-server/plugins/plugin/click/object/ObjectFirstClickPlugin.java`

- Vorkath, Kraken, Cerberus gated by slayer — Zulrah is not. Confirm whether intentional.
- **Bug:** Kraken entrance (line ~813) has no `break;` — falls through to Zulrah case. Add `break;`.

---

## Go-Live Gate (Smoke Checklist)

- [ ] `:game-server:test` all 106+ tests green
- [ ] Manual boot smoke: `Loaded: 133 plugins`, `Startup service finished`
- [ ] Port 43594 listening
- [ ] Login as regular player → staff commands denied
- [ ] Login as dev account with `dev_commands_enabled = true` → commands work
- [ ] Donor bond redemption path
- [ ] Slayer: assign → cancel → reassign → kill → points increment → `totalPoints` increments
- [ ] Slayer shop: invalid slot no crash; valid slot deducts points and adds item
- [ ] Boss entrances: gated bosses block without task, allow with task
- [ ] Shop buy/sell, trade, drop/pickup
- [ ] Logout/restart persistence: position, inventory, bank, slayer task, points, totalPoints

---

## Open Questions

1. **Owner-command split (G2):** single `DEV_COMMANDS_ENABLED` flag, or finer split (moderation always-on, items/teleports gated)?
2. **Zulrah slayer check (G6):** is it intentional that Zulrah doesn't require a task? Audit Vorkath and other boss entrances.
3. **PlayerRight staff-as-donator (G4):** keep current behavior, or drop the staff short-circuit in `isExtreme`/`isElite`/`isKing`?
4. **LogReader bootstrap:** who creates `backup/logs/referrals.txt` on first run — `LogReader`, donation subsystem, or `Starter`?

---

## Name Index + Tooling

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
