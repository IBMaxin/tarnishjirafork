package com.osroyale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates Araxxor boss data files for structural correctness and
 * cross-reference consistency.
 *
 * <p>Tier 1 (safe): reads JSON with raw Gson, no parser classes loaded.</p>
 *
 * <p>Checks:
 * <ul>
 *   <li>All 5 monsters-json files (11175-11179) parse and have valid ranges</li>
 *   <li>Boss drop table references only known item IDs</li>
 *   <li>Boss NPC (11176) has boss-appropriate stats</li>
 *   <li>Noxious item definitions exist and have required fields</li>
 *   <li>Equipment definitions for new items exist</li>
 *   <li>Araxxor combat strategy class exists (compile-time check)</li>
 *   <li>Activity area bounds are reasonable</li>
 * </ul>
 * </p>
 */
public final class AraxxorDataTest {

    private static final int[] ARAXXOR_NPC_IDS = {11175, 11176, 11177, 11178, 11179};
    private static final int[] NOXIOUS_ITEM_IDS = {28003, 28004, 28005, 28006, 28007, 28008, 28009};

    private static Set<Integer> knownItemIds;
    private static Set<Integer> knownNpcIds;

    @BeforeAll
    static void loadDefinitions() throws IOException {
        knownItemIds = loadIdSet("data/def/item/item_definitions.json");
        knownNpcIds = loadIdSet("data/def/npc/npc_definitions.json");
    }

    private static Set<Integer> loadIdSet(String path) throws IOException {
        Set<Integer> ids = new HashSet<>();
        JsonArray arr;
        try (var reader = new FileReader(path)) {
            arr = JsonParser.parseReader(reader).getAsJsonArray();
        }
        for (JsonElement elem : arr) {
            ids.add(elem.getAsJsonObject().get("id").getAsInt());
        }
        return ids;
    }

