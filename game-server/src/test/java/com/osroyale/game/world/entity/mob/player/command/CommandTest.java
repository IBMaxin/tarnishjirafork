package com.osroyale.game.world.entity.mob.player.command;

import com.osroyale.game.world.entity.mob.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Command} — names, identity, and constructor behavior.
 */
public class CommandTest {

    @Test
    public void testSingleName() {
        Command cmd = command("test");
        assertEquals(1, cmd.getNames().length);
        assertEquals("test", cmd.getNames()[0]);
    }

    @Test
    public void testMultipleNames() {
        Command cmd = command("a", "b", "c");
        assertEquals(3, cmd.getNames().length);
        assertEquals("a", cmd.getNames()[0]);
        assertEquals("b", cmd.getNames()[1]);
        assertEquals("c", cmd.getNames()[2]);
    }

    @Test
    public void testEqualsIdentity() {
        Command a = command("x");
        assertNotEquals(a, command("x"));
        assertEquals(a, a);
    }

    @Test
    public void testHashCodeIdentity() {
        Command a = command("z");
        assertEquals(System.identityHashCode(a), a.hashCode());
    }

    @Test
    public void testMultipleCommandsWithSameNameNotEqual() {
        Command foo1 = command("foo");
        Command foo2 = command("foo");
        assertNotEquals(foo1, foo2);
        assertNotEquals(foo1.hashCode(), foo2.hashCode());
    }

    private static Command command(String... names) {
        return new Command(names) {
            @Override
            public void execute(Player player, CommandParser parser) {
            }
        };
    }
}
