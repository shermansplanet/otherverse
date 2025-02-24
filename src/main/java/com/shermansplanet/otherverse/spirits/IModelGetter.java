package com.shermansplanet.otherverse.spirits;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public interface IModelGetter {
    public BlockModel loadBlockModelPublic(ResourceLocation p_119365_) throws IOException;

    public ItemModelGenerator getItemModelGenerator();
}
