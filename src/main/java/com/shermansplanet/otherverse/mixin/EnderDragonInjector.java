package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.binding.BoundGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EnderDragon.class)
public class EnderDragonInjector extends Mob implements Enemy {

    @Shadow
    private final EndDragonFight dragonFight;

    @Shadow
    private final EnderDragonPhaseManager phaseManager;

    @Override
    protected PathNavigation createNavigation(Level lvl) {
        return new PathNavigation(this, lvl) {
            BlockPos targetPos = null;

            @Override
            public void stop() {
                this.targetPos = null;
            }

            @Override
            public BlockPos getTargetPos() {
                return this.targetPos;
            }

            @Override
            protected Path createPath(Set<BlockPos> pos, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_) {
                if (pos.isEmpty()) return null;
                for (var p : pos) {
                    targetPos = p;
                    return null;
                }
                return null;
            }

            @Override
            protected PathFinder createPathFinder(int p_26531_) {
                return null;
            }

            @Override
            protected Vec3 getTempMobPos() {
                return mob.position();
            }

            @Override
            protected boolean canUpdatePath() {
                return false;
            }
        };
    }

    protected EnderDragonInjector(EntityType<? extends Mob> p_21368_, Level p_21369_) {
        super(p_21368_, p_21369_);
        dragonFight = null;
        phaseManager = null;
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void onAiStep(CallbackInfo ci) {
        if (!getPersistentData().hasUUID("bindingId")) return;
        for (var goal : goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof BoundGoal bg) {
                bg.tick();
            }
        }
        phaseManager.setPhase(EnderDragonPhase.SITTING_SCANNING);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    public void onAiStepEnd(CallbackInfo ci) {
        if (!getPersistentData().hasUUID("bindingId")) return;
        if (phaseManager.getCurrentPhase().getFlyTargetLocation() != null) return;
        setDeltaMovement(Vec3.ZERO);
    }
}
