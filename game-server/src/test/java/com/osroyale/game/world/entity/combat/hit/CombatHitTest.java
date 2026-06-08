package com.osroyale.game.world.entity.combat.hit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;

/**
 * Unit tests for {@link CombatHit} focusing on copy/modify behavior and getters.
 */
public class CombatHitTest {

    @Test
    public void testCopyAndModifySingleHit() {
        Hit original = new Hit(10, Hitsplat.NORMAL, HitIcon.NONE, true);
        CombatHit combat = new CombatHit(original, 2, 3);
        Function<Integer, Integer> addFive = d -> d + 5;
        CombatHit copy = combat.copyAndModify(addFive);

        // Verify a new instance is created
        assertNotSame(combat, copy);
        // Original damage unchanged
        assertEquals(10, combat.getDamage());
        // Copy damage should be modified
        assertEquals(15, copy.getDamage());
        // Delays should be preserved
        assertEquals(2, copy.getHitDelay());
        assertEquals(3, copy.getHitsplatDelay());
    }

    @Test
    public void testMultipleHitsConstructorAndGetters() {
        Hit h1 = new Hit(4, Hitsplat.NORMAL, HitIcon.NONE, true);
        Hit h2 = new Hit(6, Hitsplat.NORMAL, HitIcon.NONE, true);
        Hit[] hits = new Hit[]{h1, h2};
        CombatHit multi = new CombatHit(hits, 5, 7);
        assertTrue(multi.getMultipleHitsAllowed());
        assertEquals(5, multi.getHitDelay());
        assertEquals(7, multi.getHitsplatDelay());
        // Total damage should be sum of hits (handled by superclass)
        assertEquals(10, multi.getDamage());
    }
}
