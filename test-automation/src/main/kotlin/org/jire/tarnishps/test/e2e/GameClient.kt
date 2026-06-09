package org.jire.tarnishps.test.e2e

import com.osroyale.net.codec.IsaacCipher
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * E2E game client that connects to a running Tarnish server via Netty,
 * performs the login handshake, and sends/receives game packets.
 *
 * Usage:
 * ```
 * val client = GameClient()
 * client.connect("localhost", 43594)
 * client.login("Zezima", "1")
 * client.sendPacket(103, byteArrayOf(...)) // click object packet
 * ```
 */
class GameClient {

    private val logger: Logger = LoggerFactory.getLogger(GameClient::class.java)

    private val group = NioEventLoopGroup(1)
    private var channel: Channel? = null

    // ISAAC ciphers for game packet encryption/decryption
    private var encryptor: IsaacCipher? = null

    /** The ISAAC decryptor, used by [ClientChannelHandler] to decode incoming packet opcodes. */
    var decryptor: IsaacCipher? = null
        private set

    // Login flow synchronisation
    private val loginLatch = CountDownLatch(1)
    private val loginResponse = AtomicReference<LoginResult>()

    // Credentials to use during login handshake
    private var loginUsername: String = ""
    private var loginPassword: String = ""

    // Incoming packet queue (processed by BotPlayer)
    val incomingPackets: ConcurrentLinkedQueue<GamePacketEvent> = ConcurrentLinkedQueue()

    // Connection state
    val connected = AtomicBoolean(false)
    val loggedIn = AtomicBoolean(false)

    // RSA keys from server settings.toml
    companion object {
        private val RSA_MODULUS = BigInteger("102353038900255891527619367941460634639078944277149869534765441701765061915480193910291695742706042386340616731973380032288127455494356031646220980795852675234525031620306539656965685802100384909448780766960597664159328648803794286947984198753216591499378109000984639229430631686267432671373106838769133939913")
        private val RSA_EXPONENT = BigInteger("53925997225795133719229887783247543425130183971584110428703113302718248056511827143879975621403130102964053208695986070996181590716154529757425030836016240047551059410105065493557574944899978363674428788471686009398071320688429363476724786245628722544641599881866860779098693203875115492310538964714249216129")
        private const val CLIENT_VERSION = 12
        private const val LOGIN_HANDSHAKE = 14
        private const val NEW_CONNECTION_OPCODE = 16
        private const val MAGIC_NUMBER = 255
    }

