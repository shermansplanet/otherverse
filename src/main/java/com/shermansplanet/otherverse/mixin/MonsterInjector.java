package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public abstract class MonsterInjector extends PathfinderMob implements Enemy {

    protected MonsterInjector(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
    }

    @Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
    protected void isSafe(Player p_33036_, CallbackInfoReturnable<Boolean> ci) {
        if (this.getPersistentData().contains("bindingId")) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }
}
