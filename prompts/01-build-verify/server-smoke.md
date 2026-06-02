# Server Smoke Test

**Goal:** Start the server, verify it boots correctly, check port is listening.

**Docs:** `AGENTS.md` §Quick Start, `docs/game-scope.md`

---

## Start Server

**Windows:** `.\gradlew.bat :game-server:run`
**WSL/Linux:** `./gradlew :game-server:run`

**Important:** This blocks — the server waits for players. It looks "hung" but it's running.

---

## Expected Boot Log

Within 15-30 seconds you should see these lines:
```
Startup service finished
Loaded: 133 plugins
Server built successfully
```

If you see these, the server is healthy and listening. Port: **43594**.

---

## Verify Port

```powershell
# Windows
Test-NetConnection localhost -Port 43594
```
```bash
# WSL/Linux
nc -zv localhost 43594
```

**Expected:** Connection succeeds.

---

## Check Listening

```bash
# See what's on the port
ss -tlnp | grep 43594
# OR
netstat -an | grep 43594
```

---

## Config Check

While server is running, verify `game-server/settings.toml`:
```toml
server_port = 43594
server_debug = false
max_players = 2048
max_bots = 10
item_definition_limit = 28473
```

---

## Boot Failures

| Symptom | Likely cause |
|---------|-------------|
| Port already in use | Previous instance still running. Kill it first. |
| Cache files missing | Check `game-server/data/cache/` has `main_file_cache.*` |
| Class not found | Recompile first: `./gradlew :game-server:classes` |
| Out of memory | Server uses 8GB heap. Check free RAM. |
| Plugin load failure | Check the plugin file at the reported path |

---

## Client Impact

Server smoke is server-only. Client is not started. But once server is up, you can connect a client to verify login works end-to-end (not covered by this prompt — see `04-systems/networking.md`).

---

## Verify

- [ ] Server logs show "Startup service finished"
- [ ] "Loaded: 133 plugins" appears
- [ ] Port 43594 is listening
- [ ] No ERROR-level log lines during boot
