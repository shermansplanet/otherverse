package com.shermansplanet.otherverse.others;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TyphloticJellyfishRenderer extends MobRenderer<TyphloticJellyfish, TyphloticJellyfishModel<TyphloticJellyfish>> {
    private static final ResourceLocation TEXTURE_LOCATION =
            new ResourceLocation(Otherverse.MODID, "textures/entity/typhlotic_jellyfish.png");

    public TyphloticJellyfishRenderer(EntityRendererProvider.Context p_173958_) {
        super(p_173958_, new TyphloticJellyfishModel<>(p_173958_.bakeLayer(TyphloticJellyfishModel.LAYER_LOCATION)), 0.5F);
    }

    public void render(TyphloticJellyfish p_115455_, float p_115456_, float p_115457_, PoseStack stack, MultiBufferSource p_115459_, int p_115460_) {
        stack.pushPose();
        stack.mulPose(Quaternion.fromXYZ(0, (float) Math.PI, 0));
        super.render(p_115455_, p_115456_, p_115457_, stack, p_115459_, p_115460_);
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(TyphloticJellyfish p_114041_) {
        return TEXTURE_LOCATION;
    }
}
