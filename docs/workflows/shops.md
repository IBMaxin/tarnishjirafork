# Shops

Shops are JSON-defined. No Java code needed to add a shop.

## Shop definitions — `game-server/data/def/store/stores.json`

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

Fields per shop:
- `name` — display name
- `currency` — `COINS`, `DONATOR_POINTS`, `PEST_POINTS`, `BLOOD_MONEY`, `SLAYER_POINTS`, `CLAN_POINTS`, `LMS_POINTS`, `MAGE_ARENA_POINTS`
- `items` — array of `{id, count, price}`

## Currency types

| Currency | Enum |
|----------|------|
| Coins | `COINS` |
| Donator points | `DONATOR_POINTS` |
| Pest control points | `PEST_POINTS` |
| Blood money | `BLOOD_MONEY` |
| Slayer points | `SLAYER_POINTS` |
| Clan points | `CLAN_POINTS` |
| LMS points | `LMS_POINTS` |
| Mage arena points | `MAGE_ARENA_POINTS` |

Defined in: `game-server/src/main/java/com/osroyale/content/store/currency/CurrencyType.java`

## Shop click handler

Shops are typically opened from NPC dialogue or object clicks. Example NPC shop:

```java
// In an NPC click handler
case 1234:  // shopkeeper NPC
    player.store.open("Custom Weapon Shop");
    return true;
```

`player.store.open(name)` opens the shop by its `name` field from stores.json.

## Adding a custom currency

1. Create currency class: `game-server/src/main/java/com/osroyale/content/store/currency/impl/CustomCurrency.java`
2. Add enum value to `CurrencyType.java`
3. Reference the enum in stores.json

## Steps

1. Add shop entry to `stores.json`
2. Wire up an NPC or object to open it (`player.store.open("name")`)
3. Recompile
4. Verify in-game: open the linked NPC/object
