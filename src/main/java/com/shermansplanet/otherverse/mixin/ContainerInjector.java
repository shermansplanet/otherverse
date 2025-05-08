package com.shermansplanet.otherverse.mixin;

import com.google.common.collect.Sets;
import com.shermansplanet.otherverse.implement.ImplementManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerInjector {

    @Shadow
    private ItemStack carried;
    @Shadow
    public final NonNullList<Slot> slots = NonNullList.create();
    @Shadow
    public final int containerId;
    @Shadow
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();

    protected ContainerInjector(int containerId) {
        this.containerId = containerId;
    }

    @OnlyIn(Dist.CLIENT)
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void doClick(int slotIndex, int p_150432_, ClickType clickType, Player player, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            System.out.println("NO PLAYER");
            return;
        }
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            System.out.println("NOT MY SLOT");
            return;
        }
        Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        if (!ImplementManager.isImplement(carried) && !ImplementManager.isImplement(stack)) {
            System.out.println("NOT IMPLEMENT");
            return;
        }
        if (Minecraft.getInstance().player.inventoryMenu.containerId == containerId) {
            if (slotIndex > 4) {
                System.out.println("IS PLAYER INVENTORY");
                return;
            }
        }
        System.out.println("CANCELING INTERACTION");
        ci.cancel();
    }
}
