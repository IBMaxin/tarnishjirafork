# Client-Server Boundary

**When server work needs client work — and when it doesn't.**

→ Reference: `AGENTS.md` for project structure, `code_index.json` for file lookup, `docs/workflows/` for patterns.

---

## Rule of Thumb

| Server change | Client impact? | What to check |
|---------------|---------------|---------------|
| JSON data file (item defs, NPC spawns, shops) | No | Server-only |
| Plugin (click handler, command, skill) | No | Server-only |
| Combat formula / modifier | No | Server-only |
| New item with ID > 28,473 | **Yes** | Client won't render it |
| New packet opcode | **Yes** | Both sides must agree |
| New interface/UI packet | **Yes** | Client must have that interface |
| Chat message / SendMessage | No | Universal opcode, all clients handle |
| NPC/player appearance change | Maybe | If model ID is new |

---

## Boundary 1: Item IDs

Server config in `game-server/settings.toml`:
```toml
item_definition_limit = 28473
```

Server will accept items up to this ID. The 317 client may NOT render items beyond OSRS cache range. 

**Real example — what the client sees:**
```java
// game-client/src/main/java/com/osroyale/Client.java (~787KB)
// Item rendering uses cache lookups. Unknown IDs → null model → invisible.
```

**Test it:** After adding an item, spawn it in-game with `::spawnitem <id>`. If invisible, the client needs cache injection or the item needs a model file in `game-server/data/def/items-json/<id>.json`.

---

## Boundary 2: Packet Opcodes

Client → Server packets are registered in server-side `ClientPackets.java`:
```java
// game-server/src/main/java/com/osroyale/net/packet/ClientPackets.java
public static final int BUTTON_CLICK = 185;
public static final int CHAT = 4;
public static final int FIRST_CLICK_OBJECT = 132;
public static final int PLAYER_COMMAND = 103;  // example
// ... ~60+ opcodes
```

Each is wired to a listener:
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

Server → Client packets use `OutgoingPacket`:
```java
// game-server/src/main/java/com/osroyale/net/packet/out/SendMessage.java
public class SendMessage extends OutgoingPacket {
    public SendMessage(Object message, MessageColor color) {
        super(253, PacketType.VAR_BYTE);  // opcode 253
        // ...
    }
}
```

Client-side parsing lives in `game-client/src/main/java/com/osroyale/Client.java` → `parsePacket()` method (~787KB file). Adding a new opcode requires:
1. Server: Add constant in `ClientPackets.java` or create `OutgoingPacket` subclass
2. Client: Add handler in `Client.java` `parsePacket()` switch statement
3. Both sides must agree on the opcode number and data format

---

## Boundary 3: Interfaces

Server sends interface commands. Client must have the interface defined.

```java
// Server sends an interface to the client
player.send(new SendChatBoxInterface(12345));  // Interface ID 12345
```

If the 317 client doesn't have that interface ID, it ignores it (no crash, just nothing happens). Interface IDs are hardcoded in the client — to add a new one requires client-side UI work.

Known interface IDs (from `InterfaceConstants.java`):
```java
// game-server/src/main/java/com/osroyale/game/world/InterfaceConstants.java
```

---

## Boundary 4: Configuration / Settings

Server config is in `game-server/settings.toml`. Client config is compiled-in or via launch arguments.

Server settings that affect the client:
- `client_version = 12` — mismatch = login rejected
- `server_port = 43594` — client must connect to this port
- RSA keys — mismatch = login rejected

---

## Quick Decision Flow

```
"Am I adding/changing X?"
    │
    ├─ JSON data (items, NPCs, shops, drops, spawns)?
    │   → Server-only. Recompile server, test with ::spawn commands.
    │
    ├─ Plugin logic (commands, clicks, skills)?
    │   → Server-only. Recompile server, use ::commands to test.
    │
    ├─ New item with high ID?
    │   → Server + maybe client cache. Spawn in-game to verify visibility.
    │
    ├─ New packet or UI?
    │   → Both. Add server opcode + client handler. Test end-to-end.
    │
    └─ Combat/game config (modifiers, limits)?
        → Server-only. Change settings.toml, restart server.
```
