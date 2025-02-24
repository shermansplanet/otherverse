package com.shermansplanet.otherverse.diagrams;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DiagramCraftingMenu extends AbstractContainerMenu {

    public DiagramCraftingMenu() {
        super(null, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return false;
    }

    @Override
    public void slotsChanged(Container p_38868_) {

    }

    @Override
    public void broadcastChanges() {

    }
}
