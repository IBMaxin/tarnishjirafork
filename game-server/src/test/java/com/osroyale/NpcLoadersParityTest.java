package com.osroyale;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osroyale.game.world.entity.mob.npc.drop.NpcDrop;
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropManager;
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropTable;
import com.osroyale.game.world.items.ItemDefinition;
import com.osroyale.util.parser.impl.NpcDropParser;
import org.jire.tarnishps.OldToNew;
import org.jire.tarnishps.defs.NpcDropFileLoader;
import org.jire.tarnishps.defs.NpcSpawnFileLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parity test: runs old parser and new file loader, then compares output.
 * Ensures the per-file migration produces identical results.
 *
 * Requires item_definitions.json to be loaded first (drops reference items).
 */
public class NpcLoadersParityTest {

    @BeforeAll
    static void loadPrerequisites() {
        OldToNew.load();
        ItemDefinition.createParser().run();
    }

    // ──────────────────────────────────────────────
    //  DROP TABLES
    // ──────────────────────────────────────────────

    @Test
    void dropTables_produceSameKeys() {
        NpcDropManager.NPC_DROPS.clear();
        new NpcDropParser().run();
        Set<Integer> oldKeys = new TreeSet<>(NpcDropManager.NPC_DROPS.keySet());
        int oldSize = oldKeys.size();

        Map<Integer, int[]> oldDropCounts = snapshotDropCounts();

        NpcDropManager.NPC_DROPS.clear();
        NpcDropFileLoader.INSTANCE.load();
        Set<Integer> newKeys = new TreeSet<>(NpcDropManager.NPC_DROPS.keySet());
        int newSize = newKeys.size();

        assertEquals(oldSize, newSize,
                "Drop table key count mismatch: old=" + oldSize + " new=" + newSize);

        Set<Integer> onlyInOld = new TreeSet<>(oldKeys);
        onlyInOld.removeAll(newKeys);
        Set<Integer> onlyInNew = new TreeSet<>(newKeys);
        onlyInNew.removeAll(oldKeys);

        assertTrue(onlyInOld.isEmpty(),
                "NPC IDs in old parser but missing from file loader: " + onlyInOld);
        assertTrue(onlyInNew.isEmpty(),
                "NPC IDs in file loader but missing from old parser: " + onlyInNew);
    }

    @Test
    void dropTables_matchDropCountsPerNpc() {
        NpcDropManager.NPC_DROPS.clear();
        new NpcDropParser().run();
        Map<Integer, int[]> oldCounts = snapshotDropCounts();

        NpcDropManager.NPC_DROPS.clear();
        NpcDropFileLoader.INSTANCE.load();
        Map<Integer, int[]> newCounts = snapshotDropCounts();

        List<String> mismatches = new ArrayList<>();

        for (int npcId : oldCounts.keySet()) {
            int[] oldC = oldCounts.get(npcId);
            int[] newC = newCounts.get(npcId);

            if (newC == null) {
                mismatches.add("NPC " + npcId + ": missing in new loader");
                continue;
            }

            if (!Arrays.equals(oldC, newC)) {
                mismatches.add(String.format(
                        "NPC %d: old=%s new=%s",
                        npcId, Arrays.toString(oldC), Arrays.toString(newC)
                ));
            }
        }

        assertTrue(mismatches.isEmpty(),
                "Drop count mismatches (" + mismatches.size() + "):\n"
                        + mismatches.stream().limit(20).collect(Collectors.joining("\n")));
    }

    @Test
    void dropTables_matchItemIdsPerNpc() {
        NpcDropManager.NPC_DROPS.clear();
        new NpcDropParser().run();
        Map<Integer, Set<Integer>> oldItemIds = snapshotItemIds();

        NpcDropManager.NPC_DROPS.clear();
        NpcDropFileLoader.INSTANCE.load();
        Map<Integer, Set<Integer>> newItemIds = snapshotItemIds();

        List<String> mismatches = new ArrayList<>();

        for (int npcId : oldItemIds.keySet()) {
            Set<Integer> oldIds = oldItemIds.get(npcId);
            Set<Integer> newIds = newItemIds.get(npcId);

            if (newIds == null) {
                mismatches.add("NPC " + npcId + ": missing in new loader");
                continue;
            }

            Set<Integer> onlyOld = new TreeSet<>(oldIds);
            onlyOld.removeAll(newIds);
            Set<Integer> onlyNew = new TreeSet<>(newIds);
            onlyNew.removeAll(oldIds);

            if (!onlyOld.isEmpty() || !onlyNew.isEmpty()) {
                mismatches.add(String.format(
                        "NPC %d: onlyOld=%s onlyNew=%s",
                        npcId, onlyOld, onlyNew
                ));
            }
        }

        assertTrue(mismatches.isEmpty(),
                "Item ID mismatches (" + mismatches.size() + "):\n"
                        + mismatches.stream().limit(20).collect(Collectors.joining("\n")));
    }

    // ──────────────────────────────────────────────
    //  NPC SPAWNS — pure data comparison
    // ──────────────────────────────────────────────

