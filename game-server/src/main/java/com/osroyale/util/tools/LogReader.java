package com.osroyale.util.tools;

import com.osroyale.game.world.entity.mob.player.Player;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for reading referral claim logs.
 *
 * <p>This class is intentionally stateless and thread-safe:
 * all state is confined to the stack of {@link #alreadyClaimedReferral(Player, Path)}.
 * No static mutable fields are used.</p>
 */
public final class LogReader {

    private static final Logger logger = LogManager.getLogger();
    private static final Path DEFAULT_LOG_PATH = Path.of("backup", "logs", "referrals.txt");

    private LogReader() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether the given player (by username or last host) already appears
     * in the referral log.
     *
     * <p>Returns {@code false} if the log file does not exist or cannot be read.
     * A missing file is not treated as an error — it simply means no referrals
     * have been claimed yet.</p>
     *
     * @param player the player to check
     * @return {@code true} if a matching referral claim is found; {@code false} otherwise
     */
    public static boolean alreadyClaimedReferral(Player player) {
        return alreadyClaimedReferral(player, DEFAULT_LOG_PATH);
    }

    /**
     * Checks whether the given player (by username or last host) already appears
     * in the referral log at the specified path.
     *
     * <p>Returns {@code false} if the log file does not exist or cannot be read.</p>
     *
     * @param player the player to check
     * @param logPath path to the referrals text file
     * @return {@code true} if a matching referral claim is found; {@code false} otherwise
     */
    public static boolean alreadyClaimedReferral(Player player, Path logPath) {
        if (!Files.isRegularFile(logPath)) {
            return false;
        }

        String username = player.getUsername();
        String lastHost = player.lastHost;

        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(username) || line.contains(lastHost)) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to read referral log at " + logPath + "; treating as no prior claim.", e);
        }

        return false;
    }
}
