# Command Permission Audit

**Goal:** Verify every command has appropriate rank enforcement. Find privilege escalation paths.

**Docs:** `AGENTS.md` §Commands, `docs/workflows/commands.md`, `00-cross-cutting/client-server-boundary.md`

---

## Target files

| File | # Commands | Risk |
|------|-----------|------|
| `game-server/plugins/plugin/command/OwnerCommandPlugin.java` | ~20 | **Critical** |
| `game-server/plugins/plugin/command/DeveloperCommandPlugin.java` | varies | **Critical** |
| `game-server/plugins/plugin/command/AdminCommandPlugin.java` | ~18 | **High** |
| `game-server/plugins/plugin/command/ManagerCommandPlugin.java` | varies | High |
| `game-server/plugins/plugin/command/ModeratorCommandPlugin.java` | varies | Medium |
| `game-server/plugins/plugin/command/HelperCommandPlugin.java` | varies | Medium |
| `game-server/plugins/plugin/command/DonatorCommandPlugin.java` | varies | Low |
| `game-server/plugins/plugin/command/PlayerCommandPlugin.java` | varies | Low |

---

## The framework question

Commands are dispatched by `CommandPacketListener`:
```java
// game-server/src/main/java/com/osroyale/net/packet/in/CommandPacketListener.java
@PacketListenerMeta(ClientPackets.PLAYER_COMMAND)
public final class CommandPacketListener implements PacketListener {
    @Override
    public void handlePacket(Player player, GamePacket packet) {
        final String input = packet.getRS2String().trim().toLowerCase();
        player.getEvents().widget(player, new CommandEvent(input));
    }
}
```

The plugin system's `CommandExtension` iterates registered commands and matches by name. **The critical question:** Does the plugin system enforce that only the correct rank's file can respond? Or can a PLAYER's `::setrank` reach the OwnerCommandPlugin if the client sends it?

---

## Steps

### 1. Extract all commands

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork
grep -n 'new Command(' game-server/plugins/plugin/command/*.java | sed 's/.*"\(.*\)".*/\1/' | sort
```

### 2. Check dispatch path

Read `CommandExtension.java` — does it filter by `PlayerRight`?
```bash
find game-server -name "CommandExtension.java" -exec cat {} \;
```

### 3. For each high-risk Owner command, check

| Command | Risk | What to verify |
|---------|------|---------------|
| `setrank` | Account takeover | Can it set DEVELOPER? Self-target check? Confirm? |
| `giveitem` / `gi` | Economy wipe | Item ID bounds? Negative amounts? Untradeable spawning? |
| `ban` / `ipban` | Denial of service | Self-ban possible? Ban DEVELOPER? Confirmation? |
| `resetplayer` | Data destruction | What gets reset? Can target staff? |
| `giveexp` | Economy | Skill ID bounds? Negative XP? Max level check? |
| `kill` | Harassment | Any player targetable? Self-kill? |
| `alltome` | Mass teleport | Does it respect wilderness/PVP zones? |
| `doubleexp` | Economy | Global toggle? Can non-owner toggle? |
| `bloodmoneychest` | Economy | What does this spawn? Bounds check? |

### 4. For each Admin command

| Command | Check |
|---------|-------|
| `spawnitem` | Item ID bounds? Amount cap? Can spawn untradeables? |
| `spawnnpc` | NPC ID bounds? Rate limit? |
| `teleport` | Can teleport into restricted zones? |
| `pnpc` | Can transform into any NPC including bosses? |

### 5. Verify Player/Donator commands

```bash
grep -A5 'new Command(' game-server/plugins/plugin/command/PlayerCommandPlugin.java
```

Check that Player commands don't include any admin-level actions (item spawning, teleporting, NPC interaction).

---

## Client Impact

Commands are sent from client as chat messages starting with `::`. The client doesn't enforce permissions — that's entirely server-side. A modified client could send `::setrank` even if the UI doesn't show it.

→ See [packet-injection.md](packet-injection.md) for client-side packet forgery risks.

---

## Verify

- [ ] `CommandExtension` enforces rank at dispatch (not just by file placement)
- [ ] `setrank` cannot produce DEVELOPER from OWNER
- [ ] `giveitem` validates item ID range (0 to `item_definition_limit`)
- [ ] `ban`/`ipban` prevent self-targeting
- [ ] No Player/Donator command performs admin actions
- [ ] All destructive commands have confirmation or are logged
