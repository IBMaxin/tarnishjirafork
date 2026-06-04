package org.jire.tarnishps.defs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.osroyale.game.world.entity.mob.Direction
import com.osroyale.game.world.entity.mob.npc.Npc
import com.osroyale.game.world.position.Position
import org.jire.tarnishps.OldToNew
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(NpcSpawnFileLoader::class.java)

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
        if (!dir.isDirectory) {
            throw IllegalStateException("NPC spawn directory not found: ${dir.absolutePath}")
        }

        var loaded = 0

        for (file in dir.listFiles() ?: emptyArray()) {
            if (file.extension != "json") continue

            try {
                // Each file contains an array of spawn entries for one NPC ID
                val spawnArray = gson.fromJson(
                    file.bufferedReader(),
                    Array<SpawnEntry>::class.java
                )
                require(spawnArray.isNotEmpty()) { "spawn file must contain at least one entry" }

                for (entry in spawnArray) {
                    var npcId = entry.id
                    require(npcId > 0) { "id must be greater than 0" }

                    // Apply OldToNew mapping (matches NpcSpawnParser logic)
                    if (entry.convertId) {
                        val newId = OldToNew.get(npcId)
                        if (newId != -1) {
                            npcId = newId
                        }
                    }

                    // Parse position
                    require(entry.position.x > 0 && entry.position.y > 0) {
                        "position x and y must be greater than 0 for npc id=${entry.id}"
                    }
                    val pos = Position(entry.position.x, entry.position.y, entry.position.height)

                    // Parse facing direction
                    val facing = Direction.valueOf(entry.facing.uppercase())

                    // Parse radius (stored as string in JSON)
                    val radius = entry.radius.toIntOrNull()
                        ?: throw IllegalArgumentException("radius must be an integer for npc id=${entry.id}")
                    require(radius >= 0) { "radius must not be negative for npc id=${entry.id}" }

                    // Create and register the NPC
                    Npc(npcId, pos, radius, entry.instance, facing).register()
                    loaded++
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load NPC spawn file ${file.path}: ${e.message}", e)
            }
        }

        require(loaded > 0) { "No NPC spawns were loaded from ${dir.absolutePath}" }
        logger.info("Loaded {} NPC spawns from {}", loaded, dir.path)
    }
}
