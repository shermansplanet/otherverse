package com.shermansplanet.otherverse.resources;

import com.mojang.serialization.Codec;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SaltFeature extends Feature<NoneFeatureConfiguration> {
    public SaltFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159956_) {
        WorldGenLevel worldgenlevel = p_159956_.level();
        BlockPos blockpos = p_159956_.origin();
        int j = worldgenlevel.getHeight(Heightmap.Types.OCEAN_FLOOR, blockpos.getX(), blockpos.getZ());
        blockpos = new BlockPos(blockpos.getX(), j, blockpos.getZ());
        if (!worldgenlevel.getBlockState(blockpos).is(Blocks.WATER)) return false;
        if (!worldgenlevel.getBlockState(blockpos.below()).isCollisionShapeFullBlock(worldgenlevel, blockpos.below()))
            return false;
        worldgenlevel.setBlock(blockpos, OtherverseBlocks.SALT_CRYSTALS.get().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true), 2);
        return true;
    }
}
