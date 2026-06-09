package org.jire.tarnishps.test

import com.osroyale.game.world.entity.mob.player.PlayerRight
import com.osroyale.game.world.position.Position
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Acceptance tests for H1 — TestClient + Player Wrapper.
 *
 * Validates that [TestClient] can create [TestPlayer] instances with
 * correct credentials, rights, and position.
 */
class TestClientTest {

    private val client = TestClient()

    @Test
    fun `login as owner returns player with OWNER rights`() {
        val testPlayer = client.login("zezima", PlayerRight.OWNER)
        assertEquals("zezima", testPlayer.username)
        assertEquals(PlayerRight.OWNER, testPlayer.rights)
    }

    @Test
    fun `login as administrator returns player with ADMINISTRATOR rights`() {
        val testPlayer = client.login("oak", PlayerRight.ADMINISTRATOR)
        assertEquals("oak", testPlayer.username)
        assertEquals(PlayerRight.ADMINISTRATOR, testPlayer.rights)
    }

    @Test
    fun `login as player returns player with PLAYER rights`() {
        val testPlayer = client.login("player1")
        assertEquals("player1", testPlayer.username)
        assertEquals(PlayerRight.PLAYER, testPlayer.rights)
    }

    @Test
    fun `login with position places player at correct coordinates`() {
        val pos = Position(3222, 3222, 0)
        val testPlayer = client.login("teleport_test", PlayerRight.PLAYER, pos)
        assertEquals(pos, testPlayer.position)
        assertEquals(3222, testPlayer.position.x)
        assertEquals(3222, testPlayer.position.y)
        assertEquals(0, testPlayer.position.height)
    }

    @Test
    fun `snapshot captures correct player state`() {
        val testPlayer = client.login("snapshot_test", PlayerRight.ADMINISTRATOR)
        val snap = testPlayer.snapshot()

        assertEquals("snapshot_test", snap.username)
        assertEquals(PlayerRight.ADMINISTRATOR, snap.rights)
        assertNotNull(snap.position)
        assertTrue(snap.inventoryCapacity > 0)
    }

    @Test
    fun `player right is set correctly via field`() {
        val testPlayer = client.login("rights_test", PlayerRight.OWNER)
        assertEquals(PlayerRight.OWNER, testPlayer.player.right)
        assertTrue(PlayerRight.isOwner(testPlayer.player))
    }

    @Test
    fun `default rights is PLAYER`() {
        val testPlayer = client.login("default_test")
        assertEquals(PlayerRight.PLAYER, testPlayer.rights)
    }
}
