# Consumables Audit (Food + Potions)

**Goal:** Audit FoodData and PotionData for healing amounts, edge cases, stat overflow, and missing effect chains.

**Docs:** `AGENTS.md`, `code_index.json`, `docs/workflows/skills.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/consume/FoodData.java` | All 65+ food definitions — IDs, heal amounts, multi-bite |
| `game-server/src/main/java/com/osroyale/content/consume/PotionData.java` | All potion definitions — effects, replacement items, drinkability |
| `game-server/plugins/plugin/click/item/EatFoodPlugin.java` | Food click handler |
| `game-server/plugins/plugin/click/item/DrinkPotionPlugin.java` | Potion click handler |
| `game-server/plugins/plugin/click/item/ItemFirstClickPlugin.java` | Generic first-click router |

---

## Steps

### 1. Audit FoodData for healing anomalies

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

python3 -c "
import re
with open('game-server/src/main/java/com/osroyale/content/consume/FoodData.java') as f:
    content = f.read()
# Extract all enum entries: NAME(ID, heal, ...)
entries = re.findall(r'(\w+)\((\d+),\s*(-?\d+)[,\)]', content)
for name, id_, heal in entries:
    h = int(heal)
    if h <= 0 or h > 30:
        print(f'{name}: id={id_} heal={h} *** ANOMALY ***')
# Sort by heal amount
sorted_entries = sorted(entries, key=lambda x: int(x[2]), reverse=True)
for name, id_, heal in sorted_entries[:10]:
    print(f'{name}: id={id_} heal={heal}')
"
```

Expected anomalies:
- `BEER(1917, 1)` — beer heals 1 HP
- `COOKED_KARAMBWAN(3144, -1, -1, 18)` — has -1 for parent and replacement (special handling)
- `EASTER_EGG1(1961, 12)` — Easter egg heals 12 HP (event item)

### 2. Check for missing OSRS foods

```bash
# What modern OSRS foods are missing?
# Rocktail (15272), Mantaray (391), Dark crab (11936), Anglerfish (13441) — present
# Missing: cooked karambwan (but has special handling), saradomin brew (6685)
grep -c "public enum FoodData" game-server/src/main/java/com/osroyale/content/consume/FoodData.java
```

### 3. Audit PotionData for effect completeness

```bash
# How many potions defined?
grep -c "new PotionData\|new SuperPotionData\|new CombatPotionData\|new AntifireData\|new PrayerPotionData\|new RestorePotionData\|new StaminaPotionData\|new SaradominBrewData" \
  game-server/src/main/java/com/osroyale/content/consume/PotionData.java
```

### 4. Check healing overflow

```bash
# Can food heal past max HP?
grep -rn "heal\|getCurrentHealth\|getMaxHealth\|heal\b" \
  game-server/plugins/plugin/click/item/EatFoodPlugin.java

# Does potion stat boost respect skill max?
grep -rn "setLevel\|boostLevel\|modifyLevel\|getMaxLevel" \
  game-server/src/main/java/com/osroyale/content/consume/PotionData.java | head -10
```

### 5. Check consume-while-stunned edge case

```bash
# Can you eat while stunned/frozen?
grep -rn "stunned\|frozen\|locked\|canEat\|canDrink\|canConsume" \
  game-server/plugins/plugin/click/item/EatFoodPlugin.java \
  game-server/plugins/plugin/click/item/DrinkPotionPlugin.java
```

### 6. Check Karambwan tick-manipulation

```bash
# Cooked karambwan (3144) — special fast-eat mechanic
grep -rn "karambwan\|isFast\|FAST_EAT\|fastEat" \
  game-server/src/main/java/com/osroyale/content/consume/ \
  game-server/plugins/plugin/click/item/
```

FoodData.java line 179-181:
```java
public boolean isFast() {
    return replacement != -1 || parent != id;
}
```

This flag is used for multi-bite foods (pizza, cake, pie) but also affects Karambwan — which uses it for the fast-eat mechanic (immediate, no delay).

---

## Findings

### F1: Karambwan is fast-eat with 18 HP heal [HIGH]

Cooked Karambwan heals 18 HP with no eat delay (fast eat). Combined with Shark (20 HP), this is 38 HP in 1 tick. This is correct OSRS behavior but powerful — verify the eat delay mechanism actually works correctly for non-Karambwan foods.

### F2: Anglerfish heal depends on HP level [LOW]

`FoodData.anglerfishHeal()` returns 0-22 based on HP level. If called for a non-anglerfish, returns wrong value. Verify the caller checks for anglerfish specifically.

### F3: Beer heals 1 HP [INFO]

Beer (1917) and Greenman's Ale (1909) heal 1 HP. These are normally stat-modifying drinks, not food. Verify they don't interfere with potion mechanics.

### F4: Easter egg heals 12 HP [INFO]

Easter egg (1961) heals 12 HP — event item with significant value. Verify it's not obtainable outside events.

---

## Client Impact

Server-only. Food/potion data is entirely server-side. The client only sends "I clicked item in slot X" — the server looks up the item ID and applies the effect.

---

## Verify

- [ ] Food healing does not exceed max HP
- [ ] Karambwan + Shark combo works but doesn't create infinite healing
- [ ] Cannot eat while stunned (movement-locked)
- [ ] Potion stat boosts respect skill maximums
- [ ] Saradomin brew correctly drains combat stats while healing
- [ ] Multi-bite foods (pizza, cake, pie) correctly track remaining bites
- [ ] All potion effects apply (check combat effect chain for Antifire/Antipoison)
