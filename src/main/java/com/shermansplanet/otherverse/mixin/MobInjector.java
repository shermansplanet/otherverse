package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobInjector extends LivingEntity {
    protected MobInjector(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    protected void isSunBurn(CallbackInfoReturnable<Boolean> ci) {
        if(FamiliarManager.isFamiliar(this)){
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    protected void cancelAi(CallbackInfo ci) {
        if(getPersistentData().contains("bindingId")){
            ci.cancel();
        }
    }
}