    @Test
    void spawns_monolithicAndPerFileHaveSameNpcIds() {
        // Read NPC IDs from monolithic npc_spawns.json
        Set<Integer> monolithicIds = readSpawnIdsFromMonolithic();

        // Read NPC IDs from per-file directory
        Set<Integer> perFileIds = readSpawnIdsFromPerFile();

        assertEquals(monolithicIds.size(), perFileIds.size(),
                "NPC ID count mismatch: monolithic=" + monolithicIds.size()
                        + " perFile=" + perFileIds.size());

        Set<Integer> onlyInMono = new TreeSet<>(monolithicIds);
        onlyInMono.removeAll(perFileIds);
        Set<Integer> onlyInPerFile = new TreeSet<>(perFileIds);
        onlyInPerFile.removeAll(monolithicIds);

        assertTrue(onlyInMono.isEmpty(),
                "NPC IDs in monolithic but missing from per-file: " + onlyInMono);
        assertTrue(onlyInPerFile.isEmpty(),
                "NPC IDs in per-file but missing from monolithic: " + onlyInPerFile);
    }

    @Test
    void spawns_monolithicAndPerFileHaveSameTotalEntries() {
        int monolithicCount = countSpawnEntries("data/def/npc/npc_spawns.json");
        int perFileCount = countSpawnEntriesInDir("data/def/npc-spawns-json/");

        assertEquals(monolithicCount, perFileCount,
                "Total spawn entry count mismatch: monolithic=" + monolithicCount
                        + " perFile=" + perFileCount);
    }

    // ──────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────

    private Map<Integer, int[]> snapshotDropCounts() {
        Map<Integer, int[]> result = new HashMap<>();
        for (Map.Entry<Integer, NpcDropTable> entry : NpcDropManager.NPC_DROPS.entrySet()) {
            NpcDropTable table = entry.getValue();
            int always = table.table[4] != null ? table.table[4].length : 0;
            int common = table.table[3] != null ? table.table[3].length : 0;
            int uncommon = table.table[2] != null ? table.table[2].length : 0;
            int rare = table.table[1] != null ? table.table[1].length : 0;
            int veryRare = table.table[0] != null ? table.table[0].length : 0;
            int total = table.drops != null ? table.drops.length : 0;
            result.put(entry.getKey(), new int[]{total, always, common, uncommon, rare, veryRare});
        }
        return result;
    }

    private Map<Integer, Set<Integer>> snapshotItemIds() {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        for (Map.Entry<Integer, NpcDropTable> entry : NpcDropManager.NPC_DROPS.entrySet()) {
            NpcDropTable table = entry.getValue();
            Set<Integer> ids = new TreeSet<>();
            if (table.drops != null) {
                for (NpcDrop drop : table.drops) {
                    ids.add(drop.id);
                }
            }
            result.put(entry.getKey(), ids);
        }
        return result;
    }

    /**
     * Read all NPC IDs from the monolithic npc_spawns.json.
     * Each top-level element has an "id" field.
     */
    private Set<Integer> readSpawnIdsFromMonolithic() {
        Set<Integer> ids = new TreeSet<>();
        try (FileReader reader = new FileReader("data/def/npc/npc_spawns.json")) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                ids.add(obj.get("id").getAsInt());
            }
        } catch (Exception e) {
            fail("Failed to read monolithic spawns: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Read all NPC IDs from per-file JSON directory.
     * Each file is an array of spawn entries with "id" fields.
     */
    private Set<Integer> readSpawnIdsFromPerFile() {
        Set<Integer> ids = new TreeSet<>();
        File dir = new File("data/def/npc-spawns-json/");
        assertTrue(dir.exists(), "npc-spawns-json/ directory does not exist");
        Gson gson = new Gson();
        for (File file : dir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            try (FileReader reader = new FileReader(file)) {
                JsonObject[] entries = gson.fromJson(reader, JsonObject[].class);
                for (JsonObject entry : entries) {
                    ids.add(entry.get("id").getAsInt());
                }
            } catch (Exception e) {
                fail("Failed to read " + file.getName() + ": " + e.getMessage());
            }
        }
        return ids;
    }

    /**
     * Count total spawn entries in the monolithic JSON file.
     */
    private int countSpawnEntries(String path) {
        try (FileReader reader = new FileReader(path)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            return array.size();
        } catch (Exception e) {
            fail("Failed to count entries in " + path + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Count total spawn entries across all per-file JSONs.
     */
    private int countSpawnEntriesInDir(String dirPath) {
        int total = 0;
        File dir = new File(dirPath);
        assertTrue(dir.exists(), dirPath + " does not exist");
        Gson gson = new Gson();
        for (File file : dir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            try (FileReader reader = new FileReader(file)) {
                JsonObject[] entries = gson.fromJson(reader, JsonObject[].class);
                total += entries.length;
            } catch (Exception e) {
                fail("Failed to count entries in " + file.getName() + ": " + e.getMessage());
            }
        }
        return total;
    }
}
