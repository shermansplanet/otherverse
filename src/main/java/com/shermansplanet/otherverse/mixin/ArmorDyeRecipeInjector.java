package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorDyeRecipe.class)
public class ArmorDyeRecipeInjector {
    @Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
    public void onAssemble(CraftingContainer container, CallbackInfoReturnable<ItemStack> ci) {
        var stack = ci.getReturnValue();
        if (stack.is(OtherverseItems.CHARCOAL_STICK.get())) {
            stack.setTag(new CompoundTag());
            ci.setReturnValue(stack);
        }

        if (!stack.is(OtherverseItems.CHALK.get())) return;

        for(int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if(!(itemstack.getItem() instanceof DyeItem dyeItem)) continue;
            var tag = new CompoundTag();
            tag.putInt("dye_color", dyeItem.getDyeColor().ordinal());
            stack.setTag(tag);
            ci.setReturnValue(stack);
            ci.cancel();
            return;
        }
    }
}
