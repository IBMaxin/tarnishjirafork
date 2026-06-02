# Database Layer Audit

**Goal:** Audit all database connections for hardcoded credentials, SQL injection, connection pool safety, and PII exposure.

**Docs:** `AGENTS.md`, `prompts/03-security/config-leak.md`, `prompts/03-security/packet-injection.md`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/settings.toml` | `[website]` forum DB creds, `[postgre]` game DB creds |
| `game-server/src/main/java/com/osroyale/Config.java` | Parses all DB config from settings.toml |
| `game-server/src/main/java/com/osroyale/game/service/ForumService.java` | MySQL forum DB via HikariCP |
| `game-server/src/main/java/com/osroyale/game/service/PostgreService.java` | PostgreSQL game DB via HikariCP |
| `game-server/src/main/java/com/osroyale/game/service/VoteService.java` | MySQL vote DB — hardcoded creds |
| `game-server/src/main/java/com/osroyale/game/service/HighscoreService.java` | MySQL hiscores DB — hardcoded root creds |
| `game-server/src/main/java/com/osroyale/game/service/DonationService.java` | MySQL store DB — hardcoded creds |
| `game-server/src/main/java/com/osroyale/net/session/LoginSession.java` | Forum auth via ForumService connection pool |
| `game-server/src/main/java/com/osroyale/game/world/entity/mob/player/persist/PlayerPersistDB.java` | Full player save/load via PostgreSQL |
| `game-server/src/main/java/com/osroyale/game/event/impl/log/*.java` | ChatLog, TradeLog, CommandLog, PMLog, DropItemLog, PickupItemLog → PostgreSQL |

---

## Database Inventory

| Service | DB Engine | Host | DB Name | Username | Password Source | Used For |
|---------|-----------|------|---------|----------|----------------|----------|
| ForumService | MySQL (HikariCP) | 173.82.152.23:3306 | osroyjs_main | osroyjs_game | settings.toml `[website]` | User auth (BCrypt checks) |
| VoteService | MySQL (DriverManager) | 173.82.152.23:3306 | osroyjs_vote | osroyjs_exo1 | **HARDCODED** `VoteService.java:18-19` | Vote reward claims |
| DonationService | MySQL (DriverManager) | 173.82.152.23:3306 | osroyjs_store_2 | osroyjs_exo1 | **HARDCODED** `DonationService.java:15-17` | Donation reward claims |
| HighscoreService | MySQL (DriverManager) | 45.88.231.118:3306 | hiscores | root | **HARDCODED** `HighscoreService.java:20-22` | Highscores save |
| PostgreService | PostgreSQL (HikariCP) | (from settings.toml) | (from settings.toml) | (from settings.toml) | settings.toml `[postgre]` | Player persistence, chat/trade/command/item logs |

---

## Steps

### 1. Map all hardcoded credentials

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Every hardcoded JDBC URL
grep -rn "jdbc:mysql://\|jdbc:postgresql://" \
  game-server/src/main/java/ --include="*.java"

# Every hardcoded username/password
grep -rn "USER\|PASS\|USERNAME\|PASSWORD" \
  game-server/src/main/java/com/osroyale/game/service/ --include="*.java" \
  | grep -v "import\|logger\|//"
```

Expected results:
```
VoteService.java:17:    private static final String CONNECTION_STRING = "jdbc:mysql://173.82.152.23:3306/osroyjs_vote";
VoteService.java:18:    private static final String USER = "osroyjs_exo1";
VoteService.java:19:    private static final String PASS = "3AXbU=W7IfzX";

HighscoreService.java:20:    private static final String CONNECTION_STRING = "jdbc:mysql://45.88.231.118:3306/hiscores";
HighscoreService.java:21:    private static final String USERNAME = "root";
HighscoreService.java:22:    private static final String PASSWORD = "bQ9R#UnPsW5^HLiU9$4LcJvE4%ZwJWLz";

DonationService.java:15:    private static final String USER = "osroyjs_exo1";
DonationService.java:16:    private static final String PASS = "3AXbU=W7IfzX";
DonationService.java:17:    private static final String CONNECTION_STRING = "jdbc:mysql://173.82.152.23:3306/osroyjs_store_2";
```

### 2. Check SQL injection surface

```bash
# Are there any raw SQL string concatenations (not parameterized)?
grep -rn "\"\s*\+\s*\|sql\s*(\s*\"[^\"]*\"\s*\+" \
  game-server/src/main/java/ --include="*.java" | grep -v "generateQuery\|StringBuilder\|sb\."

# Check all .sql() calls for parameterized queries
grep -rn "\.sql(" game-server/src/main/java/ --include="*.java" | grep -v "import\|test"

# Check all PreparedStatement usage
grep -rn "prepareStatement\|createStatement" game-server/src/main/java/ --include="*.java" \
  | grep -v "import\|test"
```

### 3. Check PlayerPersistDB for SQL injection

```bash
# PlayerPersistDB.java — 2000+ lines of player persistence
# Every read/write uses JdbcSession parameterized API
grep -rn "\.sql(" game-server/src/main/java/com/osroyale/game/world/entity/mob/player/persist/PlayerPersistDB.java
```

All queries use `?` parameters — no string concatenation found. **Positive finding: parameterized queries throughout.**

### 4. Check LoginSession forum auth SQL

```java
// LoginSession.java lines 224-226
new JdbcSession(ForumService.getConnectionPool())
    .sql(isEmail 
        ? "SELECT member_id, members_pass_hash, name, temp_ban FROM core_members WHERE UPPER(email) = ?"
        : "SELECT member_id, members_pass_hash, temp_ban FROM core_members WHERE UPPER(name) = ?")
    .set(username.toUpperCase())
```

Uses `?` parameter — safe from SQL injection. BCrypt for password verification. **Positive finding.**

### 5. Check connection pool health

```bash
# ForumService — pool size 10, timeout 5s
grep -A5 "HikariConfig" game-server/src/main/java/com/osroyale/game/service/ForumService.java

# PostgreService — pool size 50, timeout 10s (non-LIVE uses DriverManager directly!)
grep -A5 "HikariConfig\|getConnection" game-server/src/main/java/com/osroyale/game/service/PostgreService.java
```

PostgreService has a concerning pattern: in non-LIVE mode, it creates a NEW connection via `DriverManager.getConnection()` every time, bypassing the pool entirely. Line 40-42:
```java
if (Config.WORLD_TYPE != WorldType.LIVE) {
    return DriverManager.getConnection(Config.POSTGRE_URL, Config.POSTGRE_USER, Config.POSTGRE_PASS);
}
```

### 6. Check PII in database logs

```bash
# ChatLogEvent, TradeLogEvent, CommandLogEvent, PrivateMessageChatLogEvent
# These log player interactions to PostgreSQL — what PII is logged?

cat game-server/src/main/java/com/osroyale/game/event/impl/log/ChatLogEvent.java
cat game-server/src/main/java/com/osroyale/game/event/impl/log/PrivateMessageChatLogEvent.java
cat game-server/src/main/java/com/osroyale/game/event/impl/log/CommandLogEvent.java
```

---

## Findings

### F1: Four hardcoded database credential sets [CRITICAL]

Three services hardcode credentials in Java source (VoteService, HighscoreService, DonationService). One reads from settings.toml (ForumService). All four connect to production servers on the public internet.

| Service | Host | Risk |
|---------|------|------|
| ForumService | 173.82.152.23 | User auth, password hashes |
| VoteService | 173.82.152.23 | Vote records |
| DonationService | 173.82.152.23 | Payment records |
| HighscoreService | 45.88.231.118 | **root** access |

### F2: HighscoreService uses MySQL root [CRITICAL]

```java
private static final String USERNAME = "root";
private static final String PASSWORD = "bQ9R#UnPsW5^HLiU9$4LcJvE4%ZwJWLz";
```

Full root access to a remote MySQL server. If this server hosts other databases, compromise gives access to all of them.

### F3: VoteService and DonationService share credentials [MEDIUM]

Both use `osroyjs_exo1` / `3AXbU=W7IfzX` — a compromise of one database means compromise of both.

### F4: PostgreService bypasses pool in non-LIVE mode [MEDIUM]

Non-LIVE mode creates a new DB connection per request via `DriverManager.getConnection()`. This is slower and leaky — each connection must be manually closed. During development/testing, this could exhaust DB connections.

### F5: No SQL injection found [POSITIVE]

All database queries use parameterized statements (`?` placeholders) via either:
- JdbcSession `.sql()` + `.set()` pattern
- PreparedStatement `.setString()` / `.setInt()`

No string-concatenated SQL queries found. This is consistent with the earlier audit finding in `rsps-security-audit`.

### F6: Database logging includes PII [MEDIUM]

ChatLogEvent, PrivateMessageChatLogEvent, TradeLogEvent, CommandLogEvent all write player names, messages, and actions to PostgreSQL. This is a GDPR/data-retention concern — what's the retention policy?

---

## Client Impact

Server-only. No client changes needed.

---

## Verify

- [ ] All DB credentials moved to environment variables or external secrets manager
- [ ] HighscoreService root access changed to limited-privilege user
- [ ] VoteService and DonationService use separate credentials
- [ ] `settings.toml` cleaned of plaintext credentials
- [ ] PostgreService non-LIVE mode fixed to use connection pool
- [ ] Database log retention policy documented
- [ ] Git history checked for committed credentials
