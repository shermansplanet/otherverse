package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BindingManager;
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

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    protected void onAiStep(CallbackInfo ci) {
        if (BindingManager.isBoundOrContracted(this)) {
            super.customServerAiStep();
            this.bossEvent.removeAllPlayers();
            if (this.level().getGameTime() % 20 == 0) {
                this.heal(1.0F);
            }
            ci.cancel();
        }
    }
}