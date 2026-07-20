package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.ReskinManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.HashSet;

@Mixin(GeoEntityRenderer.class)
public abstract class GeoEntityRendererInjector<T extends Entity & GeoAnimatable> extends EntityRenderer<T> implements GeoRenderer<T> {
    protected GeoEntityRendererInjector(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    private static HashSet<LivingEntity> golems = new HashSet<>();

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    protected void onGetTexLoc(T animatable, CallbackInfoReturnable<ResourceLocation> ci) {
        if (!(animatable instanceof LivingEntity le)) return;
        if (!ReskinManager.shouldReskin(le)) return;
        if(!golems.contains(le)) {
            golems.add(le);
            return;
        }
        ci.setReturnValue(ReskinManager.getSkinLocation(le));
        ci.cancel();
    }
}
