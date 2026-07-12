package com.shermansplanet.otherverse.resources;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.ruins.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class OtherverseFeatureGen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, Otherverse.MODID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SALT =
            FEATURES.register("salt_crystals", () -> new SaltFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MISERY_POOL =
            FEATURES.register("misery_pool", () -> new MiseryPoolFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> DISGUST_POOL =
            FEATURES.register("disgust_pool", () -> new DisgustPoolFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SHOCK_SPIRE =
            FEATURES.register("shock_spire", () -> new ShockSpireFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BURNT_TREE =
            FEATURES.register("burnt_tree", () -> new ProceduralTreeFeature(NoneFeatureConfiguration.CODEC,
                    new BlockState[]{
                            Blocks.STRIPPED_ACACIA_LOG.defaultBlockState(),
                            Blocks.TERRACOTTA.defaultBlockState(),
                            Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(),
                            Blocks.DEAD_FIRE_CORAL_BLOCK.defaultBlockState()
                    },
                    new BlockState[]{
                            Blocks.DEAD_FIRE_CORAL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false),
                            Blocks.DEAD_FIRE_CORAL_FAN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false),
                    }, true));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FEAR_BONES =
            FEATURES.register("fear_bones", () -> new FearBonesFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TRUST_TREE =
            FEATURES.register("trust_tree", () -> new TrustTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TRUST_LIGHT =
            FEATURES.register("trust_light", () -> new TrustLightFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> VIGIL_MAZE =
            FEATURES.register("vigil_maze", () -> new VigilMazeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ECSTASY_STRAND =
            FEATURES.register("ecstasy_strand", () -> new EcstasyStrandFeature(NoneFeatureConfiguration.CODEC));
}
