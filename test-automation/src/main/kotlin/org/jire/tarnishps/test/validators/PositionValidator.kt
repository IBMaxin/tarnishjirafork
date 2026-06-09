package org.jire.tarnishps.test

import com.osroyale.game.world.position.Position

/**
 * Asserts that a player's position matches expectations.
 *
 * Provides detailed diff output on failure, showing the expected vs
 * actual coordinates.
 */
object PositionValidator {

    /**
     * Asserts that [actual] equals [expected].
     *
     * @throws AssertionError with a detailed diff message on mismatch.
     */
    fun assertEquals(expected: Position, actual: Position, label: String = "position") {
        if (expected != actual) {
            val diff = buildString {
                appendLine("$label mismatch:")
                appendLine("  Expected: ($expected)")
                appendLine("  Actual:   ($actual)")
                if (expected.x != actual.x) appendLine("  Δx = ${actual.x - expected.x}")
                if (expected.y != actual.y) appendLine("  Δy = ${actual.y - expected.y}")
                if (expected.height != actual.height) appendLine("  Δz = ${actual.height - expected.height}")
            }
            throw AssertionError(diff.trimEnd())
        }
    }

    /**
     * Asserts that [actual] is at the given coordinates.
     */
    fun assertEquals(x: Int, y: Int, z: Int, actual: Position, label: String = "position") {
        assertEquals(Position(x, y, z), actual, label)
    }

    /**
     * Asserts that [actual] is within [tiles] tiles of [expected].
     * Useful for approximate position checks (e.g., random spawn offsets).
     */
    fun assertNear(expected: Position, actual: Position, tiles: Int = 1, label: String = "position") {
        val dx = kotlin.math.abs(actual.x - expected.x)
        val dy = kotlin.math.abs(actual.y - expected.y)
        val dz = kotlin.math.abs(actual.height - expected.height)
        if (dx > tiles || dy > tiles || dz > 0) {
            throw AssertionError(
                "$label not within $tiles tiles of expected:" +
                        "\n  Expected: ($expected)" +
                        "\n  Actual:   ($actual)" +
                        "\n  Δx=$dx Δy=$dy Δz=$dz"
            )
        }
    }
}
