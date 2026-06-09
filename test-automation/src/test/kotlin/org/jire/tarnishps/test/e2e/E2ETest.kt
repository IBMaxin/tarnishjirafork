package org.jire.tarnishps.test.e2e

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Base class for E2E tests that connect to a running server.
 *
 * Handles connect/login lifecycle. Subclasses define specific scenarios.
 *
 * Usage:
 * ```
 * class TeleportE2ETest : E2ETest() {
 *     @Test
 *     fun `teleport to home`() {
 *         bot.teleportTo(3087, 3500)
 *         bot.waitForMessage("You have teleported")
 *     }
 * }
 * ```
 *
 * Prerequisites:
 * - Server must be running on localhost:43594
 * - Test accounts must exist in data/profile/save/
 */
abstract class E2ETest {

    private val logger: Logger = LoggerFactory.getLogger(E2ETest::class.java)

    /** The low-level network client. */
    protected lateinit var client: GameClient
        private set

    /** The high-level bot API for sending actions and waiting for responses. */
    protected lateinit var bot: BotPlayer
        private set

    /** Configuration for this test run. */
    protected open val serverHost: String get() = "localhost"
    protected open val serverPort: Int get() = 43594
    protected open val loginUsername: String get() = "Oak"
    protected open val loginPassword: String get() = "1"
    protected open val connectTimeoutMs: Long get() = 10_000L
    protected open val loginTimeoutMs: Long get() = 15_000L

    /**
     * Connects to the server and logs in before each test.
     */
    @BeforeEach
    open fun setUp() {
        logger.info("=== E2E Test Setup ===")
        logger.info("Connecting to {}:{} as '{}'...", serverHost, serverPort, loginUsername)

        client = GameClient()
        val connected = client.connect(serverHost, serverPort)
        require(connected) { "Failed to connect to $serverHost:$serverPort — is the server running?" }

        val result = client.login(loginUsername, loginPassword)
        require(result is LoginResult.Success) {
            "Login failed for '$loginUsername': ${(result as? LoginResult.Failure)?.reason ?: "unknown"}"
        }

        bot = BotPlayer(client)
        logger.info("Logged in successfully! Rights={}, Flagged={}", result.rights, result.flagged)

        // Wait a moment for the server to finish initialising the player
        bot.sleep(500)
        bot.pollPackets()
    }

    /**
     * Disconnects from the server after each test.
     */
    @AfterEach
    open fun tearDown() {
        logger.info("=== E2E Test Teardown ===")
        try {
            bot.pollPackets()
            logger.info("Messages received during test: {}", bot.messages.size)
            bot.messages.forEach { logger.debug("  [MSG] {}", it) }
        } catch (_: Exception) { }
        try {
            client.disconnect()
        } catch (_: Exception) { }
        logger.info("Disconnected.")
    }
}
