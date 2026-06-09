@echo off
title Tarnish Server — Quick Start
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ═══════════════════════════════════════════════════════════════════════════
:: Tarnish Server — Quick Start (Windows)
::
:: Production-quality launcher. Safe to run repeatedly — skips rebuild if
:: classes are up-to-date, detects port conflicts, logs everything.
::
:: Usage:  quickstart.bat              (normal)
::         quickstart.bat --force      (force rebuild)
::         quickstart.bat --skip-build (start without rebuilding)
:: ═══════════════════════════════════════════════════════════════════════════

set "SCRIPT_NAME=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "LOG_FILE=%SCRIPT_DIR%logs\server-quickstart.log"
set "SERVER_PORT=43594"
set "FORCE_BUILD=false"
set "SKIP_BUILD=false"

:: Parse arguments
if /i "%~1"=="--force"    set "FORCE_BUILD=true"
if /i "%~1"=="--skip-build" set "SKIP_BUILD=true"

:: ── Logging helpers ────────────────────────────────────────────────────────
if not exist "%SCRIPT_DIR%logs" mkdir "%SCRIPT_DIR%logs"
echo [%DATE% %TIME%] === %SCRIPT_NAME% started === >> "%LOG_FILE%"

call :log "INFO" "=== Tarnish Server — Quick Start ==="
call :log "INFO" "Args: %*"

:: ── Step 1: Check Java ────────────────────────────────────────────────────
call :log "STEP" "Checking Java..."
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    call :log "FATAL" "Java not found on PATH"
    echo.
    echo ❌ Java not found!
    echo.
    echo Tarnish needs Java 21 (JDK) to run.
    echo Download and install it from:
    echo   https://adoptium.net/temurin/releases/?version=21
    echo.
    echo After installing, restart this script.
    echo.
    pause
    exit /b 1
)

:: Extract major version
set JVER=0
set JMAJOR=0
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%g
set JVER=%JVER:"=%
for /f "delims=. tokens=1" %%v in ("%JVER%") do set JMAJOR=%%v

if %JMAJOR% LSS 17 (
    call :log "FATAL" "Java %JMAJOR% found, need 17+"
    echo.
    echo ❌ Java %JMAJOR% found, but Java 17+ is required.
    echo    Download JDK 21 from:
    echo      https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)

call :log "OK" "Java %JVER% found (major: %JMAJOR%)"
echo    ✅ Java %JVER% found
echo.

:: ── Step 2: Check Gradle wrapper ──────────────────────────────────────────
call :log "STEP" "Checking Gradle wrapper..."
if not exist "%SCRIPT_DIR%gradlew.bat" (
    call :log "FATAL" "gradlew.bat not found at %SCRIPT_DIR%"
    echo ❌ gradlew.bat not found! Are you in the project root?
    pause
    exit /b 1
)
call :log "OK" "gradlew.bat found"
echo    ✅ Gradle wrapper found
echo.

:: ── Step 3: Check cache ───────────────────────────────────────────────────
call :log "STEP" "Checking game cache..."
if not exist "%SCRIPT_DIR%game-server\data\cache\main_file_cache.dat" (
    call :log "WARN" "Cache not found"
    echo.
    echo ⚠️  Cache not found in game-server\data\cache\
    echo.
    echo Download the cache from:
    echo   https://files.jire.org/cache-tarnish-218.zip
    echo.
    echo Extract the zip so that the files are in:
    echo   game-server\data\cache\
    echo.
    echo Files should include: main_file_cache.dat, main_file_cache.idx0..5
    echo.
    pause
    exit /b 1
)
call :log "OK" "Cache files found"
echo    ✅ Cache files found
echo.

