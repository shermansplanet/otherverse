package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BindingManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Brain.class)
public class BrainInjector<E extends LivingEntity> {

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void onTick(ServerLevel p_21866_, E mob, CallbackInfo ci) {
        if(mob instanceof Villager && BindingManager.isBoundOrContracted(mob)) ci.cancel();
    }
}
