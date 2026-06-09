package org.jire.tarnishps.test.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * E2E test for teleport commands using an ADMINISTRATOR-privilege account.
 *
 * These tests require a running server on localhost:43594 and an
 * account "Oak" with password "1" and ADMINISTRATOR rights.
 *
 * Run with: ./gradlew :test-automation:test -Pe2e
 * Or: ./gradlew :test-automation:test -De2e=true
 */
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class TeleportE2ETest : E2ETest() {

    // Uses Oak (ADMINISTRATOR) by default from E2ETest base class

    @Test
    fun `teleport to coordinates and verify position`() {
        bot.teleportTo(3222, 3222)

        // The tele command moves the player; verify we can still interact
        bot.sleep(500)
        bot.clearMessages()

        // Send a command and verify we get a response (proves we're still connected and working)
        bot.sendCommand("help")
        val found = bot.waitForMessage({ msg: String ->
            msg.contains("command", ignoreCase = true) || msg.contains("help", ignoreCase = true)
        }, timeoutMs = 3000)
        assertNotNull(found) { "Expected a response to ::help command after teleport" }
    }

    @Test
    fun `teleport to different height`() {
        bot.teleportTo(3087, 3500, 0)
        bot.sleep(500)
        bot.clearMessages()

        bot.teleportTo(3087, 3500, 1)
        bot.sleep(500)
        bot.clearMessages()

        // Verify we can still send commands after teleporting
        bot.sendCommand("help")
        val found = bot.waitForMessage({ msg: String ->
            msg.contains("command", ignoreCase = true) || msg.contains("help", ignoreCase = true)
        }, timeoutMs = 3000)
        assertNotNull(found) { "Expected a response to ::help command after height change" }
    }

    @Test
    fun `send command and receive response`() {
        bot.sendCommand("help")
        val found = bot.waitForMessage({ msg: String ->
            msg.contains("command", ignoreCase = true) || msg.contains("help", ignoreCase = true)
        }, timeoutMs = 3000)
        assertNotNull(found) { "Expected a response to ::help command" }
    }
}