:: ── Step 4: Check port conflict ───────────────────────────────────────────
call :log "STEP" "Checking port %SERVER_PORT%..."
powershell -NoProfile -Command "& {$tcp = New-Object System.Net.Sockets.TcpClient; try { $tcp.Connect('127.0.0.1', %SERVER_PORT%); $tcp.Close(); exit 0 } catch { exit 1 }}" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    call :log "FATAL" "Port %SERVER_PORT% is already in use — server may already be running"
    echo.
    echo ❌ Port %SERVER_PORT% is already in use!
    echo.
    echo The server (or another process) is already listening on that port.
    echo If the server is already running, you don't need to start it again.
    echo If it crashed, wait a moment or check Task Manager for lingering java.exe.
    echo.
    pause
    exit /b 1
)
call :log "OK" "Port %SERVER_PORT% is free"
echo    ✅ Port %SERVER_PORT% is available
echo.

:: ── Step 5: Build the server ──────────────────────────────────────────────
if "%SKIP_BUILD%"=="true" (
    call :log "INFO" "Skipping build (--skip-build flag)"
    echo [3/5] Skipping build (--skip-build flag)
) else (
    call :log "STEP" "Building server..."

    :: Check if build is already up-to-date (unless --force)
    if "%FORCE_BUILD%"=="false" (
        if exist "%SCRIPT_DIR%game-server\build\classes\java\main\com\osroyale\GameServer.class" (
            call :log "INFO" "Classes appear up-to-date, skipping build (use --force to rebuild)"
            echo [3/5] Classes up-to-date — skipping build (use --force to rebuild)
            goto :build_done
        )
    )

    if "%FORCE_BUILD%"=="true" (
        call :log "INFO" "Forcing rebuild (--force flag)"
        echo [3/5] Building server (forced rebuild)...
    ) else (
        echo [3/5] Building server (this may take a moment)...
    )

    call .\gradlew.bat :game-server:classes
    if !ERRORLEVEL! neq 0 (
        call :log "FATAL" "Build failed with exit code !ERRORLEVEL!"
        echo.
        echo ❌ Build failed. See errors above.
        echo.
        echo Possible causes:
        echo   - Network issues (Gradle needs to download dependencies)
        echo   - Java version mismatch (server needs JDK 21)
        echo   - Corrupted Gradle cache (try: .\gradlew.bat clean)
        echo.
        pause
        exit /b 1
    )
    call :log "OK" "Build successful"
    echo    ✅ Build successful
)
:build_done
echo.

:: ── Step 6: Start the server ──────────────────────────────────────────────
call :log "STEP" "Starting server..."
echo [4/5] Starting server...
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║  Server is starting. Wait for:                           ║
echo ║    "Startup service finished"                            ║
echo ║    "Loaded: 133 plugins"                                 ║
echo ║    "Server built successfully"                           ║
echo ║                                                          ║
echo ║  Then open a SECOND terminal and connect with the        ║
echo ║  client, or use:                                         ║
echo ║    Test-NetConnection localhost -Port 43594              ║
echo ║                                                          ║
echo ║  Press Ctrl+C to stop the server.                        ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

call :log "INFO" "Launching: .\gradlew.bat :game-server:run"
echo [%DATE% %TIME%] Server output below — also logged to %LOG_FILE% >> "%LOG_FILE%"

:: Run the server — tee output to log file
.\gradlew.bat :game-server:run >> "%LOG_FILE%" 2>&1

:: Capture exit code
set "EXIT_CODE=!ERRORLEVEL!"
call :log "INFO" "Server process exited with code !EXIT_CODE!"
echo.
echo Server process exited with code !EXIT_CODE!.
echo Full log available at: %LOG_FILE%
pause
exit /b !EXIT_CODE!

:: ═══════════════════════════════════════════════════════════════════════════
:: Logging subroutine
::
:: Usage: call :log LEVEL "message"
::   LEVEL = INFO, STEP, OK, WARN, FATAL
:: ═══════════════════════════════════════════════════════════════════════════
:log
set "_LEVEL=%~1"
set "_MSG=%~2"
set "_TIMESTAMP=%DATE% %TIME%"
echo [%_TIMESTAMP%] [%_LEVEL%] %_MSG% >> "%LOG_FILE%"
goto :eof