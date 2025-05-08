package com.shermansplanet.otherverse.others;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class TyphloticJellyfish extends FlyingMob {
    public AnimationState idleAnimationState = new AnimationState();
    public int age;

    public TyphloticJellyfish(EntityType<? extends FlyingMob> p_20806_, Level p_20807_) {
        super(p_20806_, p_20807_);
        this.idleAnimationState.start(this.tickCount);
        this.moveControl = new JellyfishMoveControl(this);
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
        this.goalSelector.addGoal(1, new JellyfishSeekPotionGoal(this));
    }

    public void customServerAiStep() {
        age = (age + 1) % 40;
        if (getTarget() == null) {
            var p = level.getNearestPlayer(TargetingConditions
                            .forCombat()
                            .range(64)
                            .ignoreLineOfSight()
                            .selector(e -> !e.getActiveEffects().isEmpty()),
                    this);
            setTarget(p);
        } else if (getTarget().distanceTo(this) > 64 || !this.canAttack(getTarget())) {
            setTarget(null);
        }
    }

    private static class JellyfishMoveControl extends MoveControl {
        private TyphloticJellyfish jellyfish;
        private float localUpSpeed = 0f;

        public JellyfishMoveControl(TyphloticJellyfish jelly) {
            super(jelly);
            this.jellyfish = jelly;
            jellyfish.setNoGravity(true);
        }

        public void tick() {
            double dx = this.wantedX - this.mob.getX();
            double dy = this.wantedY - this.mob.getY();
            double dz = this.wantedZ - this.mob.getZ();
            if (dx * dx + dy * dy + dz * dz < (double) 2.5000003E-7F) {
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
                return;
            }

            float f = (float) (Mth.atan2(dz, dx) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, 90.0F));

            var pushTime = 29;
            var isPushing = (jellyfish.age % 40) >= pushTime;
            if (isPushing) localUpSpeed += 0.015f * (40 - (jellyfish.age % 40)) / (40 - pushTime);
            localUpSpeed *= 0.9f;
            var dir = new Vec3(dx, dy + 0.9f, dz).normalize().scale(localUpSpeed);
            mob.push(dir.x, dir.y - 0.01f, dir.z);
        }
    }

    private static class JellyfishSeekPotionGoal extends Goal {
        private final TyphloticJellyfish jellyfish;
        private int pathfindCooldown;
        private int attackCooldown;

        public JellyfishSeekPotionGoal(TyphloticJellyfish jelly) {
            this.jellyfish = jelly;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return jellyfish.getTarget() != null;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            var target = jellyfish.getTarget();
            if (target == null) return;
            pathfindCooldown--;
            if (pathfindCooldown <= 0) {
                this.jellyfish.getNavigation().moveTo(target, 1);
                pathfindCooldown = 5;
            }
            attackCooldown--;
            if (attackCooldown <= 0 && jellyfish.distanceTo(target) < 1f) {
                jellyfish.doHurtTarget(target);
                attackCooldown = 20;
            }
        }
    }
}
