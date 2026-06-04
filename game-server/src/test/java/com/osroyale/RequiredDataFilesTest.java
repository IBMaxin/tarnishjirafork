package com.osroyale;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that every critical data file exists before parser tests run.
 * Uses only {@code Files.exists()} — no data loaded, no parsing, no mutable state touched.
 * <p>
 * This catches missing cache or definition files immediately rather than
 * producing cryptic NullPointerExceptions or FileNotFoundExceptions later.
 */
public final class RequiredDataFilesTest {

    private static final String DATA_DIR = "data";

    /** Core definition files that must exist for the server to function. */
    private static final List<String> REQUIRED_FILES = List.of(
            // Items
            "data/def/item/item_definitions.json",
            // Equipment
            "data/def/equipment/equipment_definitions.json",
            // NPCs
            "data/def/npc/npc_definitions.json",
            "data/def/npc/npc_drops.json",
            "data/def/npc/npc_spawns.json",
            "data/def/npc/npc_force_chat.json",
            // Stores
            "data/def/store/stores.json",
            // Objects
            "data/def/object/global_objects.json",
            "data/def/object/removed_objects.json",
            "data/def/object_examines.json",
            // Combat
            "data/def/combat/projectile_definitions.json",
            // IO
            "data/io/message_sizes.json",
            // Profiles
            "data/profile/save/Zezima.json",
            "data/profile/save/Oak.json",
            "data/profile/world_profile_list.json"
    );

    @Test
    public void allRequiredDataFilesExist() {
        List<String> missing = new ArrayList<>();

        for (String relativePath : REQUIRED_FILES) {
            Path fullPath = Path.of(relativePath);
            if (!Files.exists(fullPath)) {
                missing.add(relativePath);
            }
        }

        if (!missing.isEmpty()) {
            fail("Missing required data files (" + missing.size() + "):\n  "
                    + String.join("\n  ", missing));
        }
    }

    @Test
    public void dataDirectoryExists() {
        assertTrue(Files.isDirectory(Path.of(DATA_DIR)),
                "Data directory '" + DATA_DIR + "' does not exist. "
                        + "Make sure tests run from the game-server working directory.");
    }
}