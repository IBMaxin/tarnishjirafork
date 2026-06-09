@echo off
title Tarnish Client — Quick Start
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ═══════════════════════════════════════════════════════════════════════════
:: Tarnish Client — Quick Start (Windows)
::
:: Production-quality launcher. Safe to run repeatedly — skips rebuild if
:: classes are up-to-date, resolves cache intelligently, logs everything.
::
:: Usage:  quickstart-client.bat              (normal)
::         quickstart-client.bat --force      (force rebuild)
::         quickstart-client.bat --skip-build (launch without rebuilding)
:: ═══════════════════════════════════════════════════════════════════════════

set "SCRIPT_NAME=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "LOG_FILE=%SCRIPT_DIR%logs\client-quickstart.log"
set "SERVER_PORT=43594"
set "FORCE_BUILD=false"
set "SKIP_BUILD=false"
set "USE_CUSTOM_CACHE=false"

:: Parse arguments
if /i "%~1"=="--force"      set "FORCE_BUILD=true"
if /i "%~1"=="--skip-build" set "SKIP_BUILD=true"

:: ── Logging helpers ────────────────────────────────────────────────────────
if not exist "%SCRIPT_DIR%logs" mkdir "%SCRIPT_DIR%logs"
echo [%DATE% %TIME%] === %SCRIPT_NAME% started === >> "%LOG_FILE%"

call :log "INFO" "=== Tarnish Client — Quick Start ==="
call :log "INFO" "Args: %*"

:: ── Banner ─────────────────────────────────────────────────────────────────
echo ╔══════════════════════════════════════════════════════════╗
echo ║      Tarnish Client — Quick Start                       ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

:: ── Step 1: Check Java (JDK 11+) ──────────────────────────────────────────
call :log "STEP" "Checking Java..."
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    call :log "FATAL" "Java not found on PATH"
    echo.
    echo ❌ Java not found!
    echo.
    echo Tarnish Client needs Java 11 or later to run.
    echo Download and install it from:
    echo   https://adoptium.net/temurin/releases/?version=11
    echo.
    echo After installing, restart this script.
    echo.
    pause
    exit /b 1
)

:: Extract major version number
set JVER=0
set JMAJOR=0
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%g
set JVER=%JVER:"=%
for /f "delims=. tokens=1" %%v in ("%JVER%") do set JMAJOR=%%v

