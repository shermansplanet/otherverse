package com.shermansplanet.otherverse;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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

    public static void onToggleServer(SightToggleMessage sightToggleMessage, Supplier<NetworkEvent.Context> contextSupplier) {
        isSightOn = sightToggleMessage.isOn();
    }

    public static void toggleSight() {
        isSightOn = !isSightOn;
        System.out.println("SIGHT TOGGLED " + (isSightOn ? "ON" : "OFF"));
        ReskinManager.onSightUpdate();
        if(isSightOn) SightOverlay.instance.recalculateColor();
        OtherversePacketHandler.INSTANCE.sendToServer(new SightToggleMessage(isSightOn));
    }
}
