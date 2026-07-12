package com.shermansplanet.otherverse.others;

import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Zombie;

public class FuryRenderer extends BlazeRenderer {
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("otherverse", "textures/entity/fury.png");

    public FuryRenderer(EntityRendererProvider.Context p_173933_) {
        super(p_173933_);
    }

    public ResourceLocation getTextureLocation(Blaze p_113771_) {
        return LOCATION;
    }
}
