package org.jire.tarnishps.test.e2e

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import kotlin.math.min

/**
 * High-level API for controlling a player in-game via the E2E automation client.
 *
 * Provides fluent methods for common in-game actions like teleporting,
 * clicking objects, sending chat messages, and waiting for responses.
 *
 * Usage:
 * ```
 * val bot = BotPlayer(client)
 * bot.teleportTo(3222, 3222)
 * bot.clickObject(23104) // Cerberus entrance
 * bot.waitForMessage("need a slayer task")
 * ```
 */
class BotPlayer(private val client: GameClient) {

    private val logger: Logger = LoggerFactory.getLogger(BotPlayer::class.java)

    /** Queue of chat messages received from the server. */
    val messages: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    /** The player's last known position (approximate, tracked from packets). */
    var lastPosition: Triple<Int, Int, Int>? = null
        private set

    /** Whether the player is currently in an activity/minigame. */
    var inActivity: Boolean = false
        private set

    /** The name of the current activity, if any. */
    var currentActivity: String? = null
        private set

    /**
     * Processes any pending packets from the server.
     * Should be called periodically (or before assertions).
     */
    fun pollPackets() {
        while (true) {
            val packet = client.incomingPackets.poll() ?: break
            handlePacket(packet)
        }
    }

    private fun handlePacket(packet: GamePacketEvent) {
        when (packet.opcode) {
            253 -> handleMessagePacket(packet.data)  // SendMessage
            104 -> handlePositionPacket(packet.data) // something with position
        }
    }

    private fun handleMessagePacket(data: ByteArray) {
        // SendMessage format: string (null-terminated) + byte (filtered)
        val message = extractString(data, 0)
        if (message != null) {
            messages.add(message)
            logger.debug("[CHAT] {}", message)
        }
    }

    private fun handlePositionPacket(data: ByteArray) {
        // Position update packets vary — this is approximate
        if (data.size >= 6) {
            val x = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val y = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
            val z = data[4].toInt() and 0xFF
            if (x in 1000..10000 && y in 1000..10000) {
                lastPosition = Triple(x, y, z)
            }
        }
    }

    /**
     * Sends a teleport command (requires OWNER/ADMIN rights).
     * Uses the command packet: opcode 103 with command string.
     */
    fun teleportTo(x: Int, y: Int, z: Int = 0) {
        logger.info("Teleporting to ({}, {}, {})", x, y, z)
        val cmd = "tele $x $y $z"
        sendCommand(cmd)
    }

    /**
     * Sends a command as the player.
     * Uses the command packet structure (opcode 103).
     */
    fun sendCommand(command: String) {
        val bytes = command.toByteArray()
        val payload = ByteArray(bytes.size + 1)
        System.arraycopy(bytes, 0, payload, 0, bytes.size)
        payload[bytes.size] = 10 // string terminator
        client.sendPacket(103, payload)
        logger.debug("Sent command: ::{}", command)
    }

    /**
     * Clicks an object with the given [objectId].
     * Uses the object click packet structure.
     */
    fun clickObject(objectId: Int, x: Int = 0, y: Int = 0) {
        logger.info("Clicking object {}", objectId)
        // Object click packet: opcode 132 (first click)
        val payload = ByteArray(6)
        payload[0] = (objectId shr 8).toByte()
        payload[1] = objectId.toByte()
        payload[2] = (x shr 8).toByte()
        payload[3] = x.toByte()
        payload[4] = (y shr 8).toByte()
        payload[5] = y.toByte()
        client.sendPacket(132, payload)
    }

    /**
     * Clicks an NPC with the given [npcId].
     */
    fun clickNpc(npcId: Int, option: Int = 0) {
        logger.info("Clicking NPC {} (option {})", npcId, option)
        // NPC click packet: opcode 17 (first click)
        val payload = ByteArray(2)
        payload[0] = (npcId shr 8).toByte()
        payload[1] = npcId.toByte()
        client.sendPacket(17, payload)
    }

    /**
     * Sends a chat message.
     */
    fun say(message: String) {
        logger.info("Saying: {}", message)
        val bytes = message.toByteArray()
        val payload = ByteArray(bytes.size + 1)
        System.arraycopy(bytes, 0, payload, 0, bytes.size)
        payload[bytes.size] = 10
        client.sendPacket(4, payload) // opcode 4 = chat message
    }

    /**
     * Waits for a specific message to appear in the chatbox.
     * Polls packets until the message is found or [timeoutMs] elapses.
     *
     * @return true if the message was received
     */
    fun waitForMessage(text: String, timeoutMs: Long = 5000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            pollPackets()
            if (messages.any { it.contains(text, ignoreCase = true) }) {
                return true
            }
            Thread.sleep(50)
        }
        return false
    }

    /**
     * Waits for any message matching the given [predicate].
     */
    fun waitForMessage(predicate: (String) -> Boolean, timeoutMs: Long = 5000): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            pollPackets()
            val match = messages.firstOrNull(predicate)
            if (match != null) return match
            Thread.sleep(50)
        }
        return null
    }

    /**
     * Clears all received messages.
     */
    fun clearMessages() {
        messages.clear()
    }

    /**
     * Gets the most recent message, if any.
     */
    fun lastMessage(): String? = messages.lastOrNull()

    /**
     * Checks if the player has received a specific message.
     */
    fun hasReceived(text: String): Boolean =
        messages.any { it.contains(text, ignoreCase = true) }

    /**
     * Sleeps for [ms] milliseconds, polling packets in the meantime.
     */
    fun sleep(ms: Long) {
        val deadline = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < deadline) {
            pollPackets()
            Thread.sleep(min(50L, deadline - System.currentTimeMillis()))
        }
    }

    private fun extractString(data: ByteArray, startIndex: Int): String? {
        val sb = StringBuilder()
        var i = startIndex
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            if (b == 10) break // string terminator
            sb.append(b.toChar())
            i++
        }
        return if (sb.isEmpty()) null else sb.toString()
    }
}
