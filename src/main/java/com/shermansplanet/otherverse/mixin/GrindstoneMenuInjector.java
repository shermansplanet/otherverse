package com.shermansplanet.otherverse.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuInjector extends AbstractContainerMenu {
    protected GrindstoneMenuInjector(@Nullable MenuType<?> p_38851_, int p_38852_) {
        super(p_38851_, p_38852_);
    }

    @Inject(method = "removeNonCurses", at = @At("RETURN"))
    private void onRemoveNonCurses(ItemStack stack, int p_39581_, int p_39582_, CallbackInfoReturnable<ItemStack> ci) {
        var tag = stack.getTag();
        if(tag == null || !tag.contains("hallow")) return;
        tag.remove("hallow");
    }
}