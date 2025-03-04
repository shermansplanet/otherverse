package com.shermansplanet.otherverse.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.ItemOrEntityType;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.integrations.jei.TransfusionRecipe;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public class MobTransfusions {

    public static final int MOB_DRAIN_COOLDOWN = 30;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void setupReplacements() {
        transfusionShortcuts.clear();
        transfusionShortcuts.put("!flowers", new ArrayList<>());
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.getDefaultInstance().is(ItemTags.FLOWERS)) {
                transfusionShortcuts.get("!flowers").add(new ItemOrEntityType(item));
            }
            if (item instanceof BlockItem bi) {
                if (bi.getBlock() instanceof FlowerBlock && !item.equals(Items.WITHER_ROSE)) {
                    transfusionShortcuts.get("!flowers").add(new ItemOrEntityType(item));
                }
            }
        }
    }

    public record MobTransfusionData(HashSet<EntityType<?>> entityTypes, ItemStack destItem, int price) {
    }

    public final static PracticeWorldManager.WorldTraitComponent<HashMap<ItemOrEntityType, List<MobTransfusionData>>> TRANSFUSIONS_FROM_JSON = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTraitComponent<HashMap<ItemOrEntityType, List<MobTransfusionData>>> TRANSFUSIONS_FROM_RECIPES = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTrait<HashMap<ItemOrEntityType, List<MobTransfusionData>>> ALL_MOB_TRANSFUSIONS =
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
                    for (var component : Arrays.stream(components).map(t -> (HashMap<ItemOrEntityType, List<MobTransfusionData>>) t.data).toList()) {
                        for (var itemData : component.entrySet()) {
                            var key = itemData.getKey();
                            if (!data.containsKey(key)) data.put(key, new ArrayList<>());
                            data.get(key).addAll(itemData.getValue());
                        }
                    }
                    return true;
                }
            };

    private static final HashMap<String, List<ItemOrEntityType>> transfusionShortcuts = new HashMap<>();

    public static List<TransfusionRecipe> GenerateRecipes() {
        List<TransfusionRecipe> recipes = new ArrayList<>();
        var i = 0;
        for (var transfusionSet : ALL_MOB_TRANSFUSIONS.data.entrySet()) {
            for (var transfusionData : transfusionSet.getValue()) {
                recipes.add(
                        new TransfusionRecipe(new ResourceLocation(Otherverse.MODID, "mob_transfusion_" + i++),
                                MobBindingInfluenceUtils.getIdols(transfusionData.entityTypes),
                                transfusionSet.getKey().getItemStack(), transfusionData.destItem,
                                transfusionData.price));
            }
        }
        return recipes;
    }

    public static void analyzeSmeltingRecipe(SmeltingRecipe recipe) {
        register(EntityType.BLAZE, recipe.getIngredients().get(0).getItems()[0].getItem(),
                recipe.getResultItem(), 1, true);
    }

    public static boolean tryLightningTransform(ServerLevel level, BlockFocus focus, Diagram diagram) {
        if (!focus.getItem().is(Items.LIGHTNING_ROD)) {
            return false;
        }
        BlockPos target = diagram.influences.get(focus.getPos());
        BindingInfo binding = DiagramManager.getBindingOrBoundMobAt(level, target);
        if (binding == null || binding.mob == null || !level.canSeeSky(binding.mob.blockPosition())) {
            return false;
        }
        if (!diagram.trySpendPower(level, focus.getPos(), 33, new HashSet<>())) {
            return false;
        }
        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
        if (lightningbolt == null) {
            return false;
        }
        lightningbolt.moveTo(Vec3.atBottomCenterOf(target));
        lightningbolt.setVisualOnly(false);
        level.addFreshEntity(lightningbolt);
        return true;
    }

    public static Collection<ItemOrEntityType> getItemOrShortcut(String s) {
        if (s.startsWith("entity.")) {
            var parts = s.split("\\.");
            return List.of(new ItemOrEntityType(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(
                    parts[1], parts[2]
            ))));
        }
        if (transfusionShortcuts.containsKey(s)) {
            return transfusionShortcuts.get(s);
        }
        return List.of(new ItemOrEntityType(ForgeRegistries.ITEMS.getValue(new ResourceLocation(s))));
    }

    public static void processOther(EntityType<? extends LivingEntity> other, JsonObject practice) {
        if (practice.has("transfusion")) {
            for (JsonElement transfusionElement : practice.getAsJsonArray("transfusion")) {
                JsonObject transfusion = transfusionElement.getAsJsonObject();
                for (var item : getItemOrShortcut(transfusion.get("input").getAsString())) {
                    register(other, item.item,
                            ForgeRegistries.ITEMS.getValue(new ResourceLocation(transfusion.get("output").getAsString())).getDefaultInstance(),
                            transfusion.get("health").getAsInt(),
                            false
                    );
                }
            }
        }
    }

    public static boolean tryFeedFromMob(ChalkCircle circle, Diagram diagram) {
        return circle.getItem().is(OtherverseItems.IDOL.get()) && tryFeedFromMob(new BindingOrFleshbinding(circle), circle.getPos(), diagram);
    }

    public static ItemStack GetEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = Items.ENCHANTED_BOOK.getDefaultInstance();
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, level));
        return book;
    }

    public static void onStartLoadingJson() {
        TRANSFUSIONS_FROM_JSON.data = new HashMap<>();
    }

    public static void onDoneLoadingJson() {
        TRANSFUSIONS_FROM_JSON.setData(TRANSFUSIONS_FROM_JSON.data);
    }

    public static void register(EntityType<? extends LivingEntity> et, Item a, ItemStack b, int price, boolean fromRecipe) {
        var ets = new HashSet<EntityType<?>>();
        ets.add(et);
        register(ets, a, b, price, fromRecipe);
    }

    public static void register(HashSet<EntityType<?>> ets, Item a, ItemStack b, int price, boolean fromRecipe) {
        var ioe = new ItemOrEntityType(a);
        var possibleTransfusions = fromRecipe ? TRANSFUSIONS_FROM_RECIPES.data : TRANSFUSIONS_FROM_JSON.data;
        if (!possibleTransfusions.containsKey(ioe)) {
            possibleTransfusions.put(ioe, new ArrayList<>());
        }
        possibleTransfusions.get(ioe).add(new MobTransfusionData(ets, b, price));
    }

    public static boolean noFurnaceInfluence(Level level, BlockPos targetPos, Diagram diagram) {
        var blockFoci = DiagramManager.getOrCreateLevelData(level).allBlockFoci;
        for (var influencePos : diagram.influences.entrySet()) {
            if (!influencePos.getValue().equals(targetPos)) {
                continue;
            }
            if (level.getBlockEntity(influencePos.getKey()) instanceof ChalkCircle cc && cc.getItem().is(Items.FURNACE)) {
                return false;
            }
            BlockFocus focus = blockFoci.get(influencePos.getKey());
            if (focus != null && focus.getItem().is(Items.FURNACE)) {
                return false;
            }
        }
        return true;
    }

    public static boolean tryTransfuse(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        var ioe = new ItemOrEntityType(circle.getItem().getItem());
        var possibleTransfusions = ALL_MOB_TRANSFUSIONS.data;
        if (!possibleTransfusions.containsKey(ioe)) {
            return false;
        }
        List<MobTransfusionData> transfusions = possibleTransfusions.get(ioe);
        for (int i = transfusions.size() - 1; i >= 0; i--) {
            var possibleInfusion = transfusions.get(i);
            if (possibleInfusion.entityTypes.size() == 1 && possibleInfusion.entityTypes.contains(EntityType.BLAZE)
                    && SpiritLabeler.doubleSmeltItems.contains(possibleInfusion.destItem.getItem())
                    && noFurnaceInfluence(level, circle.getPos(), diagram)
            ) {
                continue;
            }
            if (diagram.trySpendPower(level, circle.getBlockPos(), possibleInfusion.price,
                    possibleInfusion.entityTypes)) {
                circle.item = possibleInfusion.destItem.copy();
                circle.markUpdated();
                var bp = circle.getBlockPos();
                for (var p = 0; p < 8; p++) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, circle.getItem()),
                            bp.getX() + 0.5, bp.getY() + 0.1, bp.getZ() + 0.5, 1, 0, 0, 0, 0.1D);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean tryFeedFromMob(BindingOrFleshbinding sourceBinding, BlockPos mobPosition, Diagram diagram) {
        if (!(diagram.level instanceof ServerLevel sl)) return false;
        BlockPos targetPos = diagram.influences.get(mobPosition);
        if (targetPos == null || sourceBinding.getHealth() <= sourceBinding.efficiencyReduction || sourceBinding.invulnerableTime() > 0) {
            return false;
        }
        var targetBinding = BindingOrFleshbinding.getFromPosition(sl, targetPos);

        if (targetBinding == null || targetBinding.canBeHealed()) {
            return false;
        }

        int targetHealthGap = targetBinding.getMaxHealth() - targetBinding.getHealth();
        if (targetHealthGap == 0) {
            return false;
        }

        var foods = MobBindingInfluenceUtils.allFoods.get(targetBinding.entityType);
        if (foods == null) {
            return false;
        }

        if (!foods.containsKey(new ItemOrEntityType(sourceBinding.entityType))) {
            return false;
        }

        int transferAmount = Math.min(sourceBinding.getHealth() - 1, targetHealthGap);
        targetBinding.changeHealth(transferAmount, sl);
        sourceBinding.changeHealth(-transferAmount * sourceBinding.efficiencyReduction, sl);
        sourceBinding.setInvulnerableTime(MOB_DRAIN_COOLDOWN);

        return true;
    }

    public static boolean tryFeed(ServerLevel level, IFocus sourceFocus, Diagram diagram) {
        Item item = sourceFocus.getItem().getItem();
        SpiritType spiritType = null;
        if (sourceFocus.getItem().hasTag() && sourceFocus.getItem().getTag().contains("hallow")) {
            CompoundTag ht = sourceFocus.getItem().getTag().getCompound("hallow");
            spiritType = Spirits.spiritsByLabel.get(ht.getString("spirit_type"));
            if(spiritType == null){
                LOGGER.error("HALLOW WITHOUT SPIRIT TYPE");
                return false;
            }
            item = Spirits.spiritItems.get(spiritType).get();
        }
        BlockPos target = diagram.influences.get(sourceFocus.getPos());
        if (target == null) {
            return false;
        }
        BindingOrFleshbinding binding = BindingOrFleshbinding.getFromPosition(level, target);
        if (binding == null || binding.getHealth() == binding.getMaxHealth()) {
            return false;
        }
        var foods = MobBindingInfluenceUtils.allFoods.get(binding.entityType);
        if (foods == null) {
            return false;
        }
        Integer amount = foods.get(new ItemOrEntityType(item));
        if (amount == null) {
            return false;
        }
        if (spiritType != null) {
            int maxHeal = binding.getMaxHealth() - binding.getHealth();
            int spiritCount = (int) Math.ceil(maxHeal / (float) amount);
            amount = sourceFocus.drainHallow(spiritType, spiritCount * 2, false, false) / 2;
            if (amount == 0) {
                return false;
            }
        }
        binding.changeHealth(amount, level);
        BlockPos bp = sourceFocus.getPos();
        for (var i = 0; i < 10; i++) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item.getDefaultInstance()),
                    bp.getX() + 0.5, bp.getY() + 0.25, bp.getZ() + 0.5, 1, 0, 0.1, 0, 0.15);
        }

        if (spiritType == null) {
            sourceFocus.removeItem();
        }

        return true;
    }

}
