package com.loren.testmod.blocks;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractChalkLineBlock extends BaseEntityBlock {
    protected AbstractChalkLineBlock(BlockBehaviour.Properties p_48660_) {
        super(p_48660_);
    }

    public @NotNull RenderShape getRenderShape(@NotNull BlockState p_48727_) {
        return RenderShape.MODEL;
    }
}
