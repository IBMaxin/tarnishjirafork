# Clan Channel System Audit

**Goal:** Audit the clan channel system for rank hierarchy, permission enforcement, chat abuse, and file-based persistence safety.

**Docs:** `AGENTS.md`, `code_index.json`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannelHandler.java` | Core handler — connect, disconnect, manage, password |
| `game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannel.java` | Clan channel entity |
| `game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanManagement.java` | Rank/permission management |
| `game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanDetails.java` | Clan metadata (name, tag, slogan, type) |
| `game-server/src/main/java/com/osroyale/content/clanchannel/ClanRank.java` | Rank enum — MEMBER, FRIEND, RECRUIT, CORPORAL, SERGEANT, LIEUTENANT, CAPTAIN, GENERAL, LEADER, SYSTEM |
| `game-server/src/main/java/com/osroyale/content/clanchannel/ClanRepository.java` | Static clan registry |
| `game-server/src/main/java/com/osroyale/content/clanchannel/ClanUtility.java` | Utility methods |
| `game-server/src/main/java/com/osroyale/content/clanchannel/ClanMember.java` | Member data class |
| `game-server/src/main/java/com/osroyale/content/clanchannel/content/ClanTask.java` | Clan task system |
| `game-server/src/main/java/com/osroyale/content/clanchannel/content/ClanShowcase.java` | Showcase/display items |
| `game-server/data/content/clan/` | Clan save files directory |

---

## Architecture

```
ClanChannel
├── owner: String
├── name, tag, slogan, password
├── details: ClanDetails (type, level, points)
├── management: ClanManagement (enter/talk/manage ranks, locked flag)
├── members: Set<ClanMember>
├── bannedMembers: Set<String>
├── showcaseItems: Item[]
└── handler: ClanChannelHandler

ClanRank (ordinal order):
  MEMBER < FRIEND < RECRUIT < CORPORAL < SERGEANT < LIEUTENANT
  < CAPTAIN < GENERAL < LEADER < SYSTEM

Permission checks:
  canEnter(member) → member.rank >= management.enterRank
  canTalk(member) → member.rank >= management.talkRank  
  canManage(member) → member.rank >= management.manageRank
```

---

## Steps

### 1. Audit rank hierarchy

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Check ClanRank enum for ordinal-based comparisons
cat game-server/src/main/java/com/osroyale/content/clanchannel/ClanRank.java
```

```bash
# Check canManage, canEnter, canTalk implementations
grep -n "canManage\|canEnter\|canTalk\|lessThan\|greaterThan" \
  game-server/src/main/java/com/osroyale/content/clanchannel/ --include="*.java"
```

### 2. Check connect() for permission bypass

```bash
cat game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannelHandler.java
```

Key checks in `connect()` (line 53-111):
1. Clan channel exists in ClanRepository
2. Channel not full (< 80 members, or "Osroyale" owner bypass)
3. Player not banned from channel
4. Clan type matches player (IRON_MAN check)
5. `handler.attemptConnection()` checks rank vs enter requirement
6. `handler.testPassword()` if password set

### 3. Verify SYSTEM rank privilege

```bash
# Line 153-154: Developers get SYSTEM rank
grep -rn "SYSTEM\|isDeveloper" \
  game-server/src/main/java/com/osroyale/content/clanchannel/
```

```java
if (PlayerRight.isDeveloper(player))
    member.rank = ClanRank.SYSTEM;
```

SYSTEM is the highest rank (ordinal 9, above LEADER at 8). Developers can bypass all clan restrictions — enter any clan, manage any clan, override any rank. **This is intentional but powerful.**

### 4. Check clan save/load safety

```bash
# Clans stored as files in ./data/content/clan/
ls game-server/data/content/clan/ 2>/dev/null || echo "Directory empty or does not exist"

# Check for path traversal in clan file operations
grep -rn "File\|Path\|read\|write\|save\|load" \
  game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannel.java \
  | grep -v "import"
```

### 5. Check chat injection in clan chat

```bash
# Clan chat uses / prefix — check if format tags are filtered
grep -rn "clan\|CLAN_CHAT\|clanChat\|sendClan" \
  game-server/src/main/java/com/osroyale/net/packet/ --include="*.java"

# Check Yell/chat filtering for clan messages
grep -rn "INVALID\|filter\|sanitize\|chat.*filter" \
  game-server/src/main/java/com/osroyale/net/packet/in/ChatMessagePacketListener.java
```

### 6. Check clan member management

```bash
# manageMember() — can a member kick/promote above their rank?
grep -A20 "manageMember" \
  game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannelHandler.java
```

Line 235 check: `other.rank.lessThan(ClanRank.LEADER)` — prevents managing the leader. But does it prevent managing members of equal or higher rank?

### 7. Check disconnect cleanup

```bash
grep -A20 "static boolean disconnect" \
  game-server/src/main/java/com/osroyale/content/clanchannel/channel/ClanChannelHandler.java
```

---

## Findings

### F1: Clan file loading on connect [MEDIUM]

`connect()` (lines 57-83) iterates over `./data/content/clan/` directory, checking if any file name contains the player's username. This is O(n) file listing on every clan join attempt. With many clan files, this could be slow.

### F2: Clan file existence check uses `.contains()` [LOW]

```java
if (file.getName().toLowerCase().contains(player.getUsername().toLowerCase().trim()) && file.length() > 0)
```

Uses `contains()` instead of `equals()`. A player named "a" would match "clan_data.json" — unlikely in practice but imprecise.

### F3: Developer SYSTEM rank bypass [MEDIUM]

Developers get `ClanRank.SYSTEM` on join — can enter/manage any clan without restriction. This is a privilege escalation vector if a developer account is compromised.

### F4: No rate limiting on clan join [LOW]

No cooldown on clan connect/disconnect. A player could rapidly join/leave clans to spam clan chat or trigger repeated file I/O.

### F5: Chat message format injection [MEDIUM]

Clan chat messages go through the same packet path as regular chat. If format tag filtering exists, verify it applies to clan chat too. If a player can send `<img=1>` in clan chat, it renders for all members.

---

## Client Impact

Server-only. Clan interface is server-rendered. Chat messages go through server-side filtering (if any exists).

---

## Verify

- [ ] Rank hierarchy enforced — cannot kick/promote above own rank
- [ ] Cannot manage members of equal rank (except self-management like leaving)
- [ ] Clan save files validated against path traversal
- [ ] Clan chat messages filtered for format injection (same as regular chat)
- [ ] Clan connect/disconnect rate limited
- [ ] Developer SYSTEM rank documented as intentional
- [ ] `manageMember()` prevents managing LEADER rank (confirmed: line 235 check)
