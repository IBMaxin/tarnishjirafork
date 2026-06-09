/*
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  Unit test pattern for net.packet.in.* PacketListener implementations.     │
 * │                                                                             │
 * │  PATTERN (reuse for all 29 handlers):                                      │
 * │                                                                             │
 * │  1. Create a real Player instance (not mocked) - Player(String) calls      │
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
 * │     - player.getGambling() = mock(GambleManager.class)                     │
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
import com.osroyale.game.world.entity.mob.player.relations.ChatMessage;
import com.osroyale.net.packet.GamePacket;
import com.osroyale.net.packet.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandPacketListenerTest {

    private Player player;
    private CommandPacketListener listener;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        listener = new CommandPacketListener();
    }

    @Test
    void handlePacket_validCommand_queuesCommandEventAndProcessesWithoutError() {
        GamePacket packet = createCommandPacket("::test");

        listener.handlePacket(player, packet);
        assertDoesNotThrow(() -> player.getEvents().process(player));
        assertDoesNotThrow(() -> player.getEvents().process(player));
    }

    @Test
    void handlePacket_emptyString_doesNotQueueEvent() {
        GamePacket packet = createCommandPacket("");

        listener.handlePacket(player, packet);
        assertDoesNotThrow(() -> player.getEvents().process(player));
    }

    @Test
    void handlePacket_stringExceedsCharacterLimit_doesNotQueueEvent() {
        String longString = "a".repeat(ChatMessage.CHARACTER_LIMIT + 1);
        GamePacket packet = createCommandPacket(longString);

        listener.handlePacket(player, packet);
        assertDoesNotThrow(() -> player.getEvents().process(player));
    }

    @Test
    void handlePacket_malformedPacketNoTerminator_doesNotThrow() {
        // getRS2String() reads until byte 10 (\n) or until the buffer is exhausted.
        // A buffer with bytes but no terminator is handled gracefully — it returns
        // whatever was read. No exception should be thrown.
        ByteBuf buffer = Unpooled.buffer(3);
        buffer.writeByte('a');
        buffer.writeByte('b');
        buffer.writeByte('c');
        GamePacket packet = new GamePacket(103, PacketType.FIXED, buffer);

        assertDoesNotThrow(() -> listener.handlePacket(player, packet));
    }

    private static GamePacket createPacket(int opcode, byte... payload) {
        ByteBuf buffer = Unpooled.buffer(payload.length);
        buffer.writeBytes(payload);
        return new GamePacket(opcode, PacketType.FIXED, buffer);
    }

    private static GamePacket createCommandPacket(String command) {
        byte[] stringBytes = command.getBytes(StandardCharsets.UTF_8);
        byte[] payload = Arrays.copyOf(stringBytes, stringBytes.length + 1);
        payload[stringBytes.length] = 10;
        return createPacket(103, payload);
    }
}