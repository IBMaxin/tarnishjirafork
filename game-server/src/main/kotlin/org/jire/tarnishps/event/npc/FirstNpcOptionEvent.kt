package org.jire.tarnishps.event.npc

import com.osroyale.content.event.EventDispatcher
import com.osroyale.content.event.impl.FirstNpcClick
import com.osroyale.game.world.entity.mob.npc.Npc
import com.osroyale.game.world.entity.mob.player.Player

/**
 * @author Jire
 */
class FirstNpcOptionEvent(slot: Int) : NpcOptionEvent(slot, 0) {

    override fun handleNpc(player: Player, npc: Npc) {
        if (npc.id == 394 && player.position.isWithinDistance(npc.position, 2)) {
            publishToPluginManager(player, npc)
        } else if (npc.id == 8059 && player.position.isWithinDistance(npc.position, 15)) {
            // Vorkath dormant: bypass walkTo — TraversalMap has no Zeah map data
            val interactionEvent = createInteractionEvent(npc)
            if (!EventDispatcher.execute(player, interactionEvent)) {
                publishToPluginManager(player, npc)
            }
        } else {
            super.handleNpc(player, npc)
        }
    }

    override fun createInteractionEvent(npc: Npc) = FirstNpcClick(npc)

}