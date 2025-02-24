package com.shermansplanet.otherverse.diagrams;

import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SlateScaffoldingBlock extends Block {

  public SlateScaffoldingBlock(Properties p_49795_) {
    super(p_49795_);
  }

  @Override
  public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
    return true;
  }

  public boolean canSurvive(BlockState p_55585_, LevelReader p_55586_, BlockPos p_55587_) {
    BlockPos blockpos = p_55587_.below();
    BlockState blockstate = p_55586_.getBlockState(blockpos);
    return this.canSurviveOn(p_55586_, blockpos, blockstate);
  }

  private boolean canSurviveOn(BlockGetter p_55613_, BlockPos p_55614_, BlockState blockState) {
    return blockState.isFaceSturdy(p_55613_, p_55614_, Direction.UP)
        || blockState.is(OtherverseBlocks.SLATE_SCAFFOLDING.get())
        || (blockState.is(OtherverseBlocks.CHALK_LINE.get())
        && blockState.getValue(ChalkLineBlock.hasScaffolding));
  }

  public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos,
      Block otherBlock, BlockPos otherPos, boolean p_55566_) {
    if (level.isClientSide) {
      return;
    }

    if (!blockState.canSurvive(level, blockPos)) {
      level.destroyBlock(blockPos, true);
    }
  }
}
