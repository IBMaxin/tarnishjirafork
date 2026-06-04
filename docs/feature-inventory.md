# Feature Inventory

This is a code-grounded inventory of features that appear to exist in the server. It does not guarantee every feature is polished or fully verified in-game; it records what is present in source/data and where to look.

## Core Game Content

### Bosses and PvM

Boss/activity implementations and combat strategies indicate support for:

- Barrows
- Cerberus
- Chaos Fanatic
- Crazy Archaeologist
- God Wars Dungeon bosses
- Hydra
- Kraken
- Mage Arena bosses: Derwen, Justicar, Porazdir
- Skotizo wilderness event boss
- Venenatis
- Vet'ion
- Vorkath
- Zulrah
- Fight Caves
- Inferno
- Recipe for Disaster
- Wintertodt

Primary locations:

- `game-server/src/main/java/com/osroyale/content/activity/impl/`
- `game-server/src/main/java/com/osroyale/content/activity/inferno/`
- `game-server/src/main/java/com/osroyale/content/wintertodt/`
- `game-server/src/main/java/com/osroyale/game/world/entity/combat/strategy/npc/boss/`

### Minigames and Activities

The codebase includes:

- Barrows
- Battleground
- Duel Arena
- Fight Caves
- God Wars
- Inferno
- Kraken
- Last Man Standing
- Mage Arena
- Pest Control
- Recipe for Disaster
- Shooting Stars
- Warrior Guild
- Wintertodt
- Zulrah
- Vorkath

Primary locations:

- `game-server/src/main/java/com/osroyale/content/activity/`
- `game-server/src/main/java/com/osroyale/content/lms/`
- `game-server/src/main/java/com/osroyale/content/shootingstar/`
- `game-server/src/main/java/com/osroyale/content/wintertodt/`

### Skills

Skill implementation directories exist for:

- Agility
- Construction
- Cooking
- Crafting
- Farming
- Firemaking
- Fishing
- Fletching
- Herblore
- Hunter
- Magic
- Mining
- Prayer
- Runecrafting
- Slayer
- Smithing
- Thieving
- Woodcutting

Primary locations:

- `game-server/src/main/java/com/osroyale/content/skill/`
- `game-server/data/content/skills/`

## Economy and Shops

### Shop System

The main shop definitions live in:

- `game-server/data/def/store/stores.json`
- `game-server/src/main/java/com/osroyale/content/store/`

Configured shops include:

- Blood Miscellaneous Store
- Blood Range Store
- Blood Magic Store
- Blood Barrows Store
- Blood Melee Store
- Grace's Graceful Store
- Pest Control Store
- General Store
- Skilling Store
- LMS Store
- Clanmaster's Store
- Range Store
- Magic Store
- Pure Store
- Tarnish Vote Store
- Prestige Rewards Store
- Ironman General Store
- Skiller Shop
- Shanomi's Armour Store
- Skilling Store Equipment
- Tzhaar Tokkul Store
- Donator Store
- Ironman Donator Store
- Crafting Store
- Kolodion's Arena Store
- Chef's Choodle Oodle Store
- Stardust Store

Special currencies seen in store data include:

- `DONATOR_POINTS`
- `VOTE_POINTS`
- `PRESTIGE_POINTS`

### Donator System

Donator ranks are implemented in `PlayerRight` and donation-related content:

- Donator
- Super Donator
- Extreme Donator
- Elite Donator
- King Donator

Rank thresholds appear to be based on total spent:

- Donator: 10
- Super Donator: 50
- Extreme Donator: 250
- Elite Donator: 500
- King Donator: 1000

Donator features visible in code:

- Donator zones
- Super donator zone
- Donator titles
- Donator bonds
- Donator point shops
- Drop rate bonuses
- Extra preset slots
- Extra deposit amount
- Blood money bonus
- `::yell`

Primary locations:

- `game-server/src/main/java/com/osroyale/game/world/entity/mob/player/PlayerRight.java`
- `game-server/src/main/java/com/osroyale/content/donators/`
- `game-server/plugins/plugin/DonatorPlugin.java`
- `game-server/plugins/plugin/command/DonatorCommandPlugin.java`

### Voting and Donation Claims

Player commands exist for:

- `::vote`
- `::voted`
- `::claimvote`
- `::claimvotes`
- `::donate`
- `::store`
- `::webstore`
- `::donated`
- `::claim`

The claim commands are currently marked as temporarily disabled until EverythingRS is replaced.

Primary location:

- `game-server/plugins/plugin/command/PlayerCommandPlugin.java`

### Other Economy Systems

Other economy/progression systems present:

- Trading post
- Blood money
- Mystery boxes
- Drop viewer
- Drop simulator
- Presets/preloads
- Prestige
- Starter kits
- Well of Goodwill
- Fame hall
- Gambling content package

Primary locations:

