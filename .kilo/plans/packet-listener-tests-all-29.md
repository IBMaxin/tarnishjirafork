# Plan: Unit Tests for All 29 Packet Listeners

## Status

- **WalkingPacketListener** — ✅ Done (4 tests, all passing)
- **Remaining: 28**

---

## Categorization by Test Complexity

### Tier 1: Trivial / No-op (3 handlers)

These have empty or commented-out `handlePacket` bodies. Test: assert no crash.

| # | Handler | Opcodes | Body |
|---|---------|---------|------|
| 1 | `IdlePacketListener` | 0 | empty |
| 2 | `MouseClickPacketListener` | 241 | empty |
| 3 | `UnusedPacketListener` | 3,35,36,58,77,78,85,86,156,200,226,238,230 | commented out |

**Effort:** ~5 min each. One test: `assertDoesNotThrow(() -> listener.handlePacket(player, anyPacket))`.

---

### Tier 2: Simple Field Setters (2 handlers)

Direct side effects on `Player` fields. No events, no packet reads.

| # | Handler | What it does |
|---|---------|-------------|
| 4 | `IdleLogoutPacketListener` | `player.idle = true` |
| 5 | `RegionChangePacketListener` | `ENTER_REGION`: reads int, may set `events.setLogOut(true)`; `LOADED_REGION`: `events.setLoadRegion(true)` |

**Effort:** ~10 min each. Follow Walking pattern but assert on `player.idle` / `player.getEvents()` state instead of mocked `Movement`.

**Challenge:** `RegionChangePacketListener` reads `packet.readInt()` — need to encode 4 bytes. ENTER_REGION checks `a != 0x3f008edd` → setLogOut, otherwise not.

---

### Tier 3: Simple Events (11 handlers)

Read a few packet fields, create an Event, queue via `player.getEvents().widget()` or `.interact()`.

| # | Handler | Reads | Event class | Notes |
|---|---------|-------|-------------|-------|
| 6 | `KeyPacketListener` | readShort | `KeyPacketEvent` | Has `locking.locked()` check; reject if locked or key<0 |
| 7 | `PrivacyOptionPacketListener` | 4× readByte | `PrivacyOptionEvent` | Straightforward |
| 8 | `CommandPacketListener` | getRS2String | `CommandEvent` | Has empty/length validation |
| 9 | `InputFieldPacketListener` | readInt + getRS2String | `InputFieldEvent` | |
| 10 | `InputStringPacketListener` | readLong | `InputStringEvent` | |
| 11 | `DialoguePacketListener` | none (static instance) | `DialogueEvent.INSTANCE` | Simplest event handler |
| 12 | `DropdownMenuPacketListener` | readInt + readByte | `DropdownMenuEvent` | Has negative-value guards |
| 13 | `DropViewerListener` | getRS2String | `DropViewerEvent` | null/empty/"null" guard |
| 14 | `MoveItemPacketListener` | readShort(LE,ADD) + readByte(NEG) + readShort(LE,ADD) + readShort(LE) | `MoveItemEvent` | Sets `player.idle = false` |
| 15 | `PickupItemPacketListener` | 3× readShort | `PickupItemEvent` | Has `locking.locked()` guard |
| 16 | `AppearanceChangePacketListener` | 13× readByte | `AppearanceChangeEvent` | Many fields, all plain reads — just verbose |

**Effort:** ~15-20 min each. Follow Walking pattern exactly, stub the event to verify `widget()`/`interact()` received the right event.

**Note:** `Player.getEvents().widget()` vs `.interact()` — two different queue types. Need to check which to stub.

---

### Tier 4: Complex Events (2 handlers)

Read bytes with advanced patterns (`getSize()`, `readBytesReverse`, `readBytes`, `ByteModification`).

| # | Handler | Challenge |
|---|---------|-----------|
| 17 | `ChatMessagePacketListener` | `packet.getSize() - 2` for length calc, `readBytesReverse(size, ADD)`, multiple validation guards (effect/color ranges, mute check, chat lock) |
| 18 | `PlayerRelationPacketListener` | `packet.getSize() - Long.BYTES` for remaining bytes, multi-opcode |

**Effort:** ~30 min each. Need to understand how `GamePacket.getSize()` works with our test buffer.

---

### Tier 5: Heavy Integration (8 handlers)

Deep interaction with `Player` fields (`inventory`, `equipment`, `spellCasting`, `combat`, `dialogueFactory`, `interfaceManager`, `gambling`), `PluginManager`, `EventDispatcher`, static classes.

