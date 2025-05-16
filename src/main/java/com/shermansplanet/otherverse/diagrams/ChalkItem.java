package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class ChalkItem extends Item implements DyeableLeatherItem {

    public ChalkItem(Properties p_41383_) {
        super(p_41383_);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext p_40613_) {
        BlockState blockstate = OtherverseBlocks.CHALK_LINE.get().getStateForPlacement(p_40613_);
        return blockstate != null && this.canPlace(p_40613_, blockstate) ? blockstate : null;
    }

    protected boolean canPlace(BlockPlaceContext p_40611_, BlockState p_40612_) {
        Player player = p_40611_.getPlayer();
        CollisionContext collisioncontext =
                player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return (p_40612_.canSurvive(p_40611_.getLevel(), p_40611_.getClickedPos())) && p_40611_
                .getLevel().isUnobstructed(p_40612_, p_40611_.getClickedPos(), collisioncontext);
    }

    protected boolean placeBlock(BlockPlaceContext p_40578_, BlockState p_40579_) {
        return p_40578_.getLevel().setBlock(p_40578_.getClickedPos(), p_40579_, 11);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        if(l.isClientSide()) DiagramSightRenderer.setCenter(null, p);
        return InteractionResultHolder.pass(p.getItemInHand(h));
    }

    @Override
    public InteractionResult useOn(UseOnContext useCtx) {
        BlockPlaceContext ctx = new BlockPlaceContext(useCtx);
        BlockEntity be = ctx.getLevel().getBlockEntity(useCtx.getClickedPos());
        boolean isOnBlockEntity = be != null && !(be instanceof ChalkCircle);
        if (useCtx.getPlayer() != null && useCtx.getPlayer().isShiftKeyDown() && !isOnBlockEntity) {
            if (!useCtx.getLevel().isClientSide()) {
                return InteractionResult.FAIL;
            }
            if (ctx.getClickedPos().equals(DiagramSightRenderer.symmetryCenter)) {
                DiagramSightRenderer.setCenter(null, useCtx.getPlayer());
            } else {
                DiagramSightRenderer.setCenter(
                        useCtx.getLevel().getBlockState(useCtx.getClickedPos()).is(OtherverseBlocks.CHALK_LINE.get())
                                ? useCtx.getClickedPos() : ctx.getClickedPos(), useCtx.getPlayer());
            }
            return InteractionResult.FAIL;
        }
        if (!ctx.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockState blockstate = this.getPlacementState(ctx);
        if (blockstate == null) {
            return InteractionResult.FAIL;
        }
        if (!this.placeBlock(ctx, blockstate)) {
            return InteractionResult.FAIL;
        }
        BlockPos blockpos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
        if (level.isClientSide) {
            level.playSound(player, blockpos, SoundEvents.CALCITE_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (player instanceof ServerPlayer sp) {
            Otherverse.ADVANCEMENTS.trigger(sp, "diagram");
        }

        if (player != null && !player.getAbilities().instabuild) {
            if (!level.isClientSide) {
                ctx.getItemInHand().hurtAndBreak(1, player, (p_150845_) -> {
                    p_150845_.broadcastBreakEvent(useCtx.getHand());
                });
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
