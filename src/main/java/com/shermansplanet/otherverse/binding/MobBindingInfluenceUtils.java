package com.shermansplanet.otherverse.binding;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.ItemOrEntityType;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.integrations.jei.BindingRecipe;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritTransfer;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public class MobBindingInfluenceUtils {

    public final static PracticeWorldManager.WorldTraitComponent<HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>>> BINDINGS_FROM_JSON = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    public final static PracticeWorldManager.WorldTraitComponent<HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>>> GENERATED_BINDINGS = new PracticeWorldManager.WorldTraitComponent<>() {
    };

    private static ServerLevel cachedLevel = null;

    public final static PracticeWorldManager.WorldTrait<HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>>> ALL_BINDING_INFLUENCES =
            new PracticeWorldManager.WorldTrait<>(new PracticeWorldManager.WorldTraitComponent[]{
                    BINDINGS_FROM_JSON, GENERATED_BINDINGS
            }
            ) {

                @Override
                public boolean synthesize() {
                    for (var component : components) {
                        if (component.data == null) return false;
                    }
                    data = new HashMap<>();
                    for (var component : Arrays.stream(components).map(t -> (HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>>) t.data).toList()) {
                        for (var itemData : component.entrySet()) {
                            var key = itemData.getKey();
                            if (!data.containsKey(key)) data.put(key, new HashMap<>());
                            var map = data.get(key);
                            for (var entry : itemData.getValue().entrySet()) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    deriveMobSpirits(cachedLevel);
                    for (var mobSpirit : mobSpirits.entrySet()) {
                        var key = mobSpirit.getKey();
                        if (!data.containsKey(key)) data.put(key, new HashMap<>());
                        var map = data.get(key);
                        map.put(new ItemOrEntityType(Spirits.spiritItems.get(mobSpirit.getValue()).get()), -1);
                        var opposite = SpiritTransfer.getOppositeSpiritType(mobSpirit.getValue());
                        map.put(new ItemOrEntityType(Spirits.spiritItems.get(opposite).get()), 1);
                    }
                    putFoodsAndInfluences(data);
                    return true;
                }
            };

    public static final HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>> allFoods = new HashMap<>();
    public static final HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>> allBindingInfluences = new HashMap<>();
    public static HashMap<EntityType<? extends LivingEntity>, SpiritType> mobSpirits = new HashMap<>();

    public static void deriveMobSpirits(ServerLevel level) {

        List<TagKey<Biome>> biomeTags = List.of(BiomeTags.IS_OVERWORLD, BiomeTags.IS_NETHER, BiomeTags.IS_END,
                Tags.Biomes.IS_UNDERGROUND, Tags.Biomes.IS_HOT, Tags.Biomes.IS_DRY, Tags.Biomes.IS_COLD,
                Tags.Biomes.IS_MAGICAL, Tags.Biomes.IS_LUSH, Tags.Biomes.IS_DEAD, BiomeTags.IS_OCEAN);

        HashMap<EntityType<?>, Set<TagKey<Biome>>> mobTags = new HashMap<>();
        HashMap<EntityType<?>, Set<TagKey<Biome>>> forbiddenMobTags = new HashMap<>();

        var registry = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);

        for (var biomeKey : registry.registryKeySet()) {
            var biome = registry.getOrCreateHolderOrThrow(biomeKey);
            var tags = new HashSet<TagKey<Biome>>();
            var antiTags = new HashSet<TagKey<Biome>>();
            for (var tag : biomeTags) {
                if (biome.is(tag)) {
                    tags.add(tag);
                } else {
                    antiTags.add(tag);
                }
            }
            for (var category : biome.get().getMobSettings().getSpawnerTypes()) {
                for (var spawnerData : biome.get().getMobSettings().getMobs(category).unwrap()) {
                    var et = spawnerData.type;
                    if (mobSpirits.containsKey(et)) continue;
                    if (!mobTags.containsKey(et)) {
                        mobTags.put(et, new HashSet<>());
                        forbiddenMobTags.put(et, new HashSet<>());
                    }
                    forbiddenMobTags.get(et).addAll(antiTags);
                    mobTags.get(et).addAll(tags);
                }
            }
        }

        for (var mobTag : mobTags.entrySet()) {
            var et = mobTag.getKey();
            var tags = mobTag.getValue();
            if (tags.isEmpty()) continue;
            for (var forbiddenTag : forbiddenMobTags.get(et)) {
                tags.remove(forbiddenTag);
            }
            var spirit = getSpiritType(tags);
            if (spirit == null) continue;
            registerMobSpirit((EntityType<? extends LivingEntity>) et, spirit);
        }
    }

    public static SpiritType getSpiritType(Collection<TagKey<Biome>> tags) {
        if (tags.contains(Tags.Biomes.IS_MAGICAL)) return Spirits.FATE;
        if (tags.contains(Tags.Biomes.IS_DEAD)) return Spirits.DEATH;
        if (tags.contains(Tags.Biomes.IS_LUSH)) return Spirits.NATURE;
        if (tags.contains(BiomeTags.IS_END)) return Spirits.END;
        if (tags.contains(BiomeTags.IS_NETHER)) return Spirits.NETHER;
        if (tags.contains(BiomeTags.IS_OCEAN)) return Spirits.WATER;
        if (tags.contains(Tags.Biomes.IS_UNDERGROUND)) return Spirits.EARTH;
        if (tags.contains(Tags.Biomes.IS_COLD)) return Spirits.COLD;
        if (tags.contains(Tags.Biomes.IS_HOT) && tags.contains(Tags.Biomes.IS_DRY)) return Spirits.FIRE;
        if (tags.contains(BiomeTags.IS_OVERWORLD)) return Spirits.OVERWORLD;
        return null;
    }

    public static void putFoodsAndInfluences(HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>> fai) {
        ALL_BINDING_INFLUENCES.data = fai;
        allFoods.clear();
        allBindingInfluences.clear();
        LOGGER.debug("RECEIVING FOODS");
        for (var entry : fai.entrySet()) {
            var foods = new HashMap<ItemOrEntityType, Integer>();
            var bindingInfluences = new HashMap<ItemOrEntityType, Integer>();
            for (var ee : entry.getValue().entrySet()) {
                var target = ee.getValue() >= 0 ? bindingInfluences : foods;
                target.put(ee.getKey(), Math.abs(ee.getValue()));
            }
            if (!foods.isEmpty()) allFoods.put(entry.getKey(), foods);
            if (!bindingInfluences.isEmpty()) allBindingInfluences.put(entry.getKey(), bindingInfluences);
        }
    }

    private static final HashMap<EntityType, ItemStack> idolCache = new HashMap<>();
    public static final HashMap<ItemStack, EntityType> typesByIdol = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<EntityType<?>> leashable = new ArrayList<>();
    private static List<EntityType<?>> allIdolTypes = new ArrayList<>();

    public static void processOther(EntityType<? extends LivingEntity> other, JsonObject practice) {
        if (practice.has("binding")) {
            for (var binding : practice.getAsJsonObject("binding").entrySet()) {
                for (var item : MobTransfusions.getItemOrShortcut(binding.getKey())) {
                    register(other, item, binding.getValue().getAsInt(), false);
                }
            }
        }

        if (practice.has("healing")) {
            for (var healing : practice.getAsJsonObject("healing").entrySet()) {
                for (var item : MobTransfusions.getItemOrShortcut(healing.getKey())) {
                    registerFood(other, item, healing.getValue().getAsInt(), false);
                }
            }
        }

        if (practice.has("spirit")) {
            var spiritType = Spirits.spiritsByLabel.get(practice.get("spirit").getAsString());
            registerMobSpirit(other, spiritType);
        }
    }

    private static void registerMobSpirit(EntityType<? extends LivingEntity> other, SpiritType spiritType) {
        mobSpirits.put(other, spiritType);
    }

    public static void loadGeneratedBindings() {
        var defaultBindings = new HashMap<ItemOrEntityType, Integer>();
        defaultBindings.put(new ItemOrEntityType(Items.CHAIN), 7);
        defaultBindings.put(new ItemOrEntityType(Items.IRON_BARS), 3);

        GENERATED_BINDINGS.data = new HashMap<>();

        for (Item item : ForgeRegistries.ITEMS) {
            if (item.getDefaultInstance().is(ItemTags.PIGLIN_REPELLENTS)) {
                register(EntityType.PIGLIN, item, 3, true);
                register(EntityType.PIGLIN_BRUTE, item, 3, true);
                register(EntityType.ZOMBIFIED_PIGLIN, item, 3, true);
            }
        }

        for (EntityType entityType : leashable) {
            register(entityType, Items.LEAD, 9, true);
        }

        for (EntityType entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            for (var binding : defaultBindings.entrySet()) {
                register(entityType, binding.getKey(), binding.getValue(), true);
            }
            if (entityType.getCategory() == MobCategory.MONSTER) {
                int hp = (int) DefaultAttributes.getSupplier(entityType).getValue(Attributes.MAX_HEALTH);
                if (hp > 20) {
                    register(entityType, EntityType.WARDEN, 999, true);
                }
            }
        }

        GENERATED_BINDINGS.setData(GENERATED_BINDINGS.data);
    }

    public static void onStartLoadingJson() {
        BINDINGS_FROM_JSON.data = new HashMap<>();
    }

    public static void onFinishLoadingJson() {
        BINDINGS_FROM_JSON.setData(BINDINGS_FROM_JSON.data);
    }

    private static void register(EntityType<? extends LivingEntity> entityType, EntityType<?> et, int influence, boolean fromGenerated) {
        register(entityType, new ItemOrEntityType(et), influence, fromGenerated);
    }

    private static void register(EntityType<? extends LivingEntity> entityType, Item item, int influence, boolean fromGenerated) {
        register(entityType, new ItemOrEntityType(item), influence, fromGenerated);
    }

    private static void register(EntityType<? extends LivingEntity> entityType, ItemOrEntityType item, int influence, boolean fromGenerated) {
        var bindingInfluences = fromGenerated ? GENERATED_BINDINGS.data : BINDINGS_FROM_JSON.data;
        if (!bindingInfluences.containsKey(entityType)) {
            bindingInfluences.put(entityType, new HashMap<>());
        }
        bindingInfluences.get(entityType).put(item, influence);
    }

    private static void registerFood(EntityType<? extends LivingEntity> et, ItemOrEntityType food, int amount, boolean fromGenerated) {
        register(et, food, -amount, fromGenerated);
    }

    public static int GetInfluence(Mob mob, ItemStack item) {
        HashMap<ItemOrEntityType, Integer> influenceMap = allBindingInfluences.get(mob.getType());

        if (influenceMap == null) {
            return 0;
        }

        if (influenceMap.isEmpty()) {
            return 0;
        }

        if (item.is(OtherverseItems.IDOL.get())) {
            var ioe = new ItemOrEntityType(IdolItem.getType(item));
            if (influenceMap.containsKey(ioe)) {
                return influenceMap.get(ioe);
            }
            return 0;
        }

        if (item.hasTag() && item.getTag().contains("hallow")) {
            CompoundTag hallowTag = item.getTag().getCompound("hallow");
            ItemOrEntityType spiritItem = new ItemOrEntityType(Spirits.spiritItems
                    .get(Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"))).get());
            if (influenceMap.containsKey(spiritItem)) {
                return influenceMap.get(spiritItem) * hallowTag.getInt("spirit_count");
            }
        }
        var ioe = new ItemOrEntityType(item.getItem());
        if (influenceMap.containsKey(ioe)) {
            return Math.max(0, influenceMap.get(ioe));
        }
        return 0;
    }

    public static int GetTotalInfluence(Mob mob, List<ItemStack> items) {
        int totalInfluence = 0;
        for (ItemStack item : items) {
            totalInfluence += GetInfluence(mob, item);
        }
        return totalInfluence;
    }

    public static boolean CanBeBound(Mob mob, List<ItemStack> items, IFocus focus) {
        int totalInfluence = GetTotalInfluence(mob, items);
        LOGGER.debug("TOTAL INFLUENCE: " + totalInfluence);

        var implementData = ImplementManager.getImplementData(focus);
        boolean hasChain = !implementData.isEmpty()
                && ForgeRegistries.ITEMS.getValue(new ResourceLocation(implementData.getString("item"))) == Items.CHAIN;

        if (!hasChain && mob instanceof TamableAnimal ta && ta.isTame()) {
            return totalInfluence >= 0;
        }

        var demesneCoeff = 1f;
        var demesne = DemesnesManager.getData((ServerLevel) mob.level, focus.getPos());
        if (demesne != null) {
            demesneCoeff = (float) Math.pow(2f / 3f, demesne.getPerkLevel(DemesnesManager.DemesnePerk.BINDING));
        }

        return totalInfluence >= mob.getMaxHealth() * demesneCoeff * (hasChain ? 2 : 3);
    }

    public static List<BindingRecipe> GenerateRecipes() {
        List<BindingRecipe> recipes = new ArrayList<>();

        for (var inf : ALL_BINDING_INFLUENCES.data.entrySet()) {

            var et = inf.getKey();
            var influenceMap = inf.getValue();
            if (DefaultAttributes.getSupplier(et) == null) {
                continue; //IntelliJ says this can't be null, but it can be!!
            }

            recipes.add(new BindingRecipe(new ResourceLocation(Otherverse.MODID, et.toString()), et, influenceMap));
        }
        return recipes;
    }

    public static void MakeIdol(EntityType<?> entityType) {
        CompoundTag tag = new CompoundTag();
        var loc = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        tag.putString("entity_type_namespace", loc.getNamespace());
        tag.putString("entity_type_path", loc.getPath());
        var stack = new ItemStack(OtherverseItems.IDOL.get());
        stack.setTag(tag);
        idolCache.put(entityType, stack);
        typesByIdol.put(stack, entityType);
        allIdolTypes.add(entityType);
    }

    public static ItemStack getIdol(EntityType<?> entityType) {
        if (!idolCache.containsKey(entityType)) {
            MakeIdol(entityType);
        }
        return idolCache.get(entityType);
    }

    private static boolean serverMobInstancesAnalyzed = false;

    public static void analyzeMobInstances(ServerLevel level) {
        if (serverMobInstancesAnalyzed) return;
        serverMobInstancesAnalyzed = true;
        cachedLevel = level;
        for (EntityType<?> et : ForgeRegistries.ENTITY_TYPES) {
            if (et.getCategory() == MobCategory.MISC) continue;
            Entity instance = et.create(level);
            if (instance instanceof Mob mob) {
                MakeIdol(et);
                mob.tick();
                var le = (EntityType<? extends LivingEntity>) et;
                if (mob instanceof WaterAnimal || mob.getMobType() == MobType.WATER) {
                    registerMobSpirit(le, Spirits.WATER);
                }
                if (mob instanceof FlyingMob || mob instanceof FlyingAnimal || mob.isNoGravity()
                        || mob.getNavigation() instanceof FlyingPathNavigation) {
                    registerMobSpirit(le, Spirits.AIR);
                }
                if (mob.getMobType() == MobType.UNDEAD) {
                    registerMobSpirit(le, Spirits.DEATH);
                }
                try {
                    if (mob.canBeLeashed(null)) {
                        leashable.add(et);
                    }
                } catch (NullPointerException ignored) {

                }
            }
            if (instance != null) {
                instance.discard();
            }
        }

        loadGeneratedBindings();
    }

    public static EntityType<?> getCycleType() {
        return allIdolTypes.get((int) ((System.currentTimeMillis() / 500) % allIdolTypes.size()));
    }

    public static HashSet<ItemStack> getIdols(HashSet<EntityType<?>> entityTypes) {
        var stacks = new HashSet<ItemStack>();
        for (var et : entityTypes) {
            stacks.add(getIdol(et));
        }
        return stacks;
    }
}
