# Tarnish PS Fork — Improvement Plan

> **Status:** Active — **115 tests, 0 failures**, 20 test files on 1,192 sources.
> **Last updated:** 2026-06-08
> **Context:** Personal fork, playable, manual in-game testing after changes.
> **Environment:** Windows, IntelliJ IDEA, JDK 21 (server + client), Gradle 9.5.1.
> **Accounts:** `Zezima` (OWNER), `Oak` (ADMIN).

---

## Completed Work (Summary)

### Build Tooling Upgrades (Highest Impact — Done)
- **Client JDK upgrade:** JDK 11 → **21** — matches server
- **Gradle upgrade:** 8.14.2 → **9.5.1** — eliminates 33 deprecation warnings
- **Kotlin upgrade:** 2.1.21 → **2.4.0** — latest stable
- **Shadow plugin:** 8.3.6 → **9.4.2** — compatible with Gradle 9.x
- **Build tooling:** Added `junit-platform-launcher` dependency (required by Gradle 9.x)
- **Gradle build cache:** `org.gradle.caching=true` in `gradle.properties`
- **Version catalog:** `gradle/libs.versions.toml` — 55 version keys, 64 library entries unifying server & client
- **Server + client build files:** Rewritten to use `libs.*` references
- **jsr305 fix:** Explicit `compileOnly(libs.jsr305)` for client (guava bump dropped transitive `@Nonnull`)

### GitHub Actions CI (Done)
- `.github/workflows/ci.yml` — JDK 21, gradle cache, `./gradlew check`, test artifact upload

### Monolithic JSON → Per-File Migration (Done)
- **Step 1 (Reconciliation):** Gap map produced for items, NPCs, drops, spawns.
- **Step 2 (Split):** Python scripts split monolithic `npc_drops.json` (1,778 files) and `npc_spawns.json` (926 files) into per-file directories.
- **Step 3 (Loaders):** `NpcDropFileLoader.kt` and `NpcSpawnFileLoader.kt` written in `org.jire.tarnishps.defs`.
- **Step 4 (Parity):** Parity confirmed between old parser and new loader output; `NpcLoadersParityTest` deleted.
- **Step 5 (Swap):** `Starter.java` now uses per-file loaders. Server boots, mobs spawn, drops work — verified in-game.
- **Step 6 (Cleanup):** All old monolithic JSONs and parser classes deleted. Tests updated.
- **Production rewire:** `NpcDropTable.main()`, `DeveloperCommandPlugin`, `NpcDropsParser` swapped to per-file loaders.
- **Admin spawnnpc:** Rewritten to write per-file JSON via Gson + try-with-resources.

### Test Infrastructure (Done)
- **Phase 0:** Six data-integrity test classes — `RequiredDataFilesTest`, `DataJsonParseTest`, `CrossReferenceTest`, `ParserSmokeTest`, `NpcFileLoaderTest`, `NpcSpawnSaveTest`.
- **Phase A:** Mockito 5.x, JaCoCo, JUnit 5 migration.
- **Phase B:** Pure unit tests — `HitTest`, `CombatHitTest`, `PlayerRightTest`, `CommandTest`.
- **Phase C1–C2:** Plugin system tests — `PluginDiscoveryTest` (≥100 plugins), `PluginContextDispatchTest` (31 event-type routing tests).
- **Phase D:** Slayer bug fixes — shop slot guard (`&&` → `||`), `totalPoints` accumulation; `SlayerTest` added.

### Bug Fixes (Done)
- **G6b — Cerberus entrance fallthrough:** Added missing `break;` after `case 23104` if-else block. Previously, entering Cerberus would fall through to web-cutting logic (`case 733`).
- **G6a — Kraken entrance cleanup:** Removed redundant inner `break;` inside `else` block. Outer `break;` after if-else handles both branches.

### Feature Integrity Tests (Done)
- **F1 — `FeatureRequiredDataTest`:** Validates 5 per-file data directories exist with minimum file counts (items-json ≥25K, monsters-json ≥10K, npc-drops-json ≥1.5K, npc-spawns-json ≥800, equipment-json ≥2.5K).
- **F2a — `FeatureShopIntegrityTest`:** 5 tests validating all 36 shops — item IDs exist in definitions, amounts positive, names unique, currencies recognized, sell types valid.
- **F2b — `DonatorFeatureIntegrityTest`:** 3 tests — donator stores exist, use DONATOR_POINTS currency, have items.

