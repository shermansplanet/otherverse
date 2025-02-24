package com.shermansplanet.otherverse.ruins;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> RUINS_KEY = ResourceKey.create(Registry.DIMENSION_REGISTRY,
            new ResourceLocation(Otherverse.MODID, "ruins"));
    public static final ResourceKey<DimensionType> EDIFICE_TYPE = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY,
            new ResourceLocation(Otherverse.MODID, "ruins"));
}
