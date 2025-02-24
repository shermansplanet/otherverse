package com.shermansplanet.otherverse.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingScanningPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DragonSittingScanningPhase.class)
public abstract class DragonSittingScanningPhaseInjector extends AbstractDragonSittingPhase {
    @Shadow
    private int scanningTime;

    public DragonSittingScanningPhaseInjector(EnderDragon p_31196_) {
        super(p_31196_);
    }

    @Override
    public Vec3 getFlyTargetLocation() {
        if (!dragon.getPersistentData().hasUUID("bindingId")) return null;
        scanningTime = 0;
        var pos = dragon.getNavigation().getTargetPos();
        if(pos == null) return null;
        return new Vec3(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }
}
