package com.shermansplanet.otherverse.sympathy;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class BloodMendingEnchantment extends Enchantment {
    public BloodMendingEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BREAKABLE, EquipmentSlot.values());
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.MENDING;
    }

    public int getMinCost(int p_45102_) {
        return p_45102_ * 25;
    }

    public int getMaxCost(int p_45105_) {
        return this.getMinCost(p_45105_) + 50;
    }

    public boolean isTreasureOnly() {
        return true;
    }
}