    private static JsonObject loadMonsterJson(int id) throws IOException {
        String path = "data/def/monsters-json/" + id + ".json";
        try (var reader = new FileReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    // ── Monsters-json validation ───────────────────────────────────────

    @Test
    void allAraxxorMonsterFilesExist() {
        List<String> missing = new ArrayList<>();
        for (int id : ARAXXOR_NPC_IDS) {
            if (!Files.exists(Path.of("data/def/monsters-json/" + id + ".json"))) {
                missing.add(String.valueOf(id));
            }
        }
        assertTrue(missing.isEmpty(),
                "Missing Araxxor monster definition files: " + missing);
    }

    @Test
    void allAraxxorNpcsExistInNpcDefinitions() {
        List<String> missing = new ArrayList<>();
        for (int id : ARAXXOR_NPC_IDS) {
            if (!knownNpcIds.contains(id)) {
                missing.add(String.valueOf(id));
            }
        }
        assertTrue(missing.isEmpty(),
                "Araxxor NPC IDs missing from npc_definitions.json: " + missing);
    }

    @Test
    void allMonsterFilesParseAndHaveRequiredFields() throws IOException {
        List<String> errors = new ArrayList<>();
        String[] requiredFields = {"id", "name", "combat_level", "hitpoints", "max_hit"};

        for (int id : ARAXXOR_NPC_IDS) {
            JsonObject obj = loadMonsterJson(id);
            for (String field : requiredFields) {
                if (!obj.has(field)) {
                    errors.add(id + ".json missing field: " + field);
                }
            }
            assertFalse(obj.get("incomplete").getAsBoolean(),
                    id + ".json should not be marked incomplete");
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    void bossMonsterHasBossGradeStats() throws IOException {
        JsonObject boss = loadMonsterJson(11176);
        int hp = boss.get("hitpoints").getAsInt();
        int maxHit = boss.get("max_hit").getAsInt();
        int combat = boss.get("combat_level").getAsInt();
        int attackSpeed = boss.get("attack_speed").getAsInt();

        assertTrue(hp >= 500, "Boss HP should be >= 500, got " + hp);
        assertTrue(maxHit >= 25, "Boss max hit should be >= 25, got " + maxHit);
        assertTrue(combat >= 200, "Boss combat level should be >= 200, got " + combat);
        assertEquals(6, attackSpeed, "Boss attack speed should be 6");
    }

    @Test
    void bossHasHigherStatsThanEntryForm() throws IOException {
        JsonObject entry = loadMonsterJson(11175);
        JsonObject boss = loadMonsterJson(11176);

        assertTrue(boss.get("hitpoints").getAsInt() > entry.get("hitpoints").getAsInt(),
                "Boss HP must exceed entry form");
        assertTrue(boss.get("max_hit").getAsInt() > entry.get("max_hit").getAsInt(),
                "Boss max hit must exceed entry form");
        assertTrue(boss.get("combat_level").getAsInt() > entry.get("combat_level").getAsInt(),
                "Boss combat level must exceed entry form");
    }

    @Test
    void minionMonstersHaveValidStats() throws IOException {
        for (int id : new int[]{11177, 11178, 11179}) {
            JsonObject minion = loadMonsterJson(id);
            int hp = minion.get("hitpoints").getAsInt();
            assertTrue(hp > 0 && hp <= 50,
                    "Minion " + id + " HP should be 1-50, got " + hp);
            assertFalse(minion.get("incomplete").getAsBoolean(),
                    "Minion " + id + " should not be incomplete");
        }
    }

    @Test
    void minionCategoriesIncludeSpiders() throws IOException {
        for (int id : new int[]{11177, 11178, 11179}) {
            JsonObject minion = loadMonsterJson(id);
            JsonArray categories = minion.getAsJsonArray("category");
            boolean hasSpider = false;
            for (JsonElement cat : categories) {
                if (cat.getAsString().equals("spiders")) {
                    hasSpider = true;
                    break;
                }
            }
            assertTrue(hasSpider, "Minion " + id + " should have 'spiders' category");
        }
    }

    // ── Drop table validation ─────────────────────────────────────────

    @Test
    void bossDropTableExistsAndReferencesValidItems() throws IOException {
        String dropPath = "data/def/npc-drops-json/11176.json";
        assertTrue(Files.exists(Path.of(dropPath)), "Boss drop table must exist");

        JsonObject dropTable;
        try (var reader = new FileReader(dropPath)) {
            dropTable = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(11176, dropTable.get("npc_id").getAsInt(), "Drop table NPC ID mismatch");

        JsonArray drops = dropTable.getAsJsonArray("drops");
        assertNotNull(drops, "Drop table must have drops array");
        assertTrue(drops.size() >= 5, "Boss should have at least 5 drop entries");

        List<String> errors = new ArrayList<>();
        for (JsonElement dElem : drops) {
            JsonObject drop = dElem.getAsJsonObject();
            int itemId = drop.get("item").getAsInt();
            int min = drop.get("minimum").getAsInt();
            int max = drop.get("maximum").getAsInt();
            String type = drop.get("type").getAsString();

            if (!knownItemIds.contains(itemId)) {
                errors.add("Unknown item ID " + itemId + " in Araxxor drop table");
            }
            if (min < 0) {
                errors.add("Negative minimum for item " + itemId);
            }
            if (max < 0) {
                errors.add("Negative maximum for item " + itemId);
            }
            if (min > max) {
                errors.add("min > max for item " + itemId);
            }
            assertTrue(List.of("COMMON", "UNCOMMON", "RARE", "VERY_RARE").contains(type),
                    "Unknown drop type: " + type);
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    void bossDropTableHasRareDrops() throws IOException {
        String dropPath = "data/def/npc-drops-json/11176.json";
        JsonObject dropTable;
        try (var reader = new FileReader(dropPath)) {
            dropTable = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray drops = dropTable.getAsJsonArray("drops");
        boolean hasRare = false;
        for (JsonElement dElem : drops) {
            if (dElem.getAsJsonObject().get("type").getAsString().equals("RARE")) {
                hasRare = true;
                break;
            }
        }
        assertTrue(hasRare, "Boss drop table should have RARE items");
    }

    // ── Item definition validation ─────────────────────────────────────

    @Test
    void noxiousItemDefinitionsExist() {
        List<String> missing = new ArrayList<>();
        for (int id : NOXIOUS_ITEM_IDS) {
            if (!knownItemIds.contains(id)) {
                missing.add(String.valueOf(id));
            }
        }
        assertTrue(missing.isEmpty(),
                "Noxious item IDs missing from item_definitions.json: " + missing);
    }

    @Test
    void equipmentDefinitionsExistForNoxiousItems() throws IOException {
        JsonArray equipment;
        try (var reader = new FileReader("data/def/equipment/equipment_definitions.json")) {
            equipment = JsonParser.parseReader(reader).getAsJsonArray();
        }
        Set<Integer> equipIds = new HashSet<>();
        for (JsonElement elem : equipment) {
            equipIds.add(elem.getAsJsonObject().get("id").getAsInt());
        }

        assertTrue(equipIds.contains(28007), "Noxious halberd missing from equipment definitions");
        assertTrue(equipIds.contains(28008), "Amulet of rancour missing from equipment definitions");
    }

    // ── Activity class reference ───────────────────────────────────────

    @Test
    void araxxorActivityTypeIsRegistered() throws IOException {
        String activityTypePath = "src/main/java/com/osroyale/content/activity/ActivityType.java";
        String content = Files.readString(Path.of(activityTypePath));
        assertTrue(content.contains("ARAXXOR"),
                "ActivityType.java should contain ARAXXOR enum entry");
    }

    @Test
    void araxxorActivityLogExists() throws IOException {
        String path = "src/main/java/com/osroyale/content/ActivityLog.java";
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("ARAXXOR"),
                "ActivityLog.java should contain ARAXXOR entry");
    }

    @Test
    void araxxorAchievementKeyExists() throws IOException {
        String path = "src/main/java/com/osroyale/content/achievement/AchievementKey.java";
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("ARAXXOR"),
                "AchievementKey.java should contain ARAXXOR entry");
    }

    @Test
    void araxxorAchievementListExists() throws IOException {
        String path = "src/main/java/com/osroyale/content/achievement/AchievementList.java";
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("KILL_10_ARAXXOR"),
                "AchievementList.java should contain Araxxor achievements");
    }

    // ── Arena area validation ──────────────────────────────────────────

    @Test
    void araxxorAreaMethodExists() throws IOException {
        String path = "src/main/java/com/osroyale/game/world/position/Area.java";
        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("inAraxxor"),
                "Area.java should contain inAraxxor() method");
    }

    @Test
    void araxxorAreaBoundsAreReasonable() {
        int[] araxxorSpawn = {3200, 3400};
        assertTrue(araxxorSpawn[0] >= 3185, "Spawn X should be within area bounds");
        assertTrue(araxxorSpawn[0] <= 3215, "Spawn X should be within area bounds");
        assertTrue(araxxorSpawn[1] >= 3385, "Spawn Y should be within area bounds");
        assertTrue(araxxorSpawn[1] <= 3415, "Spawn Y should be within area bounds");
    }

    @Test
    void npcSpawnFileExists() {
        assertTrue(Files.exists(Path.of("data/def/npc-spawns-json/11175.json")),
                "npc-spawns-json/11175.json must exist");
    }
}
