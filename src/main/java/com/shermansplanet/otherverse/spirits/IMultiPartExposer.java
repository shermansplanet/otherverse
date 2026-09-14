package com.shermansplanet.otherverse.spirits;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public interface IMultiPartExposer {
    public StateDefinition<Block, BlockState> getDefinition();
}
