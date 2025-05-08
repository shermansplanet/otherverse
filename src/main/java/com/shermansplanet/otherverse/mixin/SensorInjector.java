package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.artifacts.ConnectionBlockManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sensor.class)
public abstract class SensorInjector<E extends LivingEntity> {

    private static void cancelIfBlocked(LivingEntity le, LivingEntity target, CallbackInfoReturnable<Boolean> ci) {
        if(le.getLastHurtByMob() == target) return;
        if (ConnectionBlockManager.isBlocked(target)) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Inject(method = "isEntityTargetable", at = @At(value = "HEAD"), cancellable = true)
    private static void isEntityTargetable(LivingEntity p_26804_, LivingEntity p_26805_, CallbackInfoReturnable<Boolean> ci) {
        cancelIfBlocked(p_26804_, p_26805_, ci);
    }

    @Inject(method = "isEntityAttackable", at = @At(value = "HEAD"), cancellable = true)
    private static void isEntityAttackable(LivingEntity p_148313_, LivingEntity p_148314_, CallbackInfoReturnable<Boolean> ci) {
        cancelIfBlocked(p_148313_, p_148314_, ci);
    }

    @Inject(method = "isEntityAttackableIgnoringLineOfSight", at = @At(value = "HEAD"), cancellable = true)
    private static void isEntityAttackableIgnoringLineOfSight(LivingEntity p_182378_, LivingEntity p_182379_, CallbackInfoReturnable<Boolean> ci) {
        cancelIfBlocked(p_182378_, p_182379_, ci);
    }
}