### Setup & Onboarding (Done)
- Simplified `settings.toml` with comments
- Beginner-friendly README with prerequisites table
- Visual setup guide (`SETUP.md`)
- Quick-start scripts with Java pre-flight check
- VS Code configs (launch, tasks, extensions)

---

## Priority-Ordered Work Queue

Items ranked by **impact × urgency**. High-impact items catch bugs or prevent crashes. Medium items improve safety and dev experience. Low items are polish.

---

### 🔴 P0 — Bugs & Crashes

| # | Item | Files | Impact | Status |
|---|------|-------|--------|--------|
| G6b | **Cerberus entrance falls through to Webs** — `case 23104` missing `break;` after if-else block | `ObjectFirstClickPlugin.java` | Players entering Cerberus also trigger web-cutting logic | ✅ **Fixed** |
| G6a | **Kraken entrance** — redundant inner `break;` in `else` block | `ObjectFirstClickPlugin.java` | Works but fragile pattern | ✅ **Cleaned up** |
| G1 | **LogEvent decouple from FORUM_INTEGRATION** — change gate from `!Config.FORUM_INTEGRATION \|\| !Config.LOG_PLAYER` to `!Config.LOG_PLAYER` | `LogEvent.java` | Player event logs incorrectly suppressed when forum integration is off | ⏸️ **Deferred** (no forums set up) |

### 🟠 P1 — Security & Access Control

| # | Item | Files | Impact | Status |
|---|------|-------|--------|--------|
| G2 | **DEV_COMMANDS_ENABLED flag** — add `Config.DEV_COMMANDS_ENABLED` bound to `settings.toml` `server.dev_commands_enabled = false`; gate admin/owner commands | `Config.java`, `AdminCommandPlugin.java`, `OwnerCommandPlugin.java`, `settings.toml` | Prevents accidental dev command exposure on live server | ⏸️ **Deferred** |
| G4 | **PlayerRight staff-as-donator spillover** — `isExtreme`/`isElite`/`isKing` short-circuit via `isAdministrator`; confirm intentional or remove | `PlayerRight.java` | Staff get unintended donator perks (drop rate, blood money, presets) | ⏸️ **Deferred** (staff can set own rank) |
| G5 | **Donation claiming config** — confirm `donations_enabled = false` means bonds are in-game only; `::setCredits`/`::points` are only mint paths | `settings.toml` | Economy integrity | ⏸️ **Deferred** (private fork, intentional) |

### 🟡 P2 — Feature Integrity Tests (Safety Net)

These tests catch broken content wiring before logging into the client. Write before making content changes.

| # | Test Class | What It Asserts | Status |
|---|-----------|-----------------|--------|
| F1 | `FeatureRequiredDataTest` | Per-file directories exist with minimum file counts | ✅ **Done** |
| F2a | `FeatureShopIntegrityTest` | Shop item IDs valid, amounts positive, names unique, currencies recognized | ✅ **Done** |
| F2b | `DonatorFeatureIntegrityTest` | Donator stores exist, use DONATOR_POINTS, have items | ✅ **Done** |
| F3a | `NpcSpawnFeatureIntegrityTest` | Spawn NPC IDs resolve, positions sane, facing maps to Direction | ⬜ Not started |
| F3b | `NpcDropFeatureIntegrityTest` | Drop item IDs valid, min≤max, chance type recognized | ⬜ Not started |
| F3c | `DropViewerFeatureIntegrityTest` | Known bosses have drop tables, viewer search works | ⬜ Not started |
| F4a | `CommandFeatureIntegrityTest` | Expected commands exist per rank tier | ⬜ Not started |
| F4b | `CommandAccessIntegrityTest` | Dangerous commands absent from lower-rank plugins | ⬜ Not started |
| F5 | Progression tests | Achievement/CollectionLog/MysteryBox integrity | ⬜ Not started |
| F6 | `TeleportFeatureIntegrityTest` | Teleport positions sane, unlock items exist | ⬜ Not started |

---

### 🟠 P1.5 — Game Action Automation Framework (Phase H)

A modular, extensible automation system that logs in as real players and executes in-game actions (teleport, kill NPCs, train skills, trade, etc.) with built-in validation, debugging, and CI integration.

#### Architecture

