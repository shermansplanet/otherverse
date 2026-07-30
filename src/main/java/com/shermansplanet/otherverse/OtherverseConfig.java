package com.shermansplanet.otherverse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class OtherverseConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> DEMESNES_MOB_GRIEFING;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNBINDABLE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKIPPED_PERKS;
    public static final ForgeConfigSpec.ConfigValue<Float> BINDING_COST;
    public static final ForgeConfigSpec.ConfigValue<Float> BINDING_ATTACK_CUTOFF;
    public static final ForgeConfigSpec.ConfigValue<Float> BINDING_ATTACK_CUTOFF_ANY;
    public static final ForgeConfigSpec.ConfigValue<Integer> DAILY_SELF;
    public static final ForgeConfigSpec.ConfigValue<Boolean> BEDROCK_REMOVAL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSIONAL_PRIMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SIGHT_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CAN_REDO_RITUALS;

    private static HashMap<String, HashSet<Item>> dimensionalPrimes;
    private static HashSet<Item> sightItems;

    public static HashSet<Item> getPrimesForDimension(Level level) {
        if (dimensionalPrimes == null) {
            dimensionalPrimes = new HashMap<>();
            for (var entry : DIMENSIONAL_PRIMES.get()) {
                var parts = entry.split("=");
                if (parts.length != 2) continue;
                var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(parts[1]));
                if (item == null) continue;
                if (!dimensionalPrimes.containsKey(parts[0])) dimensionalPrimes.put(parts[0], new HashSet<>());
                dimensionalPrimes.get(parts[0]).add(item);
            }
        }
        var id = level.dimensionTypeId().location().toString();
        if (!dimensionalPrimes.containsKey(id)) id = "default";
        return dimensionalPrimes.get(id);
    }

    static {
        BUILDER.push("Pact Magic Settings");
        UNBINDABLE_MOBS = BUILDER.comment("A list of mob IDs that cannot be bound.").defineListAllowEmpty("unbindable_mobs", new ArrayList<>(), e -> e instanceof String);
        DEMESNES_MOB_GRIEFING = BUILDER.comment("If enabled, ghasts and creepers may spawn during the demesnes ritual.").define("demesnes_mob_griefing", true);
        SKIPPED_PERKS = BUILDER.comment("Demesne perks in this list will be disabled (but will not block progress in the perk tree). Format them as they appear in the perk tree, e.g. \"Mandatory Hospitality\".").defineListAllowEmpty("skipped_perks", new ArrayList<>(), e -> e instanceof String);
        CAN_REDO_RITUALS = BUILDER.comment("Can the player re-do the three big rituals? If enabled, re-doing a ritual will cost 1/3 of your Self and severely lower your spiritual affinity across the board, slowing down your diagrams until you recover.").define("can_redo_rituals", false);
        BINDING_COST = BUILDER.comment("Base multiplier of a mob's max health required to bind it.").define("binding_cost", 3f);
        BINDING_ATTACK_CUTOFF = BUILDER.comment("Hostile mobs with more than this much max HP will attack their bindings.").define("binding_attack_cutoff_hostile", 20f);
        BINDING_ATTACK_CUTOFF_ANY = BUILDER.comment("ALL mobs with more than this much max HP will attack their bindings.").define("binding_attack_cutoff_any", 100f);
        DAILY_SELF = BUILDER.comment("Self recovered by sleeping.").define("daily_self", 3);
        BEDROCK_REMOVAL = BUILDER.comment("Can earth shrines / diagrams break bedrock?").define("bedrock_removal", true);
        DIMENSIONAL_PRIMES = BUILDER.comment("Prime materials for each dimension. If there are multiple entries for the same dimension, any of them will work.").defineList("dimensional_primes", List.of(
                "minecraft:overworld=otherverse:salt",
                "minecraft:the_nether=otherverse:sulfur",
                "minecraft:the_end=otherverse:quicksilver",
                "default=otherverse:cinnabar"
        ), e -> e instanceof String);
        SIGHT_ITEMS = BUILDER.comment("If this list is not empty, at least one of these items must be in your inventory for you to use the Sight.").defineListAllowEmpty("sight_items", new ArrayList<>(), e -> e instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static HashSet<Item> getSightItems() {
        if(sightItems == null){
            sightItems = new HashSet<>();
            for(var itemString : SIGHT_ITEMS.get()){
                var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemString));
                if (item == null) continue;
                sightItems.add(item);
            }
        }
        return sightItems;
    }
}
