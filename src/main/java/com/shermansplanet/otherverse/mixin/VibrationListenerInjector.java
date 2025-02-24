package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VibrationListener.class)
public abstract class VibrationListenerInjector implements GameEventListener {

    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    public void onHandleGameEvent(ServerLevel sl, GameEvent.Message msg, CallbackInfoReturnable<Boolean> ci) {
        var sourceEntity = msg.context().sourceEntity();
        if (sourceEntity == null) return;
        if (FamiliarManager.isFamiliar(sourceEntity) && (
                sourceEntity.getType().equals(EntityType.CAT)
                        || sourceEntity.getType().equals(EntityType.OCELOT)
                        || sourceEntity.getType().equals(EntityType.FOX)
        )) {
            ci.setReturnValue(false);
            ci.cancel();
        }
        if (!(msg.context().sourceEntity() instanceof ServerPlayer sp)) return;
        if (!FamiliarManager.hasCatlikeBlessing(sp)) return;
        ci.setReturnValue(false);
        ci.cancel();
    }
}
