package com.osroyale.game.world.entity.combat.strategy.npc.boss;

import com.osroyale.game.Animation;
import com.osroyale.game.Graphic;
import com.osroyale.game.UpdatePriority;
import com.osroyale.game.task.Task;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.combat.Combat;
import com.osroyale.game.world.entity.combat.CombatType;
import com.osroyale.game.world.entity.combat.attack.FightType;
import com.osroyale.game.world.entity.combat.hit.CombatHit;
import com.osroyale.game.world.entity.combat.hit.Hit;
import com.osroyale.game.world.entity.combat.projectile.CombatProjectile;
import com.osroyale.game.world.entity.combat.strategy.CombatStrategy;
import com.osroyale.game.world.entity.combat.strategy.npc.MultiStrategy;
import com.osroyale.game.world.entity.combat.strategy.npc.NpcMagicStrategy;
import com.osroyale.game.world.entity.combat.strategy.npc.NpcMeleeStrategy;
import com.osroyale.game.world.entity.combat.strategy.npc.NpcRangedStrategy;
import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.object.CustomGameObject;
import com.osroyale.game.world.position.Area;
import com.osroyale.game.world.position.Position;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.util.RandomUtils;
import com.osroyale.util.Utility;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import static com.osroyale.game.world.entity.combat.CombatUtil.createStrategyArray;

public class Araxxor extends MultiStrategy {

    private static final Logger logger = Logger.getLogger("Araxxor");

    private static final NpcMeleeStrategy MELEE = NpcMeleeStrategy.get();
    private static final RangedAttack RANGED = new RangedAttack();
    private static final MagicAttack MAGIC = new MagicAttack();
    private static final VenomDrip VENOM_DRIP = new VenomDrip();
    private static final CleaveAttack CLEAVE = new CleaveAttack();
    private static final VenomBomb VENOM_BOMB = new VenomBomb();

    private static final CombatStrategy<Npc>[] NORMAL_STRATEGIES = createStrategyArray(MELEE, RANGED, MAGIC);
    private static final CombatStrategy<Npc>[] SPECIALS = createStrategyArray(VENOM_DRIP, CLEAVE, VENOM_BOMB);

    private final Deque<CombatStrategy<Npc>> strategyQueue = new ArrayDeque<>();
    private int normalAttackCount;
    private boolean enraged;

    public Araxxor() {
        currentStrategy = MELEE;
    }

    @Override
    public void init(Npc attacker, Mob defender) {
        try {
            normalAttackCount++;

            if (normalAttackCount == 3 || (normalAttackCount > 3 && (normalAttackCount - 3) % 6 == 0)) {
                spawnMinion(attacker, defender);
            }

            if (normalAttackCount >= 6) {
                normalAttackCount = 0;
                currentStrategy = RandomUtils.random(SPECIALS);
            } else {
                if (strategyQueue.isEmpty()) {
                    for (int i = 0; i < 3; i++) {
                        strategyQueue.add(RandomUtils.random(NORMAL_STRATEGIES));
                    }
                }
                currentStrategy = strategyQueue.poll();
                if (currentStrategy == null) {
                    currentStrategy = RandomUtils.random(NORMAL_STRATEGIES);
                }
            }
        } catch (Exception e) {
            logger.severe("Araxxor init error: " + e.getMessage() + " - falling back to melee");
            currentStrategy = MELEE;
        }
    }

    private void spawnMinion(Npc attacker, Mob defender) {
        try {
            if (!defender.isPlayer()) return;

            int color = Utility.random(2);
            int npcId = switch (color) {
                case 0 -> 11177;
                case 1 -> 11178;
                case 2 -> 11179;
                default -> 11177;
            };

            Position spawnPos = new Position(
                    attacker.getX() + Utility.random(-2, 2),
                    attacker.getY() + Utility.random(-2, 2),
                    attacker.getHeight()
            );
            Npc minion = new Npc(npcId, attacker.instance, spawnPos);
            minion.register();

            String colorName = switch (color) {
                case 0 -> "acidic";
                case 1 -> "mirrorback";
                case 2 -> "explosive";
                default -> "unknown";
            };
            if (defender.isPlayer()) {
                defender.getPlayer().message("@red@An " + colorName + " araxyte has appeared!");
            }
            logger.fine("Spawned " + colorName + " araxyte at " + spawnPos);
        } catch (Exception e) {
            logger.warning("Failed to spawn minion: " + e.getMessage());
        }
    }

