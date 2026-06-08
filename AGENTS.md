# AGENTS.md — Tarnish Jira Fork

AI agent instructions for this codebase. Read this first before making any changes.

→ **Docs index:** [docs/README.md](docs/README.md)
→ **Game scope:** [docs/game-scope.md](docs/game-scope.md)
→ **Knowledge bank:** [docs/knowledge-bank.md](docs/knowledge-bank.md)
→ **Test plan:** [docs/test-plan.md](docs/test-plan.md)
→ **Prompt pack:** [prompts/README.md](prompts/README.md)
→ **Workflows:** [docs/workflows/](docs/workflows/README.md)

## Quick Start

> **AI Agent Prompt Pack:** 35 self-contained prompts for audit/test/dev: **[prompts/](prompts/README.md)**

Commands shown for both Windows (PowerShell) and WSL/Linux (Bash). Use whichever matches your terminal.

**Build server:**
```powershell
# Windows (PowerShell)
.\gradlew.bat :game-server:classes
```
```bash
# WSL/Linux
./gradlew :game-server:classes
```

**Build client:**
```powershell
.\gradlew.bat :game-client:classes
```
```bash
./gradlew :game-client:classes
```

**Run tests:**
```powershell
.\gradlew.bat :game-server:test
```
```bash
./gradlew :game-server:test
```

