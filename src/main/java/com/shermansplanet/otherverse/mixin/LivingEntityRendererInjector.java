package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.ReskinManager;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererInjector<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {

    @Shadow
    protected M model;
    protected LivingEntityRendererInjector(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    protected void onGetRenderType(T le, boolean p_115323_, boolean p_115324_, boolean p_115325_, CallbackInfoReturnable<RenderType> ci) {
        if(!ReskinManager.shouldReskin(le)) return;
        ResourceLocation resourcelocation = ReskinManager.getSkinLocation(le);
        ci.setReturnValue(RenderType.entityCutoutNoCull(resourcelocation));
        /*if (p_115324_) {
            ci.setReturnValue(RenderType.itemEntityTranslucentCull(resourcelocation));
        } else if (p_115323_) {
            ci.setReturnValue(this.model.renderType(resourcelocation));
        } else {
            ci.setReturnValue(p_115325_ ? RenderType.outline(resourcelocation) : null);
        }*/
        ci.cancel();
    }
}
