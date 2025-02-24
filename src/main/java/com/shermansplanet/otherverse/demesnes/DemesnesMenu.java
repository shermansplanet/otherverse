package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.EnumMap;

public class DemesnesMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClaimedDemesneData claimedDemesneData;
    public final EnumMap<DemesnesManager.DemesnePerk, DataSlot> perks = new EnumMap<>(DemesnesManager.DemesnePerk.class);
    public final DataSlot beaconX = DataSlot.standalone();
    public final DataSlot beaconY = DataSlot.standalone();
    public final DataSlot beaconZ = DataSlot.standalone();
    public final DataSlot spiritType = DataSlot.standalone();

    public DemesnesMenu(int containerId, ClaimedDemesneData data, BlockPos beaconPos) {
        super(Otherverse.DEMESNES_MENU.get(), containerId);
        addDemesnesDataSlots();
        claimedDemesneData = data;
        for (var perk : DemesnesManager.DemesnePerk.values()) {
            perks.get(perk).set(claimedDemesneData.getPerkLevel(perk));
        }
        beaconX.set(beaconPos.getX());
        beaconY.set(beaconPos.getY());
        beaconZ.set(beaconPos.getZ());
        spiritType.set(data.spiritType == null ? -1 : data.spiritType.id());
    }

    private void addDemesnesDataSlots() {
        for (var perk : DemesnesManager.DemesnePerk.values()) {
            var slot = DataSlot.standalone();
            perks.put(perk, slot);
            addDataSlot(slot);
        }
        addDataSlot(beaconX);
        addDataSlot(beaconY);
        addDataSlot(beaconZ);
        addDataSlot(spiritType);
    }

    // Client constructor
    public DemesnesMenu(int containerId, Inventory inventory, FriendlyByteBuf friendlyByteBuf) {
        super(Otherverse.DEMESNES_MENU.get(), containerId);
        addDemesnesDataSlots();
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }

    public BlockPos getBeaconPosition() {
        return new BlockPos(beaconX.get(), beaconY.get(), beaconZ.get());
    }
}
