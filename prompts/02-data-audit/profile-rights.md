# Profile Rights Audit

**Goal:** Verify all seeded player accounts have valid, consistent rank assignments.

**Docs:** `AGENTS.md` §Profile and Rights, `docs/game-scope.md`, workspace test `ProfileRightsTest.java`

---

## Current seeded accounts

From `game-server/data/profile/save/`:
- `Zezima.json` — expected OWNER
- `Oak.json` — expected ADMINISTRATOR

Profile files contain:
```json
{
  "username": "Zezima",
  "player-rights": "OWNER",
  "position": {"x": 3087, "y": 3500, "height": 0},
  ...
}
```

World profile list at `game-server/data/profile/world_profile_list.json`:
```json
{
  "Zezima": {"rank": "OWNER"},
  "Oak": {"rank": "ADMINISTRATOR"}
}
```

---

## Steps

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

python3 -c "
import json, os

# Valid ranks from PlayerRight.java
VALID_RANKS = {
    'PLAYER', 'MODERATOR', 'ADMINISTRATOR', 'OWNER', 'DEVELOPER',
    'DONATOR', 'SUPER_DONATOR', 'EXTREME_DONATOR', 'ELITE_DONATOR', 'KING_DONATOR',
    'HELPER'
}

save_dir = 'game-server/data/profile/save'
if os.path.isdir(save_dir):
    for fname in os.listdir(save_dir):
        if not fname.endswith('.json'):
            continue
        fpath = os.path.join(save_dir, fname)
        with open(fpath) as f:
            data = json.load(f)
        username = data.get('username', 'UNKNOWN')
        rights = data.get('player-rights', 'UNSET')
        if rights not in VALID_RANKS:
            print(f'INVALID RANK: {username} has \"{rights}\"')
        else:
            print(f'OK: {username} = {rights}')

# Check world_profile_list matches
wpl_path = 'game-server/data/profile/world_profile_list.json'
if os.path.exists(wpl_path) and os.path.getsize(wpl_path) > 2:
    with open(wpl_path) as f:
        wpl = json.load(f)
    for username, entry in wpl.items():
        rank = entry.get('rank', 'UNSET')
        # Check if save file exists
        save_path = f'game-server/data/profile/save/{username}.json'
        if not os.path.exists(save_path):
            print(f'MISMATCH: {username} in world_profile_list but no save file')
        else:
            with open(save_path) as f:
                save_data = json.load(f)
            save_rank = save_data.get('player-rights', '')
            if save_rank != rank:
                print(f'MISMATCH: {username} save={save_rank} wpl={rank}')
"
```

---

## Rank hierarchy (from `PlayerRight.java`)

```
DEVELOPER (highest)
  └─ OWNER
       └─ ADMINISTRATOR
            └─ MODERATOR
                 └─ HELPER
                      └─ KING_DONATOR → ELITE_DONATOR → EXTREME_DONATOR → SUPER_DONATOR → DONATOR
                           └─ PLAYER
```

Permission checks are chained: `isDeveloper()` implies `isOwner()` implies `isAdministrator()` etc.

---

## Security check

- Can a `setrank` command elevate a player above the caller's rank?
- Are there any saved player profiles with DEVELOPER rank? (Answer: only if explicitly seeded — there's no auto-promotion)
- Does `world_profile_list.json` match `save/*.json` for all accounts?

---

## Client Impact

None. Profiles are server-side persistence.

---

## Verify

- [ ] All save files have valid `player-rights` values
- [ ] `world_profile_list.json` is consistent with save files
- [ ] No account has DEVELOPER rank unless intentionally seeded
- [ ] Run existing test: `./gradlew :game-server:test`
