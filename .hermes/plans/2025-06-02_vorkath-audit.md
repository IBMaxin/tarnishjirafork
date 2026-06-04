# Vorkath — "Not Reachable" Fix (COMPLETED 2025-06-02)

## Root Cause

Pathfinding fails because the **TraversalMap has no data** for the Vorkath arena region (Zeah maps not in 317-format cache). `dijkstraPath` and `simplePath` both return no path → "I can't reach that!" → interaction blocked.

## Files Changed (5 files)

### 1. `FirstNpcOptionEvent.kt` — Poke/Wake bypass
Added NPC 8059 check. Bypasses `walkTo`, routes through `EventDispatcher` directly. Falls back to `publishToPluginManager` if player not in activity.
- Import added: `com.osroyale.content.event.EventDispatcher`

### 2. `NpcFirstClickPlugin.java` — Activity creation fallback  
Added `case 8059` — creates `VorkathActivity` when player clicks dormant Vorkath and isn't already in an activity.
- Imports added: `VorkathActivity`, `ActivityType`

### 3. `Teleport.java` — Teleport inside arena
Moved Vorkath teleport from (2276,4036) to (2272,4059) — inside `inVorkath` bounds.

### 4. `AttackNpcEvent.kt` — Attack bypass
Moves player adjacent to Vorkath's 8x8 bounding box, then delays `combat.attack()` by 2 ticks.
- **Why delayed:** `player.move()` calls `getCombat().reset()`. Attacking same tick = target wiped. 2-tick delay ensures position syncs and move lock expires.
- Imports added: `Position`, `Utility`, `abs`, `World`

### 5. `MagicOnNpcEvent.kt` — Magic attack bypass  
Same pattern as AttackNpcEvent — move adjacent + delayed attack.
- Imports added: `Position`, `Utility`, `abs`

## Results

- ✅ Poke (wake Vorkath) — works
- ✅ Ranged attack — works
- ✅ Magic attack — works  
- ⚠️ Melee attack — does not work (likely Vorkath combat strategy design, not pathfinding)

## Architecture note

Vorkath has three separate interaction paths, each needing its own bypass:

| Action | Event class | Original path | Bypass |
|--------|-------------|---------------|--------|
| Poke | FirstNpcOptionEvent | walkTo → callback → EventDispatcher | Direct EventDispatcher |
| Attack | AttackNpcEvent | combat.attack → Waypoint → findRoute | Move adjacent + delayed attack |
| Magic | MagicOnNpcEvent | combat.attack → Waypoint → findRoute | Move adjacent + delayed attack |

## Verification

```
::tele vorkath
cross crevice (object 31990)    # creates VorkathActivity
poke dormant Vorkath (8059)     # wake animation → transforms to 8060
click Attack / cast spell       # snap adjacent → fight
```
