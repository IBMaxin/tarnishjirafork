# Game Scope

What's in the codebase. Source presence ≠ runtime function — everything here is a candidate for testing, not a guarantee.

→ **Read first:** `AGENTS.md` for project structure, build commands, and file discovery.
→ **Verify with:** `prompts/` for systematic audit prompts.
→ **How-to guides:** [workflows/](workflows/README.md) — add commands, items, NPCs, shops, skills.
→ **Test strategy:** [test-plan.md](test-plan.md)

---

## Current State

- Fork: `https://github.com/IBMaxin/tarnishjirafork.git`
- Upstream: `https://github.com/Jire/tarnish`
- Client: runs without SwiftFUP using local cache at `%USERPROFILE%\.tarnish\cache`
- Server boot: `Startup service finished` → `Loaded: 133 plugins` → `Server built successfully`
- Tests: `Zezima` = OWNER, `Oak` = ADMINISTRATOR

---

## Activities & Bossing

| Activity | Directory |
|----------|-----------|
| Barrows | `content/activity/impl/barrows/` |
| Battleground | `content/activity/impl/battleground/` |
| Duel Arena | `content/activity/impl/duelarena/` |
| Fight Caves | `content/activity/impl/fightcaves/` |
| God Wars | `content/activity/impl/godwars/` |
| Inferno | `content/activity/inferno/` (separate from impl/) |
| Kraken | `content/activity/impl/kraken/` |
| Last Man Standing | `content/activity/impl/` + `content/lms/` |
| Mage Arena | `content/activity/impl/magearena/` |
| Pest Control | `content/activity/impl/pestcontrol/` |
| Recipe for Disaster | `content/activity/impl/recipefordisaster/` |
| Shooting Stars | `content/shootingstar/` |
| Warrior Guild | `content/activity/impl/warriorguild/` |
| Wintertodt | `content/wintertodt/` + `plugins/click/object/wintertodt/` |
| Zulrah | `content/activity/impl/zulrah/` |

---

## Skills

All 23 skills have implementation directories under `content/skill/impl/`:

`Agility` `Construction` `Cooking` `Crafting` `Farming` `Firemaking` `Fishing` `Fletching` `Herblore` `Hunter` `Magic` `Mining` `Prayer` `Runecrafting` `Slayer` `Smithing` `Thieving` `Woodcutting`

Base class: `content/skill/SkillAction.java`
Data files: `data/content/skills/`
XP modifiers: `settings.toml` (all 30x except combat 12.5x)

---

## Economy & Progression

| System | Location |
|--------|----------|
| Achievements | `content/achievement/` |
| Collection log | `content/collectionlog/` |
| Drop viewer/simulator | `content/simulator/` |
| Mystery boxes | `content/mysterybox/` |
| Pets | `content/pet/` |
| Presets | `content/preset/` |
| Prestige | `content/prestige/` |
| Shops | `data/def/store/stores.json` + `content/store/` |
| Trading post | `content/tradingpost/` |
| Starter kits | `StarterKit.java` |
| Donator zones | `content/donators/` |
| Blood money | `content/bloodmoney/` |
| Clue scrolls | `plugins/plugin/click/item/ClueScrollPlugin.java` |
| Utility bags | rune pouch, looting bag, gem bag, coal bag — `plugins/itemon/` |

---

## Commands by Rank

Commands are in `plugins/plugin/command/<Rank>CommandPlugin.java`:

| Rank | File | Example commands |
|------|------|-----------------|
| Player | `PlayerCommandPlugin.java` | home, players, staff, drops, vote, donate, trivia |
| Donator | `DonatorCommandPlugin.java` | yell, donor-zone |
| Helper | `HelperCommandPlugin.java` | — |
| Moderator | `ModeratorCommandPlugin.java` | — |
| Admin | `AdminCommandPlugin.java` | spawnitem, spawnnpc, teleport, bank |
| Manager | `ManagerCommandPlugin.java` | — |
| Developer | `DeveloperCommandPlugin.java` | — |
| Owner | `OwnerCommandPlugin.java` | giveitem, setrank, ban, kill, resetplayer |

→ Full audit: `prompts/03-security/command-audit.md`

---

## Offline Verification

What can be checked without logging into the game client.

### Build & Test

**Windows:**
```powershell
.\gradlew.bat :game-server:classes
.\gradlew.bat :game-client:classes
.\gradlew.bat :game-server:test
```

**WSL/Linux:**
```bash
./gradlew :game-server:classes
./gradlew :game-client:classes
./gradlew :game-server:test
```

→ Prompts: `prompts/01-build-verify/compile.md`, `test.md`

### Server Smoke

Start server and watch for:
```
Startup service finished
Loaded: 133 plugins
Server built successfully
```

**Windows:** `.\gradlew.bat :game-server:run`
**WSL:** `./gradlew :game-server:run`

Verify port: `Test-NetConnection localhost -Port 43594` (Windows) / `nc -zv localhost 43594` (WSL)

→ Prompt: `prompts/01-build-verify/server-smoke.md`

### Profile Rights

Check `data/profile/save/Zezima.json` and `Oak.json`:
- Zezima → `player-rights: OWNER`
- Oak → `player-rights: ADMINISTRATOR`, not OWNER

→ Prompt: `prompts/02-data-audit/profile-rights.md`

### Data File Sanity

Parse all JSON under `data/def/`:
- `stores.json` (75 KB)
- `npc_spawns.json` (515 KB)
- `npc_drops.json` (1.9 MB)
- `item_definitions.json` (2.8 MB)

→ Prompts: `prompts/02-data-audit/json-parse.md`, `item-consistency.md`, `npc-consistency.md`

### Cache Files

Client cache at `%USERPROFILE%\.tarnish\cache\`:
```
main_file_cache.dat
main_file_cache.idx0 .. idx5
```
`idx3` may be empty — not a failure.

---

## Needs In-Game Verification

These require logging in with a client:

- Login flow, character rendering, region loading
- Eating, potions, inventory actions
- Teleport destination safety
- Staff/donator command behavior
- Combat with NPCs and players
- Shop buy/sell transactions
- Trading post
- NPC drops after kills
- Skill XP gain and level progression
- Minigame entry, completion, rewards, exits
- Dialogue and interface behavior
- Pathing, clipping, doors, ladders, objects
- Account persistence after logout/restart

---

## In-Game Smoke Checklist

Once logged in as Admin:

1. Login as Oak, confirm admin crown
2. Try owner command → confirm blocked (Oak is not owner)
3. `::spawnitem 315` — eat shrimp, verify healing
4. `::teleport` home, donor zone
5. Kill low-level NPC, check drops
6. Open shop, buy item, sell item
7. Try one skill: fish, mine, woodcut, cook
8. Enter and exit one minigame
9. Logout, restart server, login — verify persistence

---

## Risk Notes

- SwiftFUP mode ≠ local-cache mode — test independently if production depends on it
- Source presence ≠ feature complete — code exists, quality unknown
- Staff commands can hide economy/permission bugs
- Server appears "hung" after boot — normal, it's waiting for players
