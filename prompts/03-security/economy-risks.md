# Economy Risk Audit

**Goal:** Identify commands, configs, and exploits that could destroy the in-game economy.

**Docs:** `AGENTS.md`, `03-security/command-audit.md`, `02-data-audit/item-consistency.md`, `docs/workflows/shops.md`

---

## Risk categories

### 1. Unlimited item generation

| Source | Risk | Mitigation |
|--------|------|-----------|
| `::giveitem` / `::gi` (Owner) | Spawn any item, any amount | Restricted to Owner rank — but no amount cap? |
| `::spawnitem` (Admin) | Spawn items for self | Admin-only, but can flood economy if Admin account compromised |
| `::bloodmoneychest` (Owner) | Unknown spawn mechanic | Read the command body — what does it spawn? |
| `StarterKit.java` | Free items on account creation | Multi-account farming potential |
| Shops | Infinite stock by default | Does `stores.json` have stock limits? |
| NPC drops | RARE items with high drop rates | Check drop rates in `npc_drops.json` |

### 2. Currency inflation

| Source | Risk |
|--------|------|
| `::giveexp` (Owner) | Fast 99s → sell accounts |
| `::doubleexp` (Owner) | Global XP multiplier |
| `settings.toml` skill modifiers (3000.0) | 30x XP rates by default |
| Blood money system | Alternative currency — does it have sinks? |
| Donator point system | Pay-to-win currency — proper sinks? |

### 3. Duplication vectors

Check server code for:

```bash
# Search for duplication-prone patterns
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Trade dupes — does item transfer use atomic removal+addition?
grep -rn "remove.*add\|remove.*give\|transfer.*item" game-server/src/ --include="*.java" | grep -i trade

# Drop dupes — can you drop in combat/PVP and pick up on alt?
grep -rn "DropItem\|dropItem\|onDrop" game-server/src/ --include="*.java" | head -10

# Bank dupes — can you withdraw while bank interface is closing?
grep -rn "bank\|Bank" game-server/plugins/plugin/click/button/BankButtonPlugin.java
```

### 4. Shop arbitrage

Check `game-server/data/def/store/stores.json`:
- Are there shops that buy for more than they sell? (arbitrage)
- Can noted items be sold to shops that buy notes?
- Are there shops that buy untradeables for coins?

```bash
python3 -c "
import json
with open('game-server/data/def/store/stores.json') as f:
    stores = json.load(f)
for store in stores:
    name = store.get('name', 'unknown')
    for item in store.get('items', []):
        # Check if any item has buy_price > sell_price
        # Check if price is 0 (free item source)
        if item.get('price', 0) == 0:
            print(f'FREE ITEM: {name} sells item {item[\"id\"]} for 0')
"
```

### 5. Drop rate abuse

From `npc_drops.json`:
```json
{"item": 4151, "chance": "ALWAYS"}
```
ALWAYS drops are guaranteed. Check that high-value items don't have ALWAYS or COMMON drop rates.

---

## Real config values to check

From `game-server/settings.toml`:
```toml
combat_modifier = 1250.0    # 12.5x combat XP
agility_modifier = 3000.0   # 30x agility XP (all skills are 30x)
max_bots = 10               # 10 bots running = resource generation
```

---

## Client Impact

Economy is entirely server-side. Client can't generate items — but a modified client could automate actions (botting). Server anti-cheat/rate-limiting is the defense.

→ See [packet-injection.md](packet-injection.md) for rate limiting.

---

## Verify

- [ ] `::giveitem` and `::spawnitem` are logged (audit trail)
- [ ] No shop has buy-above-sell arbitrage
- [ ] No high-value item has ALWAYS drop rate
- [ ] Trade/drop/bank handlers are dupe-resistant
- [ ] Blood money and donator points have proper sinks
