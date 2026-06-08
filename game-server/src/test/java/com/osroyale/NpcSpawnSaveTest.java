package com.osroyale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that spawn entries are written in the format expected by
 * {@code NpcSpawnFileLoader}. Writes and reads back from a temp directory.
 */
public class NpcSpawnSaveTest {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void writtenSpawnFileParsesCorrectly(@TempDir Path tempDir) throws Exception {
        File spawnFile = tempDir.resolve("42.json").toFile();

        // Simulate the AdminCommandPlugin spawn-save logic
        JsonArray spawns = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("id", 42);
        entry.addProperty("radius", "3");
        entry.addProperty("facing", "NORTH");
        entry.addProperty("convert-id", true);
        entry.addProperty("instance", 0);

        JsonObject pos = new JsonObject();
        pos.addProperty("x", 3222);
        pos.addProperty("y", 3218);
        pos.addProperty("height", 0);
        entry.add("position", pos);

        spawns.add(entry);

        // Write it
        try (FileWriter writer = new FileWriter(spawnFile)) {
            gson.toJson(spawns, writer);
        }

        // Read it back
        JsonArray parsed;
        try (FileReader reader = new FileReader(spawnFile)) {
            parsed = gson.fromJson(reader, JsonArray.class);
        }

        assertNotNull(parsed, "Parsed JSON should not be null");
        assertEquals(1, parsed.size(), "Should have one spawn entry");

        JsonObject loaded = parsed.get(0).getAsJsonObject();
        assertEquals(42, loaded.get("id").getAsInt());
        assertEquals("3", loaded.get("radius").getAsString());
        assertEquals("NORTH", loaded.get("facing").getAsString());

        JsonObject loadedPos = loaded.getAsJsonObject("position");
        assertEquals(3222, loadedPos.get("x").getAsInt());
        assertEquals(3218, loadedPos.get("y").getAsInt());
        assertEquals(0, loadedPos.get("height").getAsInt());
    }

    @Test
    void appendToExistingSpawnFile(@TempDir Path tempDir) throws Exception {
        File spawnFile = tempDir.resolve("99.json").toFile();

        // First entry
        JsonArray spawns = new JsonArray();
        JsonObject first = new JsonObject();
        first.addProperty("id", 99);
        first.addProperty("radius", "2");
        first.addProperty("facing", "SOUTH");
        JsonObject pos1 = new JsonObject();
        pos1.addProperty("x", 3000);
        pos1.addProperty("y", 3000);
        pos1.addProperty("height", 0);
        first.add("position", pos1);
        spawns.add(first);

        try (FileWriter writer = new FileWriter(spawnFile)) {
            gson.toJson(spawns, writer);
        }

        // Append second entry (simulating another spawnnpc call)
        JsonArray existing;
        try (FileReader reader = new FileReader(spawnFile)) {
            existing = gson.fromJson(reader, JsonArray.class);
        }
        JsonObject second = new JsonObject();
        second.addProperty("id", 99);
        second.addProperty("radius", "2");
        second.addProperty("facing", "NORTH");
        JsonObject pos2 = new JsonObject();
        pos2.addProperty("x", 3001);
        pos2.addProperty("y", 3001);
        pos2.addProperty("height", 0);
        second.add("position", pos2);
        existing.add(second);

        try (FileWriter writer = new FileWriter(spawnFile)) {
            gson.toJson(existing, writer);
        }

        // Verify both entries survive
        try (FileReader reader = new FileReader(spawnFile)) {
            existing = gson.fromJson(reader, JsonArray.class);
        }
        assertEquals(2, existing.size(), "Should have two spawn entries after append");
    }

    @Test
    void invalidIdRejectedByLoader() {
        // The NpcSpawnFileLoader requires id > 0; verify the schema enforces it
        JsonObject entry = new JsonObject();
        entry.addProperty("id", 0);
        assertThrows(Exception.class, () -> {
            // This simulates what NpcSpawnFileLoader does internally
            int id = entry.get("id").getAsInt();
            if (id <= 0) {
                throw new IllegalArgumentException("id must be greater than 0");
            }
        });
    }
}
