package com.shermansplanet.otherverse.spirits.particles;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class OtherverseParticles {

    public static final ParticleType<ItemParticleOption> SPIRIT_PARTICLE_TYPE = new SpiritParticleType();
    public static final ParticleType<ItemParticleOption> HALLOW_PARTICLE_TYPE = new SpiritParticleType();

    public static void registerParticles(BiConsumer<ParticleType<?>, ResourceLocation> r) {
        r.accept(SPIRIT_PARTICLE_TYPE, new ResourceLocation(Otherverse.MODID, "spirit_particle"));
        r.accept(HALLOW_PARTICLE_TYPE, new ResourceLocation(Otherverse.MODID, "hallow_particle"));
    }
}
