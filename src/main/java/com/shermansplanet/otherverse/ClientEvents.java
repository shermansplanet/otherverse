package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.demesnes.DemesnesClaimScreen;
import com.shermansplanet.otherverse.demesnes.DemesnesScreen;
import com.shermansplanet.otherverse.diagrams.ChalkLineBlock;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowParticle;
import com.shermansplanet.otherverse.spirits.HallowTextureManager;
import com.shermansplanet.otherverse.spirits.SpiritItem;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import com.shermansplanet.otherverse.spirits.particles.SpiritParticle;
import com.shermansplanet.otherverse.sympathy.ColorableBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    public static HallowTextureManager HALLOW_TEXTURE_MANAGER;
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == Otherverse.TAB_PRACTICE.getKey()) {
            for (var item : OtherverseItems.ITEMS.getEntries()) {
                if (item.get() instanceof SpiritItem) continue;
                if (item == OtherverseItems.IDOL || item == OtherverseItems.SELF) continue;
                event.accept(item);
            }
        }
        else if (event.getTabKey() == Otherverse.TAB_SPIRITS.getKey()) {
            for (var item : OtherverseItems.ITEMS.getEntries()) {
                if (item.get() instanceof SpiritItem) event.accept(item);
            }
        }
        else if (event.getTabKey() == Otherverse.TAB_OTHERS.getKey()){
            var encounteredTypes = new HashSet<EntityType<?>>();
            var denyList = new HashSet<String>();
            denyList.add("reaching_hand");
            denyList.add("big_reaching_hand");
            denyList.add("whirlpool");
            denyList.add("baal_whirlpool");
            for (var item : MobBindingInfluenceUtils.typesByIdol.entrySet()) {
                if (denyList.contains(ForgeRegistries.ENTITY_TYPES.getKey(item.getValue()).getPath())) continue;
                if (encounteredTypes.add(item.getValue())) {
                    event.accept(item.getKey());
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerFactories(RegisterParticleProvidersEvent evt) {
        evt.registerSpecial(OtherverseParticles.SPIRIT_PARTICLE_TYPE, new SpiritParticle.Provider());
        evt.registerSpecial(OtherverseParticles.HALLOW_PARTICLE_TYPE, new HallowParticle.Provider());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tintIndex) -> state.getValue(ChalkLineBlock.color).getFireworkColor(),
                OtherverseBlocks.CHALK_LINE.get());

        event.register((state, getter, pos, tintIndex) -> state.getValue(ColorableBlock.color).getFireworkColor(),
                OtherverseBlocks.WEB_OF_FATE.get());

        event.register((state, getter, pos, tintIndex) -> state.getValue(ColorableBlock.color).getFireworkColor(),
                OtherverseBlocks.SELECTOR.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, color) ->
                        stack.hasTag() ? DyeColor.values()[stack.getTag().getInt("dye_color")].getFireworkColor() : 0xffffff,
                OtherverseItems.CHALK.get());
    }

    @SubscribeEvent
    public static void onFMLClientSetupEvent(final FMLClientSetupEvent event) {
        LOGGER.error("ACCESS_TOKEN " + Minecraft.getInstance().getUser().getAccessToken());
        LOGGER.error("UUID " + Minecraft.getInstance().getUser().getUuid());

        PracticeWorldManager.noJeiPending = false;
        PracticeWorldManager.worldSetUp = false;

        event.enqueueWork(
                () -> {
                    MenuScreens.register(Otherverse.DEMESNES_CLAIM_MENU.get(), DemesnesClaimScreen::new);
                    MenuScreens.register(Otherverse.DEMESNES_MENU.get(), DemesnesScreen::new);
                }
        );

        HALLOW_TEXTURE_MANAGER = new HallowTextureManager(Minecraft.getInstance().getTextureManager());
        ((ReloadableResourceManager) (Minecraft.getInstance().getResourceManager())).registerReloadListener(HALLOW_TEXTURE_MANAGER);
    }
}
