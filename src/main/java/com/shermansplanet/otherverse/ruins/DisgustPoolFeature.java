package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import java.util.HashSet;

public class DisgustPoolFeature extends Feature<NoneFeatureConfiguration> {
    public DisgustPoolFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos.MutableBlockPos mutPos = ctx.origin().below().mutable();
        var water = Blocks.WATER.defaultBlockState();
        var moss = Blocks.MOSS_BLOCK.defaultBlockState();
        var grass = Blocks.GRASS.defaultBlockState();
        var tallgrass = Blocks.TALL_GRASS.defaultBlockState();
        var lilypad = Blocks.LILY_PAD.defaultBlockState();

        var iterations = ctx.random().nextInt(1, 8);
        var positions = new HashSet<BlockPos>();
        for (var i = 0; i < iterations; i++) {
            var timeout = 3;
            while (level.isEmptyBlock(mutPos) && timeout > 0) {
                mutPos.setY(mutPos.getY() - 1);
                timeout--;
            }
            if (timeout == 0) break;
            level.setBlock(mutPos, water, 2);
            level.scheduleTick(mutPos, Fluids.WATER, 0);
            if (ctx.random().nextFloat() < 0.1f) {
                level.setBlock(mutPos.above(), lilypad, 2);
            }
            positions.add(mutPos.immutable());
            var dir = ctx.random().nextInt(4);
            switch (dir) {
                case 0 -> mutPos.setX(mutPos.getX() + 1);
                case 1 -> mutPos.setX(mutPos.getX() - 1);
                case 2 -> mutPos.setZ(mutPos.getZ() + 1);
                case 3 -> mutPos.setZ(mutPos.getZ() - 1);
            }
        }
        for (var i = 0; i < 2; i++) {
            for (var position : positions) {
                var dir = ctx.random().nextInt(4);
                switch (dir) {
                    case 0 -> position = position.offset(1, 0, 0);
                    case 1 -> position = position.offset(-1, 0, 0);
                    case 2 -> position = position.offset(0, 0, 1);
                    case 3 -> position = position.offset(0, 0, -1);
                }
                if (positions.contains(position)) continue;
                level.setBlock(position, moss, 2);
                if (ctx.random().nextFloat() < 0.3f) {
                    level.setBlock(position.above(), grass, 2);
                } else if (ctx.random().nextFloat() < 0.2f) {
                    level.setBlock(position.above(), tallgrass, 2);
                }
            }
        }
        return true;
    }
}
