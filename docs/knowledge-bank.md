# Knowledge Bank — Tarnish PS Fork

> Use this file as a quick reference. Everything here is a durable fact about this project — no session- or task-specific state.
> **Repo root:** `C:\Users\bob\IdeaProjects\tarnishjirafork`

---

## 1. Project Overview

| Property | Value |
|----------|-------|
| Type | OSRS private server fork (personal, ~5 players) |
| Server JDK | 21 |
| Client JDK | 11 |
| IDE | IntelliJ IDEA on Windows |
| Build | Gradle (wrapper: `gradlew.bat`) |
| Test accounts | `Zezima` (OWNER), `Oak` (ADMIN) |
| Server port | 43594 |
| Startup signals | `Startup service finished` → `Loaded: 133 plugins` → `Server built successfully` |
| Game testing | Manual in-game after changes (login with client) |
| Improvement plan | `docs/improvement-plan.md` |
| Unzip.java | Removed: dead code, no callers |

---

## 2. Key Commands

→ See AGENTS.md §Quick Start for build/test/run commands.

---

## 3. Data Systems — The Dual System Problem

The codebase has **two parallel definition systems** that write to the same in-memory arrays at runtime. They don't fully agree.

| Aspect | Old System (monolithic) | New System (per-file) | Status |
|--------|------------------------|----------------------|--------|
| Items | `data/def/item/item_definitions.json` (145K lines, ~26K items) | `data/def/items-json/` (~27K files) | Both loaded, merged at runtime |
| NPCs | `data/def/npc/npc_definitions.json` (118K lines, ~11K NPCs) | `data/def/monsters-json/` (3,246 files) | Both loaded, merged at runtime |
| Drops | ❌ Deleted | `data/def/npc-drops-json/` (1,778 files) | ✅ Per-file active in Starter.java |
| Equipment | `data/def/equipment/equipment_definitions.json` (66K lines) | `data/def/equipment-json/` | Old monolith active; per-file directory exists as migration/reference data |
| Spawns | ❌ Deleted | `data/def/npc-spawns-json/` (926 files) | ✅ Per-file active in Starter.java |
| Stores | `data/def/store/stores.json` (4.5K lines) | ❌ None yet | Only old system (small, keep as-is) |

**All paths are relative to `game-server/`.**

### Old System — Item Format (`item_definitions.json`)

```json
{
  "id": 2,
  "name": "Cannonball",
  "stackable": true,
  "low-alch": 2,
  "high-alch": 3,
  "base-value": 5,
  "street-value": 192
}
```

Key fields: `id`, `name`, `stackable`, `noted-id`, `unnoted-id`, `low-alch`, `high-alch`, `base-value`, `street-value`, `weight`, `destroy-message`, `destroyable`

### Old System — NPC Format (`npc_definitions.json`)

```json
{
  "id": 2,
  "name": "Aberrant spectre",
  "size": 2,
  "combat-level": 96,
  "stand": 1506,
  "walk": 1505,
  "attackable": true,
  "aggressive": true,
  "attack-style": "Magic",
  "hitpoints-level": 90,
  "defence-level": 90
}
```

Key fields: `id`, `name`, `combat-level`, `size`, `stand`, `walk`, `attack-animation`, `death-animation`, `block-animation`, `hitpoints-level`, `attack-level`, `strength-level`, `defence-level`, `ranged-level`, `magic-level`, `attackable`, `aggressive`, `attack-style`, `weakness`, `attack-cooldown`, `defence-stab/slash/crush/magic/ranged`

### New System — Item Per-File Format (`items-json/{id}.json`)

```json
{
  "id": 20997,
  "name": "Twisted bow",
  "cost": 4000000,
  "lowalch": 1600000,
  "highalch": 2400000,
  "weight": 1.814,
  "tradeable": true,
  "stackable": false,
  "noted": false,
  "noteable": true,
  "equipable": true,
  "equipable_weapon": true,
  "members": true,
  "linked_id_noted": 20998,
  "buy_limit": 8,
  "examine": "...",
  "equipment": {
    "attack_stab": 0,
    "attack_ranged": 70,
    "ranged_strength": 20,
    "slot": "2h",
    "requirements": { "ranged": 75 }
  },
  "weapon": {
    "attack_speed": 6,
    "weapon_type": "bow",
    "stances": [ { "combat_style": "accurate", "boosts": "accuracy and damage" } ]
  }
}
```

