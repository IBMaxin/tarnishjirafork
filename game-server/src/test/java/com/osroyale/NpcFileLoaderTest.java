package com.osroyale;

import com.osroyale.game.world.items.ItemDefinition;
import org.jire.tarnishps.OldToNew;
import org.jire.tarnishps.defs.NpcDropFileLoader;
import org.jire.tarnishps.defs.NpcSpawnFileLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test that verifies both per-file loaders function correctly
 * with the current data on disk.
 */
public class NpcFileLoaderTest {

    @BeforeAll
    static void setup() {
        OldToNew.load();
        ItemDefinition.createParser().run();
    }

    @Test
    void spawnDataDirectoryExists() {
        assertTrue(new File("data/def/npc-spawns-json").isDirectory(),
                "NPC spawn data directory must exist");
    }

    @Test
    void dropDataDirectoryExists() {
        assertTrue(new File("data/def/npc-drops-json").isDirectory(),
                "NPC drop data directory must exist");
    }

    @Test
    void dropFileLoaderLoadsWithoutErrors() {
        assertDoesNotThrow(() -> NpcDropFileLoader.load(),
                "NpcDropFileLoader should load without exceptions");
        assertTrue(com.osroyale.game.world.entity.mob.npc.drop.NpcDropManager.NPC_DROPS.size() > 0,
                "Drop tables should have been loaded");
    }

    @Test
    void spawnFileLoaderLoadsWithoutErrors() {
        assertDoesNotThrow(() -> NpcSpawnFileLoader.load(),
                "NpcSpawnFileLoader should load without exceptions");
    }
}
