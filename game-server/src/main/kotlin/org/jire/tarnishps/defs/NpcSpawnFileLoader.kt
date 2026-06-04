package org.jire.tarnishps.defs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.osroyale.game.world.entity.mob.Direction
import com.osroyale.game.world.entity.mob.npc.Npc
import com.osroyale.game.world.position.Position
import org.jire.tarnishps.OldToNew
import java.io.File

/**
 * Loads NPC spawns from per-file JSON directory (npc-spawns-json/).
 *
 * Replaces NpcSpawnParser which reads from the monolithic npc_spawns.json.
 * Each file is named {npcId}.json and contains an array of spawn entries:
 *   [{ "id": Int, "radius": String, "facing": String, "position": {x, y, height} }]
 *
 * An NPC ID can have multiple spawn locations (multiple entries in the array).
 */
object NpcSpawnFileLoader {

    /** Data class matching the per-file JSON shape for a spawn entry. */
    private data class SpawnEntry(
        val id: Int = 0,
        val radius: String = "2",
        val facing: String = "SOUTH",
        val position: SpawnPosition = SpawnPosition(),
        @SerializedName("convert-id") val convertId: Boolean = true,
        val instance: Int = 0
    )

    /** Data class for the position sub-object. */
    private data class SpawnPosition(
        val x: Int = 0,
        val y: Int = 0,
        val height: Int = 0
    )

    @JvmStatic
    @JvmOverloads
    fun load(
        gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
    ) {
        val dir = File("data/def/npc-spawns-json/")
        if (!dir.exists()) {
            println("[NpcSpawnFileLoader] Directory not found: ${dir.absolutePath}")
            return
        }

        var loaded = 0
        var skipped = 0

        for (file in dir.listFiles() ?: emptyArray()) {
            if (file.extension != "json") continue

            try {
                // Each file contains an array of spawn entries for one NPC ID
                val spawnArray = gson.fromJson(
                    file.bufferedReader(),
                    Array<SpawnEntry>::class.java
                )

                for (entry in spawnArray) {
                    var npcId = entry.id
                    if (npcId == 0) {
                        skipped++
                        continue
                    }

                    // Apply OldToNew mapping (matches NpcSpawnParser logic)
                    if (entry.convertId) {
                        val newId = OldToNew.get(npcId)
                        if (newId != -1) {
                            npcId = newId
                        }
                    }

                    // Parse position
                    val pos = Position(entry.position.x, entry.position.y, entry.position.height)

                    // Parse facing direction
                    val facing = try {
                        Direction.valueOf(entry.facing.uppercase())
                    } catch (e: Exception) {
                        Direction.SOUTH
                    }

                    // Parse radius (stored as string in JSON)
                    val radius = entry.radius.toIntOrNull() ?: 2

                    // Create and register the NPC
                    Npc(npcId, pos, radius, entry.instance, facing).register()
                    loaded++
                }
            } catch (e: Exception) {
                println("[NpcSpawnFileLoader] Error loading ${file.name}: ${e.message}")
                skipped++
            }
        }

        println("[NpcSpawnFileLoader] Loaded $loaded NPC spawns, skipped $skipped")
    }
}
