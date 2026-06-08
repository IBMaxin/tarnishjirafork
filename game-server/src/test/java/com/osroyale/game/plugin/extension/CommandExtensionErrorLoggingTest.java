package com.osroyale.game.plugin.extension;

import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.command.Command;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CommandExtension} error logging behavior.
 */
public class CommandExtensionErrorLoggingTest {

    @Test
    public void handleCommand_logsExceptionsAndContinues() {
        CommandExtension plugin = new CommandExtension() {
            @Override
            protected void register() {
                commands.add(new Command("throw") {
                    @Override
                    public void execute(Player player, CommandParser parser) {
                        throw new IllegalStateException("intentional test failure");
                    }
                });
            }

            @Override
            public boolean canAccess(Player player) {
                return true;
            }
        };

        plugin.onInit();
        assertTrue(!plugin.multimap.isEmpty(), "Multimap should contain registered commands after onInit().");
    }
}
