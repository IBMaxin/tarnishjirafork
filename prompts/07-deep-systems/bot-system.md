# Bot System Audit

**Goal:** Audit the PlayerBot system for economy impact, abuse vectors, and architectural risks.

**Docs:** `AGENTS.md`, `code_index.json`, `prompts/03-security/economy-risks.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/bot/PlayerBot.java` | Extends Player directly — full player API access |
| `game-server/src/main/java/com/osroyale/content/bot/BotUtility.java` | Name generation, appearance, titles |
| `game-server/src/main/java/com/osroyale/content/bot/objective/BotObjective.java` | Enum: WALK_IN_WILDERNESS, WALK_TO_DITCH, WALK_TO_BANK, RESTOCK, COMBAT |
| `game-server/src/main/java/com/osroyale/content/bot/objective/impl/CombatObjective.java` | Combat behavior, targeting |
| `game-server/src/main/java/com/osroyale/content/bot/objective/impl/BankObjective.java` | Banking behavior |
| `game-server/src/main/java/com/osroyale/content/bot/objective/impl/RestockObjective.java` | Restocking inventory |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/BotClass.java` | Bot class interface |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/impl/PureMelee.java` | Melee bot preset |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/impl/WelfareRuneMelee.java` | Welfare rune melee preset |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/impl/AGSRuneMelee.java` | AGS rune melee preset |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/impl/ZerkerMelee.java` | Zerker melee preset |
| `game-server/src/main/java/com/osroyale/content/bot/botclass/impl/PureRangeMelee.java` | Range-melee hybrid preset |
| `game-server/settings.toml` | `max_bots = 10` |
| `game-server/src/main/java/com/osroyale/game/task/impl/BotStartupEvent.java` | Bot spawn/startup logic |

---

## Architecture

`PlayerBot extends Player` — this is significant. Bots have the full `Player` API:

```
PlayerBot
├── Player
│   ├── inventory, equipment, bank
│   ├── combat (getCombat(), attack(), retaliate())
│   ├── skills, prayer
│   ├── teleportation (Teleportation.teleport())
│   ├── spellbook (Lunar by default)
│   ├── CombatSpecial (100% spec restored on creation)
│   └── All Player methods accessible
├── Bot-specific:
│   ├── schedule(ticks, Runnable) — deferred action
│   ├── loop(ticks, Runnable) — repeating action
│   ├── pot(Mob, ItemClickEvent, PotionData) — drink potion
│   ├── botClass.eat() / botClass.pot() — consumable management
│   ├── botClass.handleCombat() — combat strategy
│   ├── opponent — current combat target
│   ├── foodRemaining, statBoostersRemaining
│   └── consumableDelay
```

---

## Steps

### 1. Verify bot count and spawn logic

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Config: max_bots = 10
grep "max_bots" game-server/settings.toml

# Bot startup event
cat game-server/src/main/java/com/osroyale/game/task/impl/BotStartupEvent.java

# Bot count tracking
grep -rn "BOT_COUNT\|max_bots" game-server/src/main/java/com/osroyale/content/bot/
```

### 2. Check if bots generate real drops

```bash
# Do bots get NPC drops? Check if NPC drops are per-player or global
grep -rn "isBot\|\.isBot\|player.isBot" game-server/src/main/java/com/osroyale/game/world/entity/mob/npc/
grep -rn "isBot" game-server/src/main/java/com/osroyale/content/activity/

# Do bot kills generate loot for real players?
grep -rn "PlayerBot\|isBot" game-server/src/main/java/com/osroyale/game/world/entity/mob/npc/drop/
```

### 3. Check bot combat capabilities

```bash
# Combat objectives — can bots attack specific players?
cat game-server/src/main/java/com/osroyale/content/bot/objective/impl/CombatObjective.java

# Bot retaliate() — line 167-188 in PlayerBot.java
# Bots auto-retaliate against any player who attacks them
# Can this be used to lure bots?
```

### 4. Check bot inventory/economy interaction

```bash
# Restock objective — where do bot items come from?
cat game-server/src/main/java/com/osroyale/content/bot/objective/impl/RestockObjective.java

# Do bots have unlimited supplies or do they draw from the economy?
grep -rn "inventory.add\|inventory.set\|equipment.equip" \
  game-server/src/main/java/com/osroyale/content/bot/botclass/impl/
```

### 5. Check bot teleportation/position abuse

PlayerBot.java line 237:
```java
Teleportation.teleport(this, Config.DEFAULT_POSITION, 20, TeleportationData.MODERN, () -> BotObjective.WALK_TO_BANK.init(this));
```

Bots teleport freely after combat. No wilderness restrictions, no teleblock checks. This is architectural (bots aren't subject to normal player restrictions).

### 6. Check bot connection to player list

```bash
# Bots register in World.getPlayers() — do they appear in player counts?
grep -rn "getPlayers\|getPlayerCount" game-server/src/main/java/com/osroyale/content/bot/

# Can players trade bots?
grep -rn "isBot\|PlayerBot" game-server/src/main/java/com/osroyale/content/tradingpost/
grep -rn "isBot\|PlayerBot" game-server/plugins/plugin/
```

---

## Findings

### F1: Bots extend Player — full API surface [HIGH]

`PlayerBot extends Player` — bots can do everything a player can: trade, drop items, pick up items, cast spells, use commands (if a command plugin doesn't check `isBot`). Any security check that only gates on `instanceof Player` passes for bots.

### F2: Bots teleport freely post-combat [MEDIUM]

Line 237: `Teleportation.teleport(this, Config.DEFAULT_POSITION, 20, ...)` — no wilderness level check, no teleblock check, no cooldown. This is intentional (bots need to reset) but means bots bypass all teleport restrictions.

### F3: Unknown item source for restocking [MEDIUM]

RestockObjective — need to verify whether bot items are spawned from thin air or drawn from actual economy. If spawned, 10 bots generating items = inflation vector.

### F4: Bot combat vs real players [LOW]

Bots retaliate against any attacker. If a real player attacks a bot and runs, the bot will pursue. In wilderness, this could be used to lure bots into multi-combat zones.

### F5: Bots count toward player limit [INFO]

`PlayerBot.register()` calls `World.getPlayers().add(this)`. 10 bots = 10 fewer slots for real players.

---

## Client Impact

Server-only. Bots exist entirely server-side — they don't have a real client connection. All bot actions are server-driven.

---

## Verify

- [ ] Bots do not generate items from thin air (verify RestockObjective source)
- [ ] Bots cannot be traded/gifted items by real players
- [ ] Bot drops (from NPC kills) are flagged or prevented
- [ ] Bot combat does not affect real player kill/death ratios
- [ ] Bot teleportation does not bypass wilderness checks (if relevant)
- [ ] `isBot` check exists in all economy-sensitive code paths
