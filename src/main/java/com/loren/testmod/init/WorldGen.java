package com.loren.testmod.init;

import com.loren.testmod.TestMod;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class WorldGen {

    public static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_) {
        return List.of(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
    }

    public static List<PlacementModifier> commonOrePlacement(int p_195344_, PlacementModifier p_195345_) {
        return orePlacement(CountPlacement.of(p_195344_), p_195345_);
    }

    public static void generateOres(final BiomeLoadingEvent event) {
        final Holder<ConfiguredFeature<OreConfiguration, ?>> CALCITE_ORE =
                FeatureUtils.register(TestMod.MOD_ID + "_calcite_ore", Feature.ORE,
                        new OreConfiguration(OreFeatures.NATURAL_STONE, BlockInit.CALCITE_BLOCK.get().defaultBlockState(), 64));

        final Holder<PlacedFeature> CALCITE_ORE_PLACED = PlacementUtils.register("calcite_ore_placed",
                CALCITE_ORE, commonOrePlacement(20,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(128))));

        List<Holder<PlacedFeature>> base =
                event.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);

        base.add(CALCITE_ORE_PLACED);
    }

    @SubscribeEvent
    public static void biomeLoadingEvent(final BiomeLoadingEvent event) {
        generateOres(event);
    }
}