    /**
     * Connects to the server at [host]:[port].
     */
    fun connect(host: String = "localhost", port: Int = 43594): Boolean {
        logger.info("Connecting to {}:{}...", host, port)
        try {
            val latch = CountDownLatch(1)
            val connectedRef = AtomicBoolean(false)

            val bootstrap = Bootstrap()
                .group(group)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(ClientChannelHandler(this@GameClient))
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)

            val future = bootstrap.connect(host, port)
            future.addListener(
                io.netty.util.concurrent.GenericFutureListener { f: io.netty.util.concurrent.Future<in Void> ->
                    if (f.isSuccess) {
                        val ch = (f as io.netty.channel.ChannelFuture).channel()
                        channel = ch
                        connected.set(true)
                        connectedRef.set(true)
                        logger.info("Connected to {}:{}", host, port)
                    } else {
                        logger.error("Failed to connect: {}", f.cause().message)
                    }
                    latch.countDown()
                }
            )

            return latch.await(10, TimeUnit.SECONDS) && connectedRef.get()
        } catch (e: Exception) {
            logger.error("Connection error", e)
            return false
        }
    }

    /**
     * Performs the full login handshake and game login.
     */
    fun login(username: String, password: String): LoginResult {
        if (!connected.get()) {
            return LoginResult.Failure("Not connected")
        }

        logger.info("Logging in as '{}'...", username)

        // Store credentials for use during the login handshake
        loginUsername = username
        loginPassword = password

        // Step 1: Send handshake (opcode 14)
        val handshakeBuf = Unpooled.buffer(2)
        handshakeBuf.writeByte(LOGIN_HANDSHAKE)
        handshakeBuf.writeByte(0) // name hash
        channel?.writeAndFlush(handshakeBuf)

        // Wait for server seed response (handled in channelRead)
        try {
            val result = loginLatch.await(15, TimeUnit.SECONDS)
            if (!result) {
                return LoginResult.Failure("Login timed out")
            }
            val response = loginResponse.get()
            if (response != null) {
                loggedIn.set(response is LoginResult.Success)
            }
            return response ?: LoginResult.Failure("No login response")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return LoginResult.Failure("Login interrupted")
        }
    }

    /**
     * Called by [ClientChannelHandler] when the server sends the handshake response.
     */
    fun handleHandshakeResponse(buf: ByteBuf) {
        try {
            // Read 17 bytes: 8 zero + 1 zero + 8 server seed
            buf.readLong() // discard 8 zero bytes
            buf.readByte() // discard 1 zero byte
            val serverSeed = buf.readLong()

            logger.debug("Received server seed: {}", serverSeed)

            // Step 2: Send connection type
            val connTypeBuf = Unpooled.buffer(1)
            connTypeBuf.writeByte(NEW_CONNECTION_OPCODE)
            channel?.writeAndFlush(connTypeBuf)

            // Step 3: Build and send login payload
            sendLoginPayload(serverSeed, loginUsername, loginPassword)
        } catch (e: Exception) {
            logger.error("Handshake error", e)
            loginResponse.set(LoginResult.Failure("Handshake failed: ${e.message}"))
            loginLatch.countDown()
        }
    }

    private fun sendLoginPayload(serverSeed: Long, username: String, password: String) {
        val random = SecureRandom()
        val clientHalf = random.nextLong()

        // Build ISAAC seed
        val isaacSeed = intArrayOf(
            (clientHalf shr 32).toInt(),
            clientHalf.toInt(),
            (serverSeed shr 32).toInt(),
            serverSeed.toInt()
        )
        decryptor = IsaacCipher(isaacSeed)

        val encryptSeed = isaacSeed.map { it + 50 }.toIntArray()
        encryptor = IsaacCipher(encryptSeed)

        // Build the RSA block
        val rsaBlock = Unpooled.buffer()
        try {
            rsaBlock.writeByte(10) // RSA magic
            rsaBlock.writeLong(clientHalf)
            rsaBlock.writeLong(serverSeed)
            rsaBlock.writeInt(random.nextInt()) // UID
            writeString(rsaBlock, "00000000-0000-0000-0000-000000000000") // UUID
            writeString(rsaBlock, "00:00:00:00:00:00") // MAC address
            writeString(rsaBlock, username)
            writeString(rsaBlock, password)

            val rsaData = ByteArray(rsaBlock.readableBytes())
            rsaBlock.readBytes(rsaData)

            // RSA encrypt
            val rsaEncrypted = BigInteger(rsaData).modPow(RSA_EXPONENT, RSA_MODULUS).toByteArray()

            // Build the full login payload
            val loginBlockSize = 38 + rsaEncrypted.size
            val payload = Unpooled.buffer(1 + loginBlockSize)

            payload.writeByte(loginBlockSize) // login block size
            payload.writeByte(MAGIC_NUMBER)   // magic ID
            payload.writeByte(CLIENT_VERSION) // client version
            payload.writeByte(0)              // memory version (low mem)
            // CRC values (9 ints, all 0)
            repeat(9) { payload.writeInt(0) }
            payload.writeByte(rsaEncrypted.size) // RSA block size
            payload.writeBytes(rsaEncrypted)

            channel?.writeAndFlush(payload)
            logger.debug("Login payload sent ({} bytes)", 1 + loginBlockSize)
        } finally {
            rsaBlock.release()
        }
    }

    /**
     * Called by [ClientChannelHandler] when the server sends the login response.
     */
    fun handleLoginResponse(buf: ByteBuf) {
        try {
            if (!buf.isReadable()) {
                loginResponse.set(LoginResult.Failure("Empty login response"))
                return
            }

            val responseCode = buf.readUnsignedByte().toInt()
            logger.info("Login response code: {}", responseCode)

            when (responseCode) {
                2 -> { // NORMAL
                    val rights = buf.readUnsignedByte().toInt()
                    val flagged = buf.readUnsignedByte().toInt() == 1
                    loggedIn.set(true)
                    loginResponse.set(LoginResult.Success(rights, flagged))
                    logger.info("Login successful! Rights={}, Flagged={}", rights, flagged)
                }
                1 -> loginResponse.set(LoginResult.Failure("Login delayed (1)"))
                3 -> loginResponse.set(LoginResult.Failure("Invalid username/password"))
                4 -> loginResponse.set(LoginResult.Failure("Account disabled"))
                5 -> loginResponse.set(LoginResult.Failure("Account already online"))
                6 -> loginResponse.set(LoginResult.Failure("Game updated"))
                7 -> loginResponse.set(LoginResult.Failure("World full"))
                8 -> loginResponse.set(LoginResult.Failure("Server being updated"))
                9 -> loginResponse.set(LoginResult.Failure("Login attempts exceeded"))
                10 -> loginResponse.set(LoginResult.Failure("Cannot connect"))
                11 -> loginResponse.set(LoginResult.Failure("Login server offline"))
                14 -> loginResponse.set(LoginResult.Failure("Bad username"))
                15 -> loginResponse.set(LoginResult.Failure("Short username"))
                16 -> loginResponse.set(LoginResult.Failure("Insufficient permissions"))
                20 -> loginResponse.set(LoginResult.Failure("Invalid email"))
                22 -> loginResponse.set(LoginResult.Failure("Email required"))
                else -> loginResponse.set(LoginResult.Failure("Unknown response: $responseCode"))
            }
        } catch (e: Exception) {
            logger.error("Login response error", e)
            loginResponse.set(LoginResult.Failure("Login response error: ${e.message}"))
        } finally {
            loginLatch.countDown()
        }
    }

    /**
     * Called by [ClientChannelHandler] when a game packet is received.
     */
    fun handleGamePacket(opcode: Int, payload: ByteBuf) {
        val data = ByteArray(payload.readableBytes())
        payload.readBytes(data)
        incomingPackets.add(GamePacketEvent(opcode, data))
    }

    /**
     * Sends a game packet with the given [opcode] and [payload] bytes.
     */
    fun sendPacket(opcode: Int, payload: ByteArray = byteArrayOf()) {
        val enc = encryptor ?: return
        val ch = channel ?: return

        val buf = Unpooled.buffer(1 + payload.size)
        // Encrypt opcode
        buf.writeByte((opcode + (enc.key and 0xFF)) and 0xFF)
        if (payload.isNotEmpty()) {
            buf.writeByte(payload.size)
            buf.writeBytes(payload)
        }
        ch.writeAndFlush(buf)
    }

    /**
     * Disconnects from the server.
     */
    fun disconnect() {
        try {
            channel?.close()
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS)
        } catch (_: Exception) { }
        connected.set(false)
        loggedIn.set(false)
    }

    private fun writeString(buf: ByteBuf, str: String) {
        buf.writeBytes(str.toByteArray())
        buf.writeByte(10) // string terminator (newline)
    }
}

