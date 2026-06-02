# Vorkath — "Not Reachable" Fix

## Root Cause

Pathfinding fails because the **TraversalMap has no data** for the Vorkath arena region. This is a 317-format cache — OSRS Zeah maps (where Vorkath lives) aren't in the cache. Both `dijkstraPath` and `simplePath` return no path → `"I can't reach that!"` → `walkTo` callback never fires → `VorkathActivity.clickNpc` never runs.

The chain:

```
FirstNpcOptionEvent.handleNpc()
  → player.walkTo(npc)  ← THIS FAILS (no TraversalMap data)
  → callback never fires
  → VorkathActivity.clickNpc never reached
```

---

## Fix: Bypass walkTo for Vorkath NPCs

Same pattern as banker NPC 394 which already short-circuits walkTo in `FirstNpcOptionEvent`.

### File 1: `game-server/src/main/kotlin/org/jire/tarnishps/event/npc/FirstNpcOptionEvent.kt`

**BEFORE (lines 12-17):**
```kotlin
    override fun handleNpc(player: Player, npc: Npc) {
        if (npc.id == 394 && player.position.isWithinDistance(npc.position, 2)) {
            publishToPluginManager(player, npc)
        } else {
            super.handleNpc(player, npc)
        }
    }
```

**AFTER:**
```kotlin
    override fun handleNpc(player: Player, npc: Npc) {
        if (npc.id == 394 && player.position.isWithinDistance(npc.position, 2)) {
            publishToPluginManager(player, npc)
        } else if (npc.id == 8059 && player.position.isWithinDistance(npc.position, 15)) {
            // Vorkath dormant: bypass walkTo — TraversalMap has no Zeah map data
            val interactionEvent = createInteractionEvent(npc)
            if (interactionEvent == null || !EventDispatcher.execute(player, interactionEvent)) {
                publishToPluginManager(player, npc)
            }
        } else {
            super.handleNpc(player, npc)
        }
    }
```

**Why:** `EventDispatcher.execute()` routes directly to `VorkathActivity.clickNpc` (via `player.inActivity()` → activity's `onEvent`) without needing pathfinding. Distance check (15 tiles) covers the whole arena.

**Imports needed:** None — `EventDispatcher` already imported in `NpcOptionEvent.kt` parent class.

---

### File 2: `game-server/plugins/plugin/click/npc/NpcFirstClickPlugin.java`

**Add Vorkath case in the switch (before default, after line 444):**

```java
            case 8059: {
                // Dormant Vorkath - create activity if player entered via teleport
                if (!player.inActivity(ActivityType.VORKATH)) {
                    VorkathActivity.create(player);
                }
                return true;
            }
```

**Imports needed:**
```java
import com.osroyale.content.activity.impl.VorkathActivity;
import com.osroyale.content.activity.ActivityType;
```

**Why:** Fallback for when player teleports directly into arena without going through crevice. Plugin creates the VorkathActivity so the next click works through the activity chain.

---

## File 3: Check — `game-server/src/main/java/com/osroyale/content/teleport/Teleport.java` (optional)

**Line 85 — current teleport position:**
```java
VORKATH("Vorkath", TeleportType.BOSS_KILLING, new Position(2276, 4036, 0), ...)
```

This is outside `inVorkath` bounds (Y=4036 < min Y=4053). Consider changing to inside the arena:
```java
VORKATH("Vorkath", TeleportType.BOSS_KILLING, new Position(2272, 4059, 0), ...)
```

But with the File 2 fix above, the teleport position doesn't matter — clicking NPC 8059 creates the activity regardless.

---

## Verification

```
::tele 2272 4059
# walk near Vorkath if not already close
# click dormant Vorkath (NPC 8059)
# → wake-up animation plays
# → transforms to 8060
# → Vorkath attacks (aggressive=true)
# → fight!
```

### Regression checks:
- Click Vorkath from across arena → works (15 tile distance check)
- `::tele` out during fight → `onRegionChange` cleanup fires
- Logout mid-fight, re-login → `setActivity()` recreates dormant → click to re-engage
- Kill Vorkath → 10-tick respawn → dormant NPC reappears → click to re-engage
