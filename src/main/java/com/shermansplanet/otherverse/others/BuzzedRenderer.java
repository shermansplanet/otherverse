package com.shermansplanet.otherverse.others;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BuzzedRenderer extends MobRenderer<Buzzed, BuzzedModel<Buzzed>> {
    private static final ResourceLocation BUZZED_TEXTURE = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/entity/buzzed.png");

    public BuzzedRenderer(EntityRendererProvider.Context p_173931_) {
        super(p_173931_, new BuzzedModel<>(p_173931_.bakeLayer(ModelLayers.BEE)), 0.4F);
    }

    public void render(Buzzed p_115455_, float p_115456_, float p_115457_, PoseStack p_115458_, MultiBufferSource p_115459_, int p_115460_) {
        super.render(p_115455_, p_115456_, p_115457_, p_115458_, p_115459_, 255);
    }

    public ResourceLocation getTextureLocation(Buzzed buzzed) {
        return BUZZED_TEXTURE;
    }
}