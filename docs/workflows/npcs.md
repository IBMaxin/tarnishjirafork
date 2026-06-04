# NPCs

Three data files control NPC definitions, spawns, and drops.

## 1. NPC definitions — `game-server/data/def/npc/npc_definitions.json`

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

## 2. NPC spawns — per-file system (active)

Each NPC gets its own file: `game-server/data/def/npc-spawns-json/{npcId}.json`

```json
[
  {
    "id": 9999,
    "position": {"x": 3200, "y": 3400, "height": 0},
    "radius": "5",
    "facing": "NORTH"
  }
]
```

Fields: `id`, `position` (x, y, height), `radius` (string), `facing` (NORTH/EAST/SOUTH/WEST), `convert-id` (boolean, default true), `instance` (int, default 0)

An NPC ID can have multiple spawn locations (multiple entries in the array).

> **Old system:** `data/def/npc/npc_spawns.json` (35K lines) — kept as reference, no longer loaded by the server.

## 3. NPC drops — per-file system (active)

Each NPC gets its own file: `game-server/data/def/npc-drops-json/{npcId}.json`

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

Chance values: `ALWAYS`, `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`

Drop types: `NORMAL`, `PET`, `CLUE`, `RDT`, `TERRITORY`

> **Old system:** `data/def/npc/npc_drops.json` (107K lines) — kept as reference, no longer loaded by the server.

## Click handler (optional)

For NPC interactions beyond combat, create a plugin:

```java
package plugin.click.npc;

import com.osroyale.game.event.impl.NpcClickEvent;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;

public class CustomNpcPlugin extends PluginContext {
    @Override
    protected boolean firstClickNpc(Player player, NpcClickEvent event) {
        switch (event.getNpc().id) {
            case 9999:
                player.dialogueFactory.sendStatement("The boss glares at you.");
                return true;
        }
        return false;
    }
}
```

File location: `game-server/plugins/plugin/click/npc/`

Available click types: `firstClickNpc`, `secondClickNpc`, `thirdClickNpc`, `fourthClickNpc`

## Steps

1. Add/update NPC in `npc_definitions.json`
2. Add spawn location in `npc-spawns-json/{npcId}.json`
3. Add drops in `npc-drops-json/{npcId}.json`
4. Optionally add click handler plugin
5. Recompile
6. Verify in-game: `::spawnnpc <id>` or walk to spawn location
