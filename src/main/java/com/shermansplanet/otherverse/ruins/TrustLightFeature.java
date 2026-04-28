package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashMap;

public class TrustLightFeature extends Feature<NoneFeatureConfiguration> {

    public TrustLightFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        ctx.level().setBlock(ctx.origin().below(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), 2);
        var r = ctx.random();
        var height = r.nextInt(4, 32);
        var radius = (int) Mth.sqrt(height);
        var coreX = ctx.origin().getX();
        var coreZ = ctx.origin().getZ();
        var blocksByPos = new HashMap<BlockPos, Block>();
        for (var h = 0; h < height; h++) {
            var y = ctx.origin().getY() + h;
            var lerp = (float) h / height;
            for (var dx = -radius; dx <= radius; dx++) {
                for (var dz = -radius; dz <= radius; dz++) {
                    var density = 1f - Mth.sqrt(dx * dx + dz * dz) / radius;
                    density -= lerp * 0.66f;
                    density -= r.nextFloat() * 0.8f;
                    if (density < 0) continue;
                    blocksByPos.put(new BlockPos(coreX + dx, y, coreZ + dz),
                            density < 0.3f ? Blocks.BLUE_STAINED_GLASS_PANE :
                                    density < 0.6f ? Blocks.PURPLE_STAINED_GLASS_PANE :
                                    Blocks.PINK_STAINED_GLASS_PANE);
                }
            }
            coreX += r.nextInt(-1, 2);
            coreZ += r.nextInt(-1, 2);
        }
        for (var bp : blocksByPos.keySet()) {
            var bs = blocksByPos.get(bp).defaultBlockState()
                    .setValue(StainedGlassPaneBlock.NORTH, blocksByPos.containsKey(bp.north()))
                    .setValue(StainedGlassPaneBlock.SOUTH, blocksByPos.containsKey(bp.south()))
                    .setValue(StainedGlassPaneBlock.EAST, blocksByPos.containsKey(bp.east()))
                    .setValue(StainedGlassPaneBlock.WEST, blocksByPos.containsKey(bp.west()));
            ctx.level().setBlock(bp, bs, 2);
        }
        return true;
    }
}
