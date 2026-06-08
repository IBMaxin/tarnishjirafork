# Game Inventory

Game inventory — what exists, what's verified. Source presence ≠ runtime function.

---

## Current State

- Fork: `https://github.com/IBMaxin/tarnishjirafork.git`
- Upstream: `https://github.com/Jire/tarnish`
- Client: runs without SwiftFUP using local cache at `%USERPROFILE%\\.tarnish\\cache`
- Server boot: `Startup service finished` → `Loaded: 133 plugins` → `Server built successfully`

---

## Activities & Bossing

| Activity | Directory |
|----------|-----------|
| Barrows | `content/activity/impl/barrows/` |
| Battleground | `content/activity/impl/battleground/` |
| Cerberus | `content/activity/impl/cerberus/` |
| Chaos Fanatic | `content/activity/impl/chaosfanatic/` |
| Crazy Archaeologist | `content/activity/impl/crazyarchaeologist/` |
| Duel Arena | `content/activity/impl/duelarena/` |
| Fight Caves | `content/activity/impl/fightcaves/` |
| God Wars | `content/activity/impl/godwars/` |
| Hydra | `content/activity/impl/hydra/` |
| Inferno | `content/activity/inferno/` (separate from impl/) |
| Kraken | `content/activity/impl/kraken/` |
| Last Man Standing | `content/activity/impl/` + `content/lms/` |
| Mage Arena | `content/activity/impl/magearena/` |
| Pest Control | `content/activity/impl/pestcontrol/` |
| Recipe for Disaster | `content/activity/impl/recipefordisaster/` |
| Shooting Stars | `content/shootingstar/` |
| Skotizo | `content/activity/impl/skotizo/` |
| Venenatis | `content/activity/impl/venenatis/` |
| Vet'ion | `content/activity/impl/vetion/` |
| Vorkath | `content/activity/impl/vorkath/` |
| Warrior Guild | `content/activity/impl/warriorguild/` |
| Wintertodt | `content/wintertodt/` + `plugins/click/object/wintertodt/` |
| Zulrah | `content/activity/impl/zulrah/` |

---

## Skills

18 skills have implementation directories under `content/skill/impl/`:

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

## Cache Files

Client cache at `%USERPROFILE%\\.tarnish\\cache\\`:
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

## Risk Notes

- SwiftFUP mode ≠ local-cache mode — test independently if production depends on it
- Source presence ≠ feature complete — code exists, quality unknown
- Staff commands can hide economy/permission bugs
- Server appears "hung" after boot — normal, it's waiting for players
