package com.shermansplanet.otherverse.diagrams;

import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SlateScaffoldingBlock extends Block implements SimpleWaterloggedBlock {

    public SlateScaffoldingBlock(Properties p_49795_) {
        super(p_49795_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.FALSE));
    }

    public static final VoxelShape STABLE_SHAPE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    static {
        VoxelShape voxelshape = Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        VoxelShape voxelshape1 = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D);
        VoxelShape voxelshape2 = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
        VoxelShape voxelshape3 = Block.box(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D);
        VoxelShape voxelshape4 = Block.box(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
        STABLE_SHAPE = Shapes.or(voxelshape, voxelshape1, voxelshape2, voxelshape3, voxelshape4);
    }

    public BlockState getStateForPlacement(BlockPlaceContext p_56023_) {
        BlockPos blockpos = p_56023_.getClickedPos();
        Level level = p_56023_.getLevel();
        return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(level.getFluidState(blockpos).getType() == Fluids.WATER));
    }

    public BlockState updateShape(BlockState p_56044_, Direction p_56045_, BlockState p_56046_, LevelAccessor p_56047_, BlockPos p_56048_, BlockPos p_56049_) {
        if (p_56044_.getValue(WATERLOGGED)) {
            p_56047_.scheduleTick(p_56048_, Fluids.WATER, Fluids.WATER.getTickDelay(p_56047_));
        }

        if (!p_56047_.isClientSide()) {
            p_56047_.scheduleTick(p_56048_, this, 1);
        }

        return p_56044_;
    }

    public FluidState getFluidState(BlockState p_56073_) {
        return p_56073_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56073_);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56051_) {
        p_56051_.add(WATERLOGGED);
    }

    @Override
    public boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    public VoxelShape getInteractionShape(BlockState p_56053_, BlockGetter p_56054_, BlockPos p_56055_) {
        return Shapes.block();
    }

    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public VoxelShape getCollisionShape(BlockState p_56068_, BlockGetter p_56069_, BlockPos p_56070_, CollisionContext p_56071_) {
        if (p_56071_.isAbove(Shapes.block(), p_56070_, true) && !p_56071_.isDescending()) {
            return STABLE_SHAPE;
        } else {
            return Shapes.empty();
        }
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
