package com.shermansplanet.otherverse.registries;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.artifacts.BrazierItem;
import com.shermansplanet.otherverse.artifacts.RealmWrackedCoalItem;
import com.shermansplanet.otherverse.artifacts.WitchHatItem;
import com.shermansplanet.otherverse.binding.ContractItem;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.demesnes.EscapeRopeItem;
import com.shermansplanet.otherverse.demesnes.WritItem;
import com.shermansplanet.otherverse.diagrams.ChalkItem;
import com.shermansplanet.otherverse.familiar.FamiliarNameTagItem;
import com.shermansplanet.otherverse.ruins.ClaimArrowItem;
import com.shermansplanet.otherverse.ruins.EyelessEyeItem;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.sympathy.SpindleItem;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class OtherverseItems {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Otherverse.MODID);
    public static final List<RegistryObject<SpindleItem>> SPINDLES = new ArrayList<>();

    public static final HashMap<Item, DyeColor> spindleColors = new HashMap<>();

    static {
        for (var spirit : Spirits.colorSpiritTypes) {
            var spindle = ITEMS.register("spindle_" + spirit.label(),
                    () -> {
                        var item = new SpindleItem(new Item.Properties());
                        for (var dyeCol : Spirits.colorsByDye.entrySet()) {
                            if (dyeCol.getValue() != spirit) continue;
                            spindleColors.put(item, dyeCol.getKey());
                            break;
                        }
                        return item;
                    });
            SPINDLES.add(spindle);
        }
    }

    public enum OtherverseArmorMaterials implements ArmorMaterial {
        RUINS("otherverse:ruins", 33, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266649_) -> {
            p_266649_.put(ArmorItem.Type.BOOTS, 3);
            p_266649_.put(ArmorItem.Type.LEGGINGS, 6);
            p_266649_.put(ArmorItem.Type.CHESTPLATE, 8);
            p_266649_.put(ArmorItem.Type.HELMET, 3);
        }), 10, SoundEvents.BONE_BLOCK_BREAK, 2.0F, 0.0F, () -> Ingredient.of(Items.SCULK));

        private OtherverseArmorMaterials(String p_268171_, int p_268303_, EnumMap<ArmorItem.Type, Integer> p_267941_, int p_268086_, SoundEvent p_268145_, float p_268058_, float p_268180_, Supplier<Ingredient> p_268256_) {
            this.name = p_268171_;
            this.durabilityMultiplier = p_268303_;
            this.protectionFunctionForType = p_267941_;
            this.enchantmentValue = p_268086_;
            this.sound = p_268145_;
            this.toughness = p_268058_;
            this.knockbackResistance = p_268180_;
            this.repairIngredient = new LazyLoadedValue<>(p_268256_);
        }

        private static final EnumMap<ArmorItem.Type, Integer> HEALTH_FUNCTION_FOR_TYPE = Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266653_) -> {
            p_266653_.put(ArmorItem.Type.BOOTS, 13);
            p_266653_.put(ArmorItem.Type.LEGGINGS, 15);
            p_266653_.put(ArmorItem.Type.CHESTPLATE, 16);
            p_266653_.put(ArmorItem.Type.HELMET, 11);
        });
        private final String name;
        private final int durabilityMultiplier;
        private final EnumMap<ArmorItem.Type, Integer> protectionFunctionForType;
        private final int enchantmentValue;
        private final SoundEvent sound;
        private final float toughness;
        private final float knockbackResistance;
        private final LazyLoadedValue<Ingredient> repairIngredient;

        public int getDurabilityForType(ArmorItem.Type p_266745_) {
            return HEALTH_FUNCTION_FOR_TYPE.get(p_266745_) * this.durabilityMultiplier;
        }

        public int getDefenseForType(ArmorItem.Type p_266752_) {
            return this.protectionFunctionForType.get(p_266752_);
        }

        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }

        public SoundEvent getEquipSound() {
            return this.sound;
        }

        public Ingredient getRepairIngredient() {
            return this.repairIngredient.get();
        }

        public String getName() {
            return this.name;
        }

        public float getToughness() {
            return this.toughness;
        }

        public float getKnockbackResistance() {
            return this.knockbackResistance;
        }

        public String getSerializedName() {
            return this.name;
        }
    }

    public static final RegistryObject<Item> CHALK = ITEMS.register("chalk",
            () -> new ChalkItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> CHARCOAL_STICK = ITEMS.register("charcoal_stick",
            () -> new ChalkItem(new Item.Properties().durability(8)));
    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFUR_ORE = ITEMS.register("sulfur_ore",
            () -> new BlockItem(OtherverseBlocks.SULFUR_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SALT_CRYSTALS = ITEMS.register("salt_crystals",
            () -> new BlockItem(OtherverseBlocks.SALT_CRYSTALS.get(), new Item.Properties()));
    public static final RegistryObject<Item> QUICKSILVER = ITEMS.register("quicksilver",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WEB_OF_FATE = ITEMS.register("web_of_fate",
            () -> new BlockItem(OtherverseBlocks.WEB_OF_FATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SELF = ITEMS.register("self",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONTRACT = ITEMS.register("contract",
            () -> new ContractItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FAMILIAR_NAME_TAG = ITEMS.register("familiar_name_tag",
            () -> new FamiliarNameTagItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SLATE_SCAFFOLDING = ITEMS.register("slate_scaffolding",
            () -> new BlockItem(OtherverseBlocks.SLATE_SCAFFOLDING.get(), new Item.Properties()));
    public static final RegistryObject<Item> IDOL = ITEMS.register("idol", IdolItem::new);
    public static final RegistryObject<Item> SPAWN_ALTAR = ITEMS.register("spawn_altar",
            () -> new BlockItem(OtherverseBlocks.SPAWN_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BIOME_BRAZIER = ITEMS.register("biome_brazier",
            () -> new BrazierItem(OtherverseBlocks.BIOME_BRAZIER.get(), new Item.Properties()));
    public static final RegistryObject<Item> CINNABAR_BLOCK = ITEMS.register("cinnabar_block",
            () -> new BlockItem(OtherverseBlocks.CINNABAR_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEMESNE_BEACON = ITEMS.register("demesne_beacon",
            () -> new BlockItem(OtherverseBlocks.DEMESNE_BEACON.get(), new Item.Properties()));
    public static final RegistryObject<Item> REDSTONE_NETHER_BRICKS = ITEMS.register("redstone_nether_bricks",
            () -> new BlockItem(OtherverseBlocks.REDSTONE_NETHER_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> MEMORY_SNARE = ITEMS.register("memory_snare",
            () -> new BlockItem(OtherverseBlocks.MEMORY_SNARE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WITCH_HAT = ITEMS.register("witch_hat",
            () -> new WitchHatItem(new Item.Properties()));

    public static final RegistryObject<Item> WRIT = ITEMS.register("writ",
            () -> new WritItem(new Item.Properties()));
    public static final RegistryObject<Item> ESCAPE_ROPE = ITEMS.register("escape_rope",
            () -> new EscapeRopeItem(new Item.Properties()));
    public static final RegistryObject<Item> SPINDLE_BLOODY = ITEMS.register("spindle_bloody",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_TABLET = ITEMS.register("spirit_tablet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REALM_WRACKED_COAL = ITEMS.register("realm_wracked_coal",
            () -> new RealmWrackedCoalItem(new Item.Properties()));
    public static final RegistryObject<Item> SCRYING_POWDER = ITEMS.register("scrying_powder",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONNECTION_BLOCKER = ITEMS.register("connection_blocker",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOMUNCULUS_HEART = ITEMS.register("homunculus_heart",
            () -> new Item(new Item.Properties().durability(20)));

    public static final RegistryObject<Item> SNUFFUR = ITEMS.register("snuffur", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHIVER_TOOTH = ITEMS.register("shiver_tooth", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GLOOM_TENDRIL = ITEMS.register("gloom_tendril", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENDED_PEARL = ITEMS.register("ended_pearl", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BANSHEE_MEMBRANE = ITEMS.register("banshee_membrane", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FURY_ROD = ITEMS.register("fury_rod", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLUE_HONEY = ITEMS.register("blue_honey", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RANCID_FLESH = ITEMS.register("rancid_flesh", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RUINS_HELMET = ITEMS.register("ruins_helmet", () -> new ArmorItem(OtherverseArmorMaterials.RUINS, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> RUINS_CHESTPLATE = ITEMS.register("ruins_chestplate", () -> new ArmorItem(OtherverseArmorMaterials.RUINS, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> RUINS_LEGGINGS = ITEMS.register("ruins_leggings", () -> new ArmorItem(OtherverseArmorMaterials.RUINS, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> RUINS_BOOTS = ITEMS.register("ruins_boots", () -> new ArmorItem(OtherverseArmorMaterials.RUINS, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> CLAIM_ARROW = ITEMS.register("claim_arrow", () -> new ClaimArrowItem(new Item.Properties()));
    public static final RegistryObject<Item> EYE_OF_THE_EYELESS = ITEMS.register("eye_of_the_eyeless", () -> new EyelessEyeItem(new Item.Properties()));

    public static final RegistryObject<Item> PLUM_BRICKS_ITEM = ITEMS.register("plum_bricks",
            () -> new BlockItem(OtherverseBlocks.PLUM_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANTI_PURPUR_BLOCK_ITEM = ITEMS.register("anti_purpur_block",
            () -> new BlockItem(OtherverseBlocks.ANTI_PURPUR_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLANDSTONE_ITEM = ITEMS.register("blandstone",
            () -> new BlockItem(OtherverseBlocks.BLANDSTONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLANDESITE_ITEM = ITEMS.register("blandesite",
            () -> new BlockItem(OtherverseBlocks.BLANDESITE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SERPENT_SOIL_ITEM = ITEMS.register("serpent_soil",
            () -> new BlockItem(OtherverseBlocks.SERPENT_SOIL.get(), new Item.Properties()));
    public static final RegistryObject<Item> COOL_BIRCH_PLANKS_ITEM = ITEMS.register("cool_birch_planks",
            () -> new BlockItem(OtherverseBlocks.COOL_BIRCH_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ROYAL_BRICKS_ITEM = ITEMS.register("royal_bricks",
            () -> new BlockItem(OtherverseBlocks.ROYAL_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> VERDANT_MUD_ITEM = ITEMS.register("verdant_mud",
            () -> new BlockItem(OtherverseBlocks.VERDANT_MUD.get(), new Item.Properties()));
    public static final RegistryObject<Item> FOOLS_EMERALD_ITEM = ITEMS.register("fools_emerald",
            () -> new BlockItem(OtherverseBlocks.FOOLS_EMERALD.get(), new Item.Properties()));
    public static final RegistryObject<Item> SKIN_CORAL_ITEM = ITEMS.register("skin_coral_block",
            () -> new BlockItem(OtherverseBlocks.SKIN_CORAL_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORBIDDEN_CANDY_ITEM = ITEMS.register("forbidden_candy_block",
            () -> new BlockItem(OtherverseBlocks.FORBIDDEN_CANDY_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> PASTEL_QUARTZ_BRICKS_ITEM = ITEMS.register("pastel_quartz_bricks",
            () -> new BlockItem(OtherverseBlocks.PASTEL_QUARTZ_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> BURGUNDY_TILES_ITEM = ITEMS.register("burgundy_tiles",
            () -> new BlockItem(OtherverseBlocks.BURGUNDY_TILES.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHISELED_PLUMSTONE_ITEM = ITEMS.register("chiseled_plumstone",
            () -> new BlockItem(OtherverseBlocks.CHISELED_PLUMSTONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GHOSTLY_NETHERRACK_ITEM = ITEMS.register("ghostly_netherrack",
            () -> new BlockItem(OtherverseBlocks.GHOSTLY_NETHERRACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> MUSTARD_DIRT_ITEM = ITEMS.register("mustard_dirt",
            () -> new BlockItem(OtherverseBlocks.MUSTARD_DIRT.get(), new Item.Properties()));

    public static final RegistryObject<Item> INTESTINES = ITEMS.register("intestines",
            () -> new BlockItem(OtherverseBlocks.INTESTINES.get(), new Item.Properties()));
    public static final RegistryObject<Item> TRUCHET = ITEMS.register("truchet",
            () -> new BlockItem(OtherverseBlocks.TRUCHET.get(), new Item.Properties()));
    public static final RegistryObject<Item> GILDED_TRUCHET = ITEMS.register("gilded_truchet",
            () -> new BlockItem(OtherverseBlocks.GILDED_TRUCHET.get(), new Item.Properties()));
    public static final RegistryObject<Item> CANDYCANE = ITEMS.register("candycane",
            () -> new BlockItem(OtherverseBlocks.CANDYCANE.get(), new Item.Properties()));
}
