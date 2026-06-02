# Add Command

**Goal:** Add a new chat command to the appropriate rank plugin.

**Docs:** `AGENTS.md` §Commands, `docs/workflows/commands.md`, `03-security/command-audit.md`, `03-security/privilege-escalation.md`

---

## Which rank file?

| Rank | File |
|------|------|
| Player | `game-server/plugins/plugin/command/PlayerCommandPlugin.java` |
| Donator | `DonatorCommandPlugin.java` |
| Helper | `HelperCommandPlugin.java` |
| Moderator | `ModeratorCommandPlugin.java` |
| Admin | `AdminCommandPlugin.java` |
| Manager | `ManagerCommandPlugin.java` |
| Developer | `DeveloperCommandPlugin.java` |
| Owner | `OwnerCommandPlugin.java` |

---

## Pattern

```java
// Inside the register() method of the appropriate plugin:

commands.add(new Command("mycommand", "mc") {  // primary name + alias
    @Override
    public void execute(Player player, CommandParser parser) {
        // 1. Validate arguments
        if (!parser.hasNext()) {
            player.send(new SendMessage("Usage: ::mycommand <arg1> [arg2]"));
            return;
        }

        int arg1 = parser.nextInt();
        String arg2 = parser.hasNext() ? parser.nextString() : "default";

        // 2. Perform action
        player.inventory.add(arg1, 1);
        player.send(new SendMessage("Spawned item " + arg1));
    }
});
```

---

## CommandParser methods

```java
parser.nextInt()       // Read next integer
parser.nextString()    // Read next string token
parser.nextLine()      // Read rest of line as string
parser.hasNext()       // More arguments available?
```

---

## Rank enforcement

Commands are dispatched from `CommandPacketListener`:
```java
// game-server/src/main/java/com/osroyale/net/packet/in/CommandPacketListener.java
player.getEvents().widget(player, new CommandEvent(input));
```

The plugin system's `CommandExtension` iterates registered commands. **Before adding a command, verify:**

1. Does the command do something rank-appropriate? (Player commands shouldn't spawn items)
2. Does the command have bounds checks? (item IDs, amounts, coordinate ranges)
3. Does the command check the player's state? (not dead, not locked, not in combat)

---

## Security checklist for each command

- [ ] Item ID validation: `itemId >= 0 && itemId <= item_definition_limit`
- [ ] Amount validation: `amount > 0 && amount < maxStack`
- [ ] Coordinate validation: within map bounds
- [ ] State check: player is not dead/locked/trading
- [ ] Rate limiting: can't be spammed (use cooldowns or one-per-tick limits)
- [ ] Destruction guard: can't self-ban, self-reset, self-demote
- [ ] Logging: destructive commands logged to `info.log` or `error.log`

---

## Recompile and test

```bash
./gradlew :game-server:classes
```

Test in-game:
```
::mycommand 4151           (with argument)
::mc                       (alias)
```

---

## Client Impact

Commands are sent as chat messages from client. No client changes needed for commands. The client sends `::<command>` as a chat packet.

---

## Verify

- [ ] Command executes with correct arguments
- [ ] Command fails gracefully with wrong/no arguments
- [ ] Command respects rank (can't be used by lower rank)
- [ ] Command has bounds/validation checks
- [ ] Destructive commands are logged
- [ ] Works with both primary name and aliases
