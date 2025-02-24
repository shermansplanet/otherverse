package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.DemesnesRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public class ClientDataInjector {
    @Inject(method = "getDayTime()J", at = @At("HEAD"), cancellable = true)
    public void onGetDayTime(CallbackInfoReturnable<Long> ci) {
        var d = DemesnesRenderer.currentDemesne;
        if (d == null || d.dayTime == -1) return;
        ci.setReturnValue(d.dayTime);
        ci.cancel();
    }
}
