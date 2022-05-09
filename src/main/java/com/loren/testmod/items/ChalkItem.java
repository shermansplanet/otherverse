package com.loren.testmod.items;

import com.loren.testmod.init.BlockInit;
import com.loren.testmod.init.TileEntityInit;
import com.loren.testmod.tiles.ChalkLineTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class ChalkItem extends Item {

    public ChalkItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        BlockPos blockpos = ctx.getClickedPos().offset(ctx.getClickedFace().getNormal());
        Level level = ctx.getLevel();
        BlockEntity entity = new ChalkLineTile(blockpos, level.getBlockState(blockpos));
        level.setBlockEntity(entity);
        level.blockEntityChanged(blockpos);
        level.blockUpdated(blockpos, entity.getBlockState().getBlock());
        level.gameEvent(ctx.getPlayer(), GameEvent.BLOCK_PLACE, blockpos);

        ItemStack stack = ctx.getItemInHand();
        stack.setDamageValue(stack.getDamageValue() + 1);
        if (stack.getDamageValue() >= stack.getMaxDamage()) stack.setCount(0);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}