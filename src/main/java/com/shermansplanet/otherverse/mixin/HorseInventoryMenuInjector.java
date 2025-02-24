package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public abstract class HorseInventoryMenuInjector extends AbstractContainerMenu {
    @Shadow
    private Container horseContainer;

    @Shadow
    private AbstractHorse horse;

    protected HorseInventoryMenuInjector(@Nullable MenuType<?> p_38851_, int p_38852_) {
        super(p_38851_, p_38852_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(int p_39656_, Inventory p_39657_, Container p_39658_, final AbstractHorse p_39659_, CallbackInfo ci) {
        if (!FamiliarManager.isChestedHorseFamiliar(this.horse)) return;

        var slotIndex = 0;

        this.addSlotWithOverride(new Slot(p_39658_, 0, 8, 8) {
            public boolean mayPlace(ItemStack p_39677_) {
                return p_39677_.is(Items.SADDLE) && !this.hasItem() && p_39659_.isSaddleable();
            }

            public boolean isActive() {
                return p_39659_.isSaddleable();
            }
        }, slotIndex++);

        for (var y = 0; y < 6; y++) {
            for (var x = 0; x < 9; x++) {
                var i = x + y * 9;
                if (i == 0) continue;
                this.addSlotWithOverride(new Slot(p_39658_, i, 8 + x * 18, 8 + y * 18), slotIndex++);
            }
        }

        for (int i1 = 0; i1 < 3; ++i1) {
            for (int k1 = 0; k1 < 9; ++k1) {
                this.addSlotWithOverride(new Slot(p_39657_, k1 + i1 * 9 + 9, 8 + k1 * 18, 128 + i1 * 18), slotIndex++);
            }
        }

        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotWithOverride(new Slot(p_39657_, j1, 8 + j1 * 18, 186), slotIndex++);
        }
    }

    protected Slot addSlotWithOverride(Slot slot, int index) {
        if (index >= this.slots.size()) {
            return addSlot(slot);
        }
        slot.index = index;
        this.slots.set(index, slot);
        return slot;
    }

}
