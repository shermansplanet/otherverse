package com.shermansplanet.otherverse.artifacts;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.extensions.IForgeItem;
import org.jetbrains.annotations.Nullable;

public class RealmWrackedCoalItem extends Item implements IForgeItem {
    public RealmWrackedCoalItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType)
    {
        return ForgeHooks.getBurnTime(Items.COAL.getDefaultInstance(), recipeType);
    }
}
