# Activities & Minigames

**Goal:** Audit all minigames — which are complete, which are stubs, which have bugs.

**Docs:** `AGENTS.md` §Activities, `docs/game-scope.md`

---

## Activity locations

```
game-server/src/main/java/com/osroyale/content/activity/
├── impl/
│   ├── barrows/
│   ├── battleground/
│   ├── duelarena/
│   ├── fightcaves/
│   ├── godwars/
│   ├── kraken/
│   ├── magearena/
│   ├── pestcontrol/
│   ├── recipefordisaster/
│   ├── warriorguild/
│   └── zulrah/
├── inferno/          # Separate from impl/ — major content
├── infernomobs/       # Inferno NPCs
├── lobby/             # Activity lobby system
├── panel/             # Activity info panels
├── randomevent/       # Random events (sandwich lady, etc.)
│   └── impl/
└── record/            # Activity records/statistics
```

Base classes: `Activity.java`, `GroupActivity.java`, `ActivityListener.java`, `ActivityType.java`, `ActivityDeathType.java`

---

## Steps

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# For each activity, check file count and size
for activity_dir in game-server/src/main/java/com/osroyale/content/activity/impl/*/; do
    name=$(basename "$activity_dir")
    files=$(find "$activity_dir" -name "*.java" | wc -l)
    size=$(du -sh "$activity_dir" 2>/dev/null | cut -f1)
    echo "$name: $files files, $size"
done

# Inferno separately
echo "inferno: $(find game-server/src/main/java/com/osroyale/content/activity/inferno -name '*.java' | wc -l) files"
```

---

## Activity quality indicators

| Indicator | Meaning |
|-----------|---------|
| <3 files | Likely stub or simple activity |
| Extends `Activity` | Proper activity lifecycle |
| Has boss/combat NPCs | Full PVM content |
| Has reward system | Implemented rewards |
| Has lobby system | Multi-player with matchmaking |
| Has plugin wiring | Players can actually access it |

---

## Activity checklist

For each activity, check:
1. Does it extend `Activity` or `GroupActivity`?
2. Is it wired to a teleport or NPC?
3. Does it have an entrance/exit mechanism?
4. Does it have a death handler? (`ActivityDeathType`)
5. Are rewards defined?
6. Is it referenced in `ActivityType.java`?

```bash
# Find where activities are registered/wired to teleports
grep -rn "Barrows\|FightCave\|Zulrah\|Godwars\|Kraken\|WarriorGuild\|PestControl\|DuelArena\|MageArena\|Battleground\|RecipeForDisaster\|Wintertodt" \
  game-server/plugins/ --include="*.java" | head -20
```

---

## LMS (Last Man Standing) — separate system

LMS has its own content directory: `game-server/src/main/java/com/osroyale/content/lms/`
- Lobby system, fog/safezone mechanics, loadout system
- Check if it references `game-server/plugins/plugin/click/object/LMSGamePlugin.java`

---

## Client Impact

Activities are server-side logic. Client only renders. No client impact unless adding new interfaces or map regions.

---

## Verify

- [ ] All listed activities have source directories with >0 files
- [ ] Activities extend `Activity` or equivalent
- [ ] Activities are wired to teleports/NPCs (reachable in-game)
- [ ] Death handlers exist for combat activities
