package com.osroyale;

import com.osroyale.content.store.Store;
import com.osroyale.game.world.entity.mob.npc.definition.NpcDefinition;
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropManager;
import com.osroyale.game.world.items.ItemDefinition;
import com.osroyale.util.parser.impl.*;
import org.jire.tarnishps.OldToNew;
import org.jire.tarnishps.defs.NpcDropFileLoader;
import org.jire.tarnishps.defs.NpcSpawnFileLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 2 smoke test that runs actual parser classes via {@code .run()}.
 * <p>
 * These tests mutate global static state ({@code Store.STORES}, definition
 * arrays, etc.). They are ordered to run after all Tier 1 tests
 * ({@link RequiredDataFilesTest}, {@link DataJsonParseTest},
 * {@link CrossReferenceTest}).
 * </p>
 * <p>
 * Each test uses a floor-count check (e.g. {@code Store.STORES.size() > 50})
 * rather than an exact number, so adding/removing data entries doesn't
 * cause spurious failures.
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class ParserSmokeTest {

    @BeforeAll
    public static void loadPrerequisites() {
        OldToNew.load();
    }

    @Test
    @Order(1)
    public void storeParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new StoreParser().run());
        assertTrue(Store.STORES.size() > 10,
                "StoreParser should load at least 10 stores, got " + Store.STORES.size());
    }

    @Test
    @Order(2)
    public void itemDefinitionParserLoadsWithoutError() {
        assertDoesNotThrow(() -> ItemDefinition.createParser().run());
    }

    @Test
    @Order(3)
    public void npcDefinitionParserLoadsWithoutError() {
        assertDoesNotThrow(() -> NpcDefinition.createParser().run());
    }

    @Test
    @Order(4)
    public void npcSpawnFileLoaderLoadsWithoutError() {
        assertDoesNotThrow(() -> NpcSpawnFileLoader.INSTANCE.load());
    }

    @Test
    @Order(5)
    public void npcDropFileLoaderLoadsWithoutError() {
        assertDoesNotThrow(() -> NpcDropFileLoader.INSTANCE.load());
        assertTrue(NpcDropManager.NPC_DROPS.size() > 100,
                "NpcDropFileLoader should load at least 100 drop tables, got " + NpcDropManager.NPC_DROPS.size());
    }

    @Test
    @Order(6)
    public void combatProjectileParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new CombatProjectileParser().run());
    }

    @Test
    @Order(7)
    public void npcForceChatParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new NpcForceChatParser().run());
    }

    @Test
    @Order(8)
    public void globalObjectParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new GlobalObjectParser().run());
    }

    @Test
    @Order(9)
    public void packetSizeParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new PacketSizeParser().run());
    }

    @Test
    @Order(10)
    public void objectRemovalParserLoadsWithoutError() {
        assertDoesNotThrow(() -> new ObjectRemovalParser().run());
    }
}
