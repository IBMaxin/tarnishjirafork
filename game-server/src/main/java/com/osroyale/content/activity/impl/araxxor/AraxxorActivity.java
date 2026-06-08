package com.osroyale.content.activity.impl.araxxor;

import com.osroyale.content.ActivityLog;
import com.osroyale.content.achievement.AchievementHandler;
import com.osroyale.content.achievement.AchievementKey;
import com.osroyale.content.activity.Activity;
import com.osroyale.content.activity.ActivityDeathType;
import com.osroyale.content.activity.ActivityType;
import com.osroyale.content.event.impl.NpcInteractionEvent;
import com.osroyale.game.task.Task;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.entity.mob.npc.NpcDeath;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.object.CustomGameObject;
import com.osroyale.game.world.position.Area;
import com.osroyale.game.world.position.Position;
import com.osroyale.util.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AraxxorActivity extends Activity {

    private static final Logger logger = Logger.getLogger("Araxxor");

    private static final int[] EGG_COLORS = {0, 1, 2};
    private static final Position[] EGG_POSITIONS = {
            new Position(3193, 3395),
            new Position(3197, 3393),
            new Position(3202, 3393),
            new Position(3205, 3396),
            new Position(3206, 3401),
            new Position(3203, 3405),
            new Position(3198, 3406),
            new Position(3194, 3403),
            new Position(3191, 3399)
    };

    private final Player player;
    private Npc araxxor;
    private final List<CustomGameObject> eggObjects = new ArrayList<>();
    private final List<Npc> trackedMinions = new ArrayList<>();
    private int[] eggColors;
    private int eggIndex;
    private int attackCounter;
    private boolean entered;
    private boolean eggsInitialized;

    private AraxxorActivity(Player player, int instance) {
        super(1, instance);
        this.player = player;
    }

    public static AraxxorActivity create(Player player) {
        try {
            final int instance = player.playerAssistant.instance();
            final AraxxorActivity activity = new AraxxorActivity(player, instance);
            activity.pause();
            activity.add(player);

            final Position spawn = new Position(3200, 3400, player.playerAssistant.instance());
            activity.araxxor = new Npc(11176, spawn);
            activity.araxxor.face(player);
            activity.add(activity.araxxor);

            player.move(new Position(3196, 3400, player.playerAssistant.instance()));

            activity.initEggs();
            activity.entered = true;
            player.gameRecord.start();
            activity.setPause(false);

            logger.info("Araxxor activity created for player: " + player.getName());
            player.message("@red@Araxxor awakens! Fight for your life!");
            return activity;
        } catch (Exception e) {
            logger.severe("Failed to create Araxxor activity for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void start() {
    }

    @Override
    public void finish() {
        try {
            boolean successful = araxxor != null && araxxor.isDead();

            cleanup();
            remove(player);

            if (successful) {
                player.activityLogger.add(ActivityLog.ARAXXOR);
                AchievementHandler.activate(player, AchievementKey.ARAXXOR);
                long duration = player.gameRecord.end(ActivityType.ARAXXOR);
                player.message("Congratulations, you have killed Araxxor! Fight duration: @red@"
                        + Utility.getTime(duration) + "</col>.");
                logger.info("Player " + player.getName() + " defeated Araxxor in "
                        + Utility.getTime(duration));

                restart(10, () -> {
                    if (Area.inAraxxor(player)) {
                        create(player);
                    } else {
                        remove(player);
                    }
                });
            }
        } catch (Exception e) {
            logger.severe("Error in AraxxorActivity.finish(): " + e.getMessage());
            cleanup();
            remove(player);
        }
    }

    @Override
    public void cleanup() {
        try {
            for (CustomGameObject egg : eggObjects) {
                egg.unregister();
            }
            eggObjects.clear();

            for (Npc minion : trackedMinions) {
                if (minion.isRegistered()) {
                    remove(minion);
                }
            }
            trackedMinions.clear();

            if (araxxor != null && araxxor.isRegistered()) {
                remove(araxxor);
            }
            logger.fine("Araxxor cleanup complete for " + (player != null ? player.getName() : "unknown"));
        } catch (Exception e) {
            logger.warning("Error during Araxxor cleanup: " + e.getMessage());
        }
    }

    @Override
    protected boolean clickNpc(Player player, NpcInteractionEvent event) {
        try {
            final Npc npc = event.getNpc();
            if (npc.equals(araxxor) && !entered) {
                entered = true;
                logger.fine("Araxxor engaged by " + player.getName());
            }
        } catch (Exception e) {
            logger.warning("Error in clickNpc: " + e.getMessage());
        }
        return false;
    }

    private void initEggs() {
        try {
            eggsInitialized = false;
            eggIndex = 0;
            attackCounter = 0;
            eggColors = shuffleColors();
            int instance = player.playerAssistant.instance();

            for (int i = 0; i < EGG_POSITIONS.length; i++) {
                int eggObjId = 50752 + eggColors[i % 3];
                Position pos = new Position(EGG_POSITIONS[i].getX(), EGG_POSITIONS[i].getY(), instance);
                CustomGameObject egg = new CustomGameObject(eggObjId, instance, pos);
                egg.register();
                eggObjects.add(egg);
            }

            eggsInitialized = true;
            logger.fine("Eggs initialized for " + player.getName());
        } catch (Exception e) {
            logger.severe("Failed to init eggs: " + e.getMessage());
        }
    }

    private int[] shuffleColors() {
        int[] colors = new int[9];
        int[] base = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        System.arraycopy(base, 0, colors, 0, 9);
        for (int i = colors.length - 1; i > 0; i--) {
            int j = Utility.random(i);
            int temp = colors[i];
            colors[i] = colors[j];
            colors[j] = temp;
        }
        return colors;
    }

    public void onBossAttack() {
        if (!eggsInitialized || araxxor == null || araxxor.isDead()) return;

        try {
            attackCounter++;

            if (attackCounter == 3 || (attackCounter > 3 && (attackCounter - 3) % 6 == 0)) {
                hatchNextEgg();
            }
        } catch (Exception e) {
            logger.warning("Error in onBossAttack: " + e.getMessage());
        }
    }

    private void hatchNextEgg() {
        try {
            if (eggIndex >= EGG_POSITIONS.length || eggIndex >= eggColors.length) return;

            int color = eggColors[eggIndex];
            int npcId;
            switch (color) {
                case 0 -> npcId = 11177;
                case 1 -> npcId = 11178;
                case 2 -> npcId = 11179;
                default -> { return; }
            }

            if (eggIndex < eggObjects.size()) {
                CustomGameObject egg = eggObjects.get(eggIndex);
                egg.unregister();
            }

            Position spawnPos = new Position(
                    EGG_POSITIONS[eggIndex].getX(),
                    EGG_POSITIONS[eggIndex].getY(),
                    player.playerAssistant.instance()
            );
            Npc minion = new Npc(npcId, spawnPos);
            minion.instance = player.playerAssistant.instance();
            add(minion);
            trackedMinions.add(minion);

            String colorName = switch (color) {
                case 0 -> "acidic";
                case 1 -> "mirrorback";
                case 2 -> "explosive";
                default -> "unknown";
            };
            logger.fine("Hatched " + colorName + " araxyte at egg " + eggIndex + " for " + player.getName());
            player.message("@red@An " + colorName + " araxyte has hatched!");

            eggIndex++;
        } catch (Exception e) {
            logger.severe("Failed to hatch egg at index " + eggIndex + ": " + e.getMessage());
        }
    }

    @Override
    public void onRegionChange(Player player) {
        if (!Area.inAraxxor(player)) {
            logger.warning("Player " + player.getName() + " left Araxxor area mid-fight");
            cleanup();
            remove(player);
        }
    }

    @Override
    public void onLogout(Player player) {
        logger.info("Player " + player.getName() + " logged out during Araxxor fight");
        cleanup();
        remove(player);
    }

    @Override
    public void onDeath(Mob mob) {
        if (mob.isNpc() && mob.getNpc().equals(araxxor)) {
            World.schedule(new NpcDeath(mob.getNpc(), this::finish));
            return;
        }
        super.onDeath(mob);
    }

    @Override
    public boolean canTeleport(Player player) {
        return true;
    }

    @Override
    public ActivityDeathType deathType() {
        return ActivityDeathType.SAFE;
    }

    @Override
    public ActivityType getType() {
        return ActivityType.ARAXXOR;
    }
}
