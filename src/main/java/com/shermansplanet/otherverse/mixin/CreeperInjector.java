package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public class CreeperInjector extends Monster implements PowerableMob {

    private static int effectDuration = 20 * 30;

    private static MobEffectInstance[] possibleEffects = new MobEffectInstance[]{
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, effectDuration),
            new MobEffectInstance(MobEffects.HEALTH_BOOST, effectDuration),
            new MobEffectInstance(MobEffects.ABSORPTION, effectDuration),
            new MobEffectInstance(MobEffects.REGENERATION, effectDuration),
            new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, effectDuration),
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effectDuration),
            new MobEffectInstance(MobEffects.LUCK, effectDuration)
    };

    protected CreeperInjector(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }

    @Inject(method = "registerGoals", at = @At("RETURN"), cancellable = true)
    protected void registerGoalsOverride(CallbackInfo ci) {
        var avoidGoal = new AvoidEntityGoal<>(this, Player.class, (p_25052_) -> true,
                16.0F, 1.0D, 1.2D,
                (entity) -> entity instanceof Player player && (FamiliarManager.hasFamiliarType(player, EntityType.CAT) || FamiliarManager.hasFamiliarType(player, EntityType.OCELOT)));
        goalSelector.addGoal(3, avoidGoal);
    }

    public boolean isPowered() {
        return false;
    }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void explodeCreeper(CallbackInfo ci) {
        if (this.level().isClientSide) return;
        removeEffect(MobEffects.MOVEMENT_SPEED);
        removeEffect(MobEffects.DAMAGE_BOOST);
        if (!(getTarget() instanceof Player player) || !FamiliarManager.hasFamiliarType(player, EntityType.CREEPER))
            return;
        AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        areaeffectcloud.setRadius(2.5F);
        areaeffectcloud.setRadiusOnUse(-0.5F);
        areaeffectcloud.setWaitTime(10);
        areaeffectcloud.setDuration(areaeffectcloud.getDuration() / 2);
        areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / (float) areaeffectcloud.getDuration());
        var r = this.level().getRandom();
        var effectCount = r.nextInt(3) + 1;
        for (var i = 0; i < effectCount; i++) {
            var mobeffectinstance = possibleEffects[r.nextInt(possibleEffects.length)];
            areaeffectcloud.addEffect(new MobEffectInstance(mobeffectinstance));
        }
        this.level().addFreshEntity(areaeffectcloud);

        this.level().playSound(null, player, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1, 1);
        this.level().playSound(null, player, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 1, 1);


        ((ServerLevel) this.level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                getX(), getY(0.5f), getZ(),
                1, 0, 0, 0, 0.15f);


        this.dead = true;
        this.discard();
        ci.cancel();
    }
}
