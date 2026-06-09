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

import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.net.packet.ClientPackets;
import com.osroyale.net.packet.GamePacket;
import com.osroyale.net.packet.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegionChangePacketListenerTest {

    private Player player;
    private RegionChangePacketListener listener;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        listener = new RegionChangePacketListener();
    }

    @Test
    void handlePacket_enterRegion_withNonMagicValue_setsLogOut() {
        int nonMagicValue = 0x12345678;
        GamePacket packet = createPacket(ClientPackets.ENTER_REGION,
                (byte) (nonMagicValue >> 24),
                (byte) (nonMagicValue >> 16),
                (byte) (nonMagicValue >> 8),
                (byte) nonMagicValue);

        listener.handlePacket(player, packet);

        assertTrue(player.getEvents().getLogOut());
    }

    @Test
    void handlePacket_enterRegion_withMagicValue_doesNotSetLogOut() {
        int magicValue = 0x3f008edd;
        GamePacket packet = createPacket(ClientPackets.ENTER_REGION,
                (byte) (magicValue >> 24),
                (byte) (magicValue >> 16),
                (byte) (magicValue >> 8),
                (byte) magicValue);

        listener.handlePacket(player, packet);

        assertFalse(player.getEvents().getLogOut());
    }

    @Test
    void handlePacket_loadedRegion_setsLoadRegion() {
        GamePacket packet = createPacket(ClientPackets.LOADED_REGION);

        listener.handlePacket(player, packet);

        assertTrue(player.getEvents().getLoadRegion());
    }

    @Test
    void handlePacket_enterRegion_emptyPayload_doesNotCrash() {
        GamePacket packet = createPacket(ClientPackets.ENTER_REGION);

        assertDoesNotThrow(() -> listener.handlePacket(player, packet));
    }

    @Test
    void handlePacket_loadedRegion_emptyPayload_doesNotCrash() {
        GamePacket packet = createPacket(ClientPackets.LOADED_REGION);

        assertDoesNotThrow(() -> listener.handlePacket(player, packet));
        assertTrue(player.getEvents().getLoadRegion());
    }

    private static GamePacket createPacket(int opcode, byte... payload) {
        ByteBuf buffer = Unpooled.buffer(payload.length);
        buffer.writeBytes(payload);
        return new GamePacket(opcode, PacketType.FIXED, buffer);
    }
}
