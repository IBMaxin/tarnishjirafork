# Packet Injection Audit

**Goal:** Find server packet handlers that trust client input without validation.

**Docs:** `AGENTS.md`, `00-cross-cutting/client-server-boundary.md` §Boundary 2, `docs/workflows/plugins.md`

---

## Architecture

Client → Server via `GamePacket`:
```
Client.java parsePacket() → sends opcode + data
    ↓
PacketRepository.registerListener(opcode, listener)
    ↓
PacketListener.handlePacket(Player, GamePacket)
```

Server → Client via `OutgoingPacket`:
```
player.send(new SendMessage(...)) → opcode + data sent to client
    ↓
Client.java parsePacket() → reads opcode, dispatches
```

---

## Client → Server attack surface

Every `PacketListener` in `game-server/src/main/java/com/osroyale/net/packet/in/` is a potential injection point:

```bash
ls game-server/src/main/java/com/osroyale/net/packet/in/
```

Key handlers to audit:

### Item manipulation
- `DropItemPacketListener.java` — Can player drop items they don't have? Negative slot?
- `FirstClickItemPacketListener.java` — Item ID validation? Slot bounds?
- `ItemOnItemPacketListener.java` — Can forge item-on-item with arbitrary IDs?

### Movement
- `MovementPacketListener.java` — Can teleport by sending invalid coords? Speed hack by rapid packets?
- `WalkOnPacketListener.java` — Path length limit? Bounds check?

### Object/NPC interaction
- `ObjectClickPacketListener.java` — Can interact with objects from any distance?
- `NpcClickPacketListener.java` — Can target NPCs that don't exist?
- `AttackNpcPacketListener.java` — Can attack without requirements?

### Chat/Commands
- `CommandPacketListener.java` — See [command-audit.md](command-audit.md)
- `ChatMessagePacketListener.java` — Injection? Rate limit?

### Trading
- Trade packet listeners — Item duplication via trade desync?

---

## What to check in each handler

```java
@Override
public void handlePacket(Player player, GamePacket packet) {
    // 1. Is the player logged in? (null check)
    // 2. Is the player in the right state? (not locked/trading/dead)
    // 3. Are packet values validated?
    //    int itemId = packet.readShort();  // Is itemId checked against valid range?
    //    int slot = packet.readShort();     // Is slot in player.inventory bounds?
    //    int amount = packet.readInt();     // Is amount positive? Capped?
    // 4. Is there a rate limit? (can this be spammed?)
}
```

---

## Common exploit patterns in RSPS

| Pattern | Exploit | Check |
|---------|---------|-------|
| Unvalidated item ID | Spawn any item | `itemId < 0 \|\| itemId > maxItems` |
| Unvalidated slot | Interface crash, dupe | `slot >= 0 && slot < container.size()` |
| Unvalidated amount | Negative amount = dupe | `amount > 0 && amount < maxStack` |
| No distance check | Interact across map | `player.getDistance(target) < maxDistance` |
| No state check | Act while dead/stunned | `!player.isDead() && !player.isLocked()` |
| No rate limit | Packet spam crash | Per-player per-opcode cooldown |

---

## Client Impact

**This is the client impact.** Every packet the client sends is a potential attack. The client is untrusted — assume a modified client sends anything.

→ Server must validate ALL client input. The 317 client is a thin rendering layer; all game logic authority lives on the server.

---

## Verify

- [ ] Every packet handler validates item IDs against `item_definition_limit`
- [ ] Every packet handler checks slot bounds
- [ ] Movement/teleport handlers check distance/coordinate bounds
- [ ] Trade handlers are dupe-resistant (state machine, not trusting client)
- [ ] Chat/command have rate limits
- [ ] No handler trusts client-reported inventory/equipment state
