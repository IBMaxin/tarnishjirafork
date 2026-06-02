# Plugin System

Plugins are the primary content extension point. They live in `game-server/plugins/plugin/` and are auto-discovered at boot (133 loaded on startup).

## Plugin types by directory

| Directory | Purpose | Base class | Key methods |
|-----------|---------|------------|-------------|
| `click/item/` | Item click/inventory use | `PluginContext` | `firstClickItem`, `secondClickItem`, `thirdClickItem` |
| `click/npc/` | NPC interaction | `PluginContext` | `firstClickNpc`, `secondClickNpc`, `thirdClickNpc`, `fourthClickNpc` |
| `click/object/` | Object interaction | `PluginContext` | `firstClickObject`, `secondClickObject`, `thirdClickObject` |
| `click/button/` | Interface button clicks | `PluginContext` | `onClick(Player, int button)` |
| `click/itemcontainer/` | Container actions (bank, etc) | `PluginContext` | Container-specific methods |
| `command/` | Chat commands | `CommandExtension` | `register()` → `commands.add(new Command(...))` |
| `itemon/item/` | Use item on item | `PluginContext` | `onItemOnItem` |
| `itemon/npc/` | Use item on NPC | `PluginContext` | `onItemOnNpc` |
| `itemon/object/` | Use item on object | `PluginContext` | `onItemOnObject` |
| `itemon/player/` | Use item on player | `PluginContext` | `onItemOnPlayer` |

## Item click pattern

```java
package plugin.click.item;

import com.osroyale.game.event.impl.ItemClickEvent;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.net.packet.out.SendMessage;

public class EatFoodPlugin extends PluginContext {

    @Override
    protected boolean firstClickItem(Player player, ItemClickEvent event) {
        if (event.getItem().matchesId(315)) {  // Shrimp
            player.heal(3);
            player.inventory.remove(event.getItem(), 1);
            player.send(new SendMessage("You eat the shrimp."));
            return true;  // handled
        }
        return false;  // not handled, pass to next plugin
    }
}
```

Key: return `true` to consume the event, `false` to let other plugins handle it.

## NPC click pattern

```java
@Override
protected boolean firstClickNpc(Player player, NpcClickEvent event) {
    switch (event.getNpc().id) {
        case 1234:
            player.dialogueFactory.sendStatement("Hello traveler!");
            return true;
    }
    return false;
}
```

## Object click pattern

```java
@Override
protected boolean firstClickObject(Player player, ObjectClickEvent event) {
    switch (event.getObject().getId()) {
        case 2213:  // Bank booth
            player.bank.open();
            return true;
    }
    return false;
}
```

## Steps to add a plugin

1. Create file in the correct `plugin/<type>/` directory
2. Extend `PluginContext` (or `CommandExtension` for commands)
3. Override the appropriate click/interaction method
4. Return `true` when handled, `false` to pass through
5. Recompile — plugins are auto-registered

## Plugin auto-discovery

Plugins are in the `plugins` source set (`game-server/build.gradle.kts` line 59). Kotlin plugins (`.kt`) are also supported.
