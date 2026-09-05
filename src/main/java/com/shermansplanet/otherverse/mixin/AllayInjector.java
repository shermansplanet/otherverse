package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BindingManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public abstract class AllayInjector extends PathfinderMob implements InventoryCarrier {

    protected AllayInjector(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    protected void onAiStep(CallbackInfo ci) {
        if (BindingManager.isBoundOrContracted(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    protected void onMobInteract(Player p_218361_, InteractionHand p_218362_, CallbackInfoReturnable<InteractionResult> ci) {
        if (BindingManager.isBoundOrContracted(this)) {
            ci.setReturnValue(InteractionResult.PASS);
            ci.cancel();
        }
    }
}