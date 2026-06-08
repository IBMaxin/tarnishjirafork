package com.osroyale;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Walks {@code data/} recursively and attempts to Gson-parse every {@code .json} file.
 * <p>
 * This is a <b>Tier 1</b> (safe) test: no parser classes are loaded, no global
 * static state is mutated. It purely validates structural JSON correctness.
 * </p>
 * <p>
 * Catches corrupted manual edits, partial writes from crashes, editor encoding
 * issues, and committed-with-errors data files.
 * </p>
 */
public final class DataJsonParseTest {

    private static final String DATA_ROOT = "data";

    @Test
    public void everyJsonFileParses() {
        List<String> failures = new ArrayList<>();
        int total = 0;

        try (Stream<Path> walk = Files.walk(Path.of(DATA_ROOT))) {
            List<Path> jsonFiles = walk
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

            for (Path file : jsonFiles) {
                try (var reader = new JsonReader(new FileReader(file.toFile()))) {
                    JsonParser.parseReader(reader);
                    total++;
                } catch (Exception e) {
                    failures.add(file.toString() + " — " + e.getMessage());
                }
            }
        } catch (Exception e) {
            fail("Failed to walk data directory: " + e.getMessage());
        }

        if (!failures.isEmpty()) {
            int failedCount = failures.size();
            // Show first 20 failures to keep output readable
            StringBuilder msg = new StringBuilder();
            msg.append(failedCount).append(" of ").append(total + failedCount)
                    .append(" JSON files failed to parse:\n");
            failures.stream().limit(20).forEach(f -> msg.append("  ").append(f).append('\n'));
            if (failures.size() > 20) {
                msg.append("  ... and ").append(failures.size() - 20).append(" more");
            }
            fail(msg.toString());
        }
    }

    @Test
    public void largeJsonFilesParse() {
        // Explicitly test the largest files — these are the most likely
        // to be corrupted by partial writes or encoding issues.
        List<String> largeFiles = List.of(
                "data/def/item/item_definitions.json",
                "data/def/npc/npc_definitions.json",
                "data/def/equipment/equipment_definitions.json",
                "data/def/store/stores.json"
        );

        List<String> failures = new ArrayList<>();
        for (String path : largeFiles) {
            try (var reader = new JsonReader(new FileReader(path))) {
                JsonParser.parseReader(reader);
            } catch (Exception e) {
                failures.add(path + " — " + e.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            fail("Large JSON files failed to parse:\n  "
                    + String.join("\n  ", failures));
        }
    }
}