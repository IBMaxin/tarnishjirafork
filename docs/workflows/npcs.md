# NPCs

Three data files control NPCs:

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

## 2. NPC spawns — `game-server/data/def/npc/npc_spawns.json`

```json
{
  "id": 9999,
  "position": {"x": 3200, "y": 3400, "height": 0},
  "walk-radius": 5,
  "face": "NORTH"
}
```

Fields: `id`, `position` (x, y, height), `walk-radius`, `face` (NORTH/EAST/SOUTH/WEST)

## 3. NPC drops — `game-server/data/def/npc/npc_drops.json`

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
2. Add spawn location in `npc_spawns.json`
3. Add drops in `npc_drops.json` (multi-NPC arrays supported)
4. Optionally add click handler plugin
5. Recompile
6. Verify in-game: `::spawnnpc <id>` or walk to spawn location
