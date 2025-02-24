package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractChestedHorse.class)
public abstract class AbstractChestedHorseInjector extends AbstractHorse {

    protected AbstractChestedHorseInjector(EntityType<? extends AbstractHorse> p_30531_, Level p_30532_) {
        super(p_30531_, p_30532_);
    }

    @Inject(method = "getInventorySize", at = @At("HEAD"), cancellable = true)
    protected void onGetInventorySize(CallbackInfoReturnable<Integer> ci) {
        if (!FamiliarManager.isChestedHorseFamiliar(this)) return;
        ci.setReturnValue(54);
        ci.cancel();
    }
}