    @Override
    public boolean canAttack(Npc attacker, Mob defender) {
        try {
            if (!enraged && attacker.getCurrentHealth() <= attacker.getMaximumHealth() * 0.25) {
                enraged = true;
                if (defender.isPlayer()) {
                    defender.getPlayer().message("@red@Araxxor lets out a piercing screech and becomes enraged!");
                }
                logger.info("Araxxor has become enraged!");
            }

            if (currentStrategy == MELEE && !MELEE.canAttack(attacker, defender)) {
                currentStrategy = RandomUtils.random(new CombatStrategy[]{RANGED, MAGIC});
            }
            return currentStrategy != null && currentStrategy.canAttack(attacker, defender);
        } catch (Exception e) {
            logger.severe("Araxxor canAttack error: " + e.getMessage());
            currentStrategy = MELEE;
            return true;
        }
    }

    @Override
    public boolean withinDistance(Npc attacker, Mob defender) {
        try {
            if (currentStrategy == MELEE && !MELEE.withinDistance(attacker, defender)) {
                currentStrategy = RandomUtils.random(new CombatStrategy[]{RANGED, MAGIC});
            }
            return currentStrategy != null && currentStrategy.withinDistance(attacker, defender);
        } catch (Exception e) {
            logger.severe("Araxxor withinDistance error: " + e.getMessage());
            return true;
        }
    }

    @Override
    public int getAttackDelay(Npc attacker, Mob defender, FightType fightType) {
        if (enraged) {
            return 4;
        }
        return 6;
    }

    private static class RangedAttack extends NpcRangedStrategy {
        RangedAttack() {
            super(CombatProjectile.getDefinition("EMPTY"));
        }

        @Override
        public Animation getAttackAnimation(Npc attacker, Mob defender) {
            return new Animation(9137, UpdatePriority.HIGH);
        }

        @Override
        public CombatHit[] getHits(Npc attacker, Mob defender) {
            CombatHit hit = nextRangedHit(attacker, defender, 26);
            if (hit.isAccurate()) {
                defender.venom();
            }
            return new CombatHit[]{hit};
        }

        @Override
        public int getAttackDistance(Npc attacker, FightType fightType) {
            return 8;
        }
    }

    private static class MagicAttack extends NpcMagicStrategy {
        MagicAttack() {
            super(CombatProjectile.getDefinition("EMPTY"));
        }

        @Override
        public Animation getAttackAnimation(Npc attacker, Mob defender) {
            return new Animation(9137, UpdatePriority.HIGH);
        }

        @Override
        public CombatHit[] getHits(Npc attacker, Mob defender) {
            CombatHit hit = nextMagicHit(attacker, defender, 26);
            hit.setAccurate(true);
            if (hit.isAccurate()) {
                defender.venom();
            }
            return new CombatHit[]{hit};
        }

        @Override
        public int getAttackDistance(Npc attacker, FightType fightType) {
            return 10;
        }
    }

    private static class VenomDrip extends NpcMagicStrategy {
        VenomDrip() {
            super(CombatProjectile.getDefinition("EMPTY"));
        }

        @Override
        public Animation getAttackAnimation(Npc attacker, Mob defender) {
            return new Animation(9137, UpdatePriority.HIGH);
        }

        @Override
        public void hit(Npc attacker, Mob defender, Hit hit) {
        }

        @Override
        public void block(Mob attacker, Npc defender, Hit hit, CombatType combatType) {
        }

        @Override
        public void start(Npc attacker, Mob defender, Hit[] hits) {
            try {
                if (!defender.isPlayer()) return;
                Player player = defender.getPlayer();
                player.message("@red@Araxxor drips venom towards you!");
                Position startPos = defender.getPosition().copy();

                World.schedule(new Task(2) {
                    int ticks = 0;
                    CustomGameObject pool;

                    @Override
                    public void execute() {
                        try {
                            if (attacker.isDead() || defender.isDead()) {
                                cancel();
                                return;
                            }
                            if (ticks == 0) {
                                pool = new CustomGameObject(33640, defender.instance, startPos);
                                pool.register();
                                World.sendGraphic(new Graphic(1487), startPos, defender.instance);
                                logger.fine("Venom pool spawned at " + startPos);
                            } else if (ticks > 10) {
                                if (pool != null) pool.unregister();
                                cancel();
                                return;
                            }
                            if (defender.getPosition().equals(startPos)) {
                                int damage = 4 * (ticks + 1);
                                defender.writeDamage(new Hit(damage));
                            }
                            ticks++;
                        } catch (Exception e) {
                            logger.warning("VenomDrip tick error: " + e.getMessage());
                            cancel();
                        }
                    }

                    @Override
                    public void onCancel(boolean logout) {
                        if (pool != null) pool.unregister();
                    }
                });
            } catch (Exception e) {
                logger.warning("VenomDrip start error: " + e.getMessage());
            }
        }

