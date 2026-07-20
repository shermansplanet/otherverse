package com.shermansplanet.otherverse.others;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TyphloticJellyfishRenderer extends MobRenderer<TyphloticJellyfish, TyphloticJellyfishModel<TyphloticJellyfish>> {
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/entity/typhlotic_jellyfish.png");

    public TyphloticJellyfishRenderer(EntityRendererProvider.Context p_173958_) {
        super(p_173958_, new TyphloticJellyfishModel<>(p_173958_.bakeLayer(TyphloticJellyfishModel.LAYER_LOCATION)), 0.5F);
    }

    protected void setupRotations(TyphloticJellyfish p_116035_, PoseStack p_116036_, float p_116037_, float p_116038_, float p_116039_) {
        float f = Mth.lerp(p_116039_, p_116035_.xBodyRotO, p_116035_.xBodyRot);
        float f1 = Mth.lerp(p_116039_, p_116035_.zBodyRotO, p_116035_.zBodyRot);
        p_116036_.translate(0.0F, 0.5F, 0.0F);
        p_116036_.mulPose(Axis.YP.rotationDegrees(180.0F - p_116038_));
        p_116036_.mulPose(Axis.XP.rotationDegrees(f));
        p_116036_.mulPose(Axis.YP.rotationDegrees(f1));
        p_116036_.translate(0.0F, -1.2F, 0.0F);
    }

    protected float getBob(TyphloticJellyfish p_116032_, float p_116033_) {
        return Mth.lerp(p_116033_, p_116032_.oldAnimTime, p_116032_.animTime);
    }

    public ResourceLocation getTextureLocation(TyphloticJellyfish p_114041_) {
        return TEXTURE_LOCATION;
    }
}
