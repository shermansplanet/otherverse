package com.shermansplanet.otherverse.diagrams;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class ChalkCircleMenu extends AbstractContainerMenu {

    public final DataSlot centerX = DataSlot.standalone();
    public final DataSlot centerY = DataSlot.standalone();
    public final DataSlot centerZ = DataSlot.standalone();

    // Server constructor
    public ChalkCircleMenu(int containerId, @UnknownNullability ContainerLevelAccess block) {
        super(Otherverse.CHALK_CIRCLE_MENU.get(), containerId);
        addDataSlots();
        block.execute((lvl, pos) -> {
            centerX.set(pos.getX());
            centerY.set(pos.getY());
            centerZ.set(pos.getZ());
        });
    }

    // Client constructor
    public ChalkCircleMenu(int containerId, Inventory inventory, FriendlyByteBuf friendlyByteBuf) {
        super(Otherverse.CHALK_CIRCLE_MENU.get(), containerId);
        addDataSlots();
    }

    private void addDataSlots() {
        this.addDataSlot(centerX);
        this.addDataSlot(centerY);
        this.addDataSlot(centerZ);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
