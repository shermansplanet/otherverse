package com.shermansplanet.otherverse.others;

import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

public class SnufferRenderer extends MobRenderer<Snuffer, SnufferModel<Snuffer>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("otherverse", "textures/entity/snuffer.png");

    public SnufferRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SnufferModel<>(ctx.bakeLayer(ModelLayers.SNIFFER)), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(Snuffer p_114482_) {
        return TEXTURE;
    }
}
