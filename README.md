# Tarnish — OSRS Private Server

> A fork of the Tarnish RSPS. Single-player or small-group play.  
> **Zero experience?** Start here. Everything you need is below.

---

## 📋 What You'll Need

| Thing | Why | Where to get it |
|-------|-----|-----------------|
| **Java 21** (JDK) | Runs the server | [Adoptium Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) |
| **The cache** | Game assets (maps, models, sprites) | [cache-tarnish-218.zip](https://files.jire.org/cache-tarnish-218.zip) (~200 MB) |
| **Git** | Clone the code | [git-scm.com](https://git-scm.com/) |

> ⚠️ **Just Java 21 for now.** Both server and client use JDK 21. See the client setup section below.

---

## 🚀 Quick Start (5 minutes)

### Setup

If you prefer a visual guide, see [SETUP.md](SETUP.md) for detailed walkthroughs and screenshots.

### 1. Get the code

```bash
git clone https://github.com/IBMaxin/tarnishjirafork.git
cd tarnishjirafork
```

### 2. Get the cache

Download [cache-tarnish-218.zip](https://files.jire.org/cache-tarnish-218.zip) and extract it into:

```
game-server/data/cache/
```

### 3. Quick start (recommended)

**Windows:** Double-click `quickstart.bat` or run:
```powershell
.\quickstart.bat
```

**WSL/Linux:**
```bash
bash quickstart.sh
```

The script checks your Java version, verifies the cache, builds the server, and starts it.

You'll see output like (this may take 10–30 seconds):
```
Startup service finished
Loaded: 133 plugins
Server built successfully
```

> **It looks hung — it's not.** The server is waiting for a player to connect.  
> Port: **43594**

### 4. Verify it's running

Open a **second terminal** and check the port:

```powershell
Test-NetConnection localhost -Port 43594
```

Expected: `TcpTestSucceeded : True`

---

## 🎮 Connecting with the Client (Optional)

The client (Java 11, Swing-based) lets you walk around and play. It needs the cache files in your user directory:

```powershell
Copy-Item -Path "game-server\data\cache\*" -Destination "$env:USERPROFILE\.tarnish\cache" -Force
```

Then build and run the client:

```powershell
.\gradlew.bat :game-client:classes
```

> Client setup details: [game-client/README.md](game-client/README.md)

---

## 🧪 Running Tests

```powershell
.\gradlew.bat :game-server:test
```

Expected output: **106 tests, 0 failures** (or more if tests have been added).

---

## 🏗 Project Structure (For Developers)

```
tarnishjirafork/
├── game-server/          # The game server (JDK 21)
│   ├── src/main/java/    # Server source code
│   ├── plugins/          # Click handlers, commands, item interactions
│   └── data/             # Game definitions, cache, player saves
├── game-client/          # The game client (JDK 21)
├── docs/                 # Documentation & workflows
│   ├── knowledge-bank.md # Deep technical reference
│   └── workflows/        # Step-by-step guides (add item, NPC, shop, etc.)
├── prompts/              # AI agent prompts for audit/test/dev (35 prompts)
└── code_index.json       # Fast file lookup for AI tools
```

**Key files for new developers:**
- `AGENTS.md` — full project map and common patterns
- `game-server/plugins/plugin/command/` — command implementations by rank
- `game-server/src/main/java/com/osroyale/content/` — skills, activities, shops
- `docs/workflows/commands.md` — how to add a new command

---

## ❓ Troubleshooting

| Problem | Likely cause | Fix |
|---------|-------------|-----|
| `Java not found` | JDK not installed or not on PATH | Install JDK 21 from [Adoptium](https://adoptium.net/temurin/releases/?version=21) and restart your terminal |
| `Unsupported class file major version` | Wrong Java version | Run `java -version` — must be 21 |
| `Startup service finished` never appears | Missing cache files | Check `game-server/data/cache/` has the .dat and .idx files |
| Port 43594 not listening | Server still starting | Wait 30 seconds and try again |
| `Connection refused` | Server not running | Start the server first, then check |

---

## 🔗 Quick Links

| What | Where |
|------|-------|
| Project map | [`AGENTS.md`](AGENTS.md) |
| Technical deep-dive | [`docs/knowledge-bank.md`](docs/knowledge-bank.md) |
| Improvement plan | [`docs/improvement-plan.md`](docs/improvement-plan.md) |
| Adding content (items, NPCs, etc.) | [`docs/workflows/`](docs/workflows/) |
| AI agent prompts | [`prompts/README.md`](prompts/README.md) |