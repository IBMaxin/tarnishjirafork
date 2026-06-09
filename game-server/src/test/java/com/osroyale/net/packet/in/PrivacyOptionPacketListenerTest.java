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
import com.osroyale.game.world.entity.mob.player.relations.PrivacyChatMode;
import com.osroyale.net.packet.GamePacket;
import com.osroyale.net.packet.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyOptionPacketListenerTest {

    private Player player;
    private PrivacyOptionPacketListener listener;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        listener = new PrivacyOptionPacketListener();
    }

    @Test
    void handlePacket_validPayload_queuesWidgetEventAndUpdatesRelations() {
        int publicMode = 0;   // ON
        int privateMode = 1;  // FRIENDS_ONLY
        int tradeMode = 2;    // OFF
        int clanMode = 0;     // ON

        GamePacket packet = createPrivacyPacket(publicMode, privateMode, tradeMode, clanMode);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        assertEquals(PrivacyChatMode.ON, player.relations.getPublicChatMode());
        assertEquals(PrivacyChatMode.FRIENDS_ONLY, player.relations.getPrivateChatMode());
        assertEquals(PrivacyChatMode.OFF, player.relations.getTradeChatMode());
        assertEquals(PrivacyChatMode.ON, player.relations.getClanChatMode());
    }

    @Test
    void handlePacket_deadPlayer_doesNotQueueEvent() throws Exception {
        int publicMode = 2;   // OFF
        int privateMode = 0;  // ON
        int tradeMode = 1;    // FRIENDS_ONLY
        int clanMode = 2;     // OFF

        // Set player as dead so canHandle() returns false
        Field deadField = player.getClass().getSuperclass().getDeclaredField("dead");
        deadField.setAccessible(true);
        deadField.set(player, true);

        GamePacket packet = createPrivacyPacket(publicMode, privateMode, tradeMode, clanMode);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        // Relations should remain at their default values (all ON)
        assertEquals(PrivacyChatMode.ON, player.relations.getPublicChatMode());
        assertEquals(PrivacyChatMode.ON, player.relations.getPrivateChatMode());
        assertEquals(PrivacyChatMode.ON, player.relations.getTradeChatMode());
        assertEquals(PrivacyChatMode.ON, player.relations.getClanChatMode());
    }

    @Test
    void handlePacket_malformedPacketTooShort_throwsIndexOutOfBounds() {
        ByteBuf buffer = Unpooled.buffer(2);
        buffer.writeByte(0);
        buffer.writeByte(1);
        GamePacket packet = new GamePacket(95, PacketType.FIXED, buffer);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            listener.handlePacket(player, packet);
        });
    }

    private static GamePacket createPrivacyPacket(int publicMode, int privateMode, int tradeMode, int clanMode) {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeByte(publicMode);
        buffer.writeByte(privateMode);
        buffer.writeByte(tradeMode);
        buffer.writeByte(clanMode);
        return new GamePacket(95, PacketType.FIXED, buffer);
    }
}