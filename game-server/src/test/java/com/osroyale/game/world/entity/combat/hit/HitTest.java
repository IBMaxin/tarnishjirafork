package com.osroyale.game.world.entity.combat.hit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;

/**
 * Unit tests for {@link Hit} focusing on pure‑value behavior.
 */
public class HitTest {

    @Test
    public void testModifyDamagePositive() {
        Hit hit = new Hit(10, Hitsplat.NORMAL, HitIcon.NONE, true);
        // Double the damage via a lambda
        Function<Integer, Integer> doubleFn = d -> d * 2;
        hit.modifyDamage(doubleFn);
        assertEquals(20, hit.getDamage());
    }

    @Test
    public void testModifyDamageZeroOrNegativeResultsInZero() {
        Hit hit = new Hit(5, Hitsplat.NORMAL, HitIcon.NONE, true);
        // Reduce to zero
        hit.modifyDamage(d -> 0);
        assertEquals(0, hit.getDamage());
        // Reduce to negative value – should also clamp to 0
        Hit hitNeg = new Hit(5, Hitsplat.NORMAL, HitIcon.NONE, true);
        hitNeg.modifyDamage(d -> -10);
        assertEquals(0, hitNeg.getDamage());
    }

    @Test
    public void testSetAsCopiesAllFields() {
        Hit source = new Hit(8, Hitsplat.NORMAL, HitIcon.NONE, false);
        source.setAccurate(true);
        Hit target = new Hit(0, Hitsplat.NORMAL, HitIcon.NONE, false);
        target.setAs(source);
        assertEquals(source.getDamage(), target.getDamage());
        assertEquals(source.getHitsplat(), target.getHitsplat());
        assertEquals(source.getHitIcon(), target.getHitIcon());
        assertEquals(source.isAccurate(), target.isAccurate());
    }

    @Test
    public void testSetAccurateAndIsAccurate() {
        Hit hit = new Hit(0, Hitsplat.NORMAL, HitIcon.NONE, false);
        assertFalse(hit.isAccurate());
        hit.setAccurate(true);
        assertTrue(hit.isAccurate());
    }
}
