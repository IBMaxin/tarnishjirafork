# Add Shop

**Goal:** Add a new shop with items, currency, and NPC wiring.

**Docs:** `AGENTS.md` §Shops, `docs/workflows/shops.md`, `03-security/economy-risks.md`

---

## Files to edit

| File | Purpose |
|------|---------|
| `game-server/data/def/store/stores.json` | Shop definition |
| `game-server/plugins/plugin/click/npc/` (or `object/`) | NPC/object to open shop |

---

## Step 1: Shop definition

Edit `game-server/data/def/store/stores.json`:

```json
{
  "name": "Custom Weapon Shop",
  "currency": "COINS",
  "items": [
    {"id": 4151, "count": 10, "price": 500000},
    {"id": 11732, "count": 5, "price": 750000},
    {"id": 1187, "count": 20, "price": 80000}
  ]
}
```

Fields:
- `name` — display name (used to open shop: `player.store.open("name")`)
- `currency` — COINS, DONATOR_POINTS, PEST_POINTS, BLOOD_MONEY, SLAYER_POINTS, CLAN_POINTS, LMS_POINTS, MAGE_ARENA_POINTS
- `items[]` — `id`, `count` (stock), `price`

---

## Step 2: Wire to an NPC

Add to an existing NPC click handler or create a new one:

```java
// In game-server/plugins/plugin/click/npc/NpcFirstClickPlugin.java
case 1234:  // Shopkeeper NPC ID
    player.store.open("Custom Weapon Shop");
    return true;
```

Or create a standalone plugin if the NPC interaction is complex.

---

## Step 3: Wire to an object (alternative)

```java
// In game-server/plugins/plugin/click/object/ObjectFirstClickPlugin.java
case 2213:  // Object ID
    player.store.open("Custom Weapon Shop");
    return true;
```

---

## Step 4: Custom currency (if needed)

If using a new currency type:

1. Create `game-server/src/main/java/com/osroyale/content/store/currency/impl/CustomCurrency.java`:

```java
package com.osroyale.content.store.currency.impl;

import com.osroyale.content.store.currency.Currency;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.items.Item;

public class CustomCurrency implements Currency {
    @Override
    public boolean canRecieveCurrency(Player player) { return true; }

    @Override
    public boolean takeCurrency(Player player, int amount) {
        // Remove from player's custom point system
        return player.customPoints >= amount;
    }

    @Override
    public void recieveCurrency(Player player, int amount) {
        // Add to player's custom point system
        player.customPoints += amount;
    }

    @Override
    public int getCurrencyAmount(Player player) {
        return player.customPoints;
    }

    @Override
    public boolean isCurrency(Item item) { return false; }
}
```

2. Register in `CurrencyType.java` enum

---

## Step 5: Recompile and test

```bash
./gradlew :game-server:classes
```

Test in-game by interacting with the wired NPC/object.

---

## Client Impact

Shops are server-side. The client receives interface packets to display the shop. No client impact for shop changes.

---

## Verify

- [ ] Shop opens from NPC/object click
- [ ] Items display with correct prices
- [ ] Currency is deducted correctly on purchase
- [ ] Stock counts work (items decrement, restock over time)
- [ ] Buy/sell prices are correct (no arbitrage)
- [ ] Shop persists after server restart
