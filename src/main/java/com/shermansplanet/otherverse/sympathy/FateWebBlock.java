package com.shermansplanet.otherverse.sympathy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FateWebBlock extends ColorableBlock{

    public FateWebBlock(Properties p_49795_) {
        super(p_49795_);
        registerDefaultState(stateDefinition.any().setValue(color, DyeColor.WHITE));
    }

    public VoxelShape getShape(BlockState p_55620_, BlockGetter p_55621_, BlockPos p_55622_, CollisionContext p_55623_) {
        return Block.box(0, 0, 0, 16, 1, 16);
    }
}
