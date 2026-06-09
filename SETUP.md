# Setup Guide — Tarnish Jira Fork

> Visual walkthrough for setting up the Tarnish server and client.
> **Zero experience?** Start with [README.md](README.md) for the quick version, then come here for details.
>
> *(Screenshots coming soon — the text steps below are complete and ready to follow.)*

---

## 1. Install Java 21

Download and install **JDK 21** from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=21).

- **Windows:** Run the `.msi` installer. It will add Java to your PATH automatically.
- **Verify:** Open a new terminal and run `java -version`. You should see something like:

```
openjdk version "21" 2023-09-19 LTS
```

> ❗ If you see a lower version or "not recognized", restart your terminal or add Java to PATH manually:
> 1. Search "Environment Variables" in Windows
> 2. Under "System variables", find `Path` and click Edit
> 3. Add the JDK 21 `bin` folder (e.g. `C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot\bin`)

---

## 2. Install Git

Download from [git-scm.com](https://git-scm.com/) and install. Use all default options.

---

## 3. Clone the Repository

Open a terminal (PowerShell on Windows, bash on Linux) and run:

```bash
git clone https://github.com/IBMaxin/tarnishjirafork.git
cd tarnishjirafork
```

> **IntelliJ IDEA alternative:** File → New → Project from Version Control → paste the URL.

---

## 4. Get the Cache

The cache contains all game assets. Download it from:

👉 [https://files.jire.org/cache-tarnish-218.zip](https://files.jire.org/cache-tarnish-218.zip) (~200 MB)

Extract the zip into `game-server/data/cache/`. The folder should look like:

```
tarnishjirafork/
└── game-server/
    └── data/
        └── cache/
            ├── main_file_cache.dat   (✓ ~250 MB)
            ├── main_file_cache.idx0  (✓ ~10 KB)
            ├── main_file_cache.idx1  (✓ ~1 MB)
            ├── main_file_cache.idx2  (✓ ~20 MB)
            ├── main_file_cache.idx3  (✓ or empty — not a failure)
            ├── main_file_cache.idx4  (✓ ~10 MB)
            └── main_file_cache.idx5  (✓ ~500 KB)
```

> If you're unsure, just extract the zip directly into `game-server/data/` — the `cache/` folder is inside it.

---

## 5. Build the Server

```powershell
# Windows
.\gradlew.bat :game-server:classes
```

```bash
# WSL / Linux
./gradlew :game-server:classes
```

**First run:** Gradle downloads itself and all dependencies (~3 minutes on a fast connection).  
**Subsequent runs:** Instant.

✅ **Success looks like:**
```
BUILD SUCCESSFUL in 35s
4 actionable tasks: 4 executed
```

---

## 6. Start the Server

```powershell
.\gradlew.bat :game-server:run
```

Wait 10–30 seconds. You should see:

```
Startup service finished
Loaded: 133 plugins
Server built successfully
```

The server is now running on **port 43594**. It looks like it's hung — that's normal. It's waiting for a player.

**Verify in a second terminal:**
```powershell
Test-NetConnection localhost -Port 43594
```

Expected: `TcpTestSucceeded : True`

---

## 7. (Optional) Run the Client

> ⚠️ The client requires **JDK 11** in addition to JDK 21. Install it from [Adoptium Temurin JDK 11](https://adoptium.net/temurin/releases/?version=11).

First, copy the cache to where the client expects it:

```powershell
# Windows
Copy-Item -Path "game-server\data\cache\*" -Destination "$env:USERPROFILE\.tarnish\cache" -Force
```

```bash
# WSL / Linux
mkdir -p ~/.tarnish/cache
cp game-server/data/cache/* ~/.tarnish/cache/
```

Build and run the client:

```powershell
.\gradlew.bat :game-client:classes
```

> The client opens a game window. If you see a black screen, wait a moment — it's loading assets from the cache.

---

## 🛠 IntelliJ IDEA Setup (Recommended)

1. **Open the project:** File → Open → select the `tarnishjirafork` folder
2. **JDK configuration:**
   - File → Project Structure → Project → SDK → select JDK 21
   - File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM → JDK 21
3. **Run configurations:**
   - Run → Edit Configurations → Add New → Gradle → `:game-server:run` (server)
   - Run → Edit Configurations → Add New → Gradle → `:game-client:run` (client, requires JDK 11)
4. **Run tests:** Gradle tool window → `game-server` → Tasks → verification → test

---

## 🐛 Common Problems

| Symptom | Cause | Fix |
|---------|-------|-----|
| `java: command not found` | JDK not installed or not on PATH | Install JDK 21 and restart terminal |
| `Build failed` with Java errors | Using wrong JDK version | `java -version` must show Java 21 |
| Server starts but `Startup service finished` never appears | Missing or corrupt cache | Re-download cache to `game-server/data/cache/` |
| Client shows black screen | Cache not in user directory | Copy cache to `~/.tarnish/cache/` |
| `Connection refused` | Server not running yet | Wait 30 seconds for server to finish booting |

---

## 📚 Next Steps

- [README.md](README.md) — overview and quick reference
- [docs/knowledge-bank.md](docs/knowledge-bank.md) — technical deep-dive
- [docs/workflows/](docs/workflows/) — how to add items, NPCs, commands, shops
- [AGENTS.md](AGENTS.md) — project map for developers