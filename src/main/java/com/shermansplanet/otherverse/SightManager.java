package com.shermansplanet.otherverse;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SightManager {

    private static boolean isSightOn;

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event){
        isSightOn = false;
    }

    public static boolean shouldRenderSight() {
        return isSightOn;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (!Keybindings.KEY_SIGHT.consumeClick()) return;
        isSightOn = !isSightOn;
        ReskinManager.onSightUpdate();
        if(isSightOn) SightOverlay.instance.recalculateColor();
    }
}
