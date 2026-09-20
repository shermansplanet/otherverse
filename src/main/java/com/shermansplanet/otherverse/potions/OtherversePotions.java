package com.shermansplanet.otherverse.potions;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.artifacts.ConnectionReboundEffect;
import com.shermansplanet.otherverse.familiar.FamiliarBlessingEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class OtherversePotions {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Otherverse.MODID);
    public static final RegistryObject<MobEffect> HEAVINESS_EFFECT = EFFECTS.register("heaviness",
            () -> new HeavinessEffect(MobEffectCategory.HARMFUL, 0x774411));
    public static final RegistryObject<MobEffect> FAMILIAR_BLESSING_EFFECT = EFFECTS.register("familiar_blessing",
            () -> new FamiliarBlessingEffect(MobEffectCategory.BENEFICIAL, 0xffe300));
    public static final RegistryObject<MobEffect> REBOUND_EFFECT = EFFECTS.register("connection_rebound",
            () -> new ConnectionReboundEffect(MobEffectCategory.HARMFUL, 0xbb0000));
    public static final RegistryObject<MobEffect> RUINS_BOUND = EFFECTS.register("ruins_bound",
            () -> new RuinsBoundEffect(MobEffectCategory.BENEFICIAL, 0x002060));
    public static final RegistryObject<MobEffect> WATER_RESISTANCE = EFFECTS.register("water_resistance",
            () -> new RuinsBoundEffect(MobEffectCategory.BENEFICIAL, 0x0099ff));

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Otherverse.MODID);
    public static final RegistryObject<Potion> HEAVINESS_POTION = POTIONS.register("heaviness",
            () -> new Potion(new MobEffectInstance(HEAVINESS_EFFECT.get(), 1800)));
    public static final RegistryObject<Potion> LEVITATION_POTION = POTIONS.register("levitation",
            () -> new Potion(new MobEffectInstance(MobEffects.LEVITATION, 20 * 13)));
    public static final RegistryObject<Potion> WATER_RESISTANCE_POTION = POTIONS.register("water_resistance",
            () -> new Potion(new MobEffectInstance(WATER_RESISTANCE.get(), 3600)));
    public static final RegistryObject<Potion> LONG_WATER_RESISTANCE_POTION = POTIONS.register("long_water_resistance",
            () -> new Potion("water_resistance", new MobEffectInstance(WATER_RESISTANCE.get(), 9600)));

//    public static final Potion FIRE_RESISTANCE = register("fire_resistance", new Potion(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600)));
//    public static final Potion LONG_FIRE_RESISTANCE = register("long_fire_resistance", new Potion("fire_resistance", new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600)));
}