Richer than old format — has `equipment`, `weapon`, `tradeable`, `wiki_name`, `release_date`, `buy_limit`, `icon` (base64), `quest_item`, `linked_id_item`, `linked_id_noted`, `linked_id_placeholder`

### New System — NPC Per-File Format (`monsters-json/{id}.json`)

```json
{
  "id": 2042,
  "name": "Zulrah",
  "combat_level": 725,
  "size": 5,
  "hitpoints": 500,
  "max_hit": 41,
  "attack_type": ["ranged"],
  "attack_speed": 3,
  "aggressive": true,
  "poisonous": true,
  "venomous": true,
  "attack_level": 1,
  "strength_level": 1,
  "defence_level": 300,
  "magic_level": 300,
  "ranged_level": 300,
  "attack_bonus": 0,
  "strength_bonus": 0,
  "attack_magic": 50,
  "magic_bonus": 20,
  "attack_ranged": 50,
  "ranged_bonus": 20,
  "defence_stab": 0,
  "defence_slash": 0,
  "defence_crush": 0,
  "defence_magic": -45,
  "defence_ranged": 50,
  "drops": [
    { "id": 12934, "name": "Zulrah's scales", "quantity": "100-299", "rarity": 1.0, "rolls": 1 }
  ],
  "slayer_monster": true,
  "slayer_level": 1,
  "slayer_xp": 500.0,
  "attributes": [],
  "category": ["bosses"],
  "examine": "...",
  "wiki_name": "Zulrah (Serpentine)"
}
```

Richer than old format — has `drops` array, `attack_type` array, `attributes`, `category`, `slayer_*`, `wiki_name`, `release_date`, `last_updated`, `incomplete`, `duplicate`, `immune_poison/venom`

---

## 4. Data Files — Per-File Status

### NPC Drops (active: `data/def/npc-drops-json/`)

The monolithic `npc_drops.json` and `NpcDropParser.java` have been **deleted**. Only the per-file system is active.

One drop table per file:
```json
{
  "npc_id": 8060,
  "rare_table": true,
  "drops": [
    { "item": 22124, "minimum": 2, "maximum": 2, "type": "ALWAYS" },
    { "item": 1305, "minimum": 1, "maximum": 1, "type": "UNCOMMON" }
  ]
}
```

**Gotcha:** Drop field names can be `"item"` or the legacy alias `"id"` for the item ID. The active loader supports both.
**Types:** ALWAYS, COMMON, UNCOMMON, RARE, VERY_RARE
**Numeric fields:** `npc_id`, `minimum`, `maximum`, optional `chance`; `type` is a string.

### Equipment (`data/def/equipment/equipment_definitions.json`)

```json
{
  "id": 35,
  "type": "WEAPON",
  "name": "Excalibur",
  "requirements": [],
  "bonuses": [20, 29, -2, 0, 0, 0, 3, 2, 1, 0, 25, 0, 0, 0]
}
```

Bonuses array is always 14 elements: attack_stab, attack_slash, attack_crush, attack_magic, attack_ranged, defence_stab, defence_slash, defence_crush, defence_magic, defence_ranged, melee_strength, ranged_strength, magic_damage, prayer.
Types: WEAPON, HAT, SHIELD, BODY, LEGS, etc.

### NPC Spawns (active: `data/def/npc-spawns-json/`)

```json
{
  "id": 1671,
  "radius": "3",
  "facing": "EAST",
  "position": { "x": 3565, "y": 3288, "height": 0 }
}
```

Note: `radius` and some other fields are **strings**, not integers.

### Stores (`data/def/store/stores.json`)

```json
{
  "name": "General Store",
  "items": [
    { "id": 1, "amount": 300, "value": 10, "type": "ITEM" }
  ]
}
```

Cleanest data file — 0 errors found.

---

## 5. Known Data Issues (Found by CrossReferenceTest)

| Issue | Count | Details |
|-------|-------|---------|
| Orphan NPC spawns | **65** | NPC spawns reference NPC IDs not in `npc_definitions.json` (IDs like 308, 3103, 6521, 119, 222, etc.) |
| Missing equipment | **1** | Equipment entry for item ID 6208 — not in `item_definitions.json` |
| Missing drop item | **1** | Item 13361 (Shayzien platebody (1)) referenced in drops — exists in `items-json/` but not in old monolithic file |
| Unknown NPC in drops | **1** | NPC 3103 referenced in drops but no definition exists |
| Min > max drops | **2** | Drop tables where minimum > maximum (e.g., item 565 min=17 max=5) |
| Drop field names | **inconsistent** | Some use `"item"`, some use `"id"` for the item ID within drop entries |

