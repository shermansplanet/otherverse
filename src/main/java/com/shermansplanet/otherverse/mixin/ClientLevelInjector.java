package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.DemesnesRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelInjector extends Level {
    private boolean wasInDemesne = false;

    protected ClientLevelInjector(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    public void onGetSkyColor(Vec3 p_171661_, float p_171662_, CallbackInfoReturnable<Vec3> ci) {
        var d = DemesnesRenderer.currentDemesne;
        if (d == null || !d.hasColor) return;
        float f = this.getTimeOfDay(p_171662_);
        float f1 = Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        ci.setReturnValue(new Vec3(
                d.color.x * f1,
                d.color.y * f1,
                d.color.z * f1
        ));
        ci.cancel();
    }
}
