# Skills

Skills live in `game-server/src/main/java/com/osroyale/content/skill/impl/` with data in `game-server/data/content/skills/`.

## Adding a skill action

Skills extend `SkillAction` base class:

```java
// File: game-server/src/main/java/com/osroyale/content/skill/impl/custom/CustomSkill.java
package com.osroyale.content.skill.impl.custom;

import com.osroyale.content.skill.SkillAction;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.object.GameObject;

public class CustomSkill extends SkillAction {
    
    public CustomSkill(Player player, GameObject object) {
        super(player, object);  // or super(player, position) for non-object skills
    }

    @Override
    public void onSuccess() {
        player.inventory.add(new Item(995, 100));  // Reward
        player.skills.addExperience(skill(), experience());
        player.sendMessage("You successfully perform the action.");
    }

    @Override
    public int experience() {
        return 100;
    }

    @Override
    public int skill() {
        return 17;  // Skill ID (see Skill constants)
    }

    @Override
    public boolean canRun() {
        // Check requirements: level, tools, inventory space
        return player.skills.getLevel(skill()) >= requiredLevel();
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double successFactor() {
        return 1.0;
    }
}
```

## Skill IDs

| ID | Skill |
|----|-------|
| 0 | Attack |
| 1 | Defence |
| 2 | Strength |
| 3 | Hitpoints |
| 4 | Ranged |
| 5 | Prayer |
| 6 | Magic |
| 7 | Cooking |
| 8 | Woodcutting |
| 9 | Fletching |
| 10 | Fishing |
| 11 | Firemaking |
| 12 | Crafting |
| 13 | Smithing |
| 14 | Mining |
| 15 | Herblore |
| 16 | Agility |
| 17 | Thieving |
| 18 | Slayer |
| 19 | Farming |
| 20 | Runecrafting |
| 21 | Construction |
| 22 | Hunter |

## Wiring a skill to an object

In an object click plugin:

```java
@Override
protected boolean firstClickObject(Player player, ObjectClickEvent event) {
    switch (event.getObject().getId()) {
        case 12345:  // Custom rock, tree, etc.
            player.action.execute(new CustomSkill(player, event.getObject()));
            return true;
    }
    return false;
}
```

## Skill data files

Skill configuration JSON lives in `game-server/data/content/skills/`. Example `agility.json` contains obstacle coordinates and course data.

## Existing skill implementations

See `game-server/src/main/java/com/osroyale/content/skill/impl/` for full examples:
- `Mining.java`, `Fishing.java`, `Woodcutting.java` — gathering skills
- `Cooking.java`, `Smithing.java`, `Fletching.java` — production skills
- `Agility.java` — complex obstacle course system

## Steps

1. Create skill class extending `SkillAction`
2. Wire it to an object/NPC via a click plugin
3. Add data file in `data/content/skills/` if needed
4. Recompile
5. Verify in-game: interact with the object
