package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.binding.LootHelper;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.demesnes.*;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.ChalkCircleRenderer;
import com.shermansplanet.otherverse.echoes.EchoEntity;
import com.shermansplanet.otherverse.echoes.EchoRenderer;
import com.shermansplanet.otherverse.familiar.ThrownTNT;
import com.shermansplanet.otherverse.integrations.jei.BindingRecipe;
import com.shermansplanet.otherverse.integrations.jei.SpiritExtractionRecipe;
import com.shermansplanet.otherverse.integrations.jei.TransfusionRecipe;
import com.shermansplanet.otherverse.others.TyphloticShark;
import com.shermansplanet.otherverse.others.TyphloticSharkModel;
import com.shermansplanet.otherverse.others.TyphloticSharkRenderer;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.resources.OtherverseFeatureGen;
import com.shermansplanet.otherverse.spawning.SpawnAltarBlockEntity;
import com.shermansplanet.otherverse.spawning.SpawnAltarRenderer;
import com.shermansplanet.otherverse.spirits.*;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(Otherverse.MODID)
public class Otherverse {

    public static final String MODID = "otherverse";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final PracticeJsonManager PRACTICE_JSON_MANAGER = new PracticeJsonManager();
    public static final LootHelper LOOT_HELPER = new LootHelper();
    public static final PracticeTrigger ADVANCEMENTS = new PracticeTrigger();

    public static final CreativeModeTab TAB_PRACTICE = new CreativeModeTab("otherverse") {
        public @NotNull ItemStack makeIcon() {
            return OtherverseItems.CHALK.get().getDefaultInstance();
        }
    };
    public static final CreativeModeTab TAB_OTHERS = new CreativeModeTab("others") {
        public @NotNull ItemStack makeIcon() {
            return OtherverseItems.IDOL.get().getDefaultInstance();
        }
    };
    public static final CreativeModeTab TAB_SPIRITS = new CreativeModeTab("spirits") {
        public @NotNull ItemStack makeIcon() {
            return Spirits.spiritItems.get(Spirits.COLOR_GRAY).get().getDefaultInstance();
        }
    };

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<EchoEntity>> ENTITY_ECHO = ENTITIES.register("echo",
            () -> EntityType.Builder.<EchoEntity>of(EchoEntity::new, MobCategory.MISC)
                    .build(String.valueOf(new ResourceLocation(MODID, "echo"))));
    public static final RegistryObject<EntityType<TyphloticShark>> TYPHLOTIC_SHARK = ENTITIES.register("typhlotic_shark",
            () -> EntityType.Builder.of(TyphloticShark::new, MobCategory.MONSTER)
                    .immuneTo(Blocks.POWDER_SNOW).sized(0.9F, 0.5F)
                    .build(String.valueOf(new ResourceLocation(MODID, "typhlotic_shark"))));