**Root cause:** The two data systems (old monolithic + new per-file) have drifted. Items/NPCs added to one system never made it to the other.

---

## 6. Test Infrastructure

| Test File | Location | Description |
|-----------|----------|-------------|
| `RequiredDataFilesTest.java` | `test/java/com/osroyale/` | Asserts critical data files exist |
| `DataJsonParseTest.java` | `test/java/com/osroyale/` | Parses all 30K+ JSON files, catches corruption |
| `CrossReferenceTest.java` | `test/java/com/osroyale/` | Cross-references IDs across stores, spawns, drops, equipment |
| `ParserSmokeTest.java` | `test/java/com/osroyale/` | Runs actual parser classes (stateful, Tier 2, ordered) |
| `ProfileRightsTest.java` | `test/java/com/osroyale/` | Existing pre-fork test (JUnit 4, runs via vintage engine) |

**Testing approach:** AI writes small Java test classes (~30-80 lines). JVM executes against the big JSON files. Never load data files into AI context.

**JUnit:** JUnit 5 + vintage engine for JUnit 4 backward compat. `useJUnitPlatform()` in `build.gradle.kts` required.

**Tier 1 (safe):** No global state mutated. Raw Gson parsing, Set<Integer> comparisons.
**Tier 2 (stateful):** Runs actual parser `.run()` methods, mutates static state. Ordered tests, run last.

---

## 7. Dual Event Systems (Architectural Risk)

| System | Language | Pattern |
|--------|----------|---------|
| `EventDispatcher` | Java | Listener list (multi-handler per event type) |
| `Events.kt` | Kotlin | Single-slot (one handler per event type) |

**Risk:** Both systems handle similar events. The Kotlin system being single-slot means adding a second handler silently replaces the first — events get dropped without error.

**Location:**
- Java: `game-server/src/main/java/com/osroyale/game/event/`
- Kotlin: `game-server/plugins/plugin/` (various files)

---

## 8. Other Known Risks

- **Player.java (943 lines):** Identified for extraction into smaller component classes (collection log, pets, achievements, farming, presets, etc.)
- **Dual combat formula paths:** `accuracy/` and `formula/` packages both implement combat calculations. Need consolidation.
- **JRebel plugin:** Included in `build.gradle.kts` but not active. If licensed, enables hot reload for faster iteration.
- **Old Java parser system:** `GsonParser` base class + subclasses (StoreParser, NpcSpawnParser, NpcDropParser, etc.) — NpcSpawnParser/NpcDropParser no longer used in production
- **New Kotlin loaders:** `ItemDefLoader`, `MonsterDefLoader` — per-file system loaders
- **NPC file loaders:** `NpcDropFileLoader`, `NpcSpawnFileLoader` — per-file drop/spawn loaders (active in Starter.java)

---

## 9. File Counts (as of last check)

| Data Set | Files |
|----------|-------|
| `items-json/` | 26,988 |
| `monsters-json/` | 3,246 |
| `item_definitions.json` entities | ~26,000 |
| `npc_definitions.json` entities | ~11,000 |

---

## 10. Key Java/Class References

| Class | Path | Purpose |
|-------|------|---------|
| `GsonParser` | `util/parser/GsonParser.java` | Base class for all old JSON parsers |
| `StoreParser` | `util/parser/impl/StoreParser.java` | Loads `stores.json` |
| ~~`NpcSpawnParser`~~ | ~~`util/parser/impl/NpcSpawnParser.java`~~ | Removed — replaced by `NpcSpawnFileLoader` |
| ~~`NpcDropParser`~~ | ~~`util/parser/impl/NpcDropParser.java`~~ | Removed — replaced by `NpcDropFileLoader` |
| `ItemDefinition` | `game/world/items/ItemDefinition.java` | Item definition + `createParser()` |
| `NpcDefinition` | `game/world/entity/mob/npc/definition/NpcDefinition.java` | NPC definition + `createParser()` |
| `ItemDefLoader` | (Kotlin, in plugins/) | Per-file item loader |
| `MonsterDefLoader` | (Kotlin, in plugins/) | Per-file NPC loader |
| `NpcDropFileLoader` | (Kotlin) `org.jire.tarnishps.defs.NpcDropFileLoader` | Per-file NPC drop loader (active) |
| `NpcSpawnFileLoader` | (Kotlin) `org.jire.tarnishps.defs.NpcSpawnFileLoader` | Per-file NPC spawn loader (active) |
| `Store.STORES` | `content/store/Store.java` | Static map of loaded stores |
| `EventDispatcher` | `game/event/EventDispatcher.java` | Java event bus |
| `Events.kt` | (Kotlin, in plugins/) | Kotlin event bus |
| `Starter.java` | Server entry point, calls all parser `.run()` methods |

