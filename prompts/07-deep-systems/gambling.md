# Gambling System Audit

**Goal:** Audit the gambling system for item duplication, state machine safety, and fairness.

**Docs:** `AGENTS.md`, `code_index.json`, `prompts/03-security/economy-risks.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/gambling/GambleManager.java` | Core gambling logic — 629 lines |
| `game-server/src/main/java/com/osroyale/content/gambling/GambleStage.java` | State machine enum: NONE, SENDING_OFFER, PLACING_BET, IN_PROGRESS |
| `game-server/src/main/java/com/osroyale/content/gambling/GambleType.java` | Game types: NONE, FIFTY_FIVE, FLOWER_POKER |
| `game-server/src/main/java/com/osroyale/content/gambling/Gamble.java` | Abstract gamble base |
| `game-server/src/main/java/com/osroyale/content/gambling/impl/FiftyFive.java` | 55x2 dice game |
| `game-server/src/main/java/com/osroyale/content/gambling/impl/FlowerPoker.java` | Flower poker game |

---

## Architecture

```
GambleManager (per-player)
├── stage: GambleStage (NONE → SENDING_OFFER → PLACING_BET → IN_PROGRESS)
├── other: Player (opponent)
├── confirmed: boolean
├── type: GambleType (NONE, FIFTY_FIVE, FLOWER_POKER)
├── game: Gamble (FiftyFive or FlowerPoker instance)
├── container: ItemContainer (18 slots, STANDARD stack policy)
├── gameFlowers: ArrayList<CustomGameObject>
└── flowers: ArrayList<Flowers>

State machine:
  NONE → sendRequest() → SENDING_OFFER → acceptRequest() → PLACING_BET
  PLACING_BET → accept() → IN_PROGRESS → game.gamble() → finish() → NONE
```

---

## Steps

### 1. Audit state machine transitions

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Check all stage transitions
grep -n "setStage\|getStage" \
  game-server/src/main/java/com/osroyale/content/gambling/GambleManager.java
```

Key transitions to verify:
- `sendRequest()` checks `Objects.isNull(requested)` to distinguish new request from accept
- `acceptRequest()` checks `canGamble(player, GambleStage.NONE) && canGamble(other, GambleStage.SENDING_OFFER)`
- `accept()` checks both confirmed
- `finish()` checks `getGame() != null && getGame().getHost() != null`

### 2. Check item transfer safety

```bash
# deposit() — lines 339-375
# Items move from player.inventory → gamble container
# Uses inventory.remove(item, slot) then container.add(item)
# Are these atomic? What if remove succeeds but add fails?

# withdraw() — lines 382-413
# Items move from gamble container → player.inventory
# Uses container.remove(itemId, amount) then inventory.add(itemId, amount)

# give() — lines 503-553
# Winner gets both containers' items
# Uses inventory.add(item) then inventory.addOrDrop(item) as fallback
```

### 3. Check for negative-amount dupe

```bash
# deposit() line 356: if(amount <= 0) return;
# withdraw() line 397: if(amount <= 0) return;
```

Both deposit and withdraw check `amount <= 0` — **positive finding**. But the `amount` comes from the packet handler. Check if the packet handler also validates before calling deposit/withdraw.

### 4. Check gambling zone boundary

```bash
# Line 40: GAMBLING_ZONE = new Boundary(3148, 3476, 3181, 3505)
# canGamble() line 137: checks Boundary.isIn(player, GAMBLING_ZONE)

# What happens if a player leaves the zone mid-gamble?
grep -rn "GAMBLING_ZONE\|gambling\|canGamble" \
  game-server/src/main/java/com/osroyale/ --include="*.java" | grep -v "GambleManager"
```

### 5. Check game outcome fairness

```bash
# FiftyFive.java — how is the roll determined?
cat game-server/src/main/java/com/osroyale/content/gambling/impl/FiftyFive.java

# Does it use Utility.random() or a seeded RNG?
# Is the host favored?
```

### 6. Check disconnect handling

```bash
# What happens when a player disconnects during a gamble?
grep -rn "gambling\|Gambling\|gambleLock" \
  game-server/src/main/java/com/osroyale/game/world/entity/mob/player/Player.java \
  | head -10

# Does logout cleanup return items?
grep -rn "gambling" game-server/src/main/java/com/osroyale/net/session/GameSession.java
grep -rn "gambling" game-server/src/main/java/com/osroyale/game/world/World.java
```

---

## Findings

### F1: decline() resets both players' state [MEDIUM]

`decline()` (line 293-332) returns items from BOTH containers to their respective owners and calls `reset()` on both. This is safe — items go back to inventory or bank if full.

### F2: `accept()` resets opponent's confirmed state on game type change [LOW]

`handleModeSelection()` (line 582-596) sets `confirmed = false` for both players when game type changes. This prevents the "change game type after confirm" trick.

### F3: 5-second modification window [LOW]

Line 235: `if (System.currentTimeMillis() - player.getLastModification() < 5_000)` — prevents accepting within 5 seconds of any change. Good anti-scam measure.

### F4: No disconnect cleanup visible in GambleManager [HIGH]

If a player disconnects or crashes during `IN_PROGRESS` stage, their items may be lost. Check `GameSession` and `World` cleanup code for gambling state reset on logout.

### F5: `give()` does not verify game outcome [MEDIUM]

The `give()` method takes winner/loser as parameters from `finish()`. If `finish()` logic has a bug, the wrong player could receive items. Verify `finish()` (line 468-493) score comparison logic.

### F6: Ironman check prevents gambling [POSITIVE]

Lines 151-158: Ironmen cannot send or receive gamble requests. Correct.

---

## Client Impact

Server-only. The gambling interface is server-rendered. All item movement and game logic runs on the server.

---

## Verify

- [ ] Items returned on disconnect during gamble (check GameSession cleanup)
- [ ] `deposit()` and `withdraw()` are atomic (no item loss on partial failure)
- [ ] Game outcome determined server-side (not influenced by client)
- [ ] `finish()` score comparison correct for all game types
- [ ] Zone boundary check works — players outside zone cannot interact with gamble
- [ ] No way to exit gambling zone with items still in container
