package com.osroyale.game.world.entity.mob.player;

import com.osroyale.content.donators.Donation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlayerRight} static helpers.
 */
public class PlayerRightTest {

    @Test
    public void testLookupValidId() {
        Optional<PlayerRight> result = PlayerRight.lookup(2);
        assertTrue(result.isPresent());
        assertEquals(PlayerRight.ADMINISTRATOR, result.get());
    }

    @Test
    public void testLookupInvalidId() {
        Optional<PlayerRight> result = PlayerRight.lookup(999);
        assertFalse(result.isPresent());
    }

    @Test
    public void testLookupNegativeId() {
        Optional<PlayerRight> result = PlayerRight.lookup(-1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testIsAdministratorTrue() throws Exception {
        Player player = mock(Player.class);
        setField(player, "right", PlayerRight.ADMINISTRATOR);
        assertTrue(PlayerRight.isAdministrator(player));
    }

    @Test
    public void testIsAdministratorFalse() throws Exception {
        Player player = mock(Player.class);
        setField(player, "right", PlayerRight.PLAYER);
        assertFalse(PlayerRight.isAdministrator(player));
    }

    @Test
    public void testIsDonatorByModerator() throws Exception {
        Player player = mock(Player.class);
        setField(player, "right", PlayerRight.MODERATOR);
        assertTrue(PlayerRight.isDonator(player));
    }

    @Test
    public void testIsDonatorBySpent() throws Exception {
        Player player = mock(Player.class);
        setField(player, "right", PlayerRight.PLAYER);
        Donation donation = mock(Donation.class);
        when(donation.getSpent()).thenReturn(10); // DONATOR moneyRequired is 10
        setField(player, "donation", donation);
        assertTrue(PlayerRight.isDonator(player));
    }

    @Test
    public void testIsDonatorFalse() throws Exception {
        Player player = mock(Player.class);
        setField(player, "right", PlayerRight.PLAYER);
        Donation donation = mock(Donation.class);
        when(donation.getSpent()).thenReturn(5); // below DONATOR threshold of 10
        setField(player, "donation", donation);
        assertFalse(PlayerRight.isDonator(player));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
