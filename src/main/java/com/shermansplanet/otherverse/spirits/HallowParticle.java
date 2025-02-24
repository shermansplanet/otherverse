package com.shermansplanet.otherverse.spirits;

import com.shermansplanet.otherverse.spirits.particles.SpiritParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HallowParticle extends SpiritParticle {

  public HallowParticle(ClientLevel level, double x, double y,
      double z, double dx, double dy, double dz, ItemStack itemStack) {
    super(level, x, y, z, dx, dy, dz, itemStack);
    this.lifetime = 80;
    this.quadSize = 0;
  }

  @Override
  public void tick() {
    this.xo = this.x;
    this.yo = this.y;
    this.zo = this.z;

    float lerp = ((float) this.age) / this.lifetime;
    this.quadSize = 0.3F * lerp * (1F - lerp);
    this.setSize(this.quadSize, this.quadSize);
    this.oRoll = this.roll;
    this.roll += 0.04F;

    if (this.age++ >= this.lifetime) {
      this.remove();
    } else {
      this.move(this.xd, this.yd, this.zd);
    }
  }

  public static class Provider implements ParticleProvider<ItemParticleOption> {

    public Provider() {
    }

    public Particle createParticle(ItemParticleOption p_105677_, ClientLevel p_105678_,
        double p_105679_, double p_105680_, double p_105681_, double p_105682_, double p_105683_,
        double p_105684_) {
      return new HallowParticle(p_105678_, p_105679_, p_105680_, p_105681_, p_105682_, p_105683_,
          p_105684_, p_105677_.getItem());
    }
  }
}