**All under:** `game-server/src/main/java/com/osroyale/`

---

## 11. Privileges, Commands, and Player State

### Rank hierarchy

| Enum value | Display | Notes |
|------------|---------|-------|
| `OWNER` | Owner | Treat as top-tier privilege. |
| `DEVELOPER` | Developer | `isDeveloper()` = owner OR developer. |
| `MANAGER` | Manager | `isManager()` = dev/owner OR manager. |
| `ADMINISTRATOR` | Administrator | `isAdministrator()` = manager OR admin. |
| `MODERATOR` | Moderator | `isModerator()` = admin OR moderator. |
| `HELPER` | Helper | `isHelper()` = moderator OR helper. |
| `DONATOR` | Donator | Donor tiers derived from `player.donation.getSpent()` thresholds. |
| `IRONMAN` / `ULTIMATE_IRONMAN` / `HARDCORE_IRONMAN` | Ironman | Gated separately for ironman rules. |

Owner and Auto-inheritance rule: most `is*(player)` helpers already include the higher ranks. For new command gates, use the most specific helper that matches the intent (e.g. `isManager(player)` for staff tools, `isModerator(player)` for moderation tools).

### Command plugin gating

- Each command plugin ends with `canAccess(Player player)` which returns the minimum rank needed.
- Managers/Admins/Owners are expected to have staff commands there; lower tiers get their own plugins.
- `OwnerCommandPlugin` is effectively staff + superadmin tools; make sure Owner/Admin both see the commands they need.

### Slayer / task manipulation

- Slayer state lives on `player.slayer`: `task`, `amount`, `points`, `blocked`, `unlocked`.
- Existing staff commands: `::removeslayertask` / `::removetask` (ManagerCommandPlugin).
- Want: `::settask <SlayerTask enum name>`, `::taskamount <amount>`, `::addtaskamount <amount>` — these should check `isManager(player)`.
- Slayer task names map to `com.osroyale.content.skill.impl.slayer.SlayerTask` enum.

### Points / currencies

- `player.points`: slayer points.
- `player.donation`: a helper object with `setCredits(int)` and `getSpent()`.
- `player.votePoints`: vote points.
- `player.pestPoints`: pest control points.
- `player.skillingPoints`: general skilling points.
- Admin already has `::points` which sets all currencies to high values. Add finer commands like `::setpoints <type> <amount>` if needed.

### Teleports/boss entry
### Kroken/Zulrah entry (fixed)
- `Teleport.KRAKEN` teleports to `(2276, 10000, 0)`. Actual instance starts on object click `537` in `ObjectFirstClickPlugin`.
- Object `537` now has a proper `break;` and no longer falls through into `case 10068` (Zulrah).
- Zulrah entry remains on object `10068` and auto-creates `ZulrahActivity`.
- Verified in-game: Kraken entrance now correctly enters Kraken fight.

### Commands discovery path for future work

Most activity entry points are in `game-server/plugins/plugin/click/object/ObjectFirstClickPlugin.java` under `firstClickObject()`. Typed activity controllers are under `game-server/src/main/java/com/osroyale/content/activity/impl/`.

### Player state fields commonly needed by admin tools

- `player.right` → current `PlayerRight`.
- `player.slayer` → slayer task + points.
- `player.donation` → donation credits + helper for rank checks.
- `player.votePoints`, `player.pestPoints`, `player.skillingPoints`.
- `player.bank`, `player.inventory`, `player.equipment` for item spawn/wipe.

### Staff command index by plugin