| # | Handler | What makes it hard |
|---|---------|--------------------|
| 19 | `ButtonClickPacketListener` | `EventDispatcher.execute()`, `PluginManager.getDataBus().publish()`, `player.isDead()`, `locking.locked()` |
| 20 | `WieldItemPacketListener` | `player.inventory.get()`, `PluginManager`, `player.equipment.equip()`, `Activity.evaluate()`, `player.skills.isMaxed()` |
| 21 | `ItemOptionPacketListener` | 3 sub-handlers, `player.inventory.get()`, `EventDispatcher`, `PluginManager`, `ItemActionRepository` |
| 22 | `ItemContainerActionPacketListener` | 8 sub-handlers, `PluginManager`, `EventDispatcher`, `player.attributes`, `SendInputAmount`, `DepositBoxPlugin` |
| 23 | `MagicOnItemPacketListener` | `player.inventory.get()`, `spellCasting.enchantItem()/cast()`, locking |
| 24 | `ExaminePacketListener` | 4 types, `ItemDefLoader`, `player.inventory.get()`, `player.attributes`, `player.settings` |
| 25 | `DropItemPacketListener` | `player.gambling`, `combat`, `interfaceManager`, `DialogueFactory`, `GroundItem`, `Area`, `PluginManager`, `ItemActionRepository` |
| 26 | `ColorPacketListener` | Direct `player.playerTitle` + `updateFlags`, `PlayerRight` check — relatively simpler than others in this tier |
| 27 | `UseItemPacketListener` | 5 sub-handlers, reads `player.inventory.get()`, mixes ItemOnNpc/Object/Player events |
| 28 | `CloseInterfacePacketListener` | `player.interfaceManager.close(false)` |

**Effort:** ~45-90 min each. May need to mock `inventory`, `equipment`, `interfaceManager`, `spellCasting`, `gambling`, `dialogueFactory`, `combat`, and possibly stub static methods (`EventDispatcher`, `PluginManager`, `ItemDefLoader`).

**Recommendation:** Start with the simpler ones in this tier: `CloseInterfacePacketListener`, `ColorPacketListener`, `MagicOnItemPacketListener`. Tackle `ButtonClickPacketListener`, `ItemOptionPacketListener`, `ItemContainerActionPacketListener` last.

---

## Execution Order (Priority)

1. **Tier 1** — 3 no-op handlers (~15 min total)
2. **Tier 2** — 2 field-setter handlers (~20 min total)
3. **Tier 3** — 11 simple event handlers (~3-4 hours total)
4. **Tier 4** — 2 complex event handlers (~1 hour total)
5. **Tier 5** — 8 integration-heavy handlers (~6-12 hours total)

**Total estimate: ~15-20 hours** for all 28 remaining handlers.

---

## Reusable Test Skeleton

Every Tier 2-4 test follows this pattern (derived from WalkingPacketListenerTest):

```java
class XxxPacketListenerTest {
    private Player player;
    private XxxPacketListener listener;

    @BeforeEach
    void setUp() throws Exception {
        player = new Player("TestPlayer");
        player.setPosition(new Position(3087, 3497, 0));
        // replace movement with mock via reflection
        Movement movement = mock(Movement.class);
        Field movementField = Mob.class.getField("movement");
        movementField.setAccessible(true);
        movementField.set(player, movement);
        // optionally mock locking for handlers that check it
        Locking locking = mock(Locking.class);
        Field lockingField = Mob.class.getField("locking");
        lockingField.setAccessible(true);
        lockingField.set(player, locking);
        listener = new XxxPacketListener();
    }

    // Helper to create GamePacket with specific opcode + bytes
    private static GamePacket createPacket(int opcode, byte... payload) {
        ByteBuf buffer = Unpooled.buffer(payload.length);
        buffer.writeBytes(payload);
        return new GamePacket(opcode, PacketType.FIXED, buffer);
    }
}
```

Key helper: `createPacket(opcode, bytes...)` — simplifies tests for all handlers.

---

## Open Questions

1. **`player.getEvents().widget()` vs `.interact()`** — Need to verify `widget()` follows the same "queue then process" pattern. If not, tests need adjustment.

2. **`GamePacket.getSize()`** — Returns remaining readable bytes from the ByteBuf. For tests, this should work as expected since we control the buffer.

3. **`GamePacket.getRS2String()`** — RS2 string encoding, need to confirm format (likely null-terminated or length-prefixed).

4. **Tier 5 strategy** — Mock `PluginManager`/`EventDispatcher` static methods with Mockito inline, or skip these handlers and document why (integration-level tests needed).

5. **Scope** — Proceed Tier-by-Tier, starting with the lowest hanging fruit? Or write them all at once?
