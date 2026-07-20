package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggInjector extends ThrowableItemProjectile {

    public ThrownEggInjector(EntityType<? extends ThrowableItemProjectile> p_37442_, Level p_37443_) {
        super(p_37442_, p_37443_);
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    protected void onHitInject(HitResult p_37488_, CallbackInfo ci) {
        if (!(getOwner() instanceof ServerPlayer sp) || !FamiliarManager.hasFamiliarType(sp, EntityType.CHICKEN)) {
            return;
        }
        super.onHit(p_37488_);
        if (!this.level().isClientSide) {
            Chicken chicken = EntityType.CHICKEN.create(this.level());
            chicken.setAge(-1200);
            chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            BindingManager.enforceLoyalty(sp, chicken, true);
            this.level().addFreshEntity(chicken);
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
        ci.cancel();
    }
}
