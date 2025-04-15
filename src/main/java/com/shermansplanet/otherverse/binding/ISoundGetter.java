package com.shermansplanet.otherverse.binding;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;

public interface ISoundGetter {
    SoundEvent publicGetHurtSound(DamageSource src);
    SoundEvent publicGetDeathSound();
}
