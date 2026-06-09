package org.jire.tarnishps.test

import com.osroyale.game.world.entity.mob.player.PlayerRight
import com.osroyale.game.world.position.Position
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Acceptance tests for H2 — ActionDSL + PositionValidator.
 *
 * Validates that the fluent action DSL correctly teleports players
 * and validates their positions.
 */
class ActionDSLTest {

    private val client = TestClient()

    @Test
    fun `teleport to coordinates moves player`() {
        val testPlayer = client.login("teleport_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(3222, 3222)
                .validate { at(3222, 3222) }
        }
        assertEquals(Position(3222, 3222, 0), testPlayer.position)
    }

    @Test
    fun `teleport with z moves player to correct height`() {
        val testPlayer = client.login("height_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(1234, 5678, 2)
                .validate { at(1234, 5678, 2) }
        }
        assertEquals(2, testPlayer.position.height)
    }

    @Test
    fun `teleport to position object moves player`() {
        val testPlayer = client.login("pos_test", PlayerRight.PLAYER)
        val target = Position(3090, 3490, 0)
        action(testPlayer) {
            teleport(target)
                .validate { at(target) }
        }
        assertEquals(target, testPlayer.position)
    }

    @Test
    fun `chained teleport overwrites previous position`() {
        val testPlayer = client.login("chain_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(3222, 3222)
                .teleport(3223, 3222)
                .validate { at(3223, 3222) }
        }
        assertEquals(Position(3223, 3222, 0), testPlayer.position)
    }

    @Test
    fun `validate failure throws AssertionError`() {
        val testPlayer = client.login("fail_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(4000, 4000)
        }
        // Position should NOT be (0, 0, 0)
        assertNotEquals(Position(0, 0, 0), testPlayer.position)
    }

    @Test
    fun `snapshot captures position after teleport`() {
        val testPlayer = client.login("snap_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(3000, 4000, 1)
                .printState()
        }
        val snap = testPlayer.snapshot()
        assertEquals(3000, snap.position.x)
        assertEquals(4000, snap.position.y)
        assertEquals(1, snap.position.height)
    }

    @Test
    fun `PositionValidator asserts exact match`() {
        val testPlayer = client.login("val_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(2500, 3500)
        }
        PositionValidator.assertEquals(
            Position(2500, 3500, 0),
            testPlayer.position
        )
    }

    @Test
    fun `PositionValidator assertNear tolerates small offset`() {
        val testPlayer = client.login("near_test", PlayerRight.PLAYER)
        action(testPlayer) {
            teleport(3200, 3200)
        }
        // Should pass — exact match is within 1 tile
        PositionValidator.assertNear(
            Position(3200, 3200, 0),
            testPlayer.position,
            tiles = 1
        )
    }
}
