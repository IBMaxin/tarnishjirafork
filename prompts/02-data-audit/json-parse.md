# JSON Parse Audit

**Goal:** Parse every JSON file under `game-server/data/` and report any that are malformed, empty, or unparseable.

**Docs:** `AGENTS.md` §Data, `code_index.json`, `00-cross-cutting/client-server-boundary.md`

---

## Target Files

All `.json` files under:
- `game-server/data/def/` — item, NPC, equipment, stores, spawns, drops
- `game-server/data/content/` — skills, clan, game configs
- `game-server/data/profile/` — player saves, world profile list
- `game-server/data/io/` — message sizes
- `game-server/data/wiki/` — wiki item dump

Skip: `game-server/data/cache/` (binary files), `items-json/` and `monsters-json/` (30K+ individual files — audit separately if needed)

---

## Steps

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Find all top-level JSON files (not in items-json/monsters-json subdirs)
find game-server/data -name "*.json" \
  ! -path "*/items-json/*" \
  ! -path "*/monsters-json/*" \
  ! -path "*/cache/*" \
  -exec python3 -c "
import json, sys
try:
    with open(sys.argv[1]) as f:
        json.load(f)
    print(f'OK: {sys.argv[1]}')
except Exception as e:
    print(f'FAIL: {sys.argv[1]} — {e}')
" {} \;
```

---

## Expected results

| File | Expected | Concern if |
|------|----------|-----------|
| `item_definitions.json` (2.8MB) | OK | Fails → no items in-game |
| `npc_definitions.json` (2.3MB) | OK | Fails → no NPCs |
| `npc_drops.json` (1.9MB) | OK | Fails → no drops |
| `equipment_definitions.json` (814KB) | OK | Fails → no equipment bonuses |
| `npc_spawns.json` (515KB) | OK | Fails → empty world |
| `stores.json` (75KB) | OK | Fails → no shops |
| `projectile_definitions.json` | OK | Fails → no projectiles in combat |
| `global_objects.json` | OK | Fails → missing world objects |
| `world_profile_list.json` | OK | Empty is ok (seeded at first boot) |
| `Zezima.json`, `Oak.json` | OK | Missing → no seeded admins |

---

## What to do on failure

1. Note the file and error
2. Open the file — is it actually JSON? Some files may be XML or text misnamed
3. Check for trailing commas (common JSON error)
4. Check for control characters or encoding issues
5. Fix or report

---

## Client Impact

None. All data files are server-only.

---

## Verify

- [ ] All `.json` files parse without error
- [ ] No empty files (except `world_profile_list.json` which is expected)
- [ ] Large files (2MB+) parse within a few seconds
