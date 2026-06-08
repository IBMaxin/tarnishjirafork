package com.osroyale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Cross-references game definition IDs to detect orphan references.
 * <p>
 * This is a <b>Tier 1</b> (safe) test: it parses JSON directly with Gson and
 * builds {@code Set<Integer>} of known IDs. No parser classes are loaded,
 * no global static state is mutated.
 * </p>
 * <p>
 * Validates:
 * <ul>
 *   <li>Every store item ID exists in item definitions</li>
 *   <li>Every NPC spawn references a known NPC ID</li>
 *   <li>Every NPC drop (NPC ID + item ID) references known definitions</li>
 *   <li>Item amounts, drop counts, and prices are non-negative where required</li>
 * </ul>
 * </p>
 */
public final class CrossReferenceTest {

    private static Set<Integer> knownItemIds;
    private static Set<Integer> knownNpcIds;

    @BeforeAll
    static void loadDefinitions() throws IOException {
        knownItemIds = loadIdSet("data/def/item/item_definitions.json");
        knownNpcIds  = loadIdSet("data/def/npc/npc_definitions.json");
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

    // ── Store references ────────────────────────────────────────────────

    @Test
    public void storeItemsReferenceValidItemIds() throws IOException {
        JsonArray stores;
        try (var reader = new FileReader("data/def/store/stores.json")) {
            stores = JsonParser.parseReader(reader).getAsJsonArray();
        }

        List<String> errors = new ArrayList<>();
        for (JsonElement sElem : stores) {
            JsonObject store = sElem.getAsJsonObject();
            String name = store.get("name").getAsString();
            JsonArray items = store.getAsJsonArray("items");

            for (JsonElement iElem : items) {
                JsonObject item = iElem.getAsJsonObject();
                int id = item.get("id").getAsInt();
                if (!knownItemIds.contains(id)) {
                    errors.add("Store '" + name + "' references unknown item ID " + id);
                }
                // Validate non-negative amounts and values
                if (item.has("amount")) {
                    int amount = item.get("amount").getAsInt();
                    if (amount < 0) {
                        errors.add("Store '" + name + "' item " + id + " has negative amount: " + amount);
                    }
                }
                if (item.has("value")) {
                    int value = item.get("value").getAsInt();
                    if (value < 0) {
                        errors.add("Store '" + name + "' item " + id + " has negative value: " + value);
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            fail("Store cross-reference errors (" + errors.size() + "):\n  "
                    + String.join("\n  ", errors));
        }
    }

    // ── NPC spawn references ────────────────────────────────────────────

    @Test
    public void npcSpawnsReferenceValidNpcIds() throws IOException {
        List<String> errors = new ArrayList<>();
        File spawnDir = new File("data/def/npc-spawns-json");
        File[] spawnFiles = spawnDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (spawnFiles == null || spawnFiles.length == 0) {
            fail("No NPC spawn files found in " + spawnDir);
        }

        for (File file : spawnFiles) {
            try (var reader = new FileReader(file)) {
                JsonArray entries = JsonParser.parseReader(reader).getAsJsonArray();
                for (JsonElement eElem : entries) {
                    JsonObject entry = eElem.getAsJsonObject();
                    int id = entry.get("id").getAsInt();
                    if (!knownNpcIds.contains(id)) {
                        errors.add("NPC spawn references unknown NPC ID " + id
                                + " in " + file.getName() + " at position "
                                + entry.getAsJsonObject("position"));
                    }
                }
            } catch (Exception e) {
                errors.add("Failed to parse " + file.getName() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            fail("NPC spawn cross-reference errors (" + errors.size() + "):\n  "
                    + String.join("\n  ", errors));
        }
    }

    // ── NPC drop references ─────────────────────────────────────────────

    @Test
    public void npcDropsReferenceValidIds() throws IOException {
        List<String> errors = new ArrayList<>();
        File dropDir = new File("data/def/npc-drops-json");
        File[] dropFiles = dropDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (dropFiles == null || dropFiles.length == 0) {
            fail("No NPC drop files found in " + dropDir);
        }

        for (File file : dropFiles) {
            try (var reader = new FileReader(file)) {
                JsonObject dropTable = JsonParser.parseReader(reader).getAsJsonObject();
                int npcId = dropTable.get("npc_id").getAsInt();

                if (!knownNpcIds.contains(npcId)) {
                    errors.add("NPC drop table references unknown NPC ID " + npcId
                            + " in " + file.getName());
                }

                JsonArray drops = dropTable.getAsJsonArray("drops");
                for (JsonElement dElem : drops) {
                    JsonObject drop = dElem.getAsJsonObject();
                    int itemId = drop.has("item") ? drop.get("item").getAsInt()
                            : drop.get("id").getAsInt();
                    if (!knownItemIds.contains(itemId)) {
                        errors.add("NPC drop table " + npcId
                                + " references unknown item ID " + itemId
                                + " in " + file.getName());
                    }
                    int minimum = drop.get("minimum").getAsInt();
                    int maximum = drop.get("maximum").getAsInt();
                    if (minimum < 0) {
                        errors.add("NPC drop table " + npcId + " item " + itemId
                                + " has negative minimum: " + minimum);
                    }
                    if (maximum < 0) {
                        errors.add("NPC drop table " + npcId + " item " + itemId
                                + " has negative maximum: " + maximum);
                    }
                    if (minimum > maximum) {
                        errors.add("NPC drop table " + npcId + " item " + itemId
                                + " has min(" + minimum + ") > max(" + maximum + ")");
                    }
                }
            } catch (Exception e) {
                errors.add("Failed to parse " + file.getName() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            fail("NPC drop cross-reference errors (" + errors.size() + "):\n  "
                    + String.join("\n  ", errors));
        }
    }

    // ── Equipment references ────────────────────────────────────────────

    @Test
    public void equipmentReferencesValidItemIds() throws IOException {
        JsonArray equipment;
        try (var reader = new FileReader("data/def/equipment/equipment_definitions.json")) {
            equipment = JsonParser.parseReader(reader).getAsJsonArray();
        }

        List<String> errors = new ArrayList<>();
        for (JsonElement eElem : equipment) {
            JsonObject eq = eElem.getAsJsonObject();
            int id = eq.get("id").getAsInt();

            if (!knownItemIds.contains(id)) {
                errors.add("Equipment definition references unknown item ID " + id);
            }

            // Validate bonuses array (should have 14 elements if present)
            if (eq.has("bonuses")) {
                JsonArray bonuses = eq.getAsJsonArray("bonuses");
                if (bonuses.size() != 14) {
                    errors.add("Equipment item " + id + " has " + bonuses.size()
                            + " bonuses (expected 14)");
                }
            }
        }

        if (!errors.isEmpty()) {
            fail("Equipment cross-reference errors (" + errors.size() + "):\n  "
                    + String.join("\n  ", errors));
        }
    }
}