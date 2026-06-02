# Item Consistency Audit

**Goal:** Verify that item IDs referenced across all data files actually exist in `item_definitions.json`.

**Docs:** `AGENTS.md` §Adding Items, `docs/workflows/items.md`, `00-cross-cutting/client-server-boundary.md`

---

## What to check

Cross-reference item IDs from these files against `item_definitions.json`:

| Source file | Item ID field |
|-------------|--------------|
| `stores.json` (75KB) | `items[].id` in each shop |
| `equipment_definitions.json` (814KB) | `id` for each equipment entry |
| `npc_drops.json` (1.9MB) | `drops[].item` for every NPC drop |
| `StarterKit.java` | Hardcoded item IDs |

---

## Steps

Write a script that:
1. Loads `game-server/data/def/item/item_definitions.json` into a Set of valid IDs
2. For each source file above, extracts all item IDs
3. Reports any ID NOT in the valid set

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

python3 -c "
import json

# Load valid item IDs
with open('game-server/data/def/item/item_definitions.json') as f:
    items = json.load(f)
valid_ids = {item['id'] for item in items}
print(f'Valid item IDs: {len(valid_ids)}')

# Check stores
with open('game-server/data/def/store/stores.json') as f:
    stores = json.load(f)
for store in stores:
    for si in store.get('items', []):
        if si['id'] not in valid_ids:
            print(f'MISSING ITEM: store \"{store[\"name\"]}\" references item {si[\"id\"]}')

# Check equipment
with open('game-server/data/def/equipment/equipment_definitions.json') as f:
    equip = json.load(f)
for e in equip:
    if e['id'] not in valid_ids:
        print(f'MISSING ITEM: equipment references item {e[\"id\"]}')

# Check NPC drops
with open('game-server/data/def/npc/npc_drops.json') as f:
    drops = json.load(f)
for drop_entry in drops:
    for d in drop_entry.get('drops', []):
        if d['item'] not in valid_ids:
            print(f'MISSING ITEM: NPC {drop_entry[\"npc\"]} drops item {d[\"item\"]}')

print('Done.')
"
```

---

## Known edge cases

- Some items may be OSRS items that exist in the OSRS cache but not in the server's `item_definitions.json`. These will show as missing but may still work if the server loads from cache.
- Noted items have separate IDs. If an item ID is missing, check if it's a noted variant.

---

## Client Impact

If an item ID exists in definitions but client can't render it (ID > ~7K or missing model), the item works server-side but appears invisible in-game. See [client-server-boundary.md](../00-cross-cutting/client-server-boundary.md#boundary-1-item-ids).

---

## Verify

- [ ] Zero missing item IDs in stores
- [ ] Zero missing item IDs in equipment definitions
- [ ] Zero missing item IDs in NPC drops (or documented exceptions)
