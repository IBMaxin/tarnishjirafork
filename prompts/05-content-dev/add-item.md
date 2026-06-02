# Add Item

**Goal:** Add a new item to the game — definitions, equipment, and cache model.

**Docs:** `AGENTS.md` §Items, `docs/workflows/items.md`, `00-cross-cutting/client-server-boundary.md` §Boundary 1, `02-data-audit/item-consistency.md`

---

## Files to edit

| File | Purpose | Required? |
|------|---------|-----------|
| `game-server/data/def/item/item_definitions.json` | Item name, value, tradeable, stackable | **Yes** |
| `game-server/data/def/equipment/equipment_definitions.json` | Equipment bonuses and requirements | Only if wearable |
| `game-server/data/def/items-json/<id>.json` | Cache model data | For rendering |

---

## Step 1: Add item definition

Edit `game-server/data/def/item/item_definitions.json`:

```json
{
  "id": 25000,
  "name": "Custom sword",
  "examine": "A finely crafted blade.",
  "value": 100000,
  "stackable": false,
  "noted": false,
  "tradeable": true,
  "members": true,
  "noteId": 25001
}
```

**ID rules:**
- Must be unique
- Must be ≤ `item_definition_limit` (28,473 in settings.toml)
- If > ~7,000: client may not render (OSRS cache range). Test with `::spawnitem` in-game.

---

## Step 2: Add equipment bonuses (wearable items only)

Edit `game-server/data/def/equipment/equipment_definitions.json`:

```json
{
  "id": 25000,
  "slot": 3,
  "requirements": [
    {"skill": 1, "level": 75}
  ],
  "bonuses": [0, 50, 30, -5, 0, 0, 0, 0, 0, 0, 75, 0, 0, 0]
}
```

Equipment slots: 0=hat, 1=body, 2=legs, 3=weapon, 4=shield, 5=cape, 9=hands, 10=feet, 12=ring, 13=ammo

Bonuses array (14 elements):
```
[stabAtk, slashAtk, crushAtk, magicAtk, rangeAtk,
 stabDef, slashDef, crushDef, magicDef, rangeDef,
 strength, prayer, rangedStr, magicDmg]
```

---

## Step 3: Cache model (for rendering)

If item ID is high (> 7K), the 317 client won't have the model. Options:
1. Add `game-server/data/def/items-json/<id>.json` with model data
2. Inject the model into client cache via cache editor
3. Accept placeholder (invisible item that works server-side)

---

## Step 4: Recompile and test

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork
./gradlew :game-server:classes
```

Test in-game (once server is running and you're logged in as admin):
```
::spawnitem 25000
::spawnitem 25000 10    (with amount)
```

---

## Client Impact

→ See [client-server-boundary.md](../00-cross-cutting/client-server-boundary.md#boundary-1-item-ids)

- Item definition: server-only
- Equipment bonuses: server-only
- Rendering: if ID > ~7K, client may not render. Must test in-game.
- Equipment slot rendering: client must support the slot. 317 client supports standard slots.

---

## Verify

- [ ] Item spawns correctly with `::spawnitem <id>`
- [ ] Item has correct name, examine text, value
- [ ] If wearable: equips to correct slot, shows correct bonuses
- [ ] If stackable: stacks correctly in inventory
- [ ] If tradeable: appears correctly in trades
- [ ] Item renders in-game (check visibility)
