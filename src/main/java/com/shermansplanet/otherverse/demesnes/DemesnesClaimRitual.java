package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

import java.util.HashSet;

public class DemesnesClaimRitual {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float INTRO_TIME_TICKS = 5 * 20;

    private ServerBossEvent demesnesEvent;
    public final ServerPlayer claimant;
    public final ServerLevel level;
    public SpiritType spiritType;
    private final HashSet<ChunkPos> chunkPositions = new HashSet<>();
    public int range = -1;
    public AABB ritualBounds;
    //private final HashSet<Entity> ritualEntities = new HashSet<>();
    public BlockPos minBlock, maxBlock;
    public int levelId;

    private int tickCount = 0;
    private int currentWaveIndex = 0;
    private DemesnesWave currentWave;

    private int burnKills, otherMobKills, techKills, witherKills, allKills;
    private boolean didEncroach = false;

    public DemesnesClaimRitual(DemesnesClaimStartMessage msg, ServerPlayer player) {
        LOGGER.debug("DEMESNES: STARTING");
        this.claimant = player;
        this.level = player.getLevel();
        if (!(this.level.getBlockEntity(msg.centerPos()) instanceof DemesnesBeacon beacon)) return;
        beacon.recalculatePositions();
        range = beacon.range;
        minBlock = beacon.minBlock;
        maxBlock = beacon.maxBlock;
        spiritType = beacon.spiritType;
        ritualBounds = new AABB(minBlock, maxBlock);
        makeEvent();
        levelId = DiagramManager.getDimensionHash(level);
    }

    private void makeEvent() {
        demesnesEvent = new ServerBossEvent(
                Component.literal(claimant.getGameProfile().getName() + "'s Demesnes Claim"),
                BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        demesnesEvent.setProgress(0f);
        demesnesEvent.setVisible(true);
        demesnesEvent.addPlayer(claimant);
    }

    public void tick() {
        tickCount++;
        if (currentWaveIndex == 0) {
            var progress = Mth.clamp(tickCount / INTRO_TIME_TICKS, 0, 1);
            demesnesEvent.setProgress(progress);
            if (progress == 1) setWave(1);
        }
        if (tickCount % 10 != 0) return;
        if (claimant == null || !claimant.isAddedToWorld() || !claimant.isAlive()) {
            abandon();
            return;
        }
        if (!ritualBounds.contains(claimant.position())) {
            claimant.displayClientMessage(Component.literal("By leaving the Claimed area during the ritual, you forfeit your Claim."), false);
            abandon();
            return;
        }

        if (currentWave == null) return;

        for (var mob : currentWave.entities) {
            if (ritualBounds.intersects(mob.getBoundingBox())) continue;
            mob.setTarget(claimant);
            mob.getNavigation().moveTo(claimant, 1);
        }

        var remainingHp = currentWave.getRemainingHp();
        if (!didEncroach && currentWave.areAnyEncroaching(ritualBounds)) didEncroach = true;
        demesnesEvent.setProgress(remainingHp);

        if (remainingHp == 0) {
            if (currentWaveIndex == 3) {
                if (!didEncroach) {
                    spiritType = Spirits.PROTECTION;
                } else if (burnKills * 1f / allKills > 0.5f) {
                    spiritType = Spirits.FIRE;
                } else if (techKills * 1f / allKills > 0.5f) {
                    spiritType = Spirits.TECH;
                } else if (otherMobKills * 1f / allKills > 0.5f) {
                    spiritType = Spirits.FLESH;
                } else if (witherKills * 1f / allKills > 0.5f) {
                    spiritType = Spirits.DEATH;
                }
                DemesnesManager.complete(this);
                cleanup();
                return;
            }
            setWave(currentWaveIndex + 1);
        }
    }

    private void setWave(int i) {
        currentWaveIndex = i;
        currentWave = new DemesnesWave(this, i);
    }

    public void abandon() {
        LOGGER.debug("DEMESNE: ABANDONING");
        DemesnesManager.abandon(this);
    }

    public void cleanup() {
        if (demesnesEvent != null) {
            demesnesEvent.removeAllPlayers();
            demesnesEvent.setVisible(false);
        }
        if (currentWave != null) {
            for (var entity : currentWave.entities) {
                if (entity == null || !entity.isAddedToWorld() || !entity.isAlive()) continue;
                var r = level.random;
                for (int i = 0; i < 8; ++i) {
                    double d0 = r.nextGaussian() * 0.02D;
                    double d1 = r.nextGaussian() * 0.02D;
                    double d2 = r.nextGaussian() * 0.02D;
                    level.sendParticles(ParticleTypes.POOF,
                            entity.getRandomX(1.0D),
                            entity.getRandomY(),
                            entity.getRandomZ(1.0D),
                            1, d0, d1, d2, 0.15f);
                }
                entity.discard();
            }
        }
    }

    public void addEntity(Entity entity) {
        currentWave.entities.add((Mob) entity);
    }

    public void onChallengerDeath(LivingDeathEvent event) {
        allKills++;
        var source = event.getSource();
        if (source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE || source == DamageSource.HOT_FLOOR || source == DamageSource.LAVA) {
            burnKills++;
        } else if (source == DamageSource.CRAMMING || source == DamageSource.FALL || source == DamageSource.FALLING_BLOCK
                || source == DamageSource.IN_WALL || source == DamageSource.FALLING_STALACTITE || source == DamageSource.ANVIL) {
            techKills++;
        } else if (source == DamageSource.WITHER) {
            witherKills++;
        } else if (source.getMsgId().equals("arrow") && source.getEntity() == null) {
            techKills++;
        }

        if (source.getEntity() != null && source.getEntity().getType() != EntityType.PLAYER) {
            if (source.getEntity().getPersistentData().getString("construct_type").equals("technology")) {
                techKills++;
            } else {
                otherMobKills++;
            }
        }
    }

    /*public void onEntityAdded(EntityJoinLevelEvent event) {
        if (event.getLevel() != level) return;
        if (!ritualRange.contains(event.getEntity().position())) return;
        ritualEntities.add(event.getEntity());
    }*/
}
