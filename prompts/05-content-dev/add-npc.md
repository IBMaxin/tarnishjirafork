# Add NPC

**Goal:** Add a new NPC with definition, spawn location, drops, and click handler.

**Docs:** `AGENTS.md` §NPCs, `docs/workflows/npcs.md`, `00-cross-cutting/client-server-boundary.md`, `02-data-audit/npc-consistency.md`

---

## Files to edit

| File | Purpose | Required? |
|------|---------|-----------|
| `game-server/data/def/npc/npc_definitions.json` | NPC name, combat, size, aggression | **Yes** |
| `game-server/data/def/npc/npc_spawns.json` | Where the NPC appears | **Yes** |
| `game-server/data/def/npc/npc_drops.json` | Drop table | Optional but expected |
| `game-server/plugins/plugin/click/npc/CustomNpcPlugin.java` | Click interaction | Optional |

---

## Step 1: NPC definition

Edit `game-server/data/def/npc/npc_definitions.json`:

```json
{
  "id": 9999,
  "name": "Custom boss",
  "examine": "It looks dangerous.",
  "combat": 250,
  "hitpoints": 500,
  "attack": 200,
  "defence": 150,
  "size": 2,
  "aggressive": true,
  "poisonous": false
}
```

---

## Step 2: Spawn location

Edit `game-server/data/def/npc/npc_spawns.json`:

```json
{
  "id": 9999,
  "position": {"x": 3200, "y": 3400, "height": 0},
  "walk-radius": 5,
  "face": "NORTH"
}
```

Face: NORTH, EAST, SOUTH, or WEST.
Walk radius: 0 = stationary, > 0 = wanders within radius tiles.

---

## Step 3: Drop table

Edit `game-server/data/def/npc/npc_drops.json`:

```json
{
  "npc": [9999],
  "drop-type": "NORMAL",
  "drops": [
    {"item": 4151, "min": 1, "max": 1, "chance": "ALWAYS"},
    {"item": 995, "min": 100000, "max": 500000, "chance": "COMMON"},
    {"item": 11732, "min": 1, "max": 1, "chance": "RARE"}
  ]
}
```

Chances: ALWAYS, COMMON, UNCOMMON, RARE, VERY_RARE
Types: NORMAL, PET, CLUE, RDT, TERRITORY

---

## Step 4: Click handler (optional)

Create `game-server/plugins/plugin/click/npc/CustomBossPlugin.java`:

```java
package plugin.click.npc;

import com.osroyale.game.event.impl.NpcClickEvent;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;

public class CustomBossPlugin extends PluginContext {

    @Override
    protected boolean firstClickNpc(Player player, NpcClickEvent event) {
        switch (event.getNpc().id) {
            case 9999:
                player.dialogueFactory.sendStatement("The boss glares at you menacingly.");
                return true;
        }
        return false;
    }
}
```

Click types: `firstClickNpc`, `secondClickNpc`, `thirdClickNpc`, `fourthClickNpc`

---

## Step 5: Recompile and test

```bash
./gradlew :game-server:classes
```

Test in-game:
```
::spawnnpc 9999         (spawn at your position)
::teleport 3200 3400     (go to spawn location)
```

---

## Client Impact

→ See [client-server-boundary.md](../00-cross-cutting/client-server-boundary.md)

- NPC definitions, spawns, drops: server-only
- NPC rendering: client needs model. If using existing OSRS NPC ID, model exists. Custom IDs may not render.
- Walk radius/facing: server handles, client renders movement

---

## Verify

- [ ] NPC spawns at correct location (walk there or use ::spawnnpc)
- [ ] NPC has correct combat level and hitpoints
- [ ] NPC drops items on death
- [ ] Click handler works (dialogue opens, shop opens, etc.)
- [ ] NPC is aggressive if configured
- [ ] NPC renders correctly in-game
