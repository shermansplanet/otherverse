package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

public class MiseryPoolFeature extends Feature<NoneFeatureConfiguration> {
    public MiseryPoolFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    private BlockState getBricks(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return ctx.random().nextFloat() < 0.25f
                ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos blockpos = ctx.origin();
        var r = ctx.random().nextFloat() * 8 + 1;
        var r2 = r * r;
        var r2plus1 = (r + 1) * (r + 1);
        var intr = (int) Math.ceil(r);
        var waterstate = Blocks.WATER.defaultBlockState();
        var mudState = Blocks.MUD.defaultBlockState();
        var wallstate = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        for (var dx = -intr; dx <= intr; dx++) {
            for (var dz = -intr; dz <= intr; dz++) {
                var d = dx * dx + dz * dz;
                if (d > r2plus1) continue;
                var pos = blockpos.offset(dx, -1, dz);
                if (d > r2) {
                    if (level.isWaterAt(pos)) continue;
                    level.setBlock(pos, getBricks(ctx), 2);
                    level.setBlock(pos.below(), getBricks(ctx), 2);
                    if (ctx.random().nextFloat() > 0.2f) continue;
                    var downlimit = ctx.random().nextInt(6, 11);
                    for (var dy = 2; dy < downlimit; dy++) {
                        var downpos = pos.below(dy);
                        if (!level.isEmptyBlock(downpos)) break;
                        level.setBlock(downpos, wallstate, 2);
                    }
                } else {
                    level.setBlock(pos, waterstate, 2);
                    level.setBlock(pos.below(), waterstate, 2);
                    if (ctx.random().nextFloat() < 0.05f && level.isEmptyBlock(pos.below(2))) {
                        level.scheduleTick(pos.below(), Fluids.WATER, 0);
                    } else {
                        level.setBlock(pos.below(2), mudState, 2);
                    }
                }
            }
        }
        return true;
    }
}
