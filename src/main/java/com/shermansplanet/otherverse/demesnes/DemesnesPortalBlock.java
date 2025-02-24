package com.shermansplanet.otherverse.demesnes;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.sympathy.ColorableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DemesnesPortalBlock extends ColorableBlock implements EntityBlock {

    public DemesnesPortalBlock(BlockBehaviour.Properties p_49795_) {
        super(p_49795_);
        registerDefaultState(stateDefinition.any().setValue(color, DyeColor.LIME));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemesnesPortal(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == Otherverse.DEMESNES_PORTAL_ENTITY.get() ? DemesnesPortal::tick : null;
    }
}