- `OwnerCommandPlugin` (`canAccess`: `OWNER` exactly) — superadmin/audit/ban/promote/spawn tools.
- `AdminCommandPlugin` (`canAccess`: `isAdministrator(player)`) — shared staff tools; also usable by Manager and Owner.
- `ManagerCommandPlugin` (`canAccess`: `isManager(player)`) — promotion and slayer task tools; also usable by Admin and Owner.
- `ModeratorCommandPlugin`, `HelperCommandPlugin`, `DonatorCommandPlugin` exist as separate tiers when needed.

New practical staff commands added:
- `::settask <SlayerTask>` — set your own task by enum name.
- `::taskamount <amount>` — set your own task amount.
- `::addtaskamount <amount>` — add to your own task amount.
- `::removetask` / `::removeslayertask` — clear your own task.

---

## 13. Admin / Owner / Manager Command Registry

### `OwnerCommandPlugin`

Access: `player.right == OWNER`.

| Command | Aliases | Notes |
|---------|---------|-------|
| `::ban` | | Bans named online player |
| `::unban` | | Unbans by username |
| `::ipban` | | IP-bans online player's host |
| `::ipmute` | | IP-mutes online player |
| `::unipmute` | | Removes IP-mute |
| `::kill` | | Kills named player |
| `::giveitem` / `::gi` | | Give item to named player |
| `::giveexp` / `::giveexperience` | | Grant 1.5M XP in a skill to named player |
| `::settitle` | | Set title for named player |
| `::setpt` / `::setplaytime` | | Set playtime for named player |
| `::setrank` / `::giverank` / `::rank` | | Promote named player to Ironman/Ultimate/Hardcore/Manager/Developer |
| `::checkaccs` | | Show accounts sharing the same host as the selected player |
| `::bombs` | | Visual effect debug |
| `::bloodmoneychest` | | Force-spawn blood money chest |
| `::resetplayer` | | Reset named player skills/inventory/equipment/bank |
| `::doubleexp` | | Toggle global double XP |
| `::wildplayers` | | List wilderness players |
| `::randomevent` | | Trigger MimeEvent for named player |
| `::alltome` | | Teleport all non-bot players to you |
| `::fight` | | Spawn/command two NPCs to fight |
| `::pnpc` | | Transform yourself into an NPC |
| `::spawnnpc` | | Spawn NPC and write per-file to `data/def/npc-spawns-json/{id}.json` |
| `::item` / `::pickup` | | Spawn item into inventory |
| `::find` / `::give` | | Search item definitions by name and open result interface |
| `::pos` / `::mypos` / `::coords` | | Print your current position |

### `AdminCommandPlugin`

Access: `isAdministrator(player)`. Inherited by Manager and Owner.

| Command | Aliases | Notes |
|---------|---------|-------|
| `::points` | | Set many currencies at once |
| `::demote` | | Demote named player to `PLAYER` |
| `::save` / `::saveworld` / `::savegame` | | Flush world state |
| `::bank` | | Open your bank |
| `::move` | | Teleport by delta |
| `::tele` | | Teleport by absolute coords |
| `::spellbook` | | Swap spellbook |
| `::starterbank` | | Reset bank to starter loadout |
| `::bigbank` | | Reset bank to large preset |
| `::maxrng` / `::maxrange` / `::maxranged` | | Fill inventory with max ranged kit |
| `::maxmelee` | | Fill inventory with max melee kit |
| `::maxmagic` / `::maxmage` | | Fill inventory with max magic kit |

Targets: commands with self-only behavior vs named targets varies by implementation.

### `ManagerCommandPlugin`

Access: `isManager(player)`. Inherited by Admin and Owner.

| Command | Aliases | Notes |
|---------|---------|-------|
| `::broadcast` | | Server-wide announcement |
| `::discord` | | Discord bridge message stub |
| `::promote` | | Promote named player to Helper/Moderator/Admin |
| `::master` | | Max all skills |
| `::removeslayertask` / `::removetask` | | Clear named player's slayer task |
| `::settask` | | Set your slayer task by `SlayerTask` enum name |
| `::taskamount` | | Set your slayer task amount |
| `::addtaskamount` | | Add to your slayer task amount |

---

## 12. Editor / Build Preferences

- Server build: `.\gradlew.bat :game-server:classes`
- Client build: `.\gradlew.bat :game-client:classes`
- Tests: `.\gradlew.bat :game-server:test`
- Run server: `.\gradlew.bat :game-server:run`
- No loose files outside intended directory structure. Scripts go in `scripts/<purpose>/`.
- Keep old files after migration swaps until verified in-game.
- Scripts that write files should always include a `--dry-run` mode.
