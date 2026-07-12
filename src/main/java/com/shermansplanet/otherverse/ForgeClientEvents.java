package com.shermansplanet.otherverse;

import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.others.Buzzed;
import com.shermansplanet.otherverse.others.BuzzedSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderFog(ViewportEvent.RenderFog event) {
        var player = Minecraft.getInstance().player;
        if (!player.level().dimension().location().getPath().equals("ruins")) return;
        if (player.getEyeInFluidType() != net.minecraftforge.common.ForgeMod.EMPTY_TYPE.get()) return;
        if (player.isInPowderSnow) return;
        event.setNearPlaneDistance(-32);
        event.setFarPlaneDistance(200);
        event.setCanceled(true);
    }

    public static void addBuzzed(Buzzed buzzed) {
        boolean flag = buzzed.getTarget() != null;
        BuzzedSoundInstance beesoundinstance;
        if (flag) {
            beesoundinstance = new BuzzedSoundInstance.BuzzedAggressiveSoundInstance(buzzed);
        } else {
            beesoundinstance = new BuzzedSoundInstance.BuzzedFlyingSoundInstance(buzzed);
        }

        Minecraft.getInstance().getSoundManager().queueTickingSound(beesoundinstance);
    }
}
