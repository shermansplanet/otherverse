package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FaceSetter;
import com.shermansplanet.otherverse.familiar.IAnchorSetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Phantom.class)
public class PhantomInjector extends FlyingMob implements Enemy, IAnchorSetter {
    @Shadow
    Vec3 moveTargetPoint = Vec3.ZERO;

    @Shadow
    BlockPos anchorPoint = BlockPos.ZERO;

    public void setAnchor(BlockPos pos) {
        anchorPoint = pos;
        moveTargetPoint = anchorPoint.getCenter();
    }

    protected PhantomInjector(EntityType<? extends FlyingMob> p_20806_, Level p_20807_) {
        super(p_20806_, p_20807_);
    }
}