```
test-automation/
├── core/                    # Framework core
│   ├── TestRunner.kt        # Central orchestrator
│   ├── ActionDSL.kt         # Fluent API for game actions
│   ├── TestClient.kt        # Login/authentication manager
│   ├── TestPlayer.kt        # Wraps Player with debug/trace
│   └── validators/          # Assertion helpers
│       ├── PositionValidator.kt
│       ├── InventoryValidator.kt
│       ├── MessageValidator.kt
│       └── CombatValidator.kt
├── modules/                 # Test scenarios by domain
│   ├── bosses/              # Boss mechanics (Cerberus, Zulrah, Kraken, etc.)
│   │   ├── CerberusTest.kt
│   │   ├── ZulrahTest.kt
│   │   └── KrakenTest.kt
│   ├── combat/              # Combat system tests
│   │   ├── PrayerTest.kt
│   │   └── SpecialAttackTest.kt
│   ├── skills/              # Skill training validation
│   │   ├── SlayerTest.kt
│   │   └── WoodcuttingTest.kt
│   ├── economy/             # Shops, trading, drops
│   │   ├── ShopTest.kt
│   │   └── DropTableTest.kt
│   └── mobility/            # Teleport, movement, object clicks
│       ├── TeleportTest.kt
│       └── ObjectClickTest.kt
├── config/
│   ├── accounts.toml        # Test account credentials
│   └── scenarios.toml       # Scenario definitions
└── ci/
    └── Jenkinsfile           # Pipeline definition
```

#### Action Coverage

| Category | Actions | Example |
|----------|---------|---------|
| **Mobility** | Teleport, walkTo, follow, randomWalk | `teleport(3222, 3222)` |
| **Combat** | Attack, barrage, setPrayer, setCombatStyle | `attack(NPCs.ZULRAH).until { npcDead() }` |
| **NPC** | ClickNpc, tradeNpc, getSlayerTask, completeTask | `clickNpc(NPCs.BANKER, option="Bank")` |
| **Object** | ClickObject, climbStairs, enterCave | `clickObject(23104)` |
| **Item** | Equip, drop, pickUp, useItemOn | `equip(Items.WHIP)` |
| **Shop** | Buy, sell, browse | `purchaseFrom(Shops.VARROCK, item=1153, amount=5)` |
| **Skill** | Train, untilLevel, setAxe, setPickaxe | `train(Skills.WOODCUTTING).at(Trees.YEW).untilLevel(90)` |
| **Boss** | StartFight, phase, forcePhaseTransition | `startBossFight(Bosses.ZULRAH) { phase(1) { ... } }` |
| **Multi-agent** | Coordinated attacks, tank/dps roles | `coordinated { tank.aggro(boss); dps.attack() }` |

#### Key Features

- **Fluent DSL:** `action { teleportTo("Varrock").clickNpc(NPCs.BANKER).validate { messageContains("Welcome") } }`
- **Debug Mode:** Step-by-step playback, packet logging, screenshots on failure
- **Failure Recovery:** `withRetry(maxAttempts=3) { attemptGodwarsRun { if (died()) resupply() } }`
- **State Assertions:** Validate position, inventory, messages, damage dealt, loot received
- **CI Integration:** `./gradlew :test-automation:run -Pmodules=bosses`

#### Atomic Implementation Steps

Each step is self-contained, testable, and builds on the previous. Steps marked ⭐ are the minimum viable core.

##### ⭐ H1 — TestClient + Player Wrapper (30 min)

**Files to create:**
```
test-automation/core/TestClient.kt
test-automation/core/TestPlayer.kt
test-automation/config/accounts.toml
```

**What it does:**
- `TestClient.login(account)` — creates a `Player` with given credentials, sets position, inventory, rights
- `TestPlayer` — wraps `Player` with debug logging, state snapshots before/after each action
- `accounts.toml` — defines test accounts: `[zezima]` `rights = "OWNER"` `[oak]` `rights = "ADMIN"`

**Acceptance:** `val p = TestClient.login("zezima"); assert(p.right == PlayerRight.OWNER)`

---

##### ⭐ H2 — ActionDSL + Position Validator (30 min)

**Files to create:**
```
test-automation/core/ActionDSL.kt
test-automation/core/validators/PositionValidator.kt
```

**What it does:**
- `ActionDSL` — fluent builder: `action { teleport(x, y, z).validate { at(x, y, z) } }`
- `teleport(x, y, z)` — calls `player.move(Position(x, y, z))`
- `PositionValidator` — asserts `player.position == expected`, reports diff on failure

**Acceptance:** `action { teleport(3222, 3222).validate { at(3222, 3222) } }` passes

---

##### ⭐ H3 — Object Click + Message Validator (30 min)

**Files to create:**
```
test-automation/core/validators/MessageValidator.kt
test-automation/modules/mobility/ObjectClickTest.kt
```

**What it does:**
- `clickObject(id)` — dispatches `ObjectFirstClickEvent` through the plugin system
- `MessageValidator` — captures `SendMessage` packets, asserts content
- `ObjectClickTest.kt` — first real test: Cerberus entrance fallthrough

**Acceptance:** `action { clickObject(23104).validate { messageContains("need a slayer task") } }` passes

---

##### H4 — NPC Click + Inventory Validator (30 min)

**Files to create:**
```
test-automation/core/validators/InventoryValidator.kt
test-automation/modules/mobility/NpcClickTest.kt
```

**What it does:**
- `clickNpc(id, option)` — dispatches `NpcClickEvent` with type matching option index
- `InventoryValidator` — asserts item presence, quantity, slot
- `NpcClickTest.kt` — bank booth, shop keeper interactions

**Acceptance:** `action { clickNpc(NPCs.BANKER, option="Bank").validate { inventoryContains(995) } }`

---

##### H5 — Combat Actions + Combat Validator (1 hr)

**Files to create:**
```
test-automation/core/validators/CombatValidator.kt
test-automation/modules/combat/CombatTest.kt
```

**What it does:**
- `attack(npc)` — dispatches combat initiation event
- `setPrayer(id)` — activates/deactivates prayer
- `setCombatStyle(style)` — switches attack style
- `CombatValidator` — asserts damage dealt, damage taken, prayer drain, special attack energy

**Acceptance:** `action { attack(NPCs.COW).validate { damageDealt(min=1) } }`

---

##### H6 — Slayer Task Actions + Slayer Validator (1 hr)

**Files to create:**
```
test-automation/core/validators/SlayerValidator.kt
test-automation/modules/skills/SlayerTest.kt
```

**What it does:**
- `getSlayerTask(master)` — assigns task from slayer master
- `completeTask()` — simulates task completion, validates points
- `SlayerValidator` — asserts task name, kill count, points, totalPoints

**Acceptance:** `action { getSlayerTask(Masters.NIEVE).validate { taskAssigned() } }`

---

##### H7 — Shop Actions + Economy Validator (1 hr)

**Files to create:**
```
test-automation/core/validators/EconomyValidator.kt
test-automation/modules/economy/ShopTest.kt
```

**What it does:**
- `purchaseFrom(shop, item, amount)` — buys item from shop, validates currency deduction
- `sellTo(shop, item, amount)` — sells item to shop, validates currency gain
- `EconomyValidator` — asserts gold/point changes, item transfer

**Acceptance:** `action { purchaseFrom(Shops.GENERAL, item=1931, amount=1).validate { goldDecreased(by=3) } }`

---

##### H8 — Item Actions (30 min)

**Files to create:**
```
test-automation/modules/economy/ItemTest.kt
```

**What it does:**
- `equip(item, slot)` — equips item from inventory
- `drop(item)` — drops item to ground
- `pickUp(item)` — picks up item from ground
- `useItemOn(item, target)` — item-on-item, item-on-object, item-on-npc

**Acceptance:** `action { equip(Items.RUNE_SCIMITAR).validate { slotContains(Equipment.WEAPON, Items.RUNE_SCIMITAR) } }`

---

##### H9 — Boss Scenario Runner (2 hr)

**Files to create:**
```
test-automation/modules/bosses/CerberusTest.kt
test-automation/modules/bosses/KrakenTest.kt
test-automation/modules/bosses/ZulrahTest.kt
test-automation/core/BossScenario.kt
```

**What it does:**
- `BossScenario` — reusable template: entrance → fight → loot validation
- `startBossFight(boss)` — clicks entrance object, waits for activity creation
- `phase(n)` — asserts current boss phase
- `forcePhaseTransition(damage)` — deals damage to trigger next phase

**Acceptance:** `scenario { boss(Bosses.CERBERUS).entrance(23104).validate { activityStarted() } }`

---

##### H10 — Multi-Agent Coordinator (2 hr)

**Files to create:**
```
test-automation/core/Coordinator.kt
test-automation/modules/bosses/GodwarsTest.kt
```

**What it does:**
- `Coordinator` — manages multiple `TestPlayer` instances in same scenario
- `coordinated { tank.aggro(boss); dps.attackFromSafeSpot() }` — parallel actions
- `GodwarsTest.kt` — tank + DPS vs General Graardor

**Acceptance:** Two players can enter Godwars together, tank holds aggro while DPS deals damage

---

##### H11 — Failure Recovery + Retry (1 hr)

**Files to create:**
```
test-automation/core/RetryDSL.kt
```

