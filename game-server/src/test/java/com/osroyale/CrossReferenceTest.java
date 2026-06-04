package com.osroyale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        JsonArray spawns;
        try (var reader = new FileReader("data/def/npc/npc_spawns.json")) {
            spawns = JsonParser.parseReader(reader).getAsJsonArray();
        }

        List<String> errors = new ArrayList<>();
        for (JsonElement sElem : spawns) {
            JsonObject spawn = sElem.getAsJsonObject();
            int id = spawn.get("id").getAsInt();

            if (!knownNpcIds.contains(id)) {
                errors.add("NPC spawn references unknown NPC ID " + id
                        + " at position " + spawn.getAsJsonObject("position"));
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
        JsonArray dropTables;
        try (var reader = new FileReader("data/def/npc/npc_drops.json")) {
            dropTables = JsonParser.parseReader(reader).getAsJsonArray();
        }

        List<String> errors = new ArrayList<>();
        Set<Integer> checkedDropTables = new HashSet<>();

        for (JsonElement dtElem : dropTables) {
            JsonObject dropTable = dtElem.getAsJsonObject();
            JsonArray npcIds = dropTable.getAsJsonArray("id");
            JsonArray drops  = dropTable.getAsJsonArray("drops");

            // Record which drop tables we've checked (dedup by NPC id set)
            int thisTableId = npcIds.get(0).getAsInt();
            if (!checkedDropTables.add(thisTableId)) continue;

            // Validate each NPC ID in the drop table
            for (JsonElement nidElem : npcIds) {
                int npcId = nidElem.getAsInt();
                if (!knownNpcIds.contains(npcId)) {
                    errors.add("NPC drop table references unknown NPC ID " + npcId);
                }
            }

            // Validate each drop entry
            for (JsonElement dElem : drops) {
                JsonObject drop = dElem.getAsJsonObject();
                // Drops inconsistently use "item" or "id" as the field name
                int itemId;
                if (drop.has("item")) {
                    itemId = drop.get("item").getAsInt();
                } else if (drop.has("id")) {
                    itemId = drop.get("id").getAsInt();
                } else {
                    errors.add("NPC drop table " + thisTableId
                            + " has a drop entry with neither 'item' nor 'id' field: " + drop);
                    continue;
                }
                if (!knownItemIds.contains(itemId)) {
                    errors.add("NPC drop table " + thisTableId
                            + " references unknown item ID " + itemId);
                }
                int minimum = drop.get("minimum").getAsInt();
                int maximum = drop.get("maximum").getAsInt();
                if (minimum < 0) {
                    errors.add("NPC drop table " + thisTableId
                            + " item " + itemId + " has negative minimum: " + minimum);
                }
                if (maximum < 0) {
                    errors.add("NPC drop table " + thisTableId
                            + " item " + itemId + " has negative maximum: " + maximum);
                }
                if (minimum > maximum) {
                    errors.add("NPC drop table " + thisTableId
                            + " item " + itemId + " has min(" + minimum + ") > max(" + maximum + ")");
                }
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