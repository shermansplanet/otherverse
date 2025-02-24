//package com.shermansplanet.otherverse.implement;
//
//import net.minecraft.resources.ResourceLocation;
//import net.minecraftforge.client.event.TextureStitchEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.InterModComms;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
//import top.theillusivec4.curios.api.CuriosApi;
//import top.theillusivec4.curios.api.SlotTypeMessage;
//
//@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
//public class ImplementSlot {
//    @SubscribeEvent
//    public static void enqueue(final InterModEnqueueEvent evt) {
//        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
//                () -> new SlotTypeMessage.Builder("implement")
//                        .icon(new ResourceLocation("otherverse", "slot/implement"))
//                        .priority(1000).size(1).build());
//
//    }
//
//    @SubscribeEvent()
//    public static void stitch(final TextureStitchEvent.Pre evt) {
//        evt.addSprite(new ResourceLocation("otherverse", "slot/implement"));
//    }
//}
