package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.familiar.FaceSetter;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Shulker.class)
public class ShulkerInjector extends AbstractGolem implements Enemy, FaceSetter {

    @Shadow
    protected static EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID;

    protected ShulkerInjector(EntityType<? extends AbstractGolem> p_27508_, Level p_27509_) {
        super(p_27508_, p_27509_);
    }

    @Override
    public void setFace(Direction dir) {
        this.entityData.set(DATA_ATTACH_FACE_ID, dir);
    }
}
