package com.shermansplanet.otherverse.demesnes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class DemesnesBeaconBlock extends Block implements EntityBlock {

    public DemesnesBeaconBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemesnesBeacon(pos, state);
    }

    public InteractionResult use(BlockState p_57083_, Level level, BlockPos pos, Player p_57086_, InteractionHand p_57087_, BlockHitResult p_57088_) {
        if (!(level.getBlockEntity(pos) instanceof DemesnesBeacon db)) return InteractionResult.PASS;
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            p_57086_.openMenu(db);
            return InteractionResult.CONSUME;
        }
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState p_49849_, @Nullable LivingEntity p_49850_, ItemStack stack) {
        if (!stack.hasCustomHoverName()) return;
        if (level.getBlockEntity(pos) instanceof DemesnesBeacon db) db.hoverName = stack.getHoverName();
    }

    /*@Nullable
    public MenuProvider getMenuProvider(BlockState p_57105_, Level p_57106_, BlockPos p_57107_) {
        return new SimpleMenuProvider((p_57074_, p_57075_, p_57076_) ->
                new DemesnesClaimMenu(p_57074_, p_57075_, ContainerLevelAccess.create(p_57106_, p_57107_)), CONTAINER_TITLE);
    }*/
}
