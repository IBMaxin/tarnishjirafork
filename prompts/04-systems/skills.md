# Skills System

**Goal:** Audit each skill's implementation depth, find gaps and bugs.

**Docs:** `AGENTS.md` §Skills, `docs/workflows/skills.md`, `docs/workflows/plugins.md`

---

## Skill locations

```
game-server/src/main/java/com/osroyale/content/skill/
├── SkillAction.java          # Base class for all skill actions
├── SkillRepository.java      # Skill registry
├── guides/                   # In-game skill guides
└── impl/                     # One directory per skill
    ├── agility/
    ├── construction/
    ├── cooking/
    ├── crafting/
    ├── farming/
    ├── firemaking/
    ├── fishing/
    ├── fletching/
    ├── herblore/
    ├── hunter/
    ├── magic/
    ├── mining/
    ├── prayer/
    ├── runecrafting/
    ├── slayer/
    ├── smithing/
    ├── thieving/
    └── woodcutting/
```

---

## Steps for each skill

1. Open `impl/<skill>/` directory — count files, get file sizes
2. Check if the skill has a data file in `game-server/data/content/skills/`
3. Check if the skill has wiring to objects/NPCs via plugins
4. Check modifier in `settings.toml`
5. Check if the skill has proper XP calculation (extends `SkillAction`)

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Quick audit of each skill implementation
for skill_dir in game-server/src/main/java/com/osroyale/content/skill/impl/*/; do
    name=$(basename "$skill_dir")
    files=$(find "$skill_dir" -name "*.java" | wc -l)
    size=$(du -sh "$skill_dir" 2>/dev/null | cut -f1)
    modifier=$(grep "${name}_modifier" game-server/settings.toml 2>/dev/null || echo "no_modifier")
    echo "$name: $files files, $size, modifier=$modifier"
done
```

---

## Skill action pattern (from `SkillAction.java`)

```java
public abstract class SkillAction {
    public abstract void onSuccess();
    public abstract int experience();
    public abstract int skill();          // Skill ID (0-22)
    public abstract boolean canRun();     // Requirements check
    public abstract int requiredLevel();
    public abstract double successFactor();
}
```

---

## Skill IDs

| ID | Skill | ID | Skill |
|----|-------|----|-------|
| 0 | Attack | 12 | Crafting |
| 1 | Defence | 13 | Smithing |
| 2 | Strength | 14 | Mining |
| 3 | Hitpoints | 15 | Herblore |
| 4 | Ranged | 16 | Agility |
| 5 | Prayer | 17 | Thieving |
| 6 | Magic | 18 | Slayer |
| 7 | Cooking | 19 | Farming |
| 8 | Woodcutting | 20 | Runecrafting |
| 9 | Fletching | 21 | Construction |
| 10 | Fishing | 22 | Hunter |
| 11 | Firemaking | | |

---

## Client Impact

Skills are server-side calculations. Client displays XP and levels. No client impact for skill changes, but new skill animations or interfaces would need client work.

---

## Verify

- [ ] All 18 skills have implementation directories
- [ ] Each skill extends `SkillAction` or equivalent
- [ ] Each skill has wiring to an object/NPC click plugin
- [ ] Skill modifiers match intended XP rates
- [ ] Data files exist for skills that need them (agility courses, etc.)
