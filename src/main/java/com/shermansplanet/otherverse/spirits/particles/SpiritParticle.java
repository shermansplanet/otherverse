package com.shermansplanet.otherverse.spirits.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpiritParticle extends TextureSheetParticle {

  private final float uo;
  private final float vo;

  public SpiritParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
      ItemStack itemStack) {
    super(level, x, y, z);
    var model = Minecraft
        .getInstance().getItemRenderer().getModel(itemStack, level, (LivingEntity) null, 0);
    this.setSprite(model.getOverrides().resolve(model, itemStack, level, null, 0)
        .getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY));
    this.quadSize /= 2.0F;
    this.uo = this.random.nextFloat() * 3.0F;
    this.vo = this.random.nextFloat() * 3.0F;
    this.lifetime = 10;
    this.hasPhysics = false;
    this.gravity = 0;
    this.friction = 1;
    this.x = x;
    this.y = y;
    this.z = z;
    this.xo = x;
    this.yo = y;
    this.zo = z;
    double d = 1D / this.lifetime;
    this.xd = dx * d;
    this.yd = dy * d;
    this.zd = dz * d;
  }

  @Override
  public void tick() {
    this.xo = this.x;
    this.yo = this.y;
    this.zo = this.z;
    this.xd *= 1.3;
    this.yd *= 1.3;
    this.zd *= 1.3;
    if (this.age++ >= this.lifetime) {
      this.remove();
    } else {
      this.move(this.xd, this.yd, this.zd);
    }
  }

  @Override
  public ParticleRenderType getRenderType() {
    return ParticleRenderType.TERRAIN_SHEET;
  }

  protected float getU0() {
    return this.sprite.getU((double) ((this.uo + 1.0F) / 4.0F * 16.0F));
  }

  protected float getU1() {
    return this.sprite.getU((double) (this.uo / 4.0F * 16.0F));
  }

  protected float getV0() {
    return this.sprite.getV((double) (this.vo / 4.0F * 16.0F));
  }

  protected float getV1() {
    return this.sprite.getV((double) ((this.vo + 1.0F) / 4.0F * 16.0F));
  }

  @OnlyIn(Dist.CLIENT)
  public static class Provider implements ParticleProvider<ItemParticleOption> {

    public Provider() {
    }

    public Particle createParticle(ItemParticleOption p_105677_, ClientLevel p_105678_,
        double p_105679_, double p_105680_, double p_105681_, double p_105682_, double p_105683_,
        double p_105684_) {
      return new SpiritParticle(p_105678_, p_105679_, p_105680_, p_105681_, p_105682_, p_105683_,
          p_105684_, p_105677_.getItem());
    }
  }
}
