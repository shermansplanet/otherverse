package com.shermansplanet.otherverse;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class OtherverseConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> DEMESNES_MOB_GRIEFING;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNBINDABLE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKIPPED_PERKS;
    public static final ForgeConfigSpec.ConfigValue<Float> BINDING_COST;
    public static final ForgeConfigSpec.ConfigValue<Float> BINDING_ATTACK_CUTOFF;
    public static final ForgeConfigSpec.ConfigValue<Integer> DAILY_SELF;
    public static final ForgeConfigSpec.ConfigValue<Boolean> BEDROCK_REMOVAL;

    static {
        BUILDER.push("Pact Magic Settings");
        UNBINDABLE_MOBS = BUILDER.comment("A list of mob IDs that cannot be bound.").defineListAllowEmpty("unbindable_mobs", new ArrayList<>(), e -> e instanceof String);
        DEMESNES_MOB_GRIEFING = BUILDER.comment("If enabled, ghasts and creepers may spawn during the demesnes ritual.").define("demesnes_mob_griefing", true);
        SKIPPED_PERKS = BUILDER.comment("Demesne perks in this list will be disabled (but will not block progress in the perk tree). Format them as they appear in the perk tree, e.g. \"Mandatory Hospitality\".").defineListAllowEmpty("skipped_perks", new ArrayList<>(), e -> e instanceof String);
        BINDING_COST = BUILDER.comment("Base multiplier of a mob's max health required to bind it.").define("binding_cost", 3f);
        BINDING_ATTACK_CUTOFF = BUILDER.comment("Hostile mobs with more than this much max HP will attack their bindings.").define("binding_attack_cutoff", 20f);
        DAILY_SELF = BUILDER.comment("Self recovered by sleeping.").define("daily_self", 3);
        BEDROCK_REMOVAL = BUILDER.comment("Can earth shrines / diagrams break bedrock?").define("bedrock_removal", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
