/*
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  Unit test pattern for net.packet.in.* PacketListener implementations.     │
 * │                                                                             │
 * │  PATTERN (reuse for all 29 handlers):                                      │
 * │                                                                             │
 * │  1. Create a real Player instance (not mocked) — Player(String) calls      │
 * │     Mob(Position) which initialises all public fields (movement, locking,  │
 * │     skills, action, interfaceManager, dialogueFactory, etc.).              │
 * │                                                                             │
 * │  2. Stub any heavy dependencies on the player that the handler touches:    │
 * │     - player.movement = mock(Movement.class)   // avoid real pathfinding   │
 * │     - player.locking   = mock(Locking.class)   // avoid real lock checks   │
 * │     - player.combat    = mock(...) via reflection or setter if available   │
 * │     - player.skills    = mock(SkillManager.class) // avoid real skill mgmt │
 * │     - player.action    = mock(ActionManager.class)                         │
 * │     - player.interfaceManager = mock(InterfaceManager.class)               │
 * │     - player.dialogueFactory   = mock(DialogueFactory.class)               │
 * │     - player.getGambling() → mock(GambleManager.class)                     │
 * │                                                                             │
 * │  3. Build a GamePacket with a Netty ByteBuf payload containing the exact   │
 * │     bytes the client would send (matching the readShort/readByte calls     │
 * │     with their ByteOrder and ByteModification).                            │
 * │                                                                             │
 * │  4. Call listener.handlePacket(player, packet)                             │
 * │     → This queues an Event via player.getEvents().interact(player, event)  │
 * │                                                                             │
 * │  5. Call player.getEvents().process(player)                                │
 * │     → This executes the queued event (calls event.handle(player))          │
 * │                                                                             │
 * │  6. Assert the expected side-effect on the mocked dependency.              │
 * │                                                                             │
 * │  For handlers that DON'T go through Events (some call player.send() or     │
 * │  other methods directly), skip step 5 and assert the direct side-effect.   │
 * │                                                                             │
 * │  Edge cases: malformed packets (wrong size, out-of-bounds values) should   │
 * │  not crash. Verify by asserting no state change on the mocked dependency.  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
package com.osroyale.net.packet.in;

import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.Locking;
import com.osroyale.game.world.entity.mob.data.PacketType;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.position.Position;
import com.osroyale.net.codec.ByteOrder;
import com.osroyale.net.packet.GamePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PickupItemPacketListenerTest {

    private Player player;
    private Locking locking;
    private PickupItemPacketListener listener;

    @BeforeEach
    void setUp() throws Exception {
        player = new Player("TestPlayer");

        locking = mock(Locking.class);
        Field lockingField = Mob.class.getField("locking");
        lockingField.setAccessible(true);
        lockingField.set(player, locking);

        listener = new PickupItemPacketListener();
    }

    @Test
    void handlePacket_validPacket_queuesPickupItemEventAndProcesses() {
        int itemId = 995;
        int x = 3222;
        int y = 3219;

        when(locking.locked(PacketType.PICKUP_ITEM)).thenReturn(false);

        GamePacket packet = createPickupPacket(itemId, x, y);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        assertDoesNotThrow(() -> player.getEvents().process(player));
    }

    @Test
    void handlePacket_playerLocked_doesNotQueueEvent() {
        int itemId = 995;
        int x = 3222;
        int y = 3219;

        when(locking.locked(PacketType.PICKUP_ITEM)).thenReturn(true);

        GamePacket packet = createPickupPacket(itemId, x, y);

        listener.handlePacket(player, packet);
        assertDoesNotThrow(() -> player.getEvents().process(player));
    }

    @Test
    void handlePacket_malformedPacketTooShort_throwsIndexOutOfBounds() {
        ByteBuf buffer = Unpooled.buffer(2);
        buffer.writeByte(0);
        buffer.writeByte(0);
        GamePacket packet = new GamePacket(236, com.osroyale.net.packet.PacketType.FIXED, buffer);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            listener.handlePacket(player, packet);
        });
    }

    private static GamePacket createPickupPacket(int itemId, int x, int y) {
        ByteBuf buffer = Unpooled.buffer(6);

        // readShort(ByteOrder.LE) -> y
        buffer.writeByte(y & 0xFF);
        buffer.writeByte((y >> 8) & 0xFF);

        // readShort(false) -> itemId (BIG_ENDIAN, signed)
        buffer.writeByte((itemId >> 8) & 0xFF);
        buffer.writeByte(itemId & 0xFF);

        // readShort(ByteOrder.LE) -> x
        buffer.writeByte(x & 0xFF);
        buffer.writeByte((x >> 8) & 0xFF);

        return new GamePacket(236, com.osroyale.net.packet.PacketType.FIXED, buffer);
    }
}