package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.spirits.SpiritType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
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
    private AABB ritualBounds;
    //private final HashSet<Entity> ritualEntities = new HashSet<>();
    public BlockPos minBlock, maxBlock;
    public int levelId;

    private int tickCount = 0;
    private int currentWaveIndex = 0;
    private DemesnesWave currentWave;

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
            LOGGER.debug("PLAYER DOESN'T EXIST");
            abandon();
            return;
        }
        if (!ritualBounds.contains(claimant.position())) {
            claimant.displayClientMessage(Component.literal("By leaving the Claimed area during the ritual, you forfeit your Claim."), false);
            LOGGER.debug("PLAYER LEFT BOUNDS");
            abandon();
            return;
        }

        if (currentWave == null) return;

        for (var mob : currentWave.entities) {
            if(ritualBounds.intersects(mob.getBoundingBox())) continue;
            mob.setTarget(claimant);
            mob.getNavigation().moveTo(claimant, 1);
        }

        var remainingHp = currentWave.getRemainingHp();
        demesnesEvent.setProgress(remainingHp);

        if (remainingHp == 0) {
            if (currentWaveIndex == 3) {
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

    /*public void onEntityAdded(EntityJoinLevelEvent event) {
        if (event.getLevel() != level) return;
        if (!ritualRange.contains(event.getEntity().position())) return;
        ritualEntities.add(event.getEntity());
    }*/
}
