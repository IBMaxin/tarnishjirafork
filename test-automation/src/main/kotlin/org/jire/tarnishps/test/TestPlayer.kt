package org.jire.tarnishps.test

import com.osroyale.game.world.entity.mob.player.Player
import com.osroyale.game.world.entity.mob.player.PlayerRight
import com.osroyale.game.world.position.Position
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Wraps a [Player] with debug logging and state snapshot capabilities
 * for test automation.
 *
 * Each action on a [TestPlayer] is logged with before/after state,
 * making it easy to trace failures in CI or debug mode.
 */
class TestPlayer(
    /** The underlying game [Player]. */
    val player: Player
) {
    private val logger: Logger = LoggerFactory.getLogger(TestPlayer::class.java)

    /** The username of this test player. */
    val username: String get() = player.username

    /** The rights/rank of this test player. */
    val rights: PlayerRight get() = player.right

    /** The current position of this test player. */
    val position: Position get() = player.position

    /**
     * Captures a snapshot of the player's current state for comparison
     * or debug output.
     */
    fun snapshot(): PlayerSnapshot {
        val snap = PlayerSnapshot(
            username = player.username,
            rights = player.right,
            position = player.position.copy(),
            inventorySize = player.inventory.size(),
            inventoryCapacity = player.inventory.capacity()
        )
        logger.trace("Snapshot: {}", snap)
        return snap
    }

    /**
     * Logs the current player state at INFO level.
     */
    fun printState() {
        logger.info(
            "Player[{}] rights={} position={} inventory={}/{}",
            player.username, player.right, player.position,
            player.inventory.size(), player.inventory.capacity()
        )
    }

    override fun toString(): String {
        return "TestPlayer(username='$username', rights=$rights, position=$position)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestPlayer) return false
        return player === other.player
    }

    override fun hashCode(): Int {
        return System.identityHashCode(player)
    }
}

/**
 * Immutable snapshot of a player's state at a point in time.
 */
data class PlayerSnapshot(
    val username: String,
    val rights: PlayerRight,
    val position: Position,
    val inventorySize: Int,
    val inventoryCapacity: Int
)
