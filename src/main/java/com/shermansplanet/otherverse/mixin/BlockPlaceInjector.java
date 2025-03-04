package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockBehaviour.class)
public abstract class BlockPlaceInjector {

  @Inject(method = "getDrops", at = @At("RETURN"))
  protected void onGetDrops(BlockState blockState, LootContext.Builder context,
      CallbackInfoReturnable<List<ItemStack>> ci) {
    BlockPos pos = new BlockPos(context.getParameter(LootContextParams.ORIGIN));
    TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(context.getLevel());
    CompoundTag tag = diagramData.getPlacedItemTag(pos);
    if (tag == null) {
      return;
    }
    diagramData.removePlacedItemTag(pos);
    Item itemToMatch = blockState.getBlock().asItem();
    List<ItemStack> drops = ci.getReturnValue();
    for (ItemStack drop : drops) {
      if (drop.is(itemToMatch)) {
        tag.remove("shrine");
        drop.getOrCreateTag().put("hallow", tag);
        HallowHelper.addFakeEnchantment(drop.getTag());
        break;
      }
    }
  }
}