package com.shermansplanet.otherverse.others;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.SelfManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.EnumSet;

public class TyphloticShark extends FlyingMob {
    private static final Logger LOGGER = LogUtils.getLogger();
    public AnimationState swimAnimationState = new AnimationState();
    public AnimationState attackAnimationState = new AnimationState();

    public TyphloticShark(EntityType<? extends FlyingMob> p_20806_, Level p_20807_) {
        super(p_20806_, p_20807_);
        swimAnimationState.start(this.tickCount);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> et, LevelAccessor level, MobSpawnType spawnType, BlockPos blockPos, RandomSource r) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            return true;
        }
        var bs = level.getBlockState(blockPos.below());
        if (!bs.is(Blocks.POWDER_SNOW)) return false;
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
        this.goalSelector.addGoal(1, new SharkSeekBloodGoal(this));
        this.goalSelector.addGoal(2, new SharkMoveGoal(this));
    }

    public boolean canFreeze() {
        return false;
    }

    public boolean hurt(DamageSource src, float amnt) {
        if (src.getDirectEntity() instanceof LivingEntity le) {
            setTarget(le);
        }
        return super.hurt(src, amnt);
    }

    public void customServerAiStep() {
        if (getTarget() == null) {
            var p = level().getNearestPlayer(TargetingConditions
                            .forCombat()
                            .range(64)
                            .ignoreLineOfSight()
                            .selector(e -> e.getHealth() < e.getMaxHealth()),
                    this);
            setTarget(p);
        } else if (getTarget().distanceTo(this) > 64 || !this.canAttack(getTarget())) {
            setTarget(null);
        }
    }

    public void swing(InteractionHand hand) {
        if (this.level().isClientSide()) {
            this.attackAnimationState.start(this.tickCount);
        } else {
            super.swing(hand);
        }
    }

    private static class SharkSeekBloodGoal extends Goal {
        private final TyphloticShark shark;
        private int pathfindCooldown;
        private int attackCooldown;

        public SharkSeekBloodGoal(TyphloticShark shark) {
            this.shark = shark;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return shark.getTarget() != null;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            var target = shark.getTarget();
            if (target == null) return;
            pathfindCooldown--;
            if (pathfindCooldown <= 0) {
                if (shark.level().getBlockState(shark.blockPosition()).is(Blocks.POWDER_SNOW)) {
                    this.shark.setDeltaMovement(this.shark.getDeltaMovement().add(0, 0.2f, 0));
                } else {
                    this.shark.getNavigation().moveTo(target, 1);
                    pathfindCooldown = shark.random.nextInt(5, 10);
                }
            }
            attackCooldown--;
            if (attackCooldown <= 0 && shark.isWithinMeleeAttackRange(target)) {
                shark.swing(InteractionHand.MAIN_HAND);
                SelfManager.SelfDrainAttack(shark, target);
                attackCooldown = 20;
            }
        }
    }

    private static class SharkMoveGoal extends Goal {
        private final TyphloticShark shark;
        private int coeff = 1;

        public SharkMoveGoal(TyphloticShark shark) {
            this.shark = shark;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public boolean canUse() {
            return shark.getTarget() == null;
        }

        public void start() {
            coeff = shark.random.nextBoolean() ? 1 : -1;
        }

        private final float lookAheadDist = 6;

        public void tick() {
            if (shark.random.nextFloat() < 0.01f) coeff = -coeff;
            var castFromTop = shark.position().add(0f, shark.getBbHeight(), 0f);
            BlockHitResult hitresultTop = shark.level().clip(new ClipContext(
                    castFromTop, castFromTop.add(shark.getForward().scale(lookAheadDist)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shark));
            BlockHitResult hitresultBottom = shark.level().clip(new ClipContext(
                    shark.position(), shark.position().add(shark.getForward().scale(lookAheadDist)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shark));
            var diff = shark.invulnerableTime > 0 ? shark.getDeltaMovement() : shark.getForward().scale(1f / 20f);
            var closenessTop = hitresultTop.getType() == HitResult.Type.MISS ? 0f
                    : (float) Math.max(0f, 1f - (hitresultTop.getLocation().distanceTo(castFromTop)) / lookAheadDist);
            var closenessBottom = hitresultBottom.getType() == HitResult.Type.MISS ? 0f
                    : (float) Math.max(0f, 1f - (hitresultBottom.getLocation().distanceTo(shark.position())) / lookAheadDist);
            var closeness = Math.max(closenessTop, closenessBottom);
            float angle = closeness * 20 + 0.5f;
            /*if (hitresult.getType() == HitResult.Type.BLOCK) {
                var bs = shark.level.getBlockState(hitresult.getBlockPos());
                if (bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK)) {
                    diff = diff.add(new Vec3(0, closeness / 2, 0));
                    if (shark.isInPowderSnow && shark.level.isEmptyBlock(hitresult.getBlockPos().above())) angle = 0;
                }
            }*/
            shark.setYRot(shark.getYRot() + angle * coeff);

            var pos = shark.blockPosition();
            for (var i = 2; i >= -2; i--) {
                var bs = shark.level().getBlockState(pos.above(i));
                if (bs.is(Blocks.POWDER_SNOW)) {
                    var dy = (pos.getY() + i) - shark.position().y + 0.49f;
                    diff = diff.add(new Vec3(0, dy / 10f, 0));
                    break;
                }
                if (!bs.is(Blocks.AIR)) {
                    var dy = (pos.getY() + i) - shark.position().y + 1.1f;
                    diff = diff.add(new Vec3(0, dy / 10f, 0));
                    break;
                }
                if (i == -2) {
                    diff = diff.add(new Vec3(0, -0.1f, 0));
                }
            }
            shark.setDeltaMovement(diff);
        }
    }
}
