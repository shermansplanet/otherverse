package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.ruins.RuinsManager;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(DimensionSpecialEffects.NetherEffects.class)
public abstract class NetherEffectsInjector extends DimensionSpecialEffects {

    public NetherEffectsInjector(float p_108866_, boolean p_108867_, SkyType p_108868_, boolean p_108869_, boolean p_108870_) {
        super(p_108866_, p_108867_, p_108868_, p_108869_, p_108870_);
    }

    @Inject(method = "getBrightnessDependentFogColor", at = @At("HEAD"), cancellable = true)
    public void onFogColor(Vec3 col, float f, CallbackInfoReturnable<Vec3> ci) {
        var mul = RuinsManager.skyMultiplier;
        mul = 1f + (mul - 0.5f) * 2.5f;
        ci.setReturnValue(col.scale(mul));
    }
}
