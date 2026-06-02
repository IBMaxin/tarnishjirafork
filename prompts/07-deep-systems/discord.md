# Discord Integration Audit

**Goal:** Audit both Discord bot implementations for exposed credentials, command surface, and remote control risk.

**Docs:** `AGENTS.md`, `prompts/03-security/config-leak.md`

---

## Dual Implementation — Two Discord Bots

This codebase has **two competing Discord implementations**:

| | Discord.java (ACTIVE) | DiscordPlugin.java (DISABLED) |
|---|---|---|
| **Library** | Discord4J | JDA (Java Discord API) |
| **Token source** | `Config.DISCORD_TOKEN` from `settings.toml` | `Constants.TOKEN` — hardcoded in Constants.java |
| **Status** | LIVE mode only (`WorldType.LIVE`) | `ENABLED = false` — never starts |
| **Commands** | 3 read-only: `::uptime`, `::players`, `::staffonline` | 6 write-capable: announcements, polls, bug reports, suggestions, punishment log, update log |
| **Prefix** | `::` | `!` |

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/settings.toml` | `[discord] token` — production token in plaintext |
| `game-server/src/main/java/com/osroyale/net/discord/Constants.java` | Hardcoded token + ALL channel IDs |
| `game-server/src/main/java/com/osroyale/net/discord/Discord.java` | Active Discord4J bot (read-only) |
| `game-server/src/main/java/com/osroyale/net/discord/DiscordDispatcher.java` | Command handler for active bot |
| `game-server/src/main/java/com/osroyale/net/discord/DiscordPlugin.java` | Disabled JDA bot (write-capable) |
| `game-server/src/main/java/com/osroyale/net/discord/BotListener.java` | JDA event listener |
| `game-server/src/main/java/com/osroyale/Config.java` | `DISCORD_TOKEN` loading from settings.toml |

---

## Steps

### 1. Verify token exposure

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Token in settings.toml
grep -A2 "\[discord\]" game-server/settings.toml

# Token hardcoded in Constants.java
grep "TOKEN" game-server/src/main/java/com/osroyale/net/discord/Constants.java

# Token loaded by Config.java
grep "DISCORD_TOKEN" game-server/src/main/java/com/osroyale/Config.java

# Check git history for token exposure
git log --all -S "MTA0ODgxMjIxOTIxMjc2NzI0Mw" --oneline
```

### 2. Audit active bot (Discord4J) commands

```bash
# DiscordDispatcher.java — all commands are switch cases
cat game-server/src/main/java/com/osroyale/net/discord/DiscordDispatcher.java
```

Current commands (read-only, safe):
- `::commands` — lists available commands
- `::uptime` — server uptime
- `::players` — player count
- `::staffonline` — lists online staff with names and ranks

**No write operations.** Cannot send messages to game, cannot execute server commands, cannot modify state.

### 3. Audit disabled bot (JDA) capabilities

```bash
cat game-server/src/main/java/com/osroyale/net/discord/DiscordPlugin.java
```

If `ENABLED` were set to `true`, this bot could:
- `sendSimpleMessage(message)` → EVENTS_CHANNEL
- `sendBugReport(playername, message)` → BUG_CHANNEL
- `sendSuggestion(playername, message)` → SUGGESTION_CHANNEL
- `sendAnnouncement(message)` → ANNOUNCEMENT_CHANNEL
- `sendPunishmentMessage(staffMember, action, playerName, time)` → PUNISHMENT_CHANNEL
- `sendEventMessage(message, receiver, icon, skillID)` → EVENTS_CHANNEL (with embed)
- `sendUpdateMessage(receiver)` → UPDATE_CHANNEL — reads from local file `source/tools/website_logs/DiscordUpdateLog.txt`
- `pollYN(question)` → POLL_CHANNEL

### 4. Check for game→Discord bridges (can players trigger Discord messages?)

```bash
# Who calls DiscordPlugin methods?
grep -rn "DiscordPlugin\." game-server/src/main/java/ --include="*.java"
grep -rn "Discord\.message\|Discord\.send" game-server/src/main/java/ --include="*.java"

# Who calls sendAnnouncement — could a player trigger it?
grep -rn "sendAnnouncement\|sendPunishmentMessage\|sendBugReport\|sendSuggestion" \
  game-server/src/main/java/ --include="*.java" | grep -v "DiscordPlugin.java"
```

### 5. Check channel IDs

All Discord channel IDs are hardcoded in `Constants.java`:
```java
UPDATE_CHANNEL = "1002695599528882287"
BUG_CHANNEL = "1029821585441423361"
SUGGESTION_CHANNEL = "1002705219563569196"
POLL_CHANNEL = "1002695686011236394"
PUNISHMENT_CHANNEL = "1020065838688116757"
EVENTS_CHANNEL = "1002695649642426419"
LEVELS_CHANNEL = "1047582983017730133"
ANNOUNCEMENT_CHANNEL = "1002695332930535494"
TEST_CHANNEL = "1047409430590922772"
```

These map to a real Discord server. If the token is still valid, anyone with this code can join the server and interact with these channels.

---

## Findings

### F1: Hardcoded Discord token in Constants.java [CRITICAL]

```java
// Constants.java line 4
public static final String TOKEN = "MTA0ODgxMjIxOTIxMjc2NzI0Mw.GI_zO7.IG56uU2cqwDnh-6pnYGM33sdZHcE1lsoQTmNQk";
```

Same token appears in `settings.toml` line 70. This token is in source control — check `git log` to confirm it's been in the repo since initial commit.

### F2: Full Discord channel map exposed [MEDIUM]

All 9 channel IDs are hardcoded with descriptive names. An attacker with the token can read/write to every channel.

### F3: Discord4J communityChannel init race [LOW]

```java
// Discord.java lines 33, 60
static final Snowflake COMMUNITY_CHANNEL = Snowflake.of(0L);  // initialized to 0
// Later:
communityChannel = gateway.getChannelById(Discord.COMMUNITY_CHANNEL).block();  // still 0L
```

`COMMUNITY_CHANNEL` is initialized to `Snowflake.of(0L)` and never changed. `getChannelById(0L)` will return null. The comment on line 19-24 acknowledges this is broken: "Problem with this is if you start the server from fresh the Discord bot will not be able to send outgoing messages (message) until someone has triggered the discord event."

### F4: Disabled bot has greater capability [MEDIUM]

The disabled JDA bot (DiscordPlugin) can send announcements, polls, and punishment messages — all write operations on the Discord server. The active bot (Discord4J) is read-only. This is inverted: the safer bot runs in production, the dangerous bot is disabled. But if someone flips `ENABLED = true`, the JDA bot activates with the same token.

---

## Client Impact

Server-only. No client changes needed. Discord is entirely server-side.

---

## Verify

- [ ] Discord token rotated and removed from source code
- [ ] Token moved to environment variable or external secrets manager
- [ ] `Constants.java` token line deleted or redacted
- [ ] Git history scrubbed of token (or repo made private)
- [ ] `DiscordPlugin.ENABLED` confirmed `false` in all environments
- [ ] `COMMUNITY_CHANNEL` Snowflake set to actual channel ID (not `0L`)
- [ ] No player-triggerable paths to `DiscordPlugin.sendAnnouncement()` or other write methods
