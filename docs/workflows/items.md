# Items

## Adding a new item

Items require definition in **two places**:

### 1. Item definition — `game-server/data/def/item/item_definitions.json`

```json
{
  "id": 9999,
  "name": "Custom sword",
  "examine": "A finely crafted blade.",
  "value": 100000,
  "stackable": false,
  "noted": false,
  "tradeable": true,
  "members": true,
  "noteId": 10000
}
```

Fields: `id`, `name`, `examine`, `value`, `stackable`, `noted`, `tradeable`, `members`, `noteId`, `noteTemplateId`, `team`, `lendId`, `lendTemplateId`

### 2. Equipment stats — `game-server/data/def/equipment/equipment_definitions.json`

Required if the item is wearable:

```json
{
  "id": 9999,
  "slot": 3,
  "requirements": [
    {"skill": 1, "level": 75}
  ],
  "bonuses": [0, 50, 30, -5, 0, 0, 0, 0, 0, 0, 75, 0, 0, 0]
}
```

Fields: `id`, `slot` (0=hat, 1=body, 2=legs, 3=weapon, 4=shield, 5=cape, 9=hands, 10=feet, 12=ring, 13=ammo), `requirements`, `bonuses` (14-element array: stab atk, slash atk, crush atk, magic atk, range atk, stab def, slash def, crush def, magic def, range def, strength, prayer, ranged str, magic dmg)

### 3. Cache model — `game-server/data/def/items-json/<id>.json`

Individual item model/cache data. OSRS items need this for correct rendering. For custom items, either inject cache data or accept placeholder visuals.

## Item class reference

`com.osroyale.game.world.items.Item`

```java
Item item = new Item(itemId, amount);
player.inventory.add(item);         // to inventory
player.bank.deposit(item);          // to bank
player.equipment.equip(item);       // equip
```

## Steps

1. Add entry to `item_definitions.json`
2. If wearable, add entry to `equipment_definitions.json`
3. Add `items-json/<id>.json` for cache data (use OSRS dump or placeholder)
4. Recompile
5. Verify in-game: `::spawnitem <id>`
