package com.osroyale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reconciliation test that compares the OLD monolithic definition files
 * against the NEW per-file definition directories.
 * <p>
 * Goal: find every gap between the two systems so we can fill them and
 * eventually move to a single per-file system.
 * </p>
 * <p>
 * <b>Tier 1 (safe):</b> reads JSON with raw Gson + walks directories.
 * No parser classes loaded, no global state mutated.
 * </p>
 * <h3>What it checks:</h3>
 * <ul>
 *   <li>Items: {@code item_definitions.json} vs {@code items-json/*.json}</li>
 *   <li>NPCs: {@code npc_definitions.json} vs {@code monsters-json/*.json}</li>
 * </ul>
 * <h3>Output:</h3>
 * <ul>
 *   <li><b>Old-only</b> IDs — in the monolithic file but missing from per-file system.
 *       These need per-file entries created.</li>
 *   <li><b>New-only</b> IDs — in per-file system but missing from old monolithic file.
 *       Already handled at runtime by merge logic, but good to know.</li>
 * </ul>
 */
public final class ReconciliationTest {

    private static final String ITEM_MONO_PATH = "data/def/item/item_definitions.json";
    private static final String ITEM_PERFILE_DIR = "data/def/items-json/";
    private static final String NPC_MONO_PATH = "data/def/npc/npc_definitions.json";
    private static final String NPC_PERFILE_DIR = "data/def/monsters-json/";

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Loads a {@code Set<Integer>} of IDs from a monolithic JSON array file. */
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

    /** Loads a {@code Set<Integer>} of IDs from a directory of {@code {id}.json} files. */
    private static Set<Integer> loadIdSetFromDir(String dirPath) throws IOException {
        return Files.walk(Paths.get(dirPath), 1)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> {
                    String name = p.getFileName().toString();
                    return name.substring(0, name.length() - 5); // strip ".json"
                })
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .boxed()
                .collect(Collectors.toSet());
    }

    /** Formats IDs for readable error output, capping at 500 to avoid OOM in test output. */
    private static String fmtDiff(Set<Integer> ids) {
        if (ids.isEmpty()) return "(none)";
        List<Integer> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        if (sorted.size() <= 100) {
            return sorted.toString();
        }
        return sorted.subList(0, 100).toString()
                + " … and " + (sorted.size() - 100) + " more";
    }

    /** Builds a sorted summary: total, range min-max, and a sample. */
    private static String summary(Set<Integer> ids, String label) {
        if (ids.isEmpty()) return label + ": 0";
        List<Integer> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        int min = sorted.get(0);
        int max = sorted.get(sorted.size() - 1);
        int count = sorted.size();
        String sample = count <= 5
                ? sorted.toString()
                : sorted.subList(0, 5).toString() + " …";
        return label + ": " + count + " (range " + min + "–" + max + ") " + sample;
    }

    // ── Item Reconciliation ──────────────────────────────────────────────

    @Test
    public void itemsMatchBetweenSystems() throws IOException {
        Set<Integer> oldIds = loadIdSet(ITEM_MONO_PATH);
        Set<Integer> newIds = loadIdSetFromDir(ITEM_PERFILE_DIR);

        Set<Integer> oldOnly = new HashSet<>(oldIds);
        oldOnly.removeAll(newIds);

        Set<Integer> newOnly = new HashSet<>(newIds);
        newOnly.removeAll(oldIds);

        System.out.println("── Item Reconciliation ──");
        System.out.println("  Monolithic file: " + ITEM_MONO_PATH + " → " + oldIds.size() + " items");
        System.out.println("  Per-file dir:    " + ITEM_PERFILE_DIR + " → " + newIds.size() + " files");
        System.out.println("  Old-only (missing from per-file): " + fmtDiff(oldOnly));
        System.out.println("  New-only (missing from old file): " + fmtDiff(newOnly));
        System.out.println(summary(oldOnly, "  Old-only summary"));
        System.out.println(summary(newOnly, "  New-only summary"));

        // The old-only IDs need per-file entries created. This test lets us
        // know when the gap has been closed to 0.
        List<String> failures = new ArrayList<>();
        if (!oldOnly.isEmpty()) {
            failures.add(oldOnly.size() + " item(s) exist in " + ITEM_MONO_PATH
                    + " but have no per-file entry in " + ITEM_PERFILE_DIR
                    + ".\n    Missing IDs: " + fmtDiff(oldOnly));
        }
        if (!newOnly.isEmpty()) {
            // New-only is informational — merge logic handles them at runtime.
            // But print it so we're aware of the drift direction.
            System.out.println("  (Note: " + newOnly.size()
                    + " item(s) only exist in per-file system — runtime merge handles this)");
        }
        // We only FAIL on oldOnly (blocking missing data). newOnly is informational.
        assertTrue(oldOnly.isEmpty(),
                "Item reconciliation failed:\n" + String.join("\n", failures));
    }

    // ── NPC Reconciliation ───────────────────────────────────────────────

    @Test
    public void npcsMatchBetweenSystems() throws IOException {
        Set<Integer> oldIds = loadIdSet(NPC_MONO_PATH);
        Set<Integer> newIds = loadIdSetFromDir(NPC_PERFILE_DIR);

        Set<Integer> oldOnly = new HashSet<>(oldIds);
        oldOnly.removeAll(newIds);

        Set<Integer> newOnly = new HashSet<>(newIds);
        newOnly.removeAll(oldIds);

        System.out.println("── NPC Reconciliation ──");
        System.out.println("  Monolithic file: " + NPC_MONO_PATH + " → " + oldIds.size() + " NPCs");
        System.out.println("  Per-file dir:    " + NPC_PERFILE_DIR + " → " + newIds.size() + " files");
        System.out.println("  Old-only (missing from per-file): " + fmtDiff(oldOnly));
        System.out.println("  New-only (missing from old file): " + fmtDiff(newOnly));
        System.out.println(summary(oldOnly, "  Old-only summary"));
        System.out.println(summary(newOnly, "  New-only summary"));

        List<String> failures = new ArrayList<>();
        if (!oldOnly.isEmpty()) {
            failures.add(oldOnly.size() + " NPC(s) exist in " + NPC_MONO_PATH
                    + " but have no per-file entry in " + NPC_PERFILE_DIR
                    + ".\n    Missing IDs: " + fmtDiff(oldOnly));
        }
        if (!newOnly.isEmpty()) {
            System.out.println("  (Note: " + newOnly.size()
                    + " NPC(s) only exist in per-file system — runtime merge handles this)");
        }
        assertTrue(oldOnly.isEmpty(),
                "NPC reconciliation failed:\n" + String.join("\n", failures));
    }
}
