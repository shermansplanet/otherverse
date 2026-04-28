package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.diagrams.SelfManager;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class TyphloticJellyfish extends FlyingMob {
    public float xBodyRot;
    public float xBodyRotO;
    public float zBodyRot;
    public float zBodyRotO;
    public float animTime;
    public float oldAnimTime;
    public AnimationState idleAnimationState = new AnimationState();

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

    public void aiStep() {
        super.aiStep();
        this.xBodyRotO = this.xBodyRot;
        this.zBodyRotO = this.zBodyRot;
        this.oldAnimTime = this.animTime;
        this.animTime++;

        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        this.yBodyRot += (-((float) Mth.atan2(vec3.x, vec3.z)) * (180F / (float) Math.PI) - this.yBodyRot) * 0.1F;
        this.setYRot(this.yBodyRot);
        this.zBodyRot += (float) Math.PI * 0.2F;
        this.xBodyRot += (-((float) Mth.atan2(d0, vec3.y)) * (180F / (float) Math.PI) - this.xBodyRot) * 0.1F;
    }

    public void handleEntityEvent(byte p_29957_) {
        if (p_29957_ == 19) {
            this.animTime = 0;
            this.oldAnimTime = 0;
        } else {
            super.handleEntityEvent(p_29957_);
        }

    }

    public void customServerAiStep() {
        if (getTarget() == null) {
            var p = level().getNearestPlayer(TargetingConditions
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
        private int pushCountdown = 0;

        public JellyfishMoveControl(TyphloticJellyfish jelly) {
            super(jelly);
            this.jellyfish = jelly;
            jellyfish.setNoGravity(true);
        }

        public void tick() {
            double dx = this.wantedX - this.mob.getX();
            double dy = this.wantedY - this.mob.getY();
            double dz = this.wantedZ - this.mob.getZ();
            if (dx * dx + dy * dy + dz * dz < 0.1) {
                jellyfish.setYya(0.0F);
                jellyfish.setZza(0.0F);
                return;
            }

            if (++pushCountdown >= 40) {
                pushCountdown = 0;
                jellyfish.level().broadcastEntityEvent(jellyfish, (byte) 19);
            }

            var toTarget = new Vec3(dx, dy, dz).normalize().scale(0.02f);

            if (pushCountdown < 10) {
                jellyfish.push(toTarget.x, toTarget.y, toTarget.z);
            }
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
            return true;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            var target = jellyfish.getTarget();
            pathfindCooldown--;
            if (target != null && pathfindCooldown > 5) pathfindCooldown = 0;
            if (pathfindCooldown <= 0) {
                if (target == null) {
                    var r = jellyfish.random;
                    var x = jellyfish.getX() + (r.nextDouble() - 0.5) * 64;
                    var z = jellyfish.getZ() + (r.nextDouble() - 0.5) * 64;
                    var y = jellyfish.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) x, (int) z)
                            + r.nextDouble() * 16;
                    this.jellyfish.getNavigation().moveTo(x, y, z, 0.5f);
                    pathfindCooldown = r.nextInt(100, 1000);
                } else {
                    this.jellyfish.getNavigation().moveTo(target, 1);
                    pathfindCooldown = 5;
                }
            }
            if (target == null) return;
            attackCooldown--;
            if (attackCooldown <= 0 && jellyfish.isWithinMeleeAttackRange(target)) {
                SelfManager.SelfDrainAttack(jellyfish, target);
                attackCooldown = 20;
            }
        }
    }
}
