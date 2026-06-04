package org.jire.tarnishps.event.npc

import com.osroyale.content.skill.impl.hunter.net.impl.Butterfly
import com.osroyale.content.skill.impl.hunter.net.impl.Impling
import com.osroyale.game.world.entity.combat.magic.CombatSpell
import com.osroyale.game.world.entity.mob.npc.Npc
import com.osroyale.game.world.entity.mob.player.Player
import com.osroyale.game.world.position.Position
import com.osroyale.net.packet.out.SendMessage
import com.osroyale.util.Utility
import kotlin.math.abs

/**
 * @author Jire
 */
class MagicOnNpcEvent(
    override val slot: Int,
    val spell: Int
) : NpcClickEvent {

    override fun handleNpc(player: Player, npc: Npc) {
        val definition = CombatSpell.get(spell) ?: return
        if (player.spellbook != definition.spellbook) return

        if (!npc.definition.isAttackable

            && !Impling.forId(npc.id).isPresent
            && !Butterfly.forId(npc.id).isPresent
        ) {
            player.send(SendMessage("This npc can not be attacked!"))
            return
        }

        player.setSingleCast(definition)

        // Vorkath bypass — TraversalMap has no Zeah data, move adjacent
        if (npc.id == 8059 || npc.id == 8060) {
            val dist = Utility.getDistance(player, npc)
            if (dist > 1) {
                val npcMaxX = npc.getX() + npc.width() - 1
                val npcMaxY = npc.getY() + npc.length() - 1
                val px = player.getX()
                val py = player.getY()

                val dxToWest = npc.getX() - 1 - px
                val dxToEast = npcMaxX + 1 - px
                val dyToSouth = npc.getY() - 1 - py
                val dyToNorth = npcMaxY + 1 - py

                val dx = when {
                    abs(dxToWest) <= abs(dxToEast) -> dxToWest
                    else -> dxToEast
                }
                val dy = when {
                    abs(dyToSouth) <= abs(dyToNorth) -> dyToSouth
                    else -> dyToNorth
                }

                val targetX = if (abs(dx) <= abs(dy)) px + dx else px
                val targetY = if (abs(dy) < abs(dx)) py + dy else py

                player.move(Position(targetX, targetY, npc.getHeight()))
                player.face(npc.getPosition())
                // Delay attack to next tick — move() resets combat, position needs to sync
                com.osroyale.game.world.World.schedule(2) {
                    if (!player.combat.attack(npc)) {
                        player.setSingleCast(null)
                        player.resetFace()
                    }
                }
                return
            }
        }

        if (!player.combat.attack(npc)) {
            player.setSingleCast(null)
            player.resetFace()
        }
    }

}