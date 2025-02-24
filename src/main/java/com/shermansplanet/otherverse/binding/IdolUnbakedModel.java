package com.shermansplanet.otherverse.binding;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class IdolUnbakedModel implements UnbakedModel {
    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Sets.newHashSet();
    }

    @Override
    public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> p_119538_, Set<Pair<String, String>> p_119539_) {
        return Sets.newHashSet();
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBakery p_119534_, Function<Material, TextureAtlasSprite> p_119535_, ModelState p_119536_, ResourceLocation p_119537_) {
        return new IdolBakedModel();
    }
}