**Start server** (blocking — waits for players, looks hung but it's running):
```powershell
.\gradlew.bat :game-server:run
```
```bash
./gradlew :game-server:run
```

**Smoke check — server ready when you see:**
```
  "Startup service finished"
  "Loaded: 133 plugins"
  "Server built successfully"
```
Port: **43594**

**Verify port is listening:**
```powershell
# Windows
Test-NetConnection localhost -Port 43594
```
```bash
# WSL/Linux
nc -zv localhost 43594
```
```

## Running the client locally

The client expects its cache files in `~/.tarnish/cache/`. For development you can copy the server cache:

```powershell
# PowerShell
Copy-Item -Path "game-server\data\cache\*" -Destination "$env:USERPROFILE\.tarnish\cache" -Force
```

Or specify a custom cache directory when launching the client:

```bash
# Bash (WSL/Linux)
java -Dtarnish.cache.dir=../game-server/data/cache -jar game-client/build/libs/Tarnish.jar
```

If you use the Gradle `:game-client:run` task, add the JVM arg:

```gradle
// In game-client/build.gradle.kts
tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Dtarnish.cache.dir=../game-server/data/cache")
}
```

Now the client will load the required assets and start correctly.

## File Discovery — USE code_index.json FIRST

Before grepping the filesystem, check `code_index.json` in the repo root. It maps every source file path to its class name. This is 100x faster than ripgrep/find for finding "where is X defined?"

```bash
# Quick lookup example
cat code_index.json | python3 -c "import json,sys; d=json.load(sys.stdin); print('\n'.join(e['path'] for e in d if 'ClassName' in e['summary']))"
```

The index has 2,663 entries covering all `.java`, `.kt`, `.kts`, `.py`, and key `.json` definition files.

## Project Structure

```
tarnishjirafork/
├── game-server/           # Server (JDK 21, Gradle)
│   ├── src/main/java/com/osroyale/
│   │   ├── content/       # Game content: skills, activities, shops, trading
│   │   │   ├── activity/  # Minigames: Barrows, FightCaves, Zulrah, Inferno, etc.
│   │   │   ├── skill/     # All 18 skills (see below)
│   │   │   ├── store/     # Shop system
│   │   │   ├── combat/    # Combat system
│   │   │   ├── teleport/  # Teleport destinations
│   │   │   └── ...        # Trading post, pets, presets, achievements, etc.
│   │   ├── game/          # Core engine: world, player, items, NPCs, events
│   │   │   ├── world/     # World class, region, entity management
│   │   │   ├── plugin/    # Plugin bootstrap & context
│   │   │   └── event/     # Event system
│   │   ├── net/           # Networking (packets, sessions)
│   │   ├── fs/            # Filesystem/cache access
│   │   ├── io/            # I/O utilities
│   │   └── util/          # General utilities
│   ├── plugins/           # Content plugins (Kotlin/Java) — 133 loaded at boot
│   │   └── plugin/
│   │       ├── click/     # Click handlers: button, item, npc, object
│   │       ├── command/   # Commands by rank (Admin, Owner, Player, etc.)
│   │       └── itemon/    # Item-on-item/npc/object/player interactions
│   ├── data/
│   │   ├── def/           # Game definitions
│   │   │   ├── item/      # item_definitions.json (~2.8MB)
│   │   │   ├── npc/       # npc_definitions.json, npc_drops.json, npc_spawns.json
│   │   │   ├── store/     # stores.json (shop inventory/prices)
│   │   │   ├── equipment/ # equipment_definitions.json
│   │   │   ├── combat/    # projectile_definitions.json
│   │   │   ├── items-json/ # Individual item JSON files (30K+ files, per-item)
│   │   │   └── monsters-json/ # Individual NPC JSON files (30K+ files, per-NPC)
│   │   ├── content/       # Content data (skills, clan, game configs)
│   │   └── profile/       # Player save files
│   └── settings.toml      # Server configuration
├── game-client/           # Client (JDK 11, Gradle)
│   └── src/main/java/     # Client source (~1,200 files)
├── build/                 # Build output (ignored)
├── code_index.json        # AI file lookup index — CHECK THIS FIRST
└── settings.gradle.kts    # Gradle settings
```

## Common Patterns

Detailed workflow docs with exact code patterns: **[docs/workflows/](docs/workflows/README.md)**

### Adding a Command
→ **[docs/workflows/commands.md](docs/workflows/commands.md)**
1. Find the rank file: `game-server/plugins/plugin/command/<Rank>CommandPlugin.java`
2. Add `new Command("name")` block inside `register()`
3. Implement `execute(Player, CommandParser)`

### Adding an Item Click Handler
→ **[docs/workflows/plugins.md](docs/workflows/plugins.md)**
1. Create file in: `game-server/plugins/plugin/click/item/`
2. Extend `PluginContext`, override `firstClickItem(Player, ItemClickEvent)`

### Adding NPC Click/Interaction
→ **[docs/workflows/npcs.md](docs/workflows/npcs.md)**
1. Click handlers: `game-server/plugins/plugin/click/npc/`
2. Item-on-NPC: `game-server/plugins/plugin/itemon/npc/`

### Adding an Object Click
→ **[docs/workflows/plugins.md](docs/workflows/plugins.md)**
1. Handler: `game-server/plugins/plugin/click/object/`
2. Item-on-object: `game-server/plugins/plugin/itemon/object/`

### Adding Shop/Trade Content
→ **[docs/workflows/shops.md](docs/workflows/shops.md)**
1. Shop definitions: `game-server/data/def/store/stores.json`
2. Shop logic: `game-server/src/main/java/com/osroyale/content/store/`

### Adding NPC Spawns/Drops
→ **[docs/workflows/npcs.md](docs/workflows/npcs.md)**
1. Spawns: `game-server/data/def/npc/npc_spawns.json`
2. Drops: `game-server/data/def/npc/npc_drops.json`
3. NPC definitions: `game-server/data/def/npc/npc_definitions.json`

### Adding Items
→ **[docs/workflows/items.md](docs/workflows/items.md)**
1. Item definitions: `game-server/data/def/item/item_definitions.json`
2. Equipment stats: `game-server/data/def/equipment/equipment_definitions.json`

### Adding a Skill Action
→ **[docs/workflows/skills.md](docs/workflows/skills.md)**
1. Skills live in: `game-server/src/main/java/com/osroyale/content/skill/impl/`
2. Extend `SkillAction`, wire to object via click plugin
3. Skill content data: `game-server/data/content/skills/`

## Skills Implemented

Agility, Construction, Cooking, Crafting, Farming, Firemaking, Fishing, Fletching, Herblore, Hunter, Magic, Mining, Prayer, Runecrafting, Slayer, Smithing, Thieving, Woodcutting

## Activities/Minigames

Barrows, Duel Arena, Fight Caves, God Wars, Inferno, Kraken, Last Man Standing, Mage Arena, Pest Control, Recipe for Disaster, Shooting Stars, Warrior Guild, Wintertodt, Zulrah, Battleground

## Build Notes

- Server: JDK 21
- Client: JDK 11
- Gradle wrapper is included (`./gradlew` on WSL, `.\gradlew.bat` on Windows)
- Kotlin 2.1.21 for plugins
- No Gradle daemon by default — use `--no-daemon` for CI
- Build cache: enable with `org.gradle.caching=true` in gradle.properties

## Testing

- Tests in `game-server/src/test/`
- Run: `.\gradlew.bat :game-server:test` (Windows) or `./gradlew :game-server:test` (WSL)
- Currently: profile rights validation tests only
- Adding tests alongside code changes is expected

## Key Data File Sizes (for context)

| File | Size |
|------|------|
| item_definitions.json | 2.8 MB |
| npc_definitions.json | 2.3 MB |
| npc_drops.json | 1.9 MB |
| equipment_definitions.json | 814 KB |
| npc_spawns.json | 515 KB |
| stores.json | 75 KB |

## Office Policy

- Prefer editing existing files over creating new modules
- Plugins are preferred pattern for new content
- Data goes in JSON definition files, not hardcoded
- Tests are expected alongside code changes
- Use code_index.json for file discovery — it's faster than grep
