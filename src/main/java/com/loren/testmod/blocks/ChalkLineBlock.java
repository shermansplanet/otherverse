package com.loren.testmod.blocks;

import com.loren.testmod.init.TileEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChalkLineBlock extends AbstractChalkLineBlock {
    public ChalkLineBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return TileEntityInit.CHALK_LINE.get().create(pos, state);
    }
}
