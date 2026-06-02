# Client-Side Security Audit

**Goal:** Map the modified-client attack surface — what can a packet-forging adversary do that a vanilla client cannot?

**Docs:** `AGENTS.md`, `code_index.json`, `prompts/00-cross-cutting/client-server-boundary.md`, `prompts/03-security/packet-injection.md`, `prompts/03-security/economy-risks.md`

---

## Trust Model

The client is fully untrusted. A modified 317 client can:
- Send any opcode with any payload
- Omit expected packets (e.g., never send keepalive)
- Send packets at arbitrary rates
- Claim any coordinate, any item ID, any amount
- Replay captured packets
- Connect multiple sessions with same credentials (race for login state)

The server is the single source of truth. Every client claim must be validated.

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-client/src/main/java/com/osroyale/Client.java` | 787KB monolith — all client packet send/parse, rendering, input |
| `game-client/src/main/java/com/osroyale/BufferedConnection.java` | Raw TCP socket — 5KB circular buffer, no TLS |
| `game-client/src/main/java/com/osroyale/AccountManager.java` | ENTIRE FILE COMMENTED OUT — dead code, client account storage |
| `game-server/src/main/java/com/osroyale/net/packet/in/` | All incoming packet handlers — the server-side validation surface |
| `game-server/src/main/java/com/osroyale/net/session/GameSession.java` | Session lifecycle — disconnect handling, packet queue |
| `game-server/settings.toml` | `[network]` section — rate limits, RSA keys, connection limits |

---

## Steps

### 1. Audit packet handlers for missing validation

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# List all incoming packet handlers
ls game-server/src/main/java/com/osroyale/net/packet/in/

# Find handlers that trust client-supplied amounts/IDs without validation
grep -rn "amount\|getAmount\|itemId\|npcId\|objectId\|slot\|index" \
  game-server/src/main/java/com/osroyale/net/packet/in/ \
  --include="*.java" | grep -v "import\|//\|test"
```

Every packet handler that accepts an `amount` from the client must validate `amount > 0` and `amount <= available`. Check each file individually.

### 2. Check movement/position validation

```bash
# Can client teleport by claiming arbitrary coordinates?
grep -rn "setPosition\|moveTo\|teleport\|position" \
  game-server/src/main/java/com/osroyale/net/packet/in/MovementPacketListener.java

# Check pathfinding validation — does server verify each step is walkable?
grep -rn "TraversalMap\|clipping\|canMove\|traversable" \
  game-server/src/main/java/com/osroyale/net/packet/in/WalkingPacketListener.java
```

### 3. Check interaction validation

```bash
# Item interactions — does server verify item exists in inventory at claimed slot?
grep -rn "firstClick\|secondClick\|thirdClick\|itemOnObject\|itemOnNpc\|itemOnItem\|itemOnPlayer" \
  game-server/src/main/java/com/osroyale/net/packet/in/ItemContainerActionPacketListener.java

# Object interactions — does server verify distance to object?
grep -rn "distance\|withinDistance\|getDistance\|position" \
  game-server/src/main/java/com/osroyale/net/packet/in/ObjectActionPacketListener.java
```

### 4. Check Client.java attack surface

```bash
# What packet opcodes does the client support sending?
grep -rn "putOpcode\|sendOpcode\|outBuffer\|packetOpcode\|PACKET_SIZES" \
  game-client/src/main/java/com/osroyale/Client.java | head -30

# Client-side account data (AccountManager is fully commented out — dead code)
wc -l game-client/src/main/java/com/osroyale/AccountManager.java
# Output: 162 lines, ALL inside /* */ block comments
```

### 5. BufferedConnection — no encryption, no TLS

```bash
# Raw TCP, 5KB circular buffer, no TLS wrapper
cat game-client/src/main/java/com/osroyale/BufferedConnection.java | grep -c "SSL\|TLS\|encrypt\|cipher"
# Expected: 0 — no encryption at transport layer
```

The RSA cipher operates at the application layer (packet payloads), not the transport layer. Session keys exchange happens after login. Packet forgery is possible if the MITM captures the RSA modulus from `settings.toml`.

### 6. Rate limiting audit

```toml
# From settings.toml [network]
connection_limit = 3           # Max connections per IP
client_packet_threshold = 30   # Packets per cycle before throttling
login_threshold = 200          # Login attempts per cycle
idle_timeout = 30              # Seconds before idle kick
```

Test with actual packet flooding: can a client send 1000 packets/second and crash the server or cause a desync?

### 7. Cache integrity

```bash
# Client cache — can models/sprites be injected?
ls game-client/src/main/java/net/runelite/cache/
find game-client/ -name "*.jag" -o -name "*.idx" -o -name "main_file_cache*" 2>/dev/null
```

---

## Known Vulnerabilities

### V1: BufferedConnection buffer overflow on write (DoS)

`BufferedConnection.java` line 78-93: 5KB circular buffer. If `queueBytes` is called when the buffer is 100 bytes from full (`buffIndex == (writeIndex + 4900) % 5000`), it throws `IOException("buffer overflow")` which sets `hasIOError = true`. Subsequent writes throw. An attacker flooding packets can trigger this and disconnect themselves, but the server-side equivalent could be more dangerous.

### V2: Client can send arbitrary item amounts

`ItemContainerActionPacketListener.java` — the client specifies the amount to move in bank/trade/inventory actions. If the server's `ItemContainer.add()`/`remove()` doesn't validate `amount <= 0`, negative amounts create items (see `economy-risks.md`).

### V3: No click-distance validation for objects/NPCs

Many packet handlers check if the object/NPC exists but NOT if the player is within interaction range. A modified client can click objects across the map.

---

## Client Impact

ALL findings are server-side validation gaps. The client is completely untrusted — a modified 317 client exists for every RSPS. The question is whether the SERVER validates what the client claims.

---

## Verify

- [ ] Every packet handler that accepts `amount` validates `amount > 0`
- [ ] Object/NPC interactions verify player is within range
- [ ] Movement packets verify each step is walkable (clipping check)
- [ ] Item interactions verify item exists in claimed inventory slot
- [ ] Rate limits effective against packet flooding
- [ ] RSA keys rotated from public Tarnish defaults
- [ ] No client-side state the server trusts without verification
