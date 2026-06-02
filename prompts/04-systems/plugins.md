# Plugin System

**Goal:** Map all 133 plugins, identify coverage gaps.

**Docs:** `AGENTS.md` §Plugins, `docs/workflows/plugins.md`, `code_index.json`

---

## Plugin directories and counts

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork
for dir in game-server/plugins/plugin/*/; do
    count=$(find "$dir" -name "*.java" -o -name "*.kt" | wc -l)
    echo "$(basename $dir): $count"
done
```

Expected categories:

| Directory | Description | Plugins |
|-----------|-------------|---------|
| `click/button/` | Interface buttons | ~25 |
| `click/item/` | Inventory item clicks | ~25 |
| `click/npc/` | NPC interaction | ~4 |
| `click/object/` | Object interaction | ~13 (incl wintertodt/) |
| `click/itemcontainer/` | Container actions | ~3 |
| `command/` | Chat commands | 8 |
| `itemon/item/` | Item on item | ~7 |
| `itemon/npc/` | Item on NPC | ~6 |
| `itemon/object/` | Item on object | ~5 |
| `itemon/player/` | Item on player | ~1 |
| Root `plugin/` | Top-level | ~5 |

---

## Plugin auto-discovery

From `game-server/build.gradle.kts`:
```kotlin
sourceSets.named("main") {
    java {
        srcDir("plugins")    // <-- plugins/ directory is a source root
    }
}
```

Plugins in `game-server/plugins/plugin/` are compiled as part of the main source set. No separate registration needed — the classgraph library (`io.github.classgraph:classgraph:4.8.179`) auto-discovers them at boot.

---

## Key plugin base classes

| Base class | For |
|-----------|-----|
| `PluginContext` | Click handlers, item-on-X |
| `CommandExtension` | Command plugins |

Both are in `game-server/src/main/java/com/osroyale/game/plugin/`

---

## Steps

1. Count plugins per category
2. For each category, check:
   - Does the plugin properly extend the right base class?
   - Does it return `true`/`false` correctly to chain or consume events?
3. Identify gaps — what common RSPS interactions have NO plugin?
   - Example: No plugin for item-on-item combining certain items
   - Example: Missing fourth-click NPC handler?

---

## Client Impact

Plugins handle server-side logic triggered by client packets. The client sends click/command packets → server dispatches to the right plugin. Plugin changes are server-only.

---

## Verify

- [ ] 133 plugins loaded at boot (check server log)
- [ ] All plugin categories have expected coverage
- [ ] No plugin throws exceptions silently (check error.log)
- [ ] Event chaining works correctly (true = consumed, false = pass through)