    public static final RegistryObject<EntityType<ThrownTNT>> ENTITY_THROWN_TNT = ENTITIES.register("thrown_tnt",
            () -> EntityType.Builder.<ThrownTNT>of(ThrownTNT::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(String.valueOf(new ResourceLocation(MODID, "thrown_tnt"))));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final RegistryObject<BlockEntityType<ChalkCircle>> CHALK_CIRCLE =
            BLOCK_ENTITIES.register("chalk_circle",
                    () -> BlockEntityType.Builder.of(ChalkCircle::new, OtherverseBlocks.CHALK_LINE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SpawnAltarBlockEntity>> SPAWN_ALTAR_ENTITY =
            BLOCK_ENTITIES.register("spawn_altar",
                    () -> BlockEntityType.Builder.of(SpawnAltarBlockEntity::new, OtherverseBlocks.SPAWN_ALTAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<DemesnesBeacon>> DEMESNES_BEACON_ENTITY =
            BLOCK_ENTITIES.register("demesnes_beacon",
                    () -> BlockEntityType.Builder.of(DemesnesBeacon::new, OtherverseBlocks.DEMESNE_BEACON.get()).build(null));
    public static final RegistryObject<BlockEntityType<DemesnesPortal>> DEMESNES_PORTAL_ENTITY =
            BLOCK_ENTITIES.register("demesnes_portal",
                    () -> BlockEntityType.Builder.of(DemesnesPortal::new, OtherverseBlocks.DEMESNE_PORTAL.get()).build(null));

    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            ForgeRegistries.RECIPE_TYPES, MODID);
    public static final RegistryObject<SimpleRecipeType<SpiritExtractionRecipe>> SPIRIT_EXTRACTION = RECIPE_TYPES
            .register("spirit_extraction", () -> new SimpleRecipeType<>(MODID + ":spirit_extraction"));
    public static final RegistryObject<SimpleRecipeType<BindingRecipe>> BINDING = RECIPE_TYPES
            .register("binding", () -> new SimpleRecipeType<>(MODID + ":binding"));
    public static final RegistryObject<SimpleRecipeType<TransfusionRecipe>> TRANSFUSION = RECIPE_TYPES
            .register("transfusion", () -> new SimpleRecipeType<>(MODID + ":transfusion"));

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final RegistryObject<MenuType<DemesnesClaimMenu>> DEMESNES_CLAIM_MENU =
            MENU_TYPES.register("demesnes_claim_menu",
                    () -> IForgeMenuType.create(DemesnesClaimMenu::new));
    public static final RegistryObject<MenuType<DemesnesMenu>> DEMESNES_MENU =
            MENU_TYPES.register("demesnes_menu",
                    () -> IForgeMenuType.create(DemesnesMenu::new));

    public Otherverse() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modEventBus);
        OtherverseBlocks.BLOCKS.register(modEventBus);
        for (SpiritType st : Spirits.allSpiritTypes) {
            RegistryObject<Item> item = OtherverseItems.ITEMS.register(st.GetResourceLocation(),
                    () -> new SpiritItem(st, new Item.Properties().tab(TAB_SPIRITS)));
            Spirits.spiritItems.put(st, item);
        }
        OtherverseItems.ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        OtherverseFeatureGen.FEATURES.register(modEventBus);
        //OtherverseParticles.PARTICLES.register(modEventBus);
        bind(Registry.PARTICLE_TYPE_REGISTRY, OtherverseParticles::registerParticles);

        OtherversePotions.EFFECTS.register(modEventBus);
        OtherversePotions.POTIONS.register(modEventBus);

        modEventBus.addListener(this::onSetup);

        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.register(BindingManager.class);
        forgeEventBus.addListener(this::addReloadListeners);

        OtherversePacketHandler.register();
    }

    private static <T> void bind(ResourceKey<Registry<T>> registry, Consumer<BiConsumer<T, ResourceLocation>> source) {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterEvent event) -> {
            if (registry.equals(event.getRegistryKey())) {
                source.accept((t, rl) -> event.register(registry, rl, () -> t));
            }
        });
    }

    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(PRACTICE_JSON_MANAGER);
        event.addListener(LOOT_HELPER);
        MobSpawnAnalyzer.addListeners(event);
    }

    public static String fromItem(ItemStack s) {
        if (s.is(OtherverseItems.IDOL.get())) {
            return "entity." + ForgeRegistries.ENTITY_TYPES.getKey(IdolItem.getType(s)).toString();
        }
        return "item." + ForgeRegistries.ITEMS.getKey(s.getItem()).toString();
    }

    public static Item primeForDimension(Level level) {
        var name = level.dimensionTypeId().location().getPath();
        return switch (name) {
            case "overworld" -> OtherverseItems.SALT.get();
            case "the_nether" -> OtherverseItems.SULFUR.get();
            case "the_end" -> OtherverseItems.QUICKSILVER.get();
            default -> null;
        };
    }

    public static ItemStack toItem(String s) {
        if (s.startsWith("entity")) {
            return MobBindingInfluenceUtils.getIdol(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(s.substring(7))));
        } else {
            return ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.substring(5))).getDefaultInstance();
        }
    }

    public void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CriteriaTriggers.register(Otherverse.ADVANCEMENTS);
        });
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            SpiritColorAnalyzer.AnalyzeAllColors();
        }
/*
        @SubscribeEvent
        public static void levelLoad(LevelEvent.Load event) {
            MobBindingInfluenceUtils.makeIdols(Minecraft.getInstance().level);
            if (event.getLevel() instanceof ClientLevel cl) DiagramManager.getOrCreateLevelData(cl); // do we need this??
        }*/
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ENTITY_ECHO.get(), EchoRenderer::new);
            event.registerEntityRenderer(TYPHLOTIC_SHARK.get(), TyphloticSharkRenderer::new);
            event.registerEntityRenderer(ENTITY_THROWN_TNT.get(), ThrownItemRenderer::new);
            event.registerBlockEntityRenderer(CHALK_CIRCLE.get(), ChalkCircleRenderer::new);
            event.registerBlockEntityRenderer(DEMESNES_BEACON_ENTITY.get(), DemesnesBeaconRenderer::new);
            event.registerBlockEntityRenderer(DEMESNES_PORTAL_ENTITY.get(), DemesnesPortalRenderer::new);
            event.registerBlockEntityRenderer(SPAWN_ALTAR_ENTITY.get(), SpawnAltarRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(TyphloticSharkModel.LAYER_LOCATION, TyphloticSharkModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerBelowAll("practitioner_sight", SightOverlay.instance);
        }
    }
}
