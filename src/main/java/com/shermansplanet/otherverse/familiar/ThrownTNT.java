package com.shermansplanet.otherverse.familiar;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownTNT extends ThrowableItemProjectile {

    public ThrownTNT(EntityType<? extends ThrownTNT> p_37473_, Level p_37474_) {
        super(p_37473_, p_37474_);
    }

    public ThrownTNT(Level p_37481_, LivingEntity p_37482_) {
        super(Otherverse.ENTITY_THROWN_TNT.get(), p_37482_, p_37481_);
    }

    public ThrownTNT(Level p_37476_, double p_37477_, double p_37478_, double p_37479_) {
        super(Otherverse.ENTITY_THROWN_TNT.get(), p_37477_, p_37478_, p_37479_, p_37476_);
    }

    protected Item getDefaultItem() {
        return Items.TNT;
    }

    protected void onHit(HitResult p_37488_) {
        super.onHit(p_37488_);
        if (this.level().isClientSide) return;
        explode();
        this.discard();
    }

    protected void explode() {
        float f = 4.0F;
        this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);
    }
}
