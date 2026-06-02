# Privilege Escalation Audit

**Goal:** Find paths where a lower-rank player can gain higher-rank capabilities.

**Docs:** AGENTS.md, `02-data-audit/profile-rights.md`, `03-security/command-audit.md`, `00-cross-cutting/client-server-boundary.md`

---

## Attack surfaces

### 1. Command dispatch bypass

If `CommandExtension` matches by command name without checking `PlayerRight`, then any client can send any command.

**Check:**
```bash
find game-server -name "CommandExtension.java" -exec grep -n "canExecute\|PlayerRight\|right\|rank\|permission" {} \;
```

If `CommandExtension` has NO rank filter → **critical vulnerability**. Fix: Add `PlayerRight` check.

### 2. Setrank escalation chains

The `setrank` command in `OwnerCommandPlugin.java`:
```java
commands.add(new Command("setrank", "giverank", "rank") {
    @Override
    public void execute(Player player, CommandParser parser) {
        // Read target player name and new rank
        // Apply rank change
    }
});
```

**Check:**
- Can an OWNER promote to DEVELOPER? (DEVELOPER > OWNER in hierarchy)
- Can an OWNER demote another OWNER?
- Is there a confirmation or logging?
- What if the target is offline? (Persisted in save file?)

### 3. Donator tier bypass

From `PlayerRight.java`:
```java
// Donator checks are based on money spent, not explicit rank assignment
public static boolean isDonator(Player player) {
    return isModerator(player) || isHelper(player) 
        || player.donation.getSpent() >= DONATOR.getMoneyRequired();
}
```

**Check:**
- Can `donation.getSpent()` be modified via save file editing?
- Can a player change their own `player-rights` in the save file?
- Does the server validate `player-rights` on login against `world_profile_list.json`?

### 4. Save file tampering

Profile files are JSON at `game-server/data/profile/save/<name>.json`. If a player can modify their own save file (file write via exploit, or direct filesystem access in shared hosting), they can change `player-rights`.

**Check:**
- Does the server re-validate `player-rights` on every login?
- Or only on first load?

### 5. Bot system abuse

From `game-server/src/main/java/com/osroyale/content/bot/`:
```java
// Bot classes with combat objectives — can they be used to:
// - Farm drops without risk?
// - Attack specific players?
// - Generate resources faster than intended?
```

---

## Real rank chain (from `PlayerRight.java`)

```java
public static boolean isDeveloper(Player player) {
    return player.right.equals(OWNER) || player.right.equals(DEVELOPER);
    // Bug? OWNER passes isDeveloper() — this means OWNER has DEVELOPER privileges
}
```

Wait — OWNER passes `isDeveloper()` check. This means OWNER commands + DEVELOPER commands are accessible to OWNER. Is this intentional or a bug? If the code checks for `isOwner()` separately, OWNER has combined OWNER+DEVELOPER access.

---

## Client Impact

All rank checks are server-side. Client doesn't know ranks beyond crown icons. But a modified client can send any command string — the server must enforce permissions. See [packet-injection.md](packet-injection.md).

---

## Verify

- [ ] Command dispatch has rank check (not just file name)
- [ ] `setrank` cannot create DEVELOPER from OWNER
- [ ] Save file `player-rights` is validated on login
- [ ] `world_profile_list.json` is authoritative for ranks
- [ ] OWNER/DEVELOPER overlap is intentional (document if so)
