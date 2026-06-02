# Configuration Leak Audit

**Goal:** Find exposed credentials, keys, and sensitive config in the codebase.

**Docs:** `AGENTS.md`, Git history

---

## Critical findings (already visible)

`game-server/settings.toml` contains **plaintext production credentials**:

```toml
[website]
forum_db_url = "jdbc:mysql://173.82.152.23:3306/osroyjs_main"
forum_db_user = "osroyjs_game"
forum_db_pass = "dJ$&38mGEsTB"

[discord]
token = "MTA0ODgxMjIxOTIxMjc2NzI0Mw.GI_zO7.IG56uU2cqwDnh-6pnYGM33sdZHcE1lsoQTmNQk"

[network]
rsa_modulus = "102353038900255..."
rsa_exponent = "539259972257951..."
```

---

## Steps

### 1. Search for hardcoded secrets

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# Passwords, tokens, API keys
grep -rn "pass\|password\|token\|secret\|api.key\|api_key" \
  --include="*.java" --include="*.kt" --include="*.toml" --include="*.json" --include="*.xml" \
  game-server/ game-client/ \
  | grep -v "test\|example\|null\|TODO\|empty\|replace.me\|password=" \
  | grep -v "highscores\|fight\|LMS\|pest\|duel\|slayer\|barrows"

# IP addresses (production servers)
grep -rn "[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}" \
  --include="*.toml" --include="*.json" --include="*.java" \
  game-server/ \
  | grep -v "127.0.0\|0\.0\.0\|localhost"
```

### 2. Check Git history for removed secrets

```bash
# Secrets committed then "removed" are still in git history
git log --all -p -- game-server/settings.toml | grep -i "pass\|token\|secret" | head -20
git log --all -S "password" --oneline
git log --all -S "token" --oneline | grep -i discord
```

### 3. Check for hardcoded RSA keys

RSA keys in settings.toml should be rotated if this fork is public. The keys match the original Tarnish release — any client with matching keys can connect.

### 4. Check client config

```bash
# Does the client have hardcoded server addresses?
grep -rn "43594\|server.*address\|host.*port\|127.0.0.1\|localhost" \
  game-client/src/ --include="*.java" | head -10
```

### 5. Check for exposed debug flags

```toml
server_debug = false       # Verify this is false for production
display_packets = true     # If true in prod → packet data leaked to logs
log_player = true          # If true → player actions logged (PII concern)
```

---

## Severity

| Finding | Severity | Action |
|---------|----------|--------|
| DB credentials in settings.toml | **Critical** | Rotate immediately, move to env vars |
| Discord token in settings.toml | **Critical** | Rotate immediately |
| RSA keys from public repo | **High** | Rotate before production launch |
| Hardcoded production IP | Medium | Move to config |
| `display_packets = true` | Medium | Disable in production |

---

## Client Impact

Client config (server address, port) is compiled-in. The 317 client connects to whatever address it's configured with. If server address is hardcoded to a production IP, every client build exposes that IP.

---

## Verify

- [ ] No plaintext passwords in any file
- [ ] No Discord/webhook tokens in any file
- [ ] RSA keys rotated from original public Tarnish keys
- [ ] `server_debug = false`
- [ ] `display_packets = false` (or documented as intentional)
- [ ] Git history checked for removed secrets
