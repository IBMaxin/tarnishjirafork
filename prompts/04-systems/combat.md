# Combat System

**Goal:** Understand and audit the combat system — formulas, equipment, modifiers.

**Docs:** `AGENTS.md`, `00-cross-cutting/client-server-boundary.md`, `settings.toml`

---

## Key files

| File | Purpose |
|------|---------|
| `game-server/src/main/java/com/osroyale/content/combat/` | Combat formulas, hit calculation |
| `game-server/src/main/java/com/osroyale/content/combat/cannon/` | Dwarf multi-cannon |
| `game-server/data/def/combat/projectile_definitions.json` (63KB) | Projectile data |
| `game-server/data/def/equipment/equipment_definitions.json` (814KB) | Equipment bonuses (14-element array) |
| `game-server/settings.toml` | `combat_modifier = 1250.0` (12.5x combat XP) |

---

## Combat formula exploration

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork
find game-server/src/main/java/com/osroyale/content/combat -name "*.java" | head -20
```

Read the main combat classes:
```bash
grep -rn "getMaxHit\|getAccuracy\|getDefence\|calculateDamage\|formula" \
  game-server/src/main/java/com/osroyale/content/combat/ --include="*.java" | head -20
```

---

## Equipment bonuses array

From `equipment_definitions.json`, each entry has a 14-element `bonuses` array:

```
Index:  0=stab atk, 1=slash atk, 2=crush atk, 3=magic atk, 4=range atk
        5=stab def, 6=slash def, 7=crush def, 8=magic def, 9=range def
        10=strength, 11=prayer, 12=ranged str, 13=magic dmg %
```

Slots: 0=hat, 1=body, 2=legs, 3=weapon, 4=shield, 5=cape, 9=hands, 10=feet, 12=ring, 13=ammo

---

## Skill modifiers (from settings.toml)

All skills use 30x XP (`modifier = 3000.0`) except:
```toml
combat_modifier = 1250.0   # 12.5x combat XP (lower than skilling)
```

This means combat training is 2.4x slower than skilling relative to base rates. Intentional?

---

## Combat-related plugins

```bash
ls game-server/plugins/plugin/click/button/ | grep -i combat
# CombatButtonPlugin.java — handles combat style switching
```

---

## Steps

1. Read combat formula source (max hit, accuracy, defense calculations)
2. Verify formulas match OSRS or are intentional custom formulas
3. Check equipment bonuses for outliers (items with impossibly high stats)
4. Verify `combat_modifier` is correct for intended XP rates
5. Check that special attacks are implemented (weapon specs)
6. Verify prayer bonuses actually reduce damage

---

## Client Impact

Combat is calculated server-side. The client only displays animations, hitsplats, and health bars. No client impact for formula changes.

---

## Verify

- [ ] Combat formulas documented (OSRS-based or custom)
- [ ] Equipment bonuses are valid for all wearable items
- [ ] `combat_modifier` matches intended rates
- [ ] Prayer/defense reductions verified
