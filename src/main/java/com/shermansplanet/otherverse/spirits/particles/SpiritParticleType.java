package com.shermansplanet.otherverse.spirits.particles;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions.Deserializer;
import net.minecraft.core.particles.ParticleType;

public class SpiritParticleType extends ParticleType<ItemParticleOption> {

  public SpiritParticleType() {
    super(false, ItemParticleOption.DESERIALIZER);
  }

  @Override
  public Codec<ItemParticleOption> codec() {
    return ItemParticleOption.codec(this);
  }
}
