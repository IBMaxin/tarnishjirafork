# Networking & Login System

**Goal:** Document the network layer — packets, login flow, security limits.

**Docs:** `AGENTS.md`, `00-cross-cutting/client-server-boundary.md` §Boundary 2, `03-security/packet-injection.md`

---

## Network architecture

```
Client (game-client/)           Server (game-server/)
    │                                │
    │  TCP :43594                    │
    │  RSA-encrypted login           │
    │  GamePacket stream             │
    │                                │
```

---

## Key server files

| File | Purpose |
|------|---------|
| `game-server/src/main/java/com/osroyale/net/` | Network package |
| `game-server/src/main/java/com/osroyale/net/packet/ClientPackets.java` | Client→Server opcode constants (~60+) |
| `game-server/src/main/java/com/osroyale/net/packet/PacketRepository.java` | Registers packet listeners |
| `game-server/src/main/java/com/osroyale/net/packet/PacketListener.java` | Packet handler interface |
| `game-server/src/main/java/com/osroyale/net/packet/PacketListenerMeta.java` | Annotation: `@PacketListenerMeta(opcode)` |
| `game-server/src/main/java/com/osroyale/net/packet/in/` | Incoming packet handlers |
| `game-server/src/main/java/com/osroyale/net/packet/out/` | Outgoing packet builders |
| `game-server/data/io/message_sizes.json` | Packet size definitions |

---

## Network config (settings.toml)

```toml
[network]
connection_limit = 3           # Max connections per IP
failed_login_attempts = 5      # Lockout threshold
failed_login_timeout = 1       # Lockout duration (hours)
login_threshold = 200          # Login rate limit
logout_threshold = 200         # Logout rate limit
client_packet_threshold = 30   # Max packets per tick per client
server_packet_threshold = 1000 # Max packets per tick server-wide
idle_timeout = 30              # Idle kick (minutes)
display_packets = true         # Log all packets
resource_leak_detection = "DISABLED"
```

---

## Login flow

1. Client connects to `:43594`
2. RSA handshake (keys in `settings.toml`)
3. Client sends login packet (username, password hash)
4. Server validates credentials, loads profile from `data/profile/save/`
5. Server sends player appearance, inventory, equipment, skills, position
6. Client renders world, player enters game

Check login handler:
```bash
find game-server/src -name "*Login*" -o -name "*Session*" | grep -i login | head -10
```

---

## Outgoing packet examples

| Packet | Opcode | Purpose |
|--------|--------|---------|
| `SendMessage` | 253 | Chat messages |
| `SendString` | varies | Interface text |
| `SendItemOnInterface` | varies | Interface items |
| `SendCoordinate` | varies | Player position |
| `SendConfig` | varies | Varbit/config changes |
| `SendBanner` | varies | Notification banners |

Full list:
```bash
ls game-server/src/main/java/com/osroyale/net/packet/out/ | wc -l
```

---

## Steps

1. Verify `connection_limit` and `failed_login_attempts` are reasonable
2. Check that `display_packets` is disabled for production (logs all packet data)
3. Verify RSA keys are NOT the default public Tarnish keys
4. Check login handler for SQL injection (if DB-backed auth)
5. Check that idle timeout actually kicks players

---

## Client Impact

This IS the client-server boundary. Every client packet is handled here. Every server response goes through here.

→ See [packet-injection.md](../03-security/packet-injection.md) for client packet attack surface.
→ See [client-server-boundary.md](../00-cross-cutting/client-server-boundary.md) for opcode coordination.

---

## Verify

- [ ] `connection_limit` prevents multi-login flooding
- [ ] `failed_login_attempts` works (test with wrong password 6 times)
- [ ] `display_packets = false` in production
- [ ] RSA keys are not default public keys
- [ ] Port 43594 is the only exposed port (no debug ports)