if %JMAJOR% LSS 11 (
    call :log "FATAL" "Java %JMAJOR% found, need 11+"
    echo.
    echo ❌ Java %JMAJOR% found, but Java 11+ is required.
    echo    Download JDK 11 or later from:
    echo      https://adoptium.net/temurin/releases/?version=11
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

:: ── Step 3: Resolve game cache ────────────────────────────────────────────
call :log "STEP" "Resolving game cache..."

set "SERVER_CACHE=%SCRIPT_DIR%game-server\data\cache"
set "CLIENT_CACHE=%USERPROFILE%\.tarnish\cache"

if exist "%SERVER_CACHE%\main_file_cache.dat" (
    call :log "OK" "Server cache found at %SERVER_CACHE%"

    if exist "%CLIENT_CACHE%\main_file_cache.dat" (
        call :log "OK" "Client cache already exists at %CLIENT_CACHE%"
        echo    ✅ Client cache found at %CLIENT_CACHE%
    ) else (
        call :log "INFO" "Client cache not found at %CLIENT_CACHE%, prompting user"
        echo.
        echo    Client cache not found at %CLIENT_CACHE%
        echo.
        echo    Options:
        echo      [C] Copy server cache to client location (recommended)
        echo      [P] Point client directly to server cache
        echo      [S] Skip — I'll set it up manually
        echo.
        set /p CACHE_CHOICE="    Choose [C/P/S]: "

        if /i "!CACHE_CHOICE!"=="C" (
            echo.
            echo    Copying cache to %CLIENT_CACHE% ...
            if not exist "%CLIENT_CACHE%" mkdir "%CLIENT_CACHE%"
            xcopy "%SERVER_CACHE%\*" "%CLIENT_CACHE%\" /E /Q /Y >nul 2>&1
            if !ERRORLEVEL! equ 0 (
                call :log "OK" "Cache copied to %CLIENT_CACHE%"
                echo    ✅ Cache copied successfully
            ) else (
                call :log "WARN" "Cache copy failed, will use --Dtarnish.cache.dir"
                echo    ⚠️  Copy failed, will point directly to server cache
                set "USE_CUSTOM_CACHE=true"
            )
        ) else if /i "!CACHE_CHOICE!"=="P" (
            call :log "INFO" "User chose to point to server cache directly"
            set "USE_CUSTOM_CACHE=true"
        ) else (
            call :log "WARN" "User skipped cache setup"
            echo    ⚠️  Skipping cache setup — client may fail to load assets
        )
    )
) else (
    call :log "WARN" "Server cache not found at %SERVER_CACHE%"
    echo.
    echo    ⚠️  Server cache not found at %SERVER_CACHE%
    echo.
    echo    The client needs cache files to run. Options:
    echo      1. Run quickstart.bat first to set up the server
    echo      2. Download from: https://files.jire.org/cache-tarnish-218.zip
    echo         and extract to: %SERVER_CACHE%
    echo.
    echo    Files needed: main_file_cache.dat, main_file_cache.idx0..5
)
echo.

:: ── Step 4: Build the client ──────────────────────────────────────────────
if "%SKIP_BUILD%"=="true" (
    call :log "INFO" "Skipping build (--skip-build flag)"
    echo [2/5] Skipping build (--skip-build flag)
) else (
    call :log "STEP" "Building client..."

    :: Skip build if classes are up-to-date (unless --force)
    if "%FORCE_BUILD%"=="false" (
        if exist "%SCRIPT_DIR%game-client\build\classes\java\main\com\osroyale\Client.class" (
            call :log "INFO" "Classes appear up-to-date, skipping build (use --force to rebuild)"
            echo [2/5] Classes up-to-date — skipping build (use --force to rebuild)
            goto :client_build_done
        )
    )

    if "%FORCE_BUILD%"=="true" (
        call :log "INFO" "Forcing rebuild (--force flag)"
        echo [2/5] Building client (forced rebuild)...
    ) else (
        echo [2/5] Building client (this may take a moment)...
    )

    call "%SCRIPT_DIR%gradlew.bat" :game-client:classes
    if !ERRORLEVEL! neq 0 (
        call :log "FATAL" "Client build failed with exit code !ERRORLEVEL!"
        echo.
        echo ❌ Build failed. See errors above.
        echo.
        echo Possible causes:
        echo   - Network issues (Gradle needs to download dependencies)
        echo   - Java version mismatch (client needs JDK 11 toolchain)
        echo   - Corrupted Gradle cache (try: .\gradlew.bat clean)
        echo.
        pause
        exit /b 1
    )
    call :log "OK" "Build successful"
    echo    ✅ Build successful
)
:client_build_done
echo.

:: ── Step 5: Check server reachability (advisory only) ─────────────────────
call :log "STEP" "Checking server reachability on port %SERVER_PORT%..."
powershell -NoProfile -Command "& {$tcp = New-Object System.Net.Sockets.TcpClient; try { $tcp.Connect('127.0.0.1', %SERVER_PORT%); $tcp.Close(); exit 0 } catch { exit 1 }}" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    call :log "OK" "Server is reachable on port %SERVER_PORT%"
    echo    ✅ Server is running on port %SERVER_PORT%
) else (
    call :log "WARN" "Server not reachable on port %SERVER_PORT%"
    echo    ⚠️  Server not detected on port %SERVER_PORT%
    echo       The client may fail to connect — start the server first with quickstart.bat
)
echo.

:: ── Step 6: Launch the client ─────────────────────────────────────────────
call :log "STEP" "Launching client..."
echo ╔══════════════════════════════════════════════════════════╗
echo ║  Client is starting...                                   ║
echo ║                                                          ║
echo ║  A game window should appear. If it doesn't, check       ║
echo ║  the terminal output for errors.                         ║
echo ║                                                          ║
echo ║  Close the game window to exit.                          ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

if "%USE_CUSTOM_CACHE%"=="true" (
    call :log "INFO" "Launching with -Dtarnish.cache.dir=%SERVER_CACHE%"
    echo    Using server cache: %SERVER_CACHE%
    echo.
    call "%SCRIPT_DIR%gradlew.bat" :game-client:run "-Dtarnish.cache.dir=%SERVER_CACHE%" >> "%LOG_FILE%" 2>&1
) else (
    call :log "INFO" "Launching: .\gradlew.bat :game-client:run"
    call "%SCRIPT_DIR%gradlew.bat" :game-client:run >> "%LOG_FILE%" 2>&1
)

:: Capture exit code
set "EXIT_CODE=!ERRORLEVEL!"
call :log "INFO" "Client process exited with code !EXIT_CODE!"
echo.
echo Client exited with code !EXIT_CODE!.
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