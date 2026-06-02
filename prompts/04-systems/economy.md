# Economy System

**Goal:** Document the full economy — currencies, shops, trading, drops, sinks.

**Docs:** `AGENTS.md`, `docs/workflows/shops.md`, `docs/workflows/items.md`, `docs/workflows/npcs.md`, `03-security/economy-risks.md`

---

## Currency types

From `game-server/src/main/java/com/osroyale/content/store/currency/CurrencyType.java` and `impl/`:

| Currency | Source | Sink |
|----------|--------|------|
| Coins (995) | NPC drops, shops, alching | Shops, construction |
| Donator points | Donations (real $) | Donator shop |
| Pest points | Pest Control minigame | Pest Control shop |
| Blood money | PVP kills | Blood money shop |
| Slayer points | Slayer tasks | Slayer shop |
| Clan points | Clan activities | Clan shop |
| LMS points | Last Man Standing | LMS shop |
| Mage Arena points | Mage Arena | Mage Arena shop |

```bash
ls game-server/src/main/java/com/osroyale/content/store/currency/impl/
```

---

## Shop system

```
game-server/data/def/store/stores.json    # Shop definitions
game-server/src/main/java/com/osroyale/content/store/  # Shop logic
└── currency/                              # Currency implementations
    └── impl/
```

Key shop classes:
```bash
ls game-server/src/main/java/com/osroyale/content/store/impl/
```

---

## Trading post

```
game-server/src/main/java/com/osroyale/content/tradingpost/
game-server/plugins/plugin/click/button/TradingPostButtonPlugin.java
```

---

## Drop system

```
game-server/data/def/npc/npc_drops.json    # All NPC drop tables
```

Drop types: `NORMAL`, `PET`, `CLUE`, `RDT` (rare drop table), `TERRITORY`

Chance values: `ALWAYS`, `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`

---

## Item sinks

Check what removes items from the economy:
- **Consumables:** Food (`consume/FoodData.java`), potions (`consume/PotionData.java`)
- **Alching:** High/Low alchemy (check magic skill)
- **Construction:** Materials consumed
- **Prayer:** Bones buried/offered
- **Degradable items:** Tentacle whip, barrows equipment

---

## Steps

1. List all shops with their currency type
```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork
python3 -c "
import json
with open('game-server/data/def/store/stores.json') as f:
    stores = json.load(f)
for s in stores:
    currency = s.get('currency', 'COINS')
    items = len(s.get('items', []))
    print(f'{s[\"name\"]}: {items} items, currency={currency}')
" | head -30
```

2. Check for economy balance issues:
   - Free items (price=0) in shops
   - Infinite stock shops
   - NPC drops that generate more GP than consumed by supplies

3. Check trading post for dupe vectors

---

## Client Impact

Economy is server-side. Shops, trading, drops all calculated server-side. No client impact.

---

## Verify

- [ ] All currencies have both sources and sinks
- [ ] No shop has buy price > sell price (arbitrage)
- [ ] No high-value items have ALWAYS drop rate
- [ ] Consumables actually consume items (check FoodData, PotionData)
