package com.shermansplanet.otherverse.artifacts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class WitchHatItem extends Item implements ICurioItem {
    public WitchHatItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        return armorType == EquipmentSlot.HEAD;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        // Restrict to the "head" or custom "hat" slot
        return slotContext.identifier().equals("head");
    }

}
