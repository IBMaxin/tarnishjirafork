# Teleport System Audit

**Goal:** Audit the teleport system for wilderness bypass, PVP zone escape, destination safety, and admin privilege leaks.

**Docs:** `AGENTS.md`, `code_index.json`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/teleport/TeleportHandler.java` | Teleport UI, destination routing, wilderness check |
| `game-server/src/main/java/com/osroyale/content/teleport/Teleport.java` | Teleport enum — all destinations |
| `game-server/src/main/java/com/osroyale/content/teleport/TeleportTablet.java` | Teleport tablet definitions |
| `game-server/src/main/java/com/osroyale/content/skill/impl/magic/teleport/Teleportation.java` | Core teleport engine |
| `game-server/src/main/java/com/osroyale/content/skill/impl/magic/teleport/TeleportationData.java` | Teleport animation/GFX data |
| `game-server/plugins/plugin/click/item/TeleportTabletPlugin.java` | Tablet click handler |

---

## Steps

### 1. Audit wilderness teleport restriction

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# TeleportHandler.teleport() line 122-126 — THE ONLY WILDERNESS CHECK
grep -A10 "public static void teleport(Player player)" \
  game-server/src/main/java/com/osroyale/content/teleport/TeleportHandler.java
```

Current check (line 123-125):
```java
if (player.wilderness > 20 && !PlayerRight.isAdministrator(player)) {
    player.send(new SendMessage("You can't teleport past 20 wilderness!"));
    return;
}
```

**Issues:**
1. Only applies to TeleportHandler UI — does NOT check TeleportTablet, spellbook teleports, or Teleportation.teleport() directly
2. Admins bypass completely — intentional but worth documenting
3. No teleblock check in this path (teleblock is checked in `special()` but not in the main `teleport()` path)
4. Below-20 wilderness allows teleport — OSRS restricts above level 20 but this is a design choice

### 2. Check all teleport entry points

```bash
# Every place that calls Teleportation.teleport()
grep -rn "Teleportation\.teleport\|teleport\(" \
  game-server/src/main/java/com/osroyale/ --include="*.java" \
  | grep -v "Teleportation.java\|TeleportHandler\|import\|//"

# Spellbook teleports
grep -rn "teleport" game-server/src/main/java/com/osroyale/content/skill/impl/magic/ --include="*.java"
```

### 3. Check Teleport enum for PVP-zone destinations

```bash
# List all teleport destinations
python3 -c "
import re
with open('game-server/src/main/java/com/osroyale/content/teleport/Teleport.java') as f:
    content = f.read()
# Find all Position constructors in the enum
positions = re.findall(r'new Position\((\d+),\s*(\d+),\s*(\d+)\)', content)
for x, y, z in positions:
    print(f'({x}, {y}, {z})')
" | head -30
```

### 4. Verify teleport tablet security

```bash
# TeleportTablet.java — do tablets have the same checks as the UI?
cat game-server/src/main/java/com/osroyale/content/teleport/TeleportTablet.java

# Tablet click handler
cat game-server/plugins/plugin/click/item/TeleportTabletPlugin.java
```

### 5. Check Teleportation.teleport() core

```bash
# The actual teleport engine — what checks does it perform?
grep -A20 "public static void teleport" \
  game-server/src/main/java/com/osroyale/content/skill/impl/magic/teleport/Teleportation.java
```

### 6. Known bug: String comparison with ==

```java
// TeleportHandler.java line 135
if (teleport.getName() == "Inferno") {
```

This uses `==` instead of `.equals()` for String comparison. In Java, `==` compares object references, not content. This comparison will ALWAYS fail unless both strings are interned to the same object. The Inferno check is effectively dead code — players teleporting to Inferno will NOT trigger `Inferno.create(player)`.

---

## Findings

### F1: Wilderness check applies to UI only, not all teleport paths [HIGH]

`TeleportHandler.teleport()` checks wilderness level, but `Teleportation.teleport()` (called directly by spellbooks, tabs) may not. Each teleport entry point must be audited separately.

### F2: No teleblock check in main teleport path [MEDIUM]

`TeleportHandler.teleport()` does NOT check `player.isTeleblocked()`. The teleblock check exists only in the `special()` method (line 178). Standard teleports bypass the teleblock check.

### F3: Inferno teleport broken due to == comparison [MEDIUM]

Line 135: `teleport.getName() == "Inferno"` — always false. The Inferno minigame is never created via teleport UI. Players teleport to the Inferno location without entering the activity instance.

### F4: Admin wilderness bypass [INFO]

Line 123: `!PlayerRight.isAdministrator(player)` — admins can teleport from any wilderness level. This is intentional for moderation but is a privilege that could be abused if an admin account is compromised.

### F5: No cooldown/delay enforcement [INFO]

No teleport delay between consecutive teleports. A player can rapidly teleport between locations. In OSRS, teleports have a casting animation delay.

---

## Client Impact

Server-only. The teleport UI is server-rendered (sends interface packets). Teleport tablet clicks and spellbook casts go through packet handlers on the server.

---

## Verify

- [ ] All teleport entry points check wilderness level
- [ ] Teleblock prevents all teleport types (UI, tablets, spellbook)
- [ ] Inferno teleport creates activity instance (fix `==` to `.equals()`)
- [ ] No teleport destinations inside locked/inaccessible areas
- [ ] Teleport animation delay enforced
- [ ] Admin wilderness bypass documented as intentional
