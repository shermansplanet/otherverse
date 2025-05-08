package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.artifacts.ConnectionBlockManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalInjector<T extends LivingEntity> extends TargetGoal {

    @Shadow
    protected LivingEntity target;

    public NearestAttackableTargetGoalInjector(Mob p_26140_, boolean p_26141_) {
        super(p_26140_, p_26141_);
    }

    @Inject(method = "findTarget", at = @At(value = "RETURN"))
    protected void onFindTarget(CallbackInfo ci) {
        if(this.mob.getLastHurtByMob() == this.target) return;
        if(ConnectionBlockManager.isBlocked(this.target)) this.target = null;
    }
}
