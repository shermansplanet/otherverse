package com.shermansplanet.otherverse.demesnes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WritItem extends Item {
    public WritItem(Properties p_41383_) {
        super(p_41383_);
    }

    public Component getName(ItemStack item) {
        if(!item.hasTag()) return Component.literal("Writ");
        var perk = DemesnesManager.DemesnePerk.values()[item.getTag().getInt("sanction")];
        return Component.literal(perk.name.replace("Sanction:", "Writ of"));
    }
}