/**
 * Result of a login attempt.
 */
sealed class LoginResult {
    data class Success(val rights: Int, val flagged: Boolean) : LoginResult()
    data class Failure(val reason: String) : LoginResult()
}

/**
 * A game packet received from the server.
 */
data class GamePacketEvent(
    val opcode: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GamePacketEvent) return false
        return opcode == other.opcode && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = opcode
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Netty channel handler for the E2E game client.
 */
class ClientChannelHandler(private val client: GameClient) : SimpleChannelInboundHandler<ByteBuf>() {

    private val logger: Logger = LoggerFactory.getLogger(ClientChannelHandler::class.java)
    private var handshakeComplete = false
    private var loginResponseReceived = false

    override fun channelRead0(ctx: ChannelHandlerContext, buf: ByteBuf) {
        if (!handshakeComplete) {
            // First message from server is the handshake response (17 bytes)
            if (buf.readableBytes() >= 17) {
                handshakeComplete = true
                client.handleHandshakeResponse(buf)
            }
        } else if (!loginResponseReceived) {
            // Second message is the login response (1-3 bytes)
            loginResponseReceived = true
            client.handleLoginResponse(buf)
        } else {
            // Game packets — decode opcode with ISAAC
            // Server encrypts: (opcode + encryptor.key) & 0xFF
            // We decrypt: (readByte() - decryptor.key) & 0xFF
            if (buf.readableBytes() < 1) return
            val rawOpcode = buf.readUnsignedByte().toInt()
            val decryptor = client.decryptor
            val opcode = if (decryptor != null) {
                (rawOpcode - (decryptor.key and 0xFF)) and 0xFF
            } else {
                rawOpcode
            }
            client.handleGamePacket(opcode, buf)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error("Channel error", cause)
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("Channel disconnected")
        client.connected.set(false)
        client.loggedIn.set(false)
    }
}
