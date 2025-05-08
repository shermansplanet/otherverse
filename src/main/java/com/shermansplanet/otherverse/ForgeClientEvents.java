package com.shermansplanet.otherverse;

import com.shermansplanet.otherverse.implement.ImplementManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        onMouseEvent(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPress(ScreenEvent.MouseButtonPressed.Pre event) {
        onMouseEvent(event);
    }

    private static void onMouseEvent(ScreenEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        var menu = mc.player.containerMenu;
        if (menu.containerId == mc.player.inventoryMenu.containerId) return;
        var stack = menu.getCarried();
        if (ImplementManager.isImplement(stack)) event.setCanceled(true);
        var slot = containerScreen.getSlotUnderMouse();
        if (slot == null) return;
        if (ImplementManager.isImplement(slot.getItem())) event.setCanceled(true);
    }
}
