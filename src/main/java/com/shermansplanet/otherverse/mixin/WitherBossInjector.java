package com.shermansplanet.otherverse.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public abstract class WitherBossInjector extends Monster implements PowerableMob, RangedAttackMob {

    @Shadow
    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    protected WitherBossInjector(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }

    @Shadow
    public int getInvulnerableTicks() {
        return 0;
    }

    @Shadow
    public void setInvulnerableTicks(int p_31511_) {
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    protected void onAiStep(CallbackInfo ci) {
        if (this.getPersistentData().hasUUID("bindingId")) {
            if (this.getInvulnerableTicks() > 0) {
                int k1 = this.getInvulnerableTicks() - 1;
                this.bossEvent.setProgress(1.0F - (float) k1 / 220.0F);
                if (k1 <= 0) {
                    this.level().explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
                    bossEvent.removeAllPlayers();
                }

                this.setInvulnerableTicks(k1);
                if (this.tickCount % 10 == 0) {
                    this.heal(10.0F);
                }

            } else {
                this.bossEvent.removeAllPlayers();
                super.customServerAiStep();
            }
            ci.cancel();
        }
    }
}