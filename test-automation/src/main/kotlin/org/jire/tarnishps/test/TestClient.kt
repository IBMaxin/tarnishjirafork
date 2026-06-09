package org.jire.tarnishps.test

import com.osroyale.game.world.entity.mob.player.Player
import com.osroyale.game.world.entity.mob.player.PlayerRight
import com.osroyale.game.world.position.Position
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates and configures [Player] instances for test automation.
 *
 * Usage:
 * ```
 * val client = TestClient()
 * val player = client.login("zezima", PlayerRight.OWNER)
 * ```
 */
class TestClient {

    private val logger: Logger = LoggerFactory.getLogger(TestClient::class.java)

    /**
     * Creates a [TestPlayer] with the given [username] and [rights],
     * positioned at the default spawn point.
     *
     * The player is NOT registered with the [World] — it exists as a
     * standalone object suitable for unit-testing plugin logic, event
     * dispatch, and state assertions.
     */
    fun login(username: String, rights: PlayerRight = PlayerRight.PLAYER): TestPlayer {
        logger.info("TestClient.login: username={}, rights={}", username, rights)
        val player = Player(username)
        player.right = rights
        return TestPlayer(player)
    }

    /**
     * Creates a [TestPlayer] at a specific [position].
     */
    fun login(username: String, rights: PlayerRight, position: Position): TestPlayer {
        logger.info("TestClient.login: username={}, rights={}, position={}", username, rights, position)
        val player = Player(username, position)
        player.right = rights
        return TestPlayer(player)
    }
}
