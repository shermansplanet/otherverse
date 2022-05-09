package com.loren.testmod.blocks;

import com.loren.testmod.TestMod;
import com.loren.testmod.init.TileEntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, value = Dist.CLIENT)
public class BlockEntityRenderers {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(TileEntityInit.CHALK_LINE.get(), ChalkLineRenderer::new);
    }
}
