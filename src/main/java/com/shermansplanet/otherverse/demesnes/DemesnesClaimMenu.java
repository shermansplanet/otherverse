package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class DemesnesClaimMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final DataSlot range = DataSlot.standalone();
    public final DataSlot centerX = DataSlot.standalone();
    public final DataSlot centerY = DataSlot.standalone();
    public final DataSlot centerZ = DataSlot.standalone();
    public final DataSlot canClaim = DataSlot.standalone();
    public final DataSlot spiritType = DataSlot.standalone();

    // Server constructor
    public DemesnesClaimMenu(int containerId, Inventory inv, ContainerLevelAccess block) {
        super(Otherverse.DEMESNES_CLAIM_MENU.get(), containerId);
        addDemesnesDataSlots();

        block.execute((lvl, pos) -> {
            centerX.set(pos.getX());
            centerY.set(pos.getY());
            centerZ.set(pos.getZ());

            if (!(lvl.getBlockEntity(pos) instanceof DemesnesBeacon beacon)) {
                LOGGER.error("No valid beacon block found.");
                return;
            }

            range.set(beacon.range);
            canClaim.set(DemesnesManager.doesIntersectExistingClaim(beacon) ? 0 : 1);
            var s = beacon.spiritType;
            LOGGER.debug("MENU SPIRIT TYPE: " + (s == null ? "NULL" : s.label()));
            spiritType.set(s == null ? -1 : s.id());
        });
    }

    private void addDemesnesDataSlots() {
        this.addDataSlot(range);
        this.addDataSlot(centerX);
        this.addDataSlot(centerY);
        this.addDataSlot(centerZ);
        this.addDataSlot(canClaim);
        this.addDataSlot(spiritType);
    }

    // Client constructor
    public DemesnesClaimMenu(int containerId, Inventory inventory, FriendlyByteBuf friendlyByteBuf) {
        super(Otherverse.DEMESNES_CLAIM_MENU.get(), containerId);
        addDemesnesDataSlots();
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
