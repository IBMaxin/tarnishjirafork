# Commands

## File: `game-server/plugins/plugin/command/<Rank>CommandPlugin.java`

Commands are organized by permission rank. Each rank has its own plugin file.

| Rank | File |
|------|------|
| Player | `PlayerCommandPlugin.java` |
| Donator | `DonatorCommandPlugin.java` |
| Helper | `HelperCommandPlugin.java` |
| Moderator | `ModeratorCommandPlugin.java` |
| Admin | `AdminCommandPlugin.java` |
| Manager | `ManagerCommandPlugin.java` |
| Developer | `DeveloperCommandPlugin.java` |
| Owner | `OwnerCommandPlugin.java` |

## Pattern

Every command plugin extends `CommandExtension` and registers commands in `register()`:

```java
package plugin.command;

import com.osroyale.game.plugin.extension.CommandExtension;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.command.Command;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import com.osroyale.net.packet.out.SendMessage;

public class AdminCommandPlugin extends CommandExtension {

    @Override
    public void register() {
        // No-arg command
        commands.add(new Command("heal") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.heal();
                player.send(new SendMessage("You have been healed!"));
            }
        });

        // Command with arguments
        commands.add(new Command("spawnitem") {
            @Override
            public void execute(Player player, CommandParser parser) {
                int itemId = parser.nextInt();
                int amount = parser.hasNext() ? parser.nextInt() : 1;
                player.inventory.add(itemId, amount);
            }
        });
    }
}
```

## Key classes

- `CommandExtension` — base class for command plugins
- `CommandParser` — `nextInt()`, `nextString()`, `hasNext()`
- `SendMessage` — send chat message to player
- `Player` — `inventory`, `equipment`, `bank`, `position`, `heal()`, etc.

## Steps to add a command

1. Open the rank-appropriate plugin file
2. Add a `new Command("name")` block inside `register()`
3. Implement `execute(Player player, CommandParser parser)`
4. Recompile: `./gradlew :game-server:classes`
5. Verify in-game: `::name`
