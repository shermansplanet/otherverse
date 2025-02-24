package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(BlockItem.class)
public class BlockItemInjector {

    @Inject(method = "place", at = @At(value = "HEAD"), cancellable = true)
    public void onPlace(BlockPlaceContext p_40577_, CallbackInfoReturnable<InteractionResult> ci) {
        if (!p_40577_.canPlace()) {
            ci.setReturnValue(InteractionResult.FAIL);
            ci.cancel();
        } else {
            BlockPlaceContext blockplacecontext = this.updatePlacementContext(p_40577_);
            if (blockplacecontext == null) {
                ci.setReturnValue(InteractionResult.FAIL);
                ci.cancel();
            } else {
                BlockState blockstate = this.getPlacementState(blockplacecontext);
                if (blockstate != null && blockstate.getBlock() instanceof TorchBlock) {
                    BlockState existingState = blockplacecontext.getLevel().getBlockState(blockplacecontext.getClickedPos());
                    if (existingState.getFluidState().is(Fluids.WATER)) {
                        var player = blockplacecontext.getPlayer();
                        var level = blockplacecontext.getLevel();
                        if (player == null || !player.getAbilities().instabuild) {
                            specialShrink(blockplacecontext.getItemInHand());
                        }
                        if (level instanceof ServerLevel sl) {
                            var pos = blockplacecontext.getClickedPos();
                            ItemEntity itementity = new ItemEntity(level,
                                    pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f,
                                    new ItemStack(OtherverseItems.CHARCOAL_STICK.get(), 1));

                            sl.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
                            itementity.setDefaultPickUpDelay();
                            level.addFreshEntity(itementity);
                        }
                        ci.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
                        ci.cancel();
                        return;
                    }
                }

                if (blockstate == null) {
                    ci.setReturnValue(InteractionResult.FAIL);
                    ci.cancel();
                } else if (!this.placeBlock(blockplacecontext, blockstate)) {
                    ci.setReturnValue(InteractionResult.FAIL);
                    ci.cancel();
                } else {
                    BlockPos blockpos = blockplacecontext.getClickedPos();
                    Level level = blockplacecontext.getLevel();
                    Player player = blockplacecontext.getPlayer();
                    ItemStack itemstack = blockplacecontext.getItemInHand();
                    BlockState blockstate1 = level.getBlockState(blockpos);
                    if (blockstate1.is(blockstate.getBlock())) {
                        blockstate1 = this.updateBlockStateFromTag(blockpos, level, itemstack, blockstate1);
                        this.updateCustomBlockEntityTag(blockpos, level, player, itemstack, blockstate1);
                        blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);
                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, blockpos, itemstack);
                        }
                    }

                    level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));
                    SoundType soundtype = blockstate1.getSoundType(level, blockpos, p_40577_.getPlayer());
                    level.playSound(player, blockpos, this.getPlaceSound(blockstate1, level, blockpos, p_40577_.getPlayer()), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    if (player == null || !player.getAbilities().instabuild) {
                        specialShrink(itemstack);
                    }

                    ci.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
                    ci.cancel();
                }
            }
        }
    }

    private void specialShrink(ItemStack itemstack) {
        if (itemstack.hasTag() && itemstack.getTag().contains("implement_max_uses")) {
            var tag = itemstack.getTag();
            var newAmount = tag.getInt("implement_remaining_uses") - 1;
            tag.putInt("implement_remaining_uses", newAmount);
            if (newAmount <= 0) {
                itemstack.shrink(1);
            }
        } else {
            itemstack.shrink(1);
        }
    }

    @Shadow
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext p_40609_) {
        return null;
    }

    @Shadow
    protected BlockState getPlacementState(BlockPlaceContext p_40613_) {
        return null;
    }

    @Shadow
    protected boolean placeBlock(BlockPlaceContext p_40578_, BlockState p_40579_) {
        return false;
    }

    @Shadow
    private BlockState updateBlockStateFromTag(BlockPos p_40603_, Level p_40604_, ItemStack p_40605_, BlockState p_40606_) {
        return null;
    }

    @Shadow
    protected boolean updateCustomBlockEntityTag(BlockPos p_40597_, Level p_40598_, @Nullable Player p_40599_, ItemStack p_40600_, BlockState p_40601_) {
        return false;
    }

    @Shadow
    protected SoundEvent getPlaceSound(BlockState state, Level world, BlockPos pos, Player entity) {
        return null;
    }
}
