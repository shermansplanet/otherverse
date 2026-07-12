package com.shermansplanet.otherverse.others;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.ForgeClientEvents;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.diagrams.SelfManager;
import com.shermansplanet.otherverse.ruins.ShockLightningMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

public class Buzzed extends FlyingMob {
    private int attackCooldown = 0;
    private boolean firstTick = true;

    public Buzzed(EntityType<? extends FlyingMob> p_20806_, Level p_20807_) {
        super(p_20806_, p_20807_);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> et, LevelAccessor level, MobSpawnType spawnType, BlockPos blockPos, RandomSource r) {
        return true;
    }

    protected @NotNull PathNavigation createNavigation(Level p_27815_) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_27815_) {
            public boolean isStableDestination(BlockPos p_27947_) {
                return !this.level.getBlockState(p_27947_.below()).isAir();
            }
        };
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(false);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(2, new AttackGoal(this));
        this.goalSelector.addGoal(4, new WanderGoal(this));
    }

    protected SoundEvent getHurtSound(DamageSource p_27845_) {
        return SoundEvents.BEE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    public void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) attackCooldown--;
    }

    public void aiStep() {
        if (level().isClientSide() && firstTick) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientEvents.addBuzzed(this));
            firstTick = false;
        }
        super.aiStep();
    }

    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && attackCooldown > 0) {
            return;
        }
        super.setTarget(target);
    }

    private static class WanderGoal extends Goal {
        private final Buzzed buzzed;
        private Vec3 velocity;
        private int turnTime = 0;
        private Quaternionf rotation = new Quaternionf();

        public WanderGoal(Buzzed buzzed) {
            this.buzzed = buzzed;
            velocity = buzzed.getForward().normalize();
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        public boolean requiresUpdateEveryTick() {
            return buzzed.getTarget() == null;
        }

        @Override
        public boolean canUse() {
            return buzzed.getTarget() == null;
        }

        public void tick() {
            if (buzzed.invulnerableTime > 0) return;
            if (turnTime == 0) {
                var r = buzzed.random;
                var axis = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
                rotation = new Quaternionf().fromAxisAngleDeg(axis, 5);
                turnTime = r.nextInt(20, 100);
            } else {
                turnTime--;
            }
            var basePos = buzzed.position().add(0, buzzed.getBbHeight() / 2f, 0);
            if (basePos.y > buzzed.level().getMaxBuildHeight() - 16) {
                velocity = new Vec3(velocity.x, -1, velocity.z).normalize();
            } else if (basePos.y < buzzed.level().getMinBuildHeight()) {
                velocity = new Vec3(velocity.x, 1, velocity.z).normalize();
            } else {
                var newVel = rotation.transform(velocity.toVector3f());
                velocity = new Vec3(newVel.x, newVel.y, newVel.z).normalize();
                BlockHitResult hit = buzzed.level().clip(new ClipContext(
                        basePos, basePos.add(velocity.scale(0.5f)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, buzzed));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    var axis = hit.getDirection().getAxis();
                    velocity = velocity.multiply(axis == Direction.Axis.X ? -1 : 1,
                            axis == Direction.Axis.Y ? -1 : 1,
                            axis == Direction.Axis.Z ? -1 : 1);
                }
            }
            buzzed.setYRot((float) (Math.atan2(velocity.z, velocity.x) * 180 / Math.PI) - 90);
            buzzed.setDeltaMovement(velocity.scale(0.5f));
        }
    }

    private static class AttackGoal extends Goal {
        private final Buzzed buzzed;
        private int pathfindingCooldown = 0;

        public AttackGoal(Buzzed buzzed) {
            this.buzzed = buzzed;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            return buzzed.getTarget() != null;
        }

        public void tick() {
            var target = buzzed.getTarget();
            if (target == null) return;
            if (target.isDeadOrDying() || target.level() != buzzed.level() || target.distanceToSqr(buzzed) > 33 * 33) {
                buzzed.setTarget(null);
                return;
            }
            if (buzzed.isWithinMeleeAttackRange(target)) {
                if (!((target instanceof Player p) && p.isCreative())) {
                    SelfManager.SelfDrainAttack(buzzed, target);
                    if (target instanceof Player p) p.giveExperienceLevels(-1);
                }
                buzzed.setTarget(null);
                buzzed.attackCooldown = buzzed.random.nextInt(100, 600);
                return;
            }
            if (pathfindingCooldown > 0) {
                pathfindingCooldown--;
                return;
            }
            pathfindingCooldown = buzzed.random.nextInt(5, 10);
            buzzed.getNavigation().moveTo(target, 1);
        }
    }
}
