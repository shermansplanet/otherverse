package com.shermansplanet.otherverse.spirits;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.integrations.jei.TransfusionRecipe;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class SpiritTransfusions {

    private final static PracticeWorldManager.WorldTraitComponent<HashMap<Item, List<SpiritTransfusionData>>> TRANSFUSIONS_FROM_JSON = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTraitComponent<HashMap<Item, List<SpiritTransfusionData>>> TRANSFUSIONS_FROM_RECIPES = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTrait<HashMap<Item, List<SpiritTransfusionData>>> ALL_SPIRIT_TRANSFUSIONS =
            new PracticeWorldManager.WorldTrait<>(new PracticeWorldManager.WorldTraitComponent[]{
                    TRANSFUSIONS_FROM_JSON, TRANSFUSIONS_FROM_RECIPES
            }
            ) {
                @Override
                public boolean synthesize() {
                    for (var component : components) {
                        if (component.data == null) return false;
                    }
                    data = new HashMap<>();
                    for (var component : Arrays.stream(components).map(t -> (HashMap<Item, List<SpiritTransfusionData>>) t.data).toList()) {
                        for (var itemData : component.entrySet()) {
                            var key = itemData.getKey();
                            if (!data.containsKey(key)) data.put(key, new ArrayList<>());
                            data.get(key).addAll(itemData.getValue());
                        }
                    }
                    return true;
                }
            };

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void loadJsonTransfusion(JsonObject practice) {
        if (practice.has("block")) {
            register(
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(practice.get("input").getAsString())),
                    Spirits.spiritsByLabel.get(practice.get("spirit").getAsString()),
                    practice.get("count").getAsInt(),
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation(practice.get("block").getAsString()))
            );
        } else {
            register(
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(practice.get("input").getAsString())),
                    Spirits.spiritsByLabel.get(practice.get("spirit").getAsString()),
                    practice.get("count").getAsInt(),
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(practice.get("item").getAsString())),
                    false
            );
        }
    }


    public record SpiritTransfusionData(SpiritType spiritType, ItemStack output, int price, Block blockOutput) {
    }

    public static void onStartLoadingJson() {
        TRANSFUSIONS_FROM_JSON.data = new HashMap<>();

        register(Items.GLASS_BOTTLE, Spirits.EARTH, 13,
                PotionUtils.setPotion(new ItemStack(Items.POTION), OtherversePotions.HEAVINESS_POTION.get()), null);

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.GRAY_WOOL, Blocks.BLACK_WOOL,
                Blocks.BROWN_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL,
                Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.CYAN_WOOL, Blocks.LIGHT_BLUE_WOOL,
                Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL, Blocks.MAGENTA_WOOL, Blocks.PINK_WOOL
        });

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
                Blocks.BLACK_TERRACOTTA,
                Blocks.BROWN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
                Blocks.YELLOW_TERRACOTTA,
                Blocks.LIME_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.CYAN_TERRACOTTA,
                Blocks.LIGHT_BLUE_TERRACOTTA,
                Blocks.BLUE_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
                Blocks.PINK_TERRACOTTA
        });

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA,
                Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.RED_GLAZED_TERRACOTTA,
                Blocks.ORANGE_GLAZED_TERRACOTTA, Blocks.YELLOW_GLAZED_TERRACOTTA,
                Blocks.LIME_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA,
                Blocks.CYAN_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.BLUE_GLAZED_TERRACOTTA, Blocks.PURPLE_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.PINK_GLAZED_TERRACOTTA
        });

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_CARPET, Blocks.LIGHT_GRAY_CARPET, Blocks.GRAY_CARPET, Blocks.BLACK_CARPET,
                Blocks.BROWN_CARPET, Blocks.RED_CARPET, Blocks.ORANGE_CARPET, Blocks.YELLOW_CARPET,
                Blocks.LIME_CARPET, Blocks.GREEN_CARPET, Blocks.CYAN_CARPET, Blocks.LIGHT_BLUE_CARPET,
                Blocks.BLUE_CARPET, Blocks.PURPLE_CARPET, Blocks.MAGENTA_CARPET, Blocks.PINK_CARPET
        });
        registerDyableBlocks(new Block[]{
                Blocks.WHITE_BED, Blocks.LIGHT_GRAY_BED, Blocks.GRAY_BED, Blocks.BLACK_BED,
                Blocks.BROWN_BED, Blocks.RED_BED, Blocks.ORANGE_BED, Blocks.YELLOW_BED,
                Blocks.LIME_BED, Blocks.GREEN_BED, Blocks.CYAN_BED, Blocks.LIGHT_BLUE_BED,
                Blocks.BLUE_BED, Blocks.PURPLE_BED, Blocks.MAGENTA_BED, Blocks.PINK_BED
        });

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE,
                Blocks.BLACK_CONCRETE,
                Blocks.BROWN_CONCRETE, Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE,
                Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE,
                Blocks.LIGHT_BLUE_CONCRETE,
                Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.PINK_CONCRETE
        });

        registerDyableBlocks(new Block[]{
                Blocks.WHITE_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER,
                Blocks.GRAY_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER,
                Blocks.BROWN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER,
                Blocks.YELLOW_CONCRETE_POWDER,
                Blocks.LIME_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER,
                Blocks.LIGHT_BLUE_CONCRETE_POWDER,
                Blocks.BLUE_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER,
                Blocks.PINK_CONCRETE_POWDER
        });

        for (int i = 0; i < 16; i++) {
            Item input = OtherverseItems.SPINDLES.get(i).get();
            for (int ii = 0; ii < 16; ii++) {
                if (ii == i) continue;
                register(input, Spirits.colorSpiritTypes[ii], 3, OtherverseItems.SPINDLES.get(ii).get(), false);
            }
        }
    }

    public static void onDoneLoadingJson() {
        TRANSFUSIONS_FROM_JSON.setData(TRANSFUSIONS_FROM_JSON.data);
    }

    public static void analyzeSmeltingRecipe(SmeltingRecipe recipe, ServerLevel sl) {
        register(recipe.getIngredients().get(0).getItems()[0].getItem(),
                Spirits.PHLOGISTON, recipe.getCookingTime() / 100, recipe.getResultItem(sl.registryAccess()).getItem(), true);
    }

    private static void registerDyableBlocks(Block[] blocks) {
        for (int i = 0; i < 16; i++) {
            Item input = blocks[i].asItem();
            for (int ii = 0; ii < 16; ii++) {
                if (ii == i) continue;
                register(input, Spirits.colorSpiritTypes[ii], 3, blocks[ii]);
            }
        }
    }

    private static void register(Item input, SpiritType spiritType, int price, Item output, boolean fromRecipe) {
        register(input, spiritType, price, output.getDefaultInstance(), output instanceof BlockItem bi ? bi.getBlock() : null, fromRecipe);
    }

    private static void register(Item input, SpiritType spiritType, int price, Block output) {
        register(input, spiritType, price, output.asItem().getDefaultInstance(), output);
        if (input == Items.DIRT && output != Blocks.GRASS_BLOCK) {
            register(Items.GRASS_BLOCK, spiritType, price, output.asItem().getDefaultInstance(), output);
        }
    }

    public static void register(Item input, SpiritType spiritType, int price, ItemStack output, Block blockOutput) {
        register(input, spiritType, price, output, blockOutput, false);
    }

    public static void register(Item input, SpiritType spiritType, int price, ItemStack output, Block blockOutput, boolean fromRecipe) {
        var spiritTransfusions = fromRecipe ? TRANSFUSIONS_FROM_RECIPES.data : TRANSFUSIONS_FROM_JSON.data;
        if (!spiritTransfusions.containsKey(input)) {
            spiritTransfusions.put(input, new ArrayList<>());
        }
        spiritTransfusions.get(input)
                .add(new SpiritTransfusionData(spiritType, output, price, blockOutput));
    }

    @SubscribeEvent
    public static void onUseItem(RightClickBlock event) {
        if (!event.getItemStack().hasTag() || !event.getItemStack().getTag().contains("hallow")) {
            return;
        }
        if (event.getItemStack().getItem() instanceof BlockItem && !event.getEntity().isShiftKeyDown()) return;
        if (DiagramManager.getOrCreateLevelData(event.getLevel()).getPlacedItemTag(event.getPos()) != null) return;
        var spiritTransfusions = ALL_SPIRIT_TRANSFUSIONS.data;
        Item inputItem = event.getLevel().getBlockState(event.getPos()).getBlock().asItem();
        CompoundTag hallowTag = event.getItemStack().getTag().getCompound("hallow");
        int spiritCount = hallowTag.getInt("spirit_count");
        SpiritType spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
        if (!spiritTransfusions.containsKey(inputItem)) {
            Level level = event.getLevel();
            BlockPos blockpos = event.getPos();
            if ((spiritType == Spirits.NATURE) && spiritCount >= 33
                    && BoneMealItem
                    .applyBonemeal(event.getItemStack().copy(), level, blockpos, event.getEntity())) {
                if (!level.isClientSide) {
                    level.levelEvent(1505, blockpos, 0);
                }
                spendSpirits(event.getEntity(), hallowTag, 33, event.getItemStack());
                event.setCanceled(true);
            }
            return;
        }
        for (SpiritTransfusionData transfusion : spiritTransfusions.get(inputItem)) {
            if (transfusion.blockOutput == null) {
                continue;
            }
            if (transfusion.spiritType != spiritType || transfusion.price > spiritCount) {
                continue;
            }
            var newBlockState = transfusion.blockOutput.defaultBlockState();
            event.getLevel().setBlockAndUpdate(event.getPos(), newBlockState);
            spendSpirits(event.getEntity(), hallowTag, transfusion.price, event.getItemStack());
            if (event.getLevel() instanceof ServerLevel sl) {
                sl.playSound(null, event.getPos(), newBlockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1, 1);
            }
            event.setCanceled(true);
            return;
        }
    }

    private static void spendSpirits(Player p, CompoundTag tag, int spentSpirits, ItemStack stack) {
        if (stack.getCount() == 1) {
            tag.putInt("spirit_count", tag.getInt("spirit_count") - spentSpirits);
            return;
        }
        stack.shrink(1);
        ItemStack newstack = stack.copy();
        newstack.setCount(1);
        tag = newstack.getTag().getCompound("hallow");
        tag.putInt("spirit_count", tag.getInt("spirit_count") - spentSpirits);
        if (!p.addItem(newstack)) {
            p.drop(newstack, false);
        }
    }

    public static void tryTransfuse(ServerLevel level, IFocus sourceFocus, Diagram diagram) {
        if (sourceFocus.getProcess() != null) {
            return;
        }
        BlockPos targetFocusPos = diagram.influences.get(sourceFocus.getPos());
        if (targetFocusPos == null) {
            return;
        }

        ItemStack sourceItem = sourceFocus.getItemNotMob();
        if (sourceItem == null || !sourceItem.hasTag() || !sourceItem.getTag().contains("hallow")) {
            return;
        }

        if (!diagram.allFocusPositions.contains(targetFocusPos)) {
            return;
        }

        CompoundTag hallowTag = sourceItem.getTag().getCompound("hallow");
        SpiritType spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
        int spiritCount = hallowTag.getInt("spirit_count");

        TransientDiagramData data = DiagramManager.getOrCreateLevelData(level);
        IFocus targetFocus = data.allBlockFoci.get(targetFocusPos);
        if (level.getBlockEntity(targetFocusPos) instanceof ChalkCircle targetCircle) {
            targetFocus = targetCircle;
        }
        if (targetFocus == null) {
            return;
        }

        ItemStack inputItem = targetFocus.getItem();
        var spiritTransfusions = ALL_SPIRIT_TRANSFUSIONS.data;
        var transfusions = spiritTransfusions.get(inputItem.getItem());
        if (transfusions == null) {
            return;
        }
        for (SpiritTransfusionData transfusion : transfusions) {
            if (transfusion.spiritType != spiritType) {
                continue;
            }
            if (transfusion.price > spiritCount) {
                continue;
            }
            if (spiritType == Spirits.PHLOGISTON
                    && SpiritLabeler.doubleSmeltItems.contains(transfusion.output.getItem())
                    && MobTransfusions.noFurnaceInfluence(level, targetFocus.getPos(), diagram)) {
                continue;
            }
            if (targetFocus.isBlock() && transfusion.blockOutput == null) {
                continue;
            }
            new TransfusionProcess(targetFocus, sourceFocus,
                    SpiritAffinityTracker.getTransferDuration(sourceFocus.getDiagram().getOwnerName(), spiritType), transfusion);
            return;
        }
    }

    public static List<TransfusionRecipe> GenerateRecipes() {
        List<TransfusionRecipe> recipes = new ArrayList<>();
        var i = 0;
        for (var transfusionSet : ALL_SPIRIT_TRANSFUSIONS.data.entrySet()) {
            for (var transfusionData : transfusionSet.getValue()) {
                var set = new HashSet<ItemStack>();
                set.add(Spirits.spiritItems.get(transfusionData.spiritType).get().getDefaultInstance());
                recipes.add(
                        new TransfusionRecipe(
                                new ResourceLocation(Otherverse.MODID, "spirit_transfusion_" + i++), set,
                                transfusionSet.getKey().getDefaultInstance(), transfusionData.output,
                                transfusionData.price));
            }
        }
        return recipes;
    }
}
