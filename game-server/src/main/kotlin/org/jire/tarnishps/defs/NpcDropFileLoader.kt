package org.jire.tarnishps.defs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.osroyale.game.world.entity.mob.npc.drop.NpcDrop
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropChance
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropManager
import com.osroyale.game.world.entity.mob.npc.drop.NpcDropTable
import com.osroyale.game.world.items.ItemDefinition
import org.jire.tarnishps.OldToNew
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Loads NPC drops from per-file JSON directory (npc-drops-json/).
 *
 * Replaces NpcDropParser which reads from the monolithic npc_drops.json.
 * Each file is named {npcId}.json and contains:
 *   { "npc_id": Int, "rare_table": Boolean, "drops": [NpcDropEntry...] }
 */
object NpcDropFileLoader {

    private val logger = LoggerFactory.getLogger(NpcDropFileLoader::class.java)

    /** Data class matching the per-file JSON shape for a single drop entry. */
    private data class DropEntry(
        @SerializedName("item", alternate = ["id"]) val item: Int = 0,
        val type: String = "ALWAYS",
        val chance: Double = 0.0,
        val minimum: Int = 1,
        val maximum: Int = 1
    )

    /** Data class matching the per-file JSON shape for a drop table file. */
    private data class DropFile(
        val npc_id: Int = 0,
        val rare_table: Boolean = false,
        val drops: List<DropEntry> = emptyList(),
        @SerializedName("roll-data") val rollData: IntArray? = null
    )

    @JvmStatic
    @JvmOverloads
    fun load(
        gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
    ) {
        val dir = File("data/def/npc-drops-json/")
        if (!dir.isDirectory) {
            throw IllegalStateException("NPC drop directory not found: ${dir.absolutePath}")
        }

        var loaded = 0

        for (file in dir.listFiles() ?: emptyArray()) {
            if (file.extension != "json") continue

            try {
                val dropFile = gson.fromJson(file.bufferedReader(), DropFile::class.java)
                val npcId = dropFile.npc_id
                require(npcId > 0) { "npc_id must be greater than 0" }
                require(dropFile.drops.isNotEmpty()) { "drops must not be empty" }

                // Apply OldToNew mapping to NPC ID
                val mappedNpcId = OldToNew.get(npcId).let { if (it != -1) it else npcId }

                // Convert DropEntry list to NpcDrop array
                val npcDrops = dropFile.drops.map { entry ->
                    require(entry.item > 0) { "drop item id must be greater than 0 for npc_id=$npcId" }
                    require(entry.minimum > 0) { "minimum must be greater than 0 for npc_id=$npcId item=${entry.item}" }
                    require(entry.maximum >= entry.minimum) {
                        "maximum must be greater than or equal to minimum for npc_id=$npcId item=${entry.item}"
                    }
                    val chance = entry.chance.toInt()
                    val type = parseDropChance(entry.type)

                    NpcDrop(entry.item, type, chance, entry.minimum, entry.maximum)
                }.toTypedArray()

                // Categorize drops (matches NpcDropParser logic)
                val always = mutableListOf<NpcDrop>()
                val common = mutableListOf<NpcDrop>()
                val uncommon = mutableListOf<NpcDrop>()
                val rare = mutableListOf<NpcDrop>()
                val veryRare = mutableListOf<NpcDrop>()

                for (drop in npcDrops) {
                    // Skip drops for non-existent items
                    if (ItemDefinition.get(drop.id) == null) continue

                    // Special clue scroll handling (matches NpcDropParser)
                    when (drop.id) {
                        12073 -> { // elite clue
                            veryRare.add(drop)
                            drop.setType(NpcDropChance.VERY_RARE)
                            continue
                        }
                        2722 -> { // hard clue
                            rare.add(drop)
                            drop.setType(NpcDropChance.RARE)
                            continue
                        }
                        2801 -> { // medium clue
                            uncommon.add(drop)
                            drop.setType(NpcDropChance.UNCOMMON)
                            continue
                        }
                        2677 -> { // easy clue
                            common.add(drop)
                            drop.setType(NpcDropChance.COMMON)
                            continue
                        }
                        11942 -> { // ecu key
                            veryRare.add(drop)
                            drop.setType(NpcDropChance.VERY_RARE)
                            continue
                        }
                    }

                    // Item ID remapping (matches NpcDropParser logic)
                    if (drop.id == 1436) drop.id = 7936
                    if (drop.id == 1437) drop.id = 7937

                    when (drop.type) {
                        NpcDropChance.ALWAYS -> always.add(drop)
                        NpcDropChance.COMMON -> common.add(drop)
                        NpcDropChance.UNCOMMON -> uncommon.add(drop)
                        NpcDropChance.RARE -> rare.add(drop)
                        NpcDropChance.VERY_RARE -> veryRare.add(drop)
                    }
                }

                // Sort all drops
                npcDrops.sort()

                // Create drop table
                val table = NpcDropTable(
                    intArrayOf(mappedNpcId),
                    dropFile.rare_table,
                    npcDrops,
                    always.toTypedArray(),
                    common.toTypedArray(),
                    uncommon.toTypedArray(),
                    rare.toTypedArray(),
                    veryRare.toTypedArray()
                )

                // Apply roll-data if present (matches NpcDropParser logic)
                dropFile.rollData?.let { table.setRollData(it) }

                NpcDropManager.NPC_DROPS[mappedNpcId] = table
                loaded++
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load NPC drop file ${file.path}: ${e.message}", e)
            }
        }

        require(loaded > 0) { "No NPC drop tables were loaded from ${dir.absolutePath}" }
        logger.info("Loaded {} NPC drop tables from {}", loaded, dir.path)
    }

    private fun parseDropChance(type: String): NpcDropChance {
        return when (type.uppercase()) {
            "ALWAYS" -> NpcDropChance.ALWAYS
            "COMMON" -> NpcDropChance.COMMON
            "UNCOMMON" -> NpcDropChance.UNCOMMON
            "RARE" -> NpcDropChance.RARE
            "VERY_RARE" -> NpcDropChance.VERY_RARE
            else -> throw IllegalArgumentException("Unknown NPC drop chance type: $type")
        }
    }
}