- `game-server/src/main/java/com/osroyale/content/tradingpost/`
- `game-server/src/main/java/com/osroyale/content/bloodmoney/`
- `game-server/src/main/java/com/osroyale/content/mysterybox/`
- `game-server/src/main/java/com/osroyale/content/simulator/`
- `game-server/src/main/java/com/osroyale/content/preset/`
- `game-server/src/main/java/com/osroyale/content/prestige/`
- `game-server/src/main/java/com/osroyale/content/StarterKit.java`
- `game-server/src/main/java/com/osroyale/content/WellOfGoodwill.java`
- `game-server/src/main/java/com/osroyale/content/famehall/`
- `game-server/src/main/java/com/osroyale/content/gambling/`

## Progression and Completion Systems

### Achievements

Achievement data and handlers exist for PvP, PvM, skilling, minigame, and miscellaneous tasks. Many systems call `AchievementHandler.activate(...)` during gameplay.

Primary location:

- `game-server/src/main/java/com/osroyale/content/achievement/`

### Collection Log

Collection log support is wired into NPC drops, Barrows, Wintertodt, Warriors Guild, and UI button plugins.

Primary location:

- `game-server/src/main/java/com/osroyale/content/collectionlog/`

### Pets

Pet content exists.

Primary location:

- `game-server/src/main/java/com/osroyale/content/pet/`

### Clue Scrolls

Clue scroll plugin and reward broadcasts exist.

Primary location:

- `game-server/plugins/plugin/click/item/ClueScrollPlugin.java`

## Broadcast and Social Systems

### Yell

`::yell` is implemented for donators/helpers/staff. It filters selected formatting strings and respects player yell settings, mute, and jail status.

Primary locations:

- `game-server/src/main/java/com/osroyale/content/Yell.java`
- `game-server/plugins/plugin/command/DonatorCommandPlugin.java`
- `game-server/plugins/plugin/command/HelperCommandPlugin.java`

### Broadcasts

Broadcast paths include:

- Manager `::broadcast`
- Rare drop announcements
- Mystery box reward announcements
- Clue reward announcements
- New player announcements
- Prestige announcements
- Shooting star announcements
- Boss spawn/defeat announcements
- Well of Goodwill announcements
- Double XP announcements
- Fame hall announcements

Primary locations:

- `game-server/src/main/java/com/osroyale/game/world/World.java`
- `game-server/plugins/plugin/command/ManagerCommandPlugin.java`
- `game-server/src/main/java/com/osroyale/game/world/entity/mob/npc/drop/NpcDropManager.java`
- `game-server/src/main/java/com/osroyale/content/mysterybox/`
- `game-server/src/main/java/com/osroyale/content/prestige/`
- `game-server/src/main/java/com/osroyale/content/shootingstar/`
- `game-server/src/main/java/com/osroyale/content/WellOfGoodwill.java`

### Discord Integration

Discord hooks exist for announcements, rare drops, suggestions, updates, and bot listener behavior.

Primary location:

- `game-server/src/main/java/com/osroyale/net/discord/`

### Trivia Bot

Trivia bot support exists and is started during server startup.

Primary location:

- `game-server/src/main/java/com/osroyale/content/triviabot/`

## Account Modes, Ranks, and Staff Tools

Player rights include:

- Player
- Helper
- Moderator
- Administrator
- Manager
- Owner
- Developer
- Donator tiers
- Veteran
- Youtuber
- Ironman
- Ultimate Ironman
- Hardcore Ironman
- Graphic

Staff command files exist by rank:

- `PlayerCommandPlugin.java`
- `DonatorCommandPlugin.java`
- `HelperCommandPlugin.java`
- `ModeratorCommandPlugin.java`
- `AdminCommandPlugin.java`
- `ManagerCommandPlugin.java`
- `DeveloperCommandPlugin.java`
- `OwnerCommandPlugin.java`

Staff tooling includes commands for:

- mute/unmute
- jail/unjail
- kick
- teleport to / teleport to me
- bank access
- spawn item
- spawn NPC
- promote/demote/set rank
- ban/unban/ip-ban/ip-mute
- reset player
- save world
- inspect accounts
- debugging NPCs, objects, regions, clipping, interfaces, animations, drops

## Utility Items and Bags

Utility bag/plugin support appears for:

- Rune pouch
- Looting bag
- Gem bag
- Coal bag
- Clue scrolls
- Donator bonds

Primary locations:

- `game-server/src/main/java/com/osroyale/content/bags/`
- `game-server/plugins/plugin/itemon/`
- `game-server/plugins/plugin/click/item/`

## Manual Verification Needed

The inventory above is based on source/data presence. These should still be verified in-game:

- Donator shop open/buy/sell flow
- Donator zone teleports
- Yell formatting and filtering
- Manager broadcasts and broadcast UI
- Discord announcement delivery
- Vote/donation claim replacement flow
- Trading post buy/sell/search
- Collection log UI updates
- Achievement UI and reward flow
- Minigame entry/exit/reward loops
- Boss combat mechanics and special attacks
- NPC drops after kills
- Account persistence after logout/restart
