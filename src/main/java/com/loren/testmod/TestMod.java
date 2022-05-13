package com.loren.testmod;

import com.loren.testmod.init.BlockInit;
import com.loren.testmod.init.ItemInit;
import com.loren.testmod.rendering.ChalkCircleRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("testmod")
public class TestMod {
    public static final String MOD_ID = "testmod";

    public TestMod() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlockInit.BLOCKS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        BlockInit.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(this::registerRenderers);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerRenderers(final EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(BlockInit.CHALK_CIRCLE.get(), ChalkCircleRenderer::new);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        event.enqueueWork(()-> {
            ItemBlockRenderTypes.setRenderLayer(BlockInit.CHALK_LINE.get(), RenderType.cutout());
        });
    }
}
