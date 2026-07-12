package com.shermansplanet.otherverse.ruins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClaimArrowItem extends ArrowItem {
    public ClaimArrowItem(Properties p_40512_) {
        super(p_40512_);
    }

    public AbstractArrow createArrow(Level p_40513_, ItemStack p_40514_, LivingEntity p_40515_) {
        return new ClaimArrow(p_40513_, p_40515_);
    }
}
