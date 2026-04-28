package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;

public class TrustTreeFeature extends Feature<NoneFeatureConfiguration> {

    public TrustTreeFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    private final TreeConfiguration cherryConfig = (new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(Blocks.STRIPPED_CHERRY_WOOD), new CherryTrunkPlacer(7, 1, 0, new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder().add(ConstantInt.of(1), 1).add(ConstantInt.of(2), 1).add(ConstantInt.of(3), 1).build()), UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)), BlockStateProvider.simple(Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)), new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25F, 0.5F, 0.16666667F, 0.33333334F), new TwoLayersFeatureSize(1, 0, 2))).ignoreVines().build();

    private static final BlockState[] flowers = new BlockState[]{
            Blocks.PEONY.defaultBlockState(),
            Blocks.ALLIUM.defaultBlockState(),
            Blocks.LILAC.defaultBlockState(),
            Blocks.PITCHER_PLANT.defaultBlockState(),
            Blocks.GRASS.defaultBlockState()
    };

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        ctx.level().setBlock(ctx.origin().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        Feature.TREE.place(cherryConfig, ctx.level(), ctx.chunkGenerator(), ctx.random(), ctx.origin());
        var radius = 8;
        for (var dx = -radius; dx <= radius; dx++) {
            for (var dz = -radius; dz <= radius; dz++) {
                var density = 1f - Mth.sqrt(dx * dx + dz * dz) / radius;
                if(density - ctx.random().nextFloat() < 0) continue;
                var x = ctx.origin().getX() + dx;
                var z = ctx.origin().getZ() + dz;
                if (dx == 0 && dz == 0) continue;
                var y = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                var pos = new BlockPos(x, y, z);
                ctx.level().setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                if (ctx.random().nextFloat() < 0.6f) {
                    ctx.level().setBlock(pos, flowers[ctx.random().nextInt(flowers.length)], 2);
                } else if (ctx.random().nextFloat() < 0.3f) {
                    ctx.level().setBlock(pos, Blocks.PINK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, ctx.random().nextInt(1, 4)), 2);
                }
            }
        }
        return true;
    }
}
