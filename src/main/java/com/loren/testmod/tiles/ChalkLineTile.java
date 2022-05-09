package com.loren.testmod.tiles;

import com.loren.testmod.init.TileEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChalkLineTile extends BlockEntity {
    public ChalkLineTile(BlockPos pos, BlockState state)
    {
        super(TileEntityInit.CHALK_LINE.get(), pos, state);
    }
}
