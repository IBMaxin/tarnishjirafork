#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
# Tarnish Server — Quick Start (Linux / WSL)
#
# Checks prerequisites, builds the server, copies the cache,
# and starts the server.
#
# Usage:  bash quickstart.sh
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo "╔══════════════════════════════════════════════════════════╗"
echo "║        Tarnish Server — Quick Start                     ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Check Java ──────────────────────────────────────
echo "[1/4] Checking Java..."

if ! command -v java &>/dev/null; then
    echo ""
    echo "❌ Java not found!"
    echo ""
    echo "Tarnish needs Java 21 (JDK) to run."
    echo "Download and install it from:"
    echo "  https://adoptium.net/temurin/releases/?version=21"
    echo ""
    exit 1
fi

JVER=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
JMAJOR=$(echo "$JVER" | cut -d'.' -f1)

# Handle versions like "21.0.1" vs "1.8.0"
if [ "$JMAJOR" -eq 1 ]; then
    JMAJOR=$(echo "$JVER" | cut -d'.' -f2)
fi

if [ "$JMAJOR" -lt 17 ]; then
    echo ""
    echo "❌ Java $JMAJOR found, but Java 17+ is required."
    echo "   Download JDK 21 from:"
    echo "     https://adoptium.net/temurin/releases/?version=21"
    echo ""
    exit 1
fi

echo "    ✅ Java $JVER found"
echo ""

# ── Step 2: Check cache ─────────────────────────────────────
echo "[2/4] Checking game cache..."

if [ ! -f "game-server/data/cache/main_file_cache.dat" ]; then
    echo ""
    echo "⚠️  Cache not found in game-server/data/cache/"
    echo ""
    echo "Download the cache from:"
    echo "  https://files.jire.org/cache-tarnish-218.zip"
    echo ""
    echo "Extract the zip so that the files are in:"
    echo "  game-server/data/cache/"
    echo ""
    echo "Files should include: main_file_cache.dat, main_file_cache.idx0..5"
    echo ""
    exit 1
fi

echo "    ✅ Cache files found"
echo ""

# ── Step 3: Build the server ────────────────────────────────
echo "[3/4] Building server (this may take a moment)..."

if ./gradlew :game-server:classes 2>&1; then
    echo "    ✅ Build successful"
    echo ""
else
    echo "    ❌ Build failed. See errors above."
    exit 1
fi

# ── Step 4: Start the server ────────────────────────────────
echo "[4/4] Starting server..."
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Server is starting. Wait for:                           ║"
echo "║    \"Startup service finished\"                           ║"
echo "║    \"Loaded: 133 plugins\"                                ║"
echo "║    \"Server built successfully\"                          ║"
echo "║                                                          ║"
echo "║  Then open a SECOND terminal and check:                  ║"
echo "║    nc -zv localhost 43594                                ║"
echo "║                                                          ║"
echo "║  Press Ctrl+C to stop the server.                        ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

./gradlew :game-server:run