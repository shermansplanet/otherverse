package com.shermansplanet.otherverse.others;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.diagrams.SelfManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
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
        this.goalSelector.addGoal(3, new JellyfishSeekPotionGoal(this));
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

        if (level() instanceof ServerLevel sl && sl.getGameTime() % 20 == 0) {
            var pract = FamiliarManager.getPractitionerForFamiliar(this);
            if (!pract.isEmpty()) {
                var item = BindingManager.getHeldItem(this);
                var heldEffects = PotionUtils.getMobEffects(item);
                var persistentData = this.getPersistentData();
                if (!heldEffects.isEmpty()) {
                    var firstEffect = heldEffects.get(0);
                    var key = ForgeRegistries.MOB_EFFECTS.getKey(firstEffect.getEffect());
                    if (key != null) {
                        persistentData.putString("lastPotion", key.toString());
                        persistentData.putLong("endTime", firstEffect.getDuration() + level().getGameTime());
                        persistentData.putInt("lastAmplifier", firstEffect.getAmplifier());
                    }
                    BindingManager.setHeldItem(this, ItemStack.EMPTY);
                    sl.playSound(null, this, SoundEvents.WITCH_DRINK, SoundSource.NEUTRAL, 1f, 1f);
                }
                var endTime = persistentData.getLong("endTime");
                var effect = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                if (endTime > level().getGameTime()) {
                    var effectName = persistentData.getString("lastPotion");
                    var customEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(effectName));
                    if (customEffect != null) {
                        effect = new MobEffectInstance(customEffect, 40, persistentData.getInt("lastAmplifier"));
                    }
                }
                for (var entity : sl.getEntities(this, this.getBoundingBox().inflate(8))) {
                    if (!(entity instanceof LivingEntity le)) continue;
                    if (!effect.getEffect().isBeneficial() && BindingManager.isAlliedWith(le, pract)) continue;
                    le.addEffect(effect);
                }
            }
        }
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
                            .selector(e -> {
                                if (e.getLastHurtMob() == this) return true;
                                for (var effect : e.getActiveEffects()) {
                                    if (effect.isVisible() && effect.getDuration() > 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }),
                    this);
            setTarget(p);
        } else if (getTarget().distanceTo(this) > 64 || !this.canAttack(getTarget())) {
            setTarget(null);
        }
        super.customServerAiStep();
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
            if (!this.hasWanted()) {
                jellyfish.setYya(0.0F);
                jellyfish.setZza(0.0F);
                return;
            }
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

    public static class JellyfishSeekPotionGoal extends Goal {
        private final TyphloticJellyfish jellyfish;
        private int pathfindCooldown;
        private int attackCooldown;

        public JellyfishSeekPotionGoal(TyphloticJellyfish jelly) {
            this.jellyfish = jelly;
            this.setFlags(EnumSet.of(Flag.MOVE));
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
            if (attackCooldown <= 0 && jellyfish.getPerceivedTargetDistanceSquareForMeleeAttack(target) < 1.75f * 1.75f) {
                SelfManager.SelfDrainAttack(jellyfish, target);
                attackCooldown = 20;
            }
        }
    }
}
