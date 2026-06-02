# Compile Check

**Goal:** Verify server and client compile without errors.

**Docs:** `AGENTS.md` §Quick Start, `.cursorrules` §Building, `00-cross-cutting/client-server-boundary.md`

---

## Server Compile

**Windows:** `.\gradlew.bat :game-server:classes`
**WSL/Linux:** `./gradlew :game-server:classes`

**Expected:** `BUILD SUCCESSFUL`. This proves ~1,250 Java/Kotlin source files + 133 plugins compile.

**If it fails:**
- Read the error — it tells you file:line
- Common causes: missing imports, syntax errors, type mismatches
- Check `game-server/build.gradle.kts` for dependency issues

---

## Client Compile

**Windows:** `.\gradlew.bat :game-client:classes`
**WSL/Linux:** `./gradlew :game-client:classes`

**Expected:** `BUILD SUCCESSFUL`. The client is ~1,260 Java files, JDK 11.

**If it fails:**
- Most common: JDK version mismatch. Client requires JDK 11.
- Check: `java -version`

---

## Full Build

**Windows:** `.\gradlew.bat classes`
**WSL/Linux:** `./gradlew classes`

---

## Client Impact

None. Compile is server + client independently.

---

## Verify

- `BUILD SUCCESSFUL` on both targets
- No warnings about deprecated APIs (the codebase has some, acceptable)
- `code_index.json` is auto-refreshed by the Gradle task on server compile
