package plugin.click.npc;

import com.osroyale.content.activity.ActivityType;
import com.osroyale.content.activity.impl.araxxor.AraxxorActivity;
import com.osroyale.game.event.impl.NpcClickEvent;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;

public class AraxxorClickPlugin extends PluginContext {

    @Override
    protected boolean firstClickNpc(Player player, NpcClickEvent event) {
        final int id = event.getNpc().id;
        switch (id) {
            case 11175:
            case 11176:
                if (!player.inActivity(ActivityType.ARAXXOR)) {
                    AraxxorActivity.create(player);
                }
                return true;
        }
        return false;
    }
}
