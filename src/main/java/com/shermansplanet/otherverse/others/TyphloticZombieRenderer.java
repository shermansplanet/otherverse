package com.shermansplanet.otherverse.others;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class TyphloticZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.fromNamespaceAndPath("otherverse", "textures/entity/typhlotic_zombie.png");

    public TyphloticZombieRenderer(EntityRendererProvider.Context p_174456_) {
        super(p_174456_);
    }

    public ResourceLocation getTextureLocation(Zombie p_113771_) {
        return ZOMBIE_LOCATION;
    }
}
