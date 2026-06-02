# Add Skill Action

**Goal:** Add a new skilling activity — gathering, production, or processing.

**Docs:** `AGENTS.md` §Skills, `docs/workflows/skills.md`, `docs/workflows/plugins.md`, `04-systems/skills.md`

---

## Files to create/edit

| File | Purpose |
|------|---------|
| `game-server/src/main/java/com/osroyale/content/skill/impl/<skill>/CustomAction.java` | New skill action class |
| `game-server/plugins/plugin/click/object/CustomSkillObjectPlugin.java` | Wire to an object |
| OR `game-server/plugins/plugin/click/npc/CustomSkillNpcPlugin.java` | Wire to an NPC |

---

## SkillAction base class

From `game-server/src/main/java/com/osroyale/content/skill/SkillAction.java`:

```java
public abstract class SkillAction {
    public abstract void onSuccess();          // What happens on success
    public abstract int experience();          // XP granted
    public abstract int skill();               // Skill ID (0-22)
    public abstract boolean canRun();          // Requirements check
    public abstract int requiredLevel();       // Level requirement
    public abstract double successFactor();    // Success rate multiplier
}
```

---

## Step 1: Create skill action

Create `game-server/src/main/java/com/osroyale/content/skill/impl/mining/CustomOreAction.java`:

```java
package com.osroyale.content.skill.impl.mining;

import com.osroyale.content.skill.SkillAction;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.object.GameObject;

public class CustomOreAction extends SkillAction {

    private final int oreId;
    private final int requiredLevel;
    private final int experience;

    public CustomOreAction(Player player, GameObject object, int oreId, int level, int xp) {
        super(player, object);
        this.oreId = oreId;
        this.requiredLevel = level;
        this.experience = xp;
    }

    @Override
    public void onSuccess() {
        player.inventory.add(new Item(oreId, 1));
        player.skills.addExperience(skill(), experience);
        player.sendMessage("You mine some ore.");
    }

    @Override
    public int experience() {
        return experience;
    }

    @Override
    public int skill() {
        return 14;  // Mining = 14
    }

    @Override
    public boolean canRun() {
        return player.skills.getLevel(skill()) >= requiredLevel
            && player.inventory.hasCapacityFor(new Item(oreId, 1));
    }

    @Override
    public int requiredLevel() {
        return requiredLevel;
    }

    @Override
    public double successFactor() {
        return 1.0;
    }
}
```

Skill IDs: 0=Attack, 1=Defence, 2=Strength, 3=Hitpoints, 4=Ranged, 5=Prayer, 6=Magic, 7=Cooking, 8=Woodcutting, 9=Fletching, 10=Fishing, 11=Firemaking, 12=Crafting, 13=Smithing, 14=Mining, 15=Herblore, 16=Agility, 17=Thieving, 18=Slayer, 19=Farming, 20=Runecrafting, 21=Construction, 22=Hunter

---

## Step 2: Wire to an object

Create or edit `game-server/plugins/plugin/click/object/CustomOrePlugin.java`:

```java
package plugin.click.object;

import com.osroyale.content.skill.impl.mining.CustomOreAction;
import com.osroyale.game.event.impl.ObjectClickEvent;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;

public class CustomOrePlugin extends PluginContext {

    @Override
    protected boolean firstClickObject(Player player, ObjectClickEvent event) {
        switch (event.getObject().getId()) {
            case 12345:  // Custom ore rock object ID
                player.action.execute(new CustomOreAction(player, event.getObject(), 451, 15, 35));
                return true;
        }
        return false;
    }
}
```

---

## Step 3: Recompile and test

```bash
./gradlew :game-server:classes
```

Test in-game by interacting with the wired object. Check:
- XP is gained
- Item appears in inventory
- Level requirement is enforced
- Animation plays correctly

---

## Client Impact

Skill actions are server-side logic. The client just receives XP drops and inventory updates. No client impact.

If you need custom animations or graphics, those require client-side model/animation data.

---

## Verify

- [ ] Skill action executes on object click
- [ ] XP is correctly calculated and granted
- [ ] Level requirement blocks low-level players
- [ ] Item is added to inventory on success
- [ ] Inventory full check works (action stops)
- [ ] Skill modifier from settings.toml is applied
