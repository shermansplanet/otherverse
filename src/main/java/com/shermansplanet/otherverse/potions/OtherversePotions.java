package com.shermansplanet.otherverse.potions;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.familiar.FamiliarBlessingEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Otherverse.MODID);
    public static final RegistryObject<Potion> HEAVINESS_POTION = POTIONS.register("heaviness",
            () -> new Potion(new MobEffectInstance(HEAVINESS_EFFECT.get(), 1800)));
}
