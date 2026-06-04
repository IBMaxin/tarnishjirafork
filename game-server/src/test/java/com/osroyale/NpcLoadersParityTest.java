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
    void spawns_monolithicAndPerFileHaveSameNormalizedEntries() {
        Set<String> monolithic = readNormalizedSpawnsFromMonolithic();
        Set<String> perFile = readNormalizedSpawnsFromPerFile();

        Set<String> onlyInMono = new TreeSet<>(monolithic);
        onlyInMono.removeAll(perFile);
        Set<String> onlyInPerFile = new TreeSet<>(perFile);
        onlyInPerFile.removeAll(monolithic);

        assertTrue(onlyInMono.isEmpty(),
                "Normalized spawns in monolithic but missing from per-file (" + onlyInMono.size() + "):\n"
                        + onlyInMono.stream().limit(20).collect(Collectors.joining("\n")));
        assertTrue(onlyInPerFile.isEmpty(),
                "Normalized spawns in per-file but missing from monolithic (" + onlyInPerFile.size() + "):\n"
                        + onlyInPerFile.stream().limit(20).collect(Collectors.joining("\n")));
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

    /** Read normalized effective spawn entries from monolithic npc_spawns.json. */
    private Set<String> readNormalizedSpawnsFromMonolithic() {
        Set<String> spawns = new TreeSet<>();
        try (FileReader reader = new FileReader("data/def/npc/npc_spawns.json")) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject spawn = el.getAsJsonObject();
                if (spawn.get("id").getAsInt() <= 0) {
                    continue;
                }
                spawns.add(normalizeSpawn(spawn));
            }
        } catch (Exception e) {
            fail("Failed to read monolithic spawns: " + e.getMessage());
        }
        return spawns;
    }

    /** Read normalized effective spawn entries from per-file NPC spawn JSONs. */
    private Set<String> readNormalizedSpawnsFromPerFile() {
        Set<String> spawns = new TreeSet<>();
        File dir = new File("data/def/npc-spawns-json/");
        assertTrue(dir.exists(), "npc-spawns-json/ directory does not exist");
        Gson gson = new Gson();
        for (File file : dir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            try (FileReader reader = new FileReader(file)) {
                JsonObject[] entries = gson.fromJson(reader, JsonObject[].class);
                for (JsonObject entry : entries) {
                    assertTrue(entry.get("id").getAsInt() > 0,
                            "Per-file spawn data must not contain NPC id <= 0: " + file.getName());
                    spawns.add(normalizeSpawn(entry));
                }
            } catch (Exception e) {
                fail("Failed to read " + file.getName() + ": " + e.getMessage());
            }
        }
        return spawns;
    }

    private String normalizeSpawn(JsonObject obj) {
        int id = obj.get("id").getAsInt();
        boolean convertId = !obj.has("convert-id") || obj.get("convert-id").getAsBoolean();
        if (convertId) {
            int mapped = OldToNew.get(id);
            if (mapped != -1) {
                id = mapped;
            }
        }

        JsonObject position = obj.getAsJsonObject("position");
        String radius = obj.has("radius") ? obj.get("radius").getAsString() : "2";
        String instance = obj.has("instance") ? obj.get("instance").getAsString() : "0";
        String facing = obj.get("facing").getAsString().toUpperCase(Locale.ROOT);

        return id + "|"
                + position.get("x").getAsInt() + "|"
                + position.get("y").getAsInt() + "|"
                + position.get("height").getAsInt() + "|"
                + Integer.parseInt(radius) + "|"
                + Integer.parseInt(instance) + "|"
                + facing;
    }
}
