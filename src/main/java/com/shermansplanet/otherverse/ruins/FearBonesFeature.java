package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class FearBonesFeature extends Feature<NoneFeatureConfiguration> {
    BlockState bonestate = Blocks.BONE_BLOCK.defaultBlockState();

    Direction[] directions = new Direction[]{
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    public FearBonesFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        var r = ctx.random();
        var diri = r.nextInt(4);
        var frontDir = directions[diri];
        var sideDir = directions[(diri + 1) % 4];
        var baseHeight = r.nextInt(3, 16);
        var ribCount = r.nextInt(1, 9);
        if (ribCount < 3) ribCount = 1;
        var heightVariance = r.nextFloat();
        var basePos = ctx.origin().above(r.nextInt(4));
        var spacing = 3;
        for (var i = 0; i < ribCount; i++) {
            var centerPos = basePos.relative(frontDir, Math.round((i - (ribCount - 1f) / 2f)) * spacing);
            if (ctx.level().isEmptyBlock(centerPos)) continue;
            var lateralLerp = i / (ribCount - 1f);
            var height = baseHeight * (1 - Math.abs(0.666f - lateralLerp) * heightVariance);
            var radius = height / 2f;
            var steps = Math.ceil(height * 2.6f);
            for (var n = 0; n < steps; n++) {
                var angle = -2.5f + n * 5f / (steps - 1f);
                var pos = centerPos.relative(sideDir, (int) Math.round(Math.sin(angle) * radius))
                        .relative(Direction.DOWN, (int) Math.round(Math.cos(angle) * radius - radius));
                ctx.level().setBlock(pos, bonestate, 2);
            }
        }
        return true;
    }
}
