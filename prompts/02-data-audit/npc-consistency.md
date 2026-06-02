# NPC Consistency Audit

**Goal:** Verify NPC IDs in spawns and drops reference real NPC definitions.

**Docs:** `AGENTS.md` §Adding NPC Spawns/Drops, `docs/workflows/npcs.md`, `00-cross-cutting/client-server-boundary.md`

---

## Source files

| File | Contains |
|------|----------|
| `game-server/data/def/npc/npc_definitions.json` (2.3MB) | All valid NPCs |
| `game-server/data/def/npc/npc_spawns.json` (515KB) | NPC spawn locations |
| `game-server/data/def/npc/npc_drops.json` (1.9MB) | NPC drop tables |

---

## Steps

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

python3 -c "
import json

# Load valid NPC IDs
with open('game-server/data/def/npc/npc_definitions.json') as f:
    npcs = json.load(f)
valid_ids = {npc['id'] for npc in npcs}
print(f'Valid NPC IDs: {len(valid_ids)}')

# Check spawns
with open('game-server/data/def/npc/npc_spawns.json') as f:
    spawns = json.load(f)
for s in spawns:
    if s['id'] not in valid_ids:
        pos = s.get('position', {})
        print(f'MISSING NPC: spawn at x={pos.get(\"x\")},y={pos.get(\"y\")} references NPC {s[\"id\"]}')

# Check drops
with open('game-server/data/def/npc/npc_drops.json') as f:
    drops = json.load(f)
for d in drops:
    for npc_id in d.get('npc', []):
        if npc_id not in valid_ids:
            print(f'MISSING NPC: drop table references NPC {npc_id}')
    # Also verify drop-type is valid
    drop_type = d.get('drop-type', '')
    valid_types = {'NORMAL', 'PET', 'CLUE', 'RDT', 'TERRITORY'}
    if drop_type not in valid_types:
        print(f'INVALID DROP TYPE: NPC {d.get(\"npc\")} has drop-type \"{drop_type}\"')

# Check spawn positions are in bounds
for s in spawns:
    pos = s.get('position', {})
    x, y = pos.get('x', 0), pos.get('y', 0)
    if not (0 <= x <= 16000 and 0 <= y <= 16000):
        print(f'BOUNDS: NPC {s[\"id\"]} spawn at ({x},{y}) out of bounds')

print('Done.')
"
```

---

## Additional checks

- **Duplicate spawns:** Two NPCs at the exact same (x, y, height) — likely copy-paste error
- **Walk radius:** Negative values or > 50 indicate config error
- **Face direction:** Must be one of NORTH, EAST, SOUTH, WEST
- **Drop rates:** Check that `ALWAYS`, `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE` are used correctly — an ALWAYS drop with 0 items is a bug

---

## Client Impact

NPC models: same boundary as items. If an NPC ID references a model the 317 client doesn't have, the NPC will be invisible or appear as a null. Check `00-cross-cutting/client-server-boundary.md`.

---

## Verify

- [ ] Zero missing NPC IDs in spawns
- [ ] Zero missing NPC IDs in drop tables
- [ ] All spawn positions in valid map bounds
- [ ] All drop types are valid enum values
