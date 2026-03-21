package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.DemesnesRenderer;
import com.shermansplanet.otherverse.spirits.Chronomancy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelInjector extends Level {
    private boolean wasInDemesne = false;

    protected ClientLevelInjector(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    public void tickNonPassenger(Entity e, CallbackInfo ci) {
        if (Chronomancy.frozenEntitiesForClient.contains(e)) {
            ci.cancel();
        }
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
