package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.diagrams.SelfManager;
import com.shermansplanet.otherverse.ruins.RuinsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import static com.ibm.icu.impl.ValidIdentifiers.Datatype.x;

public class Snuffer extends Monster {

    private final static float DETECTION_RANGE = 8;

    public Snuffer(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
        this.getNavigation().setCanFloat(true);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> et, LevelAccessor level, MobSpawnType spawnType, BlockPos blockPos, RandomSource r) {
        return true;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SnufferMoveGoal(this));
    }

    protected float getBlockSpeedFactor() {
        return this.onSoulSpeedBlock() ? 1.0F : super.getBlockSpeedFactor();
    }

    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 4));
            return false;
        }
        return super.hurt(source, amount);
    }

    private class SnufferMoveGoal extends Goal {
        private Snuffer snuffer;
        private int attackCooldown;
        private int pathfindCooldown;

        public SnufferMoveGoal(Snuffer snuffer) {
            this.snuffer = snuffer;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            if (attackCooldown > 0) attackCooldown--;
            if (pathfindCooldown > 0) {
                pathfindCooldown--;
                return;
            }
            pathfindCooldown = snuffer.random.nextInt(10, 20);
            var target = snuffer.getTarget();
            if (target == null) {
                var player = snuffer.level().getNearestPlayer(snuffer, DETECTION_RANGE);
                if (player != null) {
                    target = player;
                    snuffer.setTarget(player);
                    snuffer.setAggressive(true);
                }
            } else {
                snuffer.navigation.moveTo(target, 1);
                var dist = target.distanceToSqr(snuffer);
                if (dist > DETECTION_RANGE * DETECTION_RANGE) {
                    target = null;
                    snuffer.setTarget(null);
                    snuffer.setAggressive(false);
                }
            }
            if (target != null && attackCooldown <= 0 && snuffer.getPerceivedTargetDistanceSquareForMeleeAttack(target) < 2 * 2) {
                SelfManager.SelfDrainAttack(snuffer, target);
                attackCooldown = 20;
            }
            if (target == null) {
                var closestScent = RuinsManager.getClosestScent(snuffer.level(), snuffer.position());
                if (closestScent == null) return;
                snuffer.navigation.moveTo(closestScent.x, closestScent.y, closestScent.z, 1);
            }
        }
    }
}
