package org.jire.tarnishps.test

import com.osroyale.game.world.position.Position
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Fluent DSL for composing in-game test actions.
 *
 * Usage:
 * ```
 * action {
 *     teleport(3222, 3222)
 *         .validate { at(3222, 3222) }
 * }
 * ```
 *
 * Each action operates on a [TestPlayer] and returns the same player
 * so calls can be chained. Validation steps throw [AssertionError] on
 * failure, making them compatible with JUnit/TestNG.
 */
class ActionDSL(private val player: TestPlayer) {

    private val logger: Logger = LoggerFactory.getLogger(ActionDSL::class.java)

    /** The underlying game player. */
    val p get() = player.player

    // ── Actions ──────────────────────────────────────────────────────

    /**
     * Teleports the player to the given [x], [y], [z] coordinates.
     * Uses [com.osroyale.game.world.entity.Entity.setPosition] directly
     * for unit-test purposes (avoids [Mob.move] region-change guard).
     */
    fun teleport(x: Int, y: Int, z: Int = 0): ActionDSL {
        logger.info("teleport({}, {}, {})", x, y, z)
        p.setPosition(Position(x, y, z))
        return this
    }

    /**
     * Teleports the player to the given [position].
     */
    fun teleport(position: Position): ActionDSL {
        logger.info("teleport({})", position)
        p.setPosition(position)
        return this
    }

    // ── Validation ───────────────────────────────────────────────────

    /**
     * Runs a validation block. The block receives this [ActionDSL] and
     * should call assertion methods like [at] or [messageContains].
     *
     * Throws [AssertionError] if any assertion fails.
     */
    fun validate(block: ActionDSL.() -> Unit): ActionDSL {
        logger.info("validate { ... }")
        try {
            block()
        } catch (e: AssertionError) {
            logger.error("Validation FAILED: {}", e.message)
            throw e
        }
        return this
    }

    /**
     * Asserts that the player is at the given [x], [y], [z] coordinates.
     */
    fun at(x: Int, y: Int, z: Int = 0) {
        val pos = p.position
        val ok = pos.x == x && pos.y == y && pos.height == z
        if (!ok) {
            throw AssertionError(
                "Expected position ($x, $y, $z) but was (${pos.x}, ${pos.y}, ${pos.height})"
            )
        }
        logger.debug("  ✓ at({}, {}, {})", x, y, z)
    }

    /**
     * Asserts that the player is at the given [expected] position.
     */
    fun at(expected: Position) {
        at(expected.x, expected.y, expected.height)
    }

    /**
     * Asserts that the player's position equals the [expected] position.
     * Alias for [at].
     */
    fun positionEquals(expected: Position) = at(expected)

    // ── Convenience ──────────────────────────────────────────────────

    /** Prints the current player state for debugging. */
    fun printState(): ActionDSL {
        player.printState()
        return this
    }

    /** Captures and returns a snapshot of the current player state. */
    fun snapshot(): PlayerSnapshot = player.snapshot()
}

/**
 * Entry point for the action DSL.
 *
 * Creates an [ActionDSL] scoped to the given [player] and executes
 * the [block]. Returns the [ActionDSL] for further chaining if needed.
 */
fun action(player: TestPlayer, block: ActionDSL.() -> Unit): ActionDSL {
    val dsl = ActionDSL(player)
    dsl.block()
    return dsl
}

