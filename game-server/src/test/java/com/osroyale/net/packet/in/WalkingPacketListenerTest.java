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
import com.osroyale.game.world.entity.mob.movement.Movement;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.position.Position;
import com.osroyale.net.codec.ByteModification;
import com.osroyale.net.codec.ByteOrder;
import com.osroyale.net.packet.GamePacket;
import com.osroyale.net.packet.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalkingPacketListenerTest {

    private static final int ORIGIN_X = 3087;
    private static final int ORIGIN_Y = 3497;

    private Player player;
    private Movement movement;
    private WalkingPacketListener listener;

    @BeforeEach
    void setUp() throws Exception {
        player = new Player("TestPlayer");
        player.setPosition(new Position(ORIGIN_X, ORIGIN_Y, 0));

        movement = mock(Movement.class);
        Field movementField = Mob.class.getField("movement");
        movementField.setAccessible(true);
        movementField.set(player, movement);

        listener = new WalkingPacketListener();
    }

    @Test
    void handlePacket_validWalkWithinRange_queuesWalkEventAndProcessesMovement() {
        int targetX = ORIGIN_X + 5;
        int targetY = ORIGIN_Y + 3;
        boolean runQueue = false;

        GamePacket packet = createWalkPacket(targetX, targetY, runQueue);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        Position expectedDestination = Position.create(targetX, targetY, 0);
        verify(movement).dijkstraPath(expectedDestination);
        verify(movement).setRunningQueue(false);
    }

    @Test
    void handlePacket_validWalkWithRunQueue_setsRunningQueue() {
        int targetX = ORIGIN_X + 2;
        int targetY = ORIGIN_Y + 2;
        boolean runQueue = true;

        GamePacket packet = createWalkPacket(targetX, targetY, runQueue);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        verify(movement).setRunningQueue(true);
        verify(movement).dijkstraPath(any(Position.class));
    }

    @Test
    void handlePacket_outOfBoundsCoordinates_doesNotTriggerMovement() {
        int targetX = ORIGIN_X + 50;
        int targetY = ORIGIN_Y + 50;

        GamePacket packet = createWalkPacket(targetX, targetY, false);

        listener.handlePacket(player, packet);
        player.getEvents().process(player);

        verify(movement, never()).dijkstraPath(any(Position.class));
        verify(movement, never()).setRunningQueue(anyBoolean());
    }

    @Test
    void handlePacket_malformedPacketTooShort_throwsIndexOutOfBounds() {
        ByteBuf buffer = Unpooled.buffer(1);
        buffer.writeByte(0);
        GamePacket packet = new GamePacket(164, PacketType.FIXED, buffer);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            listener.handlePacket(player, packet);
        });
    }

    private static GamePacket createWalkPacket(int targetX, int targetY, boolean runQueue) {
        ByteBuf buffer = Unpooled.buffer(5);

        // readShort(ByteOrder.LE) → targetX
        buffer.writeByte(targetX & 0xFF);
        buffer.writeByte((targetX >> 8) & 0xFF);

        // readShort(ByteOrder.LE, ByteModification.ADD) → targetY
        // ADD means each byte has 128 subtracted on read, so we add 128 on write
        int yLow = (targetY & 0xFF) + 128;
        int yHigh = ((targetY >> 8) & 0xFF);
        buffer.writeByte(yLow & 0xFF);
        buffer.writeByte(yHigh & 0xFF);

        // readByte(ByteModification.NEG) → runQueue flag (negated on read)
        // NEG means value = -readByte(), so we write -runQueue
        buffer.writeByte(runQueue ? -1 : 0);

        return new GamePacket(164, PacketType.FIXED, buffer);
    }
}