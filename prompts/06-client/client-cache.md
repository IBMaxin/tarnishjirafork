# Client Cache Integrity Audit

**Goal:** Audit the client cache for model/sprite injection risk and cache format integrity.

**Docs:** `AGENTS.md`, `prompts/06-client/client-overview.md`, `prompts/06-client/client-security.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-client/src/main/java/com/osroyale/Js5.java` | JS5 cache protocol client — fetches cache from server |
| `game-client/src/main/java/com/osroyale/Signlink.java` | Cache I/O, file system access for cache |
| `game-client/src/main/java/net/runelite/cache/` | RuneLite cache library |
| `game-client/src/main/java/net/runelite/cache/fs/` | Cache filesystem abstraction |
| `game-client/src/main/java/net/runelite/cache/IndexType.java` | Cache index types (models, sprites, configs, etc.) |
| `game-server/settings.toml` | `npc_definition_limit = 32767`, `item_definition_limit = 28473` |
| `game-server/data/def/item/item_definitions.json` | 2.8MB item definitions |
| `game-server/data/def/npc/npc_definitions.json` | 2.3MB NPC definitions |

---

## Cache Architecture

The OSRS cache uses JS5 (Jagex Store 5) format:

```
Cache root/
├── main_file_cache.dat2    # Master data file
├── main_file_cache.idx0    # Index 0: Animation/sequence definitions
├── main_file_cache.idx1    # Index 1: Model definitions
├── main_file_cache.idx2    # Index 2: Config (items, NPCs, objects)
├── main_file_cache.idx3    # Index 3: Interface definitions
├── main_file_cache.idx4    # Index 4: Sound/music
├── main_file_cache.idx5    # Index 5: Maps
├── main_file_cache.idx6    # Index 6: Sprites
├── main_file_cache.idx7    # Index 7: Textures
├── main_file_cache.idx8    # Index 8: Huffman encoding
├── main_file_cache.idx255  # Index 255: Reference table (checksums)
```

The server serves cache files on-demand via JS5 protocol (port 43594, same as game port, different request type).

---

## Steps

### 1. Locate the cache files

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Check for cache in user home (typical RSPS location)
find ~/ -name "main_file_cache.dat2" 2>/dev/null
find ~/ -path "*/tarnish*" -name "*.dat2" 2>/dev/null

# Check for cache in game-client directory
find game-client/ -name "*.dat2" -o -name "*.idx*" 2>/dev/null

# Check for cache in project root
find . -maxdepth 3 -name "main_file_cache*" 2>/dev/null
```

### 2. Check JS5 server (cache serving)

```bash
# How does the server serve cache files?
grep -rn "Js5\|js5\|JS5\|cache\|Cache\|ondemand\|OnDemand" \
  game-server/src/main/java/com/osroyale/ --include="*.java" | grep -v "import" | head -20

# Check cache request handling
find game-server/ -name "*Cache*" -o -name "*Js5*" -o -name "*Ondemand*" | grep -v ".class"
```

### 3. Audit cache integrity model

```bash
# RuneLite Cache library — provides read/write access to JS5 format
find game-client/src/main/java/net/runelite/cache/ -name "*.java" | wc -l

# Check Cache.java for write capabilities
grep -rn "write\|save\|export\|pack\|encode" \
  game-client/src/main/java/net/runelite/cache/Cache.java 2>/dev/null || \
  find game-client/src/main/java/net/runelite/cache/ -name "Cache.java" -exec grep -rn "write\|save\|export" {} \;
```

### 4. Check model/sprite injection surface

```bash
# Can models be added at runtime?
grep -rn "addModel\|loadModel\|modelOffset\|modelIndex" \
  game-client/src/main/java/com/osroyale/Client.java | head -10

# Can sprites be replaced at runtime?
grep -rn "sprite\|Sprite\|loadSprite\|addSprite" \
  game-client/src/main/java/com/osroyale/Client.java | head -10
```

### 5. Check server-side cache validation

```bash
# Does the server validate item/NPC IDs against cache limits?
grep -rn "definition_limit\|DEFINITION_LIMIT\|item_definition_limit\|npc_definition_limit" \
  game-server/ --include="*.java" --include="*.toml"

# What happens when a client requests an out-of-bounds ID?
grep -rn "getDefinition\|getDef\|ItemDefinition.get\|NpcDefinition.get" \
  game-server/src/main/java/com/osroyale/net/packet/in/ --include="*.java" | head -10
```

### 6. Check custom item model support

```bash
# Are there custom items (models not in standard OSRS cache)?
grep -rn "custom\|Custom\|CUSTOM" \
  game-server/data/def/item/item_definitions.json | head -5

# Check for model injection via packet
grep -rn "model\|Model\|sendModel\|addModel\|inject" \
  game-server/src/main/java/com/osroyale/net/packet/ --include="*.java"
```

---

## Findings

### F1: 317 cache has limited modern item models [MEDIUM]

The 317 cache (2006-era) doesn't have models for modern OSRS items (Toxic Blowpipe 12926, Twisted Bow 20997, etc.). These either:
- Use placeholder models (look wrong in-game)
- Have been injected via custom cache patches
- Are defined server-side but render incorrectly in client

### F2: No runtime cache integrity check [MEDIUM]

The JS5 protocol serves cache files on request. The server doesn't verify the client's cache integrity — a modified client can replace sprites, models, or textures locally. This is a client-side visual cheat (e.g., making all items appear as a different model, making walls transparent).

### F3: RuneLite cache library enables cache manipulation [LOW]

The `net.runelite.cache` library provides full read/write access to the JS5 cache format. This is useful for development (packing new models) but can also be used to inject modified cache data.

### F4: Definition limits in settings.toml [INFO]

```toml
npc_definition_limit = 32767
item_definition_limit = 28473
```

These cap the maximum ID the server will serve. Requests for IDs above these limits are rejected. This prevents some crash vectors but doesn't prevent a modified client from using custom local models.

---

## Cache Integrity Model

```
Trusted:
  Server-side definitions (item_definitions.json, npc_definitions.json)
  Server-enforced ID limits (npc_definition_limit, item_definition_limit)

Untrusted:
  Client-side cache files (can be modified locally)
  Client-rendered models/sprites (visual only, doesn't affect server state)
  Client-claimed item IDs (server must validate against definitions)
```

**Key principle:** The client cache only affects what the player SEES. Server-side validation ensures the cache doesn't affect what HAPPENS. A player with an injected cache that makes sharks look like coins still gets 20 HP from eating a shark — the server determines the effect, not the client's visual.

---

## Client Impact

Client-side only — cache integrity affects visual rendering, not server state. However, a poisoned cache can:
- Hide certain game elements (transparent walls in PVP)
- Replace item appearances (confuse players in trades)
- Inject malicious sprites (social engineering)

Server-side validation is the defense.

---

## Verify

- [ ] Server validates item/NPC IDs against definition limits
- [ ] Cache files are not writable by the server process (if on same machine)
- [ ] JS5 cache served from a trusted source (not user-writable directory)
- [ ] No server-side dependency on client cache being untampered
- [ ] All game mechanics (healing, damage, drops) use server-side definitions, not client cache data
