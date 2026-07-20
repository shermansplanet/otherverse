package com.shermansplanet.otherverse.artifacts;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class BiomeBrazierBlock extends Block implements EntityBlock {
    public static final BooleanProperty SCRY = BooleanProperty.create("scry");

    public BiomeBrazierBlock(Properties p_56359_) {
        super(p_56359_);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.LIT, false)
                .setValue(SCRY, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
        builder.add(SCRY);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiomeBrazierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (level.isClientSide || type != Otherverse.BIOME_BRAZIER_ENTITY.get()) ? null : BiomeBrazierBlockEntity::tick;
    }
}
