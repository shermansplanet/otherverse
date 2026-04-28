package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

public class ProceduralTreeFeature extends Feature<NoneFeatureConfiguration> {

    private static final Direction[] sideDirs = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static final Vec3 up = new Vec3(0, 1, 0);
    private final BlockState[] palette;
    private final BlockState[] topDecor;
    private boolean isAnger;

    public ProceduralTreeFeature(Codec<NoneFeatureConfiguration> p_65786_, BlockState[] palette, BlockState[] topDecor, boolean isAnger) {
        super(p_65786_);
        this.palette = palette;
        this.topDecor = topDecor;
        this.isAnger = isAnger;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        var origin = ctx.origin();
        var blockBelow = ctx.level().getBlockState(origin.below());
        if (isAnger && !blockBelow.is(Blocks.ORANGE_TERRACOTTA) && !blockBelow.is(Blocks.RED_SAND)) return false;
        var r = ctx.random();
        var totalBlockCount = r.nextInt(6, 48);
        generateSegment(new Vec3(origin.getX() + 0.5f, origin.getY() - 0.5f, origin.getZ() + 0.5f),
                new Vec3(r.nextFloat() - 0.5f, 2, r.nextFloat() - 0.5f).normalize(),
                Math.min(r.nextInt(2, 8), totalBlockCount / 2), totalBlockCount, 0, r, ctx.level());
        return true;
    }

    private void generateSegment(Vec3 pos, Vec3 dir, int length, int blocksRemaining, float lerp, RandomSource r, WorldGenLevel level) {
        var lerpStep = (1f - lerp) / blocksRemaining;
        if (blocksRemaining - length < 2) {
            length = blocksRemaining;
        }
        var realdir = dir.scale(0.8f);
        for (var i = 0; i < length; i++) {
            pos = pos.add(realdir);
            var blockPos = BlockPos.containing(pos);
            level.setBlock(blockPos, getBlockstate(lerp, r), 2);
            if (blocksRemaining > 25) {
                level.setBlock(blockPos.north(), getBlockstate(lerp, r), 2);
                level.setBlock(blockPos.south(), getBlockstate(lerp, r), 2);
                level.setBlock(blockPos.east(), getBlockstate(lerp, r), 2);
                level.setBlock(blockPos.west(), getBlockstate(lerp, r), 2);
            }
            for (var direction : sideDirs) {
                if (r.nextFloat() > lerp * (isAnger ? 0.66f : 0.33f)) continue;
                var rel = blockPos.relative(direction);
                if (!level.isEmptyBlock(rel)) continue;
                if(isAnger) {
                    level.setBlock(rel, Blocks.DEAD_FIRE_CORAL_WALL_FAN.defaultBlockState()
                            .setValue(BlockStateProperties.WATERLOGGED, false)
                            .setValue(BaseCoralWallFanBlock.FACING, direction), 2);
                }else{
                    level.setBlock(rel, Blocks.CHERRY_LEAVES.defaultBlockState(), 2);
                }
            }
            lerp += lerpStep;
        }
        if (blocksRemaining == length) {
            level.setBlock(BlockPos.containing(pos).above(), getRandomCoralTop(r), 2);
            return;
        }
        blocksRemaining -= length;
        var branch1 = new Vec3(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f)
                .cross(dir).normalize();
        var branch2 = branch1.scale(-1);
        var branch1lerp = r.nextFloat();
        branch1 = branch1.scale(1 - branch1lerp).add(dir.scale(branch1lerp)).normalize();
        var branch2lerp = Math.min(1, r.nextFloat() * (1.5f - branch1lerp));
        branch2 = branch2.scale(1 - branch2lerp).add(dir.scale(branch2lerp)).normalize();

        branch2lerp = r.nextFloat();
        branch2 = branch2.scale(1 - branch2lerp / 2).add(new Vec3(0, branch2lerp / 2, 0)).normalize();
        branch1lerp = Math.min(1, r.nextFloat() * (1.5f - branch1lerp));
        branch1 = branch1.scale(1 - branch1lerp / 2).add(new Vec3(0, branch1lerp / 2, 0)).normalize();

        var branch2Blocks = Math.min(blocksRemaining - 1, Math.max(1,
                (int) r.triangle(blocksRemaining / 2f, blocksRemaining / 2f)));
        var branch1Blocks = blocksRemaining - branch2Blocks;

        generateSegment(pos, branch1, Math.min(r.nextInt(2, 7), branch1Blocks),
                branch1Blocks, lerp, r, level);

        generateSegment(pos, branch2, Math.min(r.nextInt(2, 7), branch2Blocks),
                branch2Blocks, lerp, r, level);
    }

    private BlockState getRandomCoralTop(RandomSource r) {
        return topDecor[r.nextInt(topDecor.length)];
    }

    private BlockState getBlockstate(float lerp, RandomSource r) {
        return palette[Math.max(0, Math.min(palette.length - 1,
                (int) ((lerp + r.nextFloat() * 0.2f - 0.1f) * palette.length)))];
    }
}
