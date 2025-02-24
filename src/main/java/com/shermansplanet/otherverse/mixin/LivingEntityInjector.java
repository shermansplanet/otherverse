package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.ISoundGetter;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInjector extends Entity implements net.minecraftforge.common.extensions.IForgeLivingEntity, ISoundGetter {

    @Shadow
    private Optional<BlockPos> lastClimbablePos = Optional.empty();

    public LivingEntityInjector(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Shadow
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.GENERIC_HURT;
    }

    public SoundEvent publicGetHurtSound(DamageSource src) {
        return getHurtSound(src);
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    public void onClimbableInject(CallbackInfoReturnable<Boolean> ci) {
        if(self().getType() != EntityType.PLAYER) return;
        var player = (Player) self();
        if (!FamiliarManager.hasFamiliarType(player, EntityType.SPIDER)) return;
        var pos = player.blockPosition().relative(player.getDirection());
        var state = player.getLevel().getBlockState(pos);
        if (state.getCollisionShape(player.level, pos).isEmpty()) return;
        ci.setReturnValue(true);
        ci.cancel();
    }
}
