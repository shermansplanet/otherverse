package com.shermansplanet.otherverse.spirits;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.diagrams.ChalkItem;
import com.shermansplanet.otherverse.integrations.jei.SpiritExtractionRecipe;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.Tags.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public class SpiritLabeler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public record SpiritAmount(SpiritType type, Integer amount) {
    }

    private final static PracticeWorldManager.WorldTraitComponent<HashMap<Item, SpiritLabeler.SpiritAmount[]>> SPIRITS_FROM_TAGS = new PracticeWorldManager.WorldTraitComponent<>() {
    };
    private final static PracticeWorldManager.WorldTraitComponent<HashMap<Item, SpiritLabeler.SpiritAmount[]>> SPIRITS_FROM_COLORS = new PracticeWorldManager.WorldTraitComponent<>() {
    };
    private final static PracticeWorldManager.WorldTraitComponent<HashMap<Item, SpiritLabeler.SpiritAmount[]>> SPIRITS_FROM_JSON = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTrait<HashMap<Item, Hashtable<SpiritType, Integer>>> SPIRIT_TYPE_OF =
            new PracticeWorldManager.WorldTrait<>(new PracticeWorldManager.WorldTraitComponent[]{
                    SPIRITS_FROM_TAGS, SPIRITS_FROM_COLORS, SPIRITS_FROM_JSON
            }
            ) {
                @Override
                public boolean synthesize() {
                    LOGGER.debug("SPIRIT LABELER: SYNTHESIZING");
                    if (SPIRITS_FROM_TAGS.data == null) LOGGER.debug("NO SPIRITS FROM TAGS");
                    if (SPIRITS_FROM_COLORS.data == null) LOGGER.debug("NO SPIRITS FROM COLORS");
                    if (SPIRITS_FROM_JSON.data == null) LOGGER.debug("NO SPIRITS FROM JSON");
                    for (var component : components) {
                        if (component.data == null) return false;
                    }
                    Set<Item> itemsWithoutSpirits = new HashSet<>();
                    itemsWithoutSpirits.add(OtherverseItems.CHALK.get());
                    itemsWithoutSpirits.add(OtherverseItems.CHARCOAL_STICK.get());
                    itemsWithoutSpirits.add(OtherverseItems.SELF.get());
                    itemsWithoutSpirits.add(OtherverseItems.SPIRIT_TABLET.get());
                    itemsWithoutSpirits.add(OtherverseItems.IDOL.get());
                    for (var item : Spirits.spiritItems.values()) {
                        itemsWithoutSpirits.add(item.get());
                    }
                    data = new HashMap<>();
                    for (var component : Arrays.stream(components).map(t -> (HashMap<Item, SpiritLabeler.SpiritAmount[]>) t.data).toList()) {
                        for (var itemAmount : component.entrySet()) {
                            if (itemsWithoutSpirits.contains(itemAmount.getKey())) continue;
                            var table = data.computeIfAbsent(itemAmount.getKey(), x -> new Hashtable<>());
                            for (var spiritAmount : itemAmount.getValue()) {
                                if (spiritAmount.amount() == 0) continue;
                                var val = table.getOrDefault(spiritAmount.type(), 0);
                                table.put(spiritAmount.type(), val + spiritAmount.amount());
                            }
                        }
                    }
                    LOGGER.debug("SPIRIT LABELER: SYNTHESIZED");
                    return true;
                }
            };

    public static HashSet<Item> doubleSmeltItems;

    public static Hashtable<SpiritType, Integer> getSpiritsFor(Item item) {
        return SPIRIT_TYPE_OF.data.get(item);
    }

    public static void onStartLoadingJson() {
        SpiritLabeler.SPIRITS_FROM_JSON.data = new HashMap<>();
    }

    public static void onDoneLoadingJson() {
        SpiritLabeler.SPIRITS_FROM_JSON.setData(SPIRITS_FROM_JSON.data);
    }

    public static List<SpiritExtractionRecipe> GenerateRecipes() {
        List<SpiritExtractionRecipe> recipes = new ArrayList<>();
        HashSet<Item> spiritItems = new HashSet<>();
        for (var spiritItem : Spirits.spiritItems.values()) {
            spiritItems.add(spiritItem.get());
        }
        for (var k : new HashSet<>(SPIRIT_TYPE_OF.data.keySet())) {
            if (spiritItems.contains(k) || k instanceof SpawnEggItem) {
                continue;
            }
            List<SpiritAmount> spiritAmounts = new ArrayList<>();
            var spiritsForItem = getSpiritsFor(k);
            for (var k2 : spiritsForItem.keySet()) {
                spiritAmounts.add(new SpiritAmount(k2, spiritsForItem.get(k2)));
            }
            spiritAmounts.sort((a, b) -> b.amount.compareTo(a.amount));
            recipes.add(new SpiritExtractionRecipe(ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, k.toString()),
                    spiritAmounts, k.getDefaultInstance()));
        }
        for (var k : MobBindingInfluenceUtils.mobSpirits.entrySet()) {
            var item = MobBindingInfluenceUtils.getIdol(k.getKey());
            recipes.add(new SpiritExtractionRecipe(ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, k.getKey().toString()),
                    List.of(new SpiritAmount(k.getValue(), 1)), item));
        }
        return recipes;
    }

    public static void onColorsUpdated(HashMap<Item, SpiritLabeler.SpiritAmount[]> cs) {
        if (SPIRITS_FROM_COLORS.data == null) SPIRITS_FROM_COLORS.setData(cs);
    }

    public static void loadJsonSpirits(JsonObject practice) {
        var spirits = practice.get("spirits").getAsJsonObject();
        for (var itemstring : spirits.keySet()) {
            var loc = ResourceLocation.parse(itemstring);
            if (!ForgeRegistries.ITEMS.containsKey(loc)) continue;
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            var spiritcounts = spirits.get(itemstring).getAsJsonArray();
            SpiritAmount[] amounts = new SpiritAmount[spiritcounts.size()];
            for (var i = 0; i < spiritcounts.size(); i++) {
                var parts = spiritcounts.get(i).getAsString().split(" ");
                amounts[i] = new SpiritAmount(Spirits.spiritsByLabel.get(parts[1]), Integer.parseInt(parts[0]));
            }
            SPIRITS_FROM_JSON.data.put(item, amounts);
        }
    }

    public interface SpiritAmountDeterminer {
        int run(Item item);
    }

    public static void AddForTag(Item itemRegistry, List<SpiritAmount> amountsList,
                                 TagKey<Item> tagKey, SpiritType spiritType, int spiritAmount) {
        if (itemRegistry.getDefaultInstance().is(tagKey)) {
            amountsList.add(new SpiritAmount(spiritType, spiritAmount));
        }
    }

    public static void AddForTag(Item itemRegistry, List<SpiritAmount> amountsList,
                                 TagKey<Item> tagKey, SpiritType spiritType, SpiritAmountDeterminer spiritAmount) {
        if (itemRegistry.getDefaultInstance().is(tagKey)) {
            amountsList.add(new SpiritAmount(spiritType, spiritAmount.run(itemRegistry)));
        }
    }

    private static void AddForTag(Item itemRegistry, ArrayList<SpiritAmount> amountsList,
                                  TagKey<Block> tagKey, SpiritType spiritType, int spiritAmount) {
        if (!(itemRegistry instanceof BlockItem bi)) return;
        if (!bi.getBlock().defaultBlockState().is(tagKey)) return;
        amountsList.add(new SpiritAmount(spiritType, spiritAmount));
    }

    public static void AnalyzeTags() {
        var yieldingSpiritTypes = new SpiritType[]{Spirits.FIRE, Spirits.NETHER};

        HashMap<Item, SpiritLabeler.SpiritAmount[]> spiritsFromTags = new HashMap<>();

        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof ChalkItem || item == OtherverseItems.SELF.get()) {
                continue;
            }
            ArrayList<SpiritAmount> spiritAmounts = new ArrayList<>();

            var modId = ForgeRegistries.ITEMS.getKey(item).getNamespace();

            for (SpiritType spiritType : yieldingSpiritTypes) {
                if (item == Spirits.spiritItems.get(spiritType).get()) {
                    spiritAmounts.add(new SpiritAmount(spiritType, 3));
                }
            }

            AddForTag(item, spiritAmounts, Items.ORES, Spirits.EARTH, 1);
            AddForTag(item, spiritAmounts, Items.NETHERRACK, Spirits.EARTH, 1);
            AddForTag(item, spiritAmounts, ItemTags.DIRT, Spirits.EARTH, 1);
            AddForTag(item, spiritAmounts, Items.SANDSTONE, Spirits.EARTH, 1);
            AddForTag(item, spiritAmounts, Items.END_STONES, Spirits.EARTH, 1);
            AddForTag(item, spiritAmounts, Items.STONE, Spirits.EARTH, 3);
            AddForTag(item, spiritAmounts, Items.COBBLESTONE, Spirits.EARTH, 1);

            AddForTag(item, spiritAmounts, ItemTags.LEAVES, Spirits.AIR, 1);

            AddForTag(item, spiritAmounts, ItemTags.CANDLES, Spirits.FIRE, 1);

            AddForTag(item, spiritAmounts, Items.GLASS, Spirits.TIME, 1);
            AddForTag(item, spiritAmounts, Items.SAND, Spirits.TIME, 1);
            AddForTag(item, spiritAmounts, ItemTags.CANDLES, Spirits.TIME, 3);

            if (item instanceof ArmorItem a) {
                spiritAmounts.add(new SpiritAmount(Spirits.PROTECTION, (int) ((a.getDefense() + a.getToughness()) * 9)));
            }

            if (item instanceof HorseArmorItem horseArmor) {
                spiritAmounts.add(new SpiritAmount(Spirits.PROTECTION, horseArmor.getProtection()));
            }

            AddForTag(item, spiritAmounts, Items.BONES, Spirits.DEATH, 9);

            SpiritAmountDeterminer tierFunc = i -> i instanceof TieredItem t ?
                    (int) (t.getTier().getAttackDamageBonus() + t.getTier().getSpeed()) : 7;

            AddForTag(item, spiritAmounts, BlockTags.DIRT, Spirits.OVERWORLD, 1);
            AddForTag(item, spiritAmounts, Tags.Blocks.GRAVEL, Spirits.OVERWORLD, 1);
            AddForTag(item, spiritAmounts, Tags.Blocks.SAND, Spirits.OVERWORLD, 1);
            AddForTag(item, spiritAmounts, Tags.Blocks.SANDSTONE, Spirits.OVERWORLD, 3);
            AddForTag(item, spiritAmounts, Tags.Blocks.STONE, Spirits.OVERWORLD, 3);
            AddForTag(item, spiritAmounts, Tags.Blocks.ORES_IN_GROUND_STONE, Spirits.OVERWORLD, 3);
            AddForTag(item, spiritAmounts, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Spirits.OVERWORLD, 3);

            AddForTag(item, spiritAmounts, Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK, Spirits.NETHER, 3);
            AddForTag(item, spiritAmounts, Tags.Blocks.ORES_IN_GROUND_NETHERRACK, Spirits.OVERWORLD, 3);

            AddForTag(item, spiritAmounts, Items.END_STONES, Spirits.END, 3);

            AddForTag(item, spiritAmounts, BlockTags.CORAL_BLOCKS, Spirits.WATER, 2);
            AddForTag(item, spiritAmounts, BlockTags.UNDERWATER_BONEMEALS, Spirits.WATER, 1);

            AddForTag(item, spiritAmounts, Items.TOOLS, Spirits.OVERWORLD, tierFunc);
            AddForTag(item, spiritAmounts, Items.TOOLS_BOWS, Spirits.AIR, tierFunc);
            AddForTag(item, spiritAmounts, Items.TOOLS_CROSSBOWS, Spirits.AIR, tierFunc);
            AddForTag(item, spiritAmounts, Items.TOOLS_FISHING_RODS, Spirits.WATER, tierFunc);
            AddForTag(item, spiritAmounts, Items.TOOLS_TRIDENTS, Spirits.WATER, tierFunc);
            AddForTag(item, spiritAmounts, Items.TOOLS_SHIELDS, Spirits.PROTECTION, tierFunc);

            AddForTag(item, spiritAmounts, ItemTags.BUTTONS, Spirits.TECH, 1);

            var modifiers = item.getDefaultInstance().getAttributeModifiers(EquipmentSlot.MAINHAND);
            if (!modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty() && !modifiers
                    .get(Attributes.ATTACK_SPEED).isEmpty()) {
                float damage = 1;
                for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_DAMAGE)) {
                    damage += mod.getAmount();
                }
                float speed = 4;
                for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_SPEED)) {
                    speed += mod.getAmount();
                }
                int war = Math.round(damage * 10f * speed);
                if (war >= 64) {
                    spiritAmounts.add(new SpiritAmount(Spirits.WAR, war - 63));
                }
            }

            int burnTime = ForgeHooks.getBurnTime(item.getDefaultInstance(), null) / 100;
            if (burnTime > 0 && item != net.minecraft.world.item.Items.LAVA_BUCKET) {
                spiritAmounts.add(new SpiritAmount(Spirits.PHLOGISTON, burnTime));
            }

            if (item.isEdible()) {
                if (modId.equals("macabre")) spiritAmounts.add(new SpiritAmount(Spirits.FLESH, 9));
                var foodProps = item.getDefaultInstance().getFoodProperties(null);
                int foodAmount = 1;
                if (foodProps != null) {
                    foodAmount = (int) (foodProps.getNutrition() *
                            (1 + foodProps.getSaturationModifier() * 2));
                    if (foodProps.isMeat() && foodProps.getSaturationModifier() < 0.5f) {
                        spiritAmounts.add(new SpiritAmount(Spirits.FLESH, foodAmount));
                    }
                }
                spiritAmounts.add(new SpiritAmount(Spirits.FOOD, foodAmount));
            } else if (ComposterBlock.COMPOSTABLES.containsKey(item)) {
                spiritAmounts.add(new SpiritAmount(Spirits.NATURE,
                        (int) (ComposterBlock.COMPOSTABLES.getFloat(item) * 10)));
            }

            if (item instanceof BlockItem bi) {
                var block = bi.getBlock();
                if (block instanceof BonemealableBlock && block != Blocks.NETHERRACK) {
                    spiritAmounts.add(new SpiritAmount(Spirits.NATURE, 27));
                }
                float strength = block.defaultDestroyTime();
                if (strength > 10) {
                    spiritAmounts.add(new SpiritAmount(Spirits.PROTECTION, (int) (strength)));
                }

                int lightEmission = block.defaultBlockState().getLightEmission();
                if (lightEmission > 0) {
                    spiritAmounts.add(new SpiritAmount(Spirits.LIGHT, lightEmission * 3));
                }

                if (ForgeRegistries.ITEMS.getKey(item).getPath().startsWith("infested")) {
                    spiritAmounts.add(new SpiritAmount(Spirits.FLESH, 13));
                }

                var itemName = item.toString();
                var time = itemName.contains("oxidized") ? 9
                        : itemName.contains("weathered") ? 6
                          : itemName.contains("exposed") ? 3 : 0;
                if (time > 0) spiritAmounts.add(new SpiritAmount(Spirits.TIME, time));
                if (itemName.contains("copper")) {
                    if (time < 9) spiritAmounts.add(new SpiritAmount(Spirits.FORTUNE, 18 - time * 2));
                    spiritAmounts.add(new SpiritAmount(Spirits.TECH, 7));
                }
            }


            if (spiritAmounts.isEmpty()) {
                continue;
            }

            spiritsFromTags.put(item, spiritAmounts.toArray(new SpiritAmount[0]));
        }
        SPIRITS_FROM_TAGS.setData(spiritsFromTags);
    }

    public static void analyzeSmeltingRecipes(List<SmeltingRecipe> smeltingRecipes, ServerLevel sl) {
        HashSet<Item> products = new HashSet<>();
        doubleSmeltItems = new HashSet<>();
        SpiritTransfusions.TRANSFUSIONS_FROM_RECIPES.data = new HashMap<>();
        MobTransfusions.TRANSFUSIONS_FROM_RECIPES.data = new HashMap<>();
        for (var recipe : smeltingRecipes) {
            products.add(recipe.getResultItem(sl.registryAccess()).getItem());
            SpiritTransfusions.analyzeSmeltingRecipe(recipe, sl);
            MobTransfusions.analyzeSmeltingRecipe(recipe, sl);
        }
        for (var recipe : smeltingRecipes) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack input : ingredient.getItems()) {
                    if (products.contains(input.getItem())) {
                        doubleSmeltItems.add(recipe.getResultItem(sl.registryAccess()).getItem());
                    }
                }
            }
        }
        for (var item : ForgeRegistries.ITEMS.getKeys()) {
            var name = item.getPath();
            var precursor = name
                    .replace("exposed_", "")
                    .replace("weathered_", "exposed_")
                    .replace("oxidized", "weathered_");
            if (precursor.equals(name)) continue;
            var newKey = ResourceLocation.fromNamespaceAndPath(item.getNamespace(), precursor);
            if (ForgeRegistries.ITEMS.containsKey(newKey)) {
                var preItem = ForgeRegistries.ITEMS.getValue(newKey);
                if (ForgeRegistries.ITEMS.getValue(item) instanceof BlockItem bi) {
                    SpiritTransfusions.register(preItem, Spirits.TIME, 3, bi.getBlock());
                }
            }
        }
        SpiritTransfusions.TRANSFUSIONS_FROM_RECIPES.setData(SpiritTransfusions.TRANSFUSIONS_FROM_RECIPES.data);
        MobTransfusions.TRANSFUSIONS_FROM_RECIPES.setData(MobTransfusions.TRANSFUSIONS_FROM_RECIPES.data);
    }
}
