package com.shermansplanet.otherverse.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemInjector extends Item implements DispensibleContainerItem {
    public BucketItemInjector(Properties p_41383_) {
        super(p_41383_);
    }

    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void onGetEmptySuccessItem(ItemStack stack, Player player, CallbackInfoReturnable<ItemStack> ci) {
        if (!stack.hasTag()) return;
        var tag = stack.getTag();
        if (tag.contains("implement_max_uses")) {
            var newAmount = tag.getInt("implement_remaining_uses") - 1;
            tag.putInt("implement_remaining_uses", newAmount);
            if (newAmount > 0) {
                ci.setReturnValue(stack);
                ci.cancel();
            }
        }
    }
}
