# Test Suite

**Goal:** Run all existing tests and verify they pass. Add new tests for changed code.

**Docs:** `AGENTS.md` §Testing, `docs/game-scope.md`

---

## Run Tests

**Windows:** `.\gradlew.bat :game-server:test`
**WSL/Linux:** `./gradlew :game-server:test`

**Current tests:** `game-server/src/test/java/com/osroyale/ProfileRightsTest.java`

```java
// The only test file — 3 test methods
@Test public void zezimaIsOwner()       // Zezima.json → OWNER
@Test public void oakIsAdministrator()  // Oak.json → ADMINISTRATOR, not OWNER
@Test public void worldProfileListMatchesSavedRights()
```

---

## Adding a Test

Place in `game-server/src/test/java/com/osroyale/`. Use JUnit 4 (`org.junit.Test`).

**Pattern:**
```java
package com.osroyale;

import org.junit.Test;
import static org.junit.Assert.*;

public class NewTest {
    @Test
    public void testName() {
        // Arrange — set up data/files
        // Act — call the code
        // Assert — verify result
        assertEquals(expected, actual);
    }
}
```

**Examples of what to test:**
- JSON data files parse correctly
- Item IDs in stores.json reference known items in item_definitions.json
- NPC spawn IDs reference known NPCs in npc_definitions.json
- Command permissions: ADMINISTRATOR can't call OWNER commands
- Skill action calculations

---

## Test-able vs Not

| Testable offline | Needs in-game |
|-----------------|---------------|
| JSON parse validity | Login flow |
| Profile rights | Combat damage |
| Data cross-references | Movement/pathing |
| Config value ranges | Shop transactions |
| Rank permission boundaries | Skill XP gain |

---

## Client Impact

None. Tests are server-only (JUnit 4, runs in Gradle test task).

---

## Verify

- `BUILD SUCCESSFUL` from `./gradlew :game-server:test`
- All 3+ tests pass
- New tests are in `game-server/src/test/java/`
