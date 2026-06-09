package com.osroyale;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that per-file data directories exist with minimum file counts.
 * This ensures the monolithic JSON → per-file migration data is actually
 * populated, not just empty directories.
 *
 * <p>Minimum counts are set below the actual counts so the test doesn't
 * need updating on every minor data change, but will catch a wholesale
 * deletion or failed migration.</p>
 */
public final class FeatureRequiredDataTest {

    private static final String DATA_DIR = "data/def";

    /** Per-file directories and their minimum expected file counts. */
    private static final List<DirRequirement> REQUIRED_DIRECTORIES = List.of(
            new DirRequirement("items-json", 25_000),
            new DirRequirement("monsters-json", 10_000),
            new DirRequirement("npc-drops-json", 1_500),
            new DirRequirement("npc-spawns-json", 800),
            new DirRequirement("equipment-json", 2_500)
    );

    @Test
    public void perFileDataDirectoriesExistWithMinimumFiles() {
        List<String> failures = new ArrayList<>();

        for (DirRequirement req : REQUIRED_DIRECTORIES) {
            Path dir = Path.of(DATA_DIR, req.name);

            if (!Files.isDirectory(dir)) {
                failures.add("Directory missing: " + DATA_DIR + "/" + req.name);
                continue;
            }

            try {
                int count = (int) Files.list(dir).filter(Files::isRegularFile).count();
                if (count < req.minFiles) {
                    failures.add(String.format(
                            "Directory '%s' has %d files, expected at least %d",
                            DATA_DIR + "/" + req.name, count, req.minFiles));
                }
            } catch (Exception e) {
                failures.add("Failed to list directory '" + DATA_DIR + "/" + req.name + "': " + e.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            fail("Per-file data directory validation failed (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    private record DirRequirement(String name, int minFiles) { }
}
