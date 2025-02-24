package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ShockSpireFeature extends Feature<NoneFeatureConfiguration> {

    private static BlockState[] lightningReplaceStates = null;

    public ShockSpireFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    private static BlockState getReplaceState(int i, RandomSource r) {
        return lightningReplaceStates[Math.max(0, Math.min(lightningReplaceStates.length - 1,
                i + r.nextInt(-1, 2)))];
    }

    public static void spawnSpire(LevelAccessor level, int raise, BlockPos blockTarget, RandomSource r, int hmul) {
        if (lightningReplaceStates == null) {
            lightningReplaceStates = new BlockState[]{
                    Blocks.SOUL_SAND.defaultBlockState(),
                    Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState(),
                    Blocks.MUD.defaultBlockState(),
                    Blocks.GRAY_WOOL.defaultBlockState(),
                    Blocks.CYAN_TERRACOTTA.defaultBlockState(),
                    OtherverseBlocks.ANTI_PURPUR_BLOCK.get().defaultBlockState(),
                    Blocks.PRISMARINE_BRICKS.defaultBlockState(),
                    Blocks.DIAMOND_BLOCK.defaultBlockState()
            };
        }
        var i = 0;
        for (i = 0; i < raise; i++)
            level.setBlock(blockTarget.above(i), getReplaceState(i / hmul, r), 3);
        var north = r.nextInt(0, raise);
        var offsetTarget = blockTarget.north();
        for (i = 0; i < north; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var south = r.nextInt(0, raise);
        offsetTarget = blockTarget.south();
        for (i = 0; i < south; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var east = r.nextInt(0, raise);
        offsetTarget = blockTarget.east();
        for (i = 0; i < east; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var west = r.nextInt(0, raise);
        offsetTarget = blockTarget.west();
        for (i = 0; i < west; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var northeast = Math.min(north, east);
        offsetTarget = blockTarget.offset(1, 0, -1);
        for (i = 0; i < northeast; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var southeast = Math.min(south, east);
        offsetTarget = blockTarget.offset(1, 0, 1);
        for (i = 0; i < southeast; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var southwest = Math.min(south, west);
        offsetTarget = blockTarget.offset(-1, 0, 1);
        for (i = 0; i < southwest; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
        var northwest = Math.min(north, west);
        offsetTarget = blockTarget.offset(-1, 0, -1);
        for (i = 0; i < northwest; i++)
            level.setBlock(offsetTarget.above(i), getReplaceState(i / hmul, r), 3);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        spawnSpire(ctx.level(), ctx.random().nextInt(1, 21), ctx.origin(), ctx.random(), 3);
        return true;
    }
}
