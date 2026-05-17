package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobInjector extends LivingEntity {
    protected MobInjector(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Shadow
    protected LookControl lookControl;
    @Shadow
    protected MoveControl moveControl;
    @Shadow
    protected JumpControl jumpControl;

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    protected void isSunBurn(CallbackInfoReturnable<Boolean> ci) {
        if (FamiliarManager.isFamiliar(this)) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Inject(method = "serverAiStep", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
            args = "ldc=mob tick"), cancellable = true)
    protected final void onServerAiStep(CallbackInfo ci) {
        if (!getPersistentData().contains("bindingId") || this.getType().equals(EntityType.WITHER)) return;
        var activity = getBrain().getActiveNonCoreActivity();
        if(activity.isPresent() && activity.get() == Activity.FIGHT) return;
        ci.cancel();
        this.level().getProfiler().push("controls");
        this.level().getProfiler().push("move");
        this.moveControl.tick();
        this.level().getProfiler().popPush("look");
        this.lookControl.tick();
        this.level().getProfiler().popPush("jump");
        this.jumpControl.tick();
        this.level().getProfiler().pop();
        this.level().getProfiler().pop();
        this.sendDebugPackets();
    }

    @Shadow
    protected void sendDebugPackets() {

    }
}