        @Override
        public CombatHit[] getHits(Npc attacker, Mob defender) {
            return new CombatHit[]{nextMagicHit(attacker, defender, 0)};
        }
    }

    private static class CleaveAttack extends NpcMagicStrategy {
        CleaveAttack() {
            super(CombatProjectile.getDefinition("EMPTY"));
        }

        @Override
        public Animation getAttackAnimation(Npc attacker, Mob defender) {
            return new Animation(9137, UpdatePriority.HIGH);
        }

        @Override
        public void hit(Npc attacker, Mob defender, Hit hit) {
        }

        @Override
        public void block(Mob attacker, Npc defender, Hit hit, CombatType combatType) {
        }

        @Override
        public void start(Npc attacker, Mob defender, Hit[] hits) {
            try {
                if (!defender.isPlayer()) return;
                defender.getPlayer().message("@red@Araxxor cleaves the ground!");
                attacker.animate(new Animation(9137, UpdatePriority.VERY_HIGH));

                Position bossPos = attacker.getPosition();
                int bossX = bossPos.getX();
                int bossY = bossPos.getY();
                int height = bossPos.getHeight();

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 1; dy <= 3; dy++) {
                        Position check = new Position(bossX + dx, bossY + dy, height);
                        if (defender.getPosition().equals(check)) {
                            int damage = Utility.random(25, 40);
                            defender.writeDamage(new Hit(damage));
                            logger.fine("Cleave hit " + defender.getName() + " for " + damage);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("CleaveAttack error: " + e.getMessage());
            }
        }

        @Override
        public CombatHit[] getHits(Npc attacker, Mob defender) {
            return new CombatHit[]{nextMagicHit(attacker, defender, 0)};
        }
    }

    private static class VenomBomb extends NpcMagicStrategy {
        VenomBomb() {
            super(CombatProjectile.getDefinition("EMPTY"));
        }

        @Override
        public Animation getAttackAnimation(Npc attacker, Mob defender) {
            return new Animation(9137, UpdatePriority.HIGH);
        }

        @Override
        public void hit(Npc attacker, Mob defender, Hit hit) {
        }

        @Override
        public void block(Mob attacker, Npc defender, Hit hit, CombatType combatType) {
        }

        @Override
        public void start(Npc attacker, Mob defender, Hit[] hits) {
            try {
                if (!defender.isPlayer()) return;
                defender.getPlayer().message("@red@Araxxor launches a venom bomb!");
                Position target = defender.getPosition().copy();

                World.schedule(2, () -> {
                    try {
                        if (attacker.isDead() || defender.isDead()) return;
                        World.sendGraphic(new Graphic(1487), target, defender.instance);

                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                Position poolPos = new Position(target.getX() + dx, target.getY() + dy, target.getHeight());
                                CustomGameObject pool = new CustomGameObject(33640, defender.instance, poolPos);
                                pool.register();

                                World.schedule(new Task(2) {
                                    int poolTicks = 0;

                                    @Override
                                    public void execute() {
                                        try {
                                            if (poolTicks >= 15) {
                                                pool.unregister();
                                                cancel();
                                                return;
                                            }
                                            if (defender.getPosition().equals(poolPos) && Area.inAraxxor(defender)) {
                                                defender.writeDamage(new Hit(Utility.random(5, 15)));
                                            }
                                            poolTicks++;
                                        } catch (Exception e) {
                                            logger.warning("VenomBomb pool tick error: " + e.getMessage());
                                            cancel();
                                        }
                                    }

                                    @Override
                                    public void onCancel(boolean logout) {
                                        pool.unregister();
                                    }
                                });
                            }
                        }
                        logger.fine("Venom bomb landed at " + target);
                    } catch (Exception e) {
                        logger.warning("VenomBomb explosion error: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.warning("VenomBomb start error: " + e.getMessage());
            }
        }

        @Override
        public CombatHit[] getHits(Npc attacker, Mob defender) {
            return new CombatHit[]{nextMagicHit(attacker, defender, 0)};
        }
    }

}