**What it does:**
- `withRetry(maxAttempts, onFailure)` — wraps action block with retry logic
- `onFailure` callback: `{ player -> resupplyFromBank(player); teleportBack(player) }`
- Logs each attempt with duration and failure reason

**Acceptance:** `withRetry(3) { attemptGodwarsRun() }` retries on death instead of failing

---

##### H12 — Debug Mode + Tracing (1 hr)

**Files to create:**
```
test-automation/core/DebugTracer.kt
```

**What it does:**
- `debugMode = true` — enables per-action tracing
- Captures: player position before/after, packets sent/received, state diffs
- On failure: writes trace log to `test-automation/reports/{test-name}/trace.log`
- `stepThrough = true` — pauses between actions for manual inspection

**Acceptance:** Failing test produces `trace.log` with full action history

---

##### H13 — CI Pipeline (30 min)

**Files to create:**
```
test-automation/ci/run-tests.sh
test-automation/ci/Jenkinsfile
```

**What it does:**
- `run-tests.sh` — starts server, waits for boot, runs test suite, stops server
- `Jenkinsfile` — pipeline: checkout → build → start server → run tests → archive reports
- Supports `-Pmodules=bosses` to run a subset

**Acceptance:** `./test-automation/ci/run-tests.sh bosses` exits 0 with all boss tests green

---

##### H14 — Documentation + Module Template (30 min)

**Files to create:**
```
test-automation/README.md
test-automation/CONTRIBUTING.md
test-automation/scripts/create-module.sh
```

**What it does:**
- `README.md` — setup, writing tests, running, CI
- `CONTRIBUTING.md` — module structure, naming conventions, best practices
- `create-module.sh` — scaffolds a new module: `./create-module.sh slayer` generates `modules/slayer/` with template test

**Acceptance:** `./create-module.sh fishing` creates `modules/fishing/FishingTest.kt` with compilable template

---

### E2E Automation Framework (Phase H — In Progress)
- **H1 — GameClient + BotPlayer + E2ETest base:** Netty-based game client with RSA handshake, login, packet send/receive. `BotPlayer` high-level API (teleportTo, sendCommand, clickObject, waitForMessage). `E2ETest` base class with connect/login lifecycle and auto-cleanup. ✅ **Built**
- **H1a — Login credentials bugfix:** `GameClient.handleHandshakeResponse()` hardcoded `"Zezima"/"1"` — fixed to use credentials passed to `login()`. ✅ **Fixed**
- **H1b — Teleport command name fix:** `BotPlayer.teleportTo()` sent `::teleport` (nonexistent) — fixed to `::tele` (actual admin command). ✅ **Fixed**
- **H1c — Switch to Oak:** `TeleportE2ETest` now uses `Oak` (ADMINISTRATOR) by default from `E2ETest` base class, avoiding "Account already online" conflicts with live Zezima sessions. ✅ **Done**
- **H1d — Test JVM system property forwarding:** `build.gradle.kts` now forwards `-Pe2e` / `-De2e=true` to test JVM for `@EnabledIfSystemProperty` gating. ✅ **Done**
- **H2 — TeleportE2ETest:** First scenario — teleport + verify via command response. Gated behind `-Pe2e`. ✅ **Built**
- **H3–H14:** ⬜ Not started (see work queue below)

---

#### Dependency Map

```
H1 ──→ H2 ──→ H3 ──→ H4 ──→ H5 ──→ H6 ──→ H7 ──→ H8 ──→ H9 ──→ H10
                                                              │
                                                              └──→ H11 ──→ H12 ──→ H13 ──→ H14
```

- **H1–H3** (⭐) = minimum viable core — can test object clicks and teleports
- **H4–H8** = domain modules — add NPC, combat, slayer, shop, item coverage
- **H9–H10** = advanced scenarios — bosses, multi-agent
- **H11–H12** = hardening — retry, debug tracing
- **H13–H14** = delivery — CI, docs

#### Quick Start (After H1–H3)

```kotlin
class CerberusEntranceTest : BaseTest() {
    @Test
    fun `entrance without task shows error`() = action {
        teleport(1240, 1243)
            .clickObject(23104)
            .validate {
                messageContains("need a slayer task")
                at(1240, 1243) // didn't teleport
            }
    }

    @Test
    fun `entrance with task teleports`() = action {
        teleport(1240, 1243)
            .setSlayerTask(SlayerTask.CERBERUS)
            .clickObject(23104)
            .validate {
                at(1240, 1243, 0) // inside cave
                activityStarted("Cerberus")
            }
    }
}
```
