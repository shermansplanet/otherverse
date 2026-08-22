package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collections;

@Mixin(Warden.class)
public abstract class WardenInjector extends Monster implements VibrationSystem {

    @Shadow
    AngerManagement angerManagement = new AngerManagement(this::canTargetEntity, Collections.emptyList());

    @Shadow
    public boolean canTargetEntity(@Nullable Entity p_219386_) {
        return true;
    }

    @Shadow
    private void syncClientAngerLevel() {
    }

    protected WardenInjector(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    protected void onAiStep(CallbackInfo ci) {
        if (!FamiliarManager.isFamiliar(this)) {
            return;
        }
        ServerLevel serverlevel = (ServerLevel) this.level();
        serverlevel.getProfiler().push("wardenBrain");
        var w = (Warden) self();
        w.getBrain().tick(serverlevel, w);
        this.level().getProfiler().pop();
        super.customServerAiStep();

        if (this.tickCount % 20 == 0) {
            this.angerManagement.tick(serverlevel, this::canTargetEntity);
            this.syncClientAngerLevel();
        }

        WardenAi.updateActivity(w);
        ci.cancel();
    }
}