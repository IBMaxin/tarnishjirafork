package com.osroyale.content.skill.impl.slayer;

import com.osroyale.game.world.entity.mob.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Slayer shop guard.
 *
 * <p>Uses reflection to instantiate {@link Player} because its constructor is
 * package-private.</p>
 */
public class SlayerTest {

    private static final int SLOT_COUNT = 10;

    private static Player newPlayer() {
        try {
            Constructor<Player> ctor = Player.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance("TestPlayer");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct test Player", e);
        }
    }

    @Test
    void store_validBoundsDoNotThrow() {
        Player player = newPlayer();
        Slayer slayer = new Slayer(player);
        assertDoesNotThrow(() -> slayer.store(0, 1, true), "Slot 0 should be valid");
        assertDoesNotThrow(() -> slayer.store(SLOT_COUNT - 1, 1, true), "Last slot should be valid");
    }

    @Test
    void store_negativeSlotDoesNotThrow() {
        Player player = newPlayer();
        Slayer slayer = new Slayer(player);
        assertDoesNotThrow(() -> slayer.store(-1, 1, true), "Negative slot should be rejected safely");
    }

    @Test
    void store_exactUpperBoundDoesNotThrow() {
        Player player = newPlayer();
        Slayer slayer = new Slayer(player);
        assertDoesNotThrow(() -> slayer.store(SLOT_COUNT, 1, true), "Exact upper-bound slot should be rejected safely");
    }

    @Test
    void store_beyondUpperBoundDoesNotThrow() {
        Player player = newPlayer();
        Slayer slayer = new Slayer(player);
        assertDoesNotThrow(() -> slayer.store(SLOT_COUNT + 1, 1, true), "Beyond upper-bound slot should be rejected safely");
    }
}
