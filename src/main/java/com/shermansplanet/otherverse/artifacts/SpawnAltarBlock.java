package com.shermansplanet.otherverse.artifacts;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SpawnAltarBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);

    public SpawnAltarBlock(Properties p_56359_) {
        super(p_56359_);
    }

    public VoxelShape getShape(BlockState p_56390_, BlockGetter p_56391_, BlockPos p_56392_, CollisionContext p_56393_) {
        return SHAPE;
    }

    public boolean useShapeForLightOcclusion(BlockState p_56395_) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56388_) {
        p_56388_.add(BlockStateProperties.WATERLOGGED);
    }

    public BlockState getStateForPlacement(BlockPlaceContext p_56361_) {
        BlockPos blockpos = p_56361_.getClickedPos();
        FluidState fluidstate = p_56361_.getLevel().getFluidState(blockpos);
        return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    public FluidState getFluidState(BlockState p_56397_) {
        return p_56397_.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56397_);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == Otherverse.SPAWN_ALTAR_ENTITY.get() ? SpawnAltarBlockEntity::tick : null;
    }

    @Override
    public void playerDestroy(Level lvl, Player player, BlockPos pos, BlockState state, @javax.annotation.Nullable BlockEntity entity, ItemStack stack) {
        super.playerDestroy(lvl, player, pos, state, entity, stack);
        if (!(lvl instanceof ServerLevel sl)) return;
        if (sl.getBlockState(pos.below()).getBlock() != OtherverseBlocks.DEMESNE_BEACON.get()) return;
        var demesne = DemesnesManager.getData(sl, pos);
        if (demesne == null || demesne.getPerkLevel(DemesnesManager.DemesnePerk.MOB_REPOSITION) == 0) return;
        demesne.removeSpawnDestination(pos);
    }

    public void setPlacedBy(Level lvl, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(lvl, pos, state, entity, stack);
        if (!(lvl instanceof ServerLevel sl)) return;
        if (sl.getBlockState(pos.below()).getBlock() != OtherverseBlocks.DEMESNE_BEACON.get()) return;
        var demesne = DemesnesManager.getData(sl, pos);
        if (demesne == null || demesne.getPerkLevel(DemesnesManager.DemesnePerk.MOB_REPOSITION) == 0) return;
        demesne.addSpawnDestination(pos);
    }
}
