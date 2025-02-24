package com.shermansplanet.otherverse.demesnes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DemesnesPerkMessage(int perkIndex, int newValue, BlockPos beaconPos) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(perkIndex);
        buffer.writeInt(newValue);
        buffer.writeBlockPos(beaconPos);
    }

    public static DemesnesPerkMessage decode(FriendlyByteBuf buffer) {
        var perkIndex = buffer.readInt();
        var newValue = buffer.readInt();
        var beaconPos = buffer.readBlockPos();
        return new DemesnesPerkMessage(perkIndex, newValue, beaconPos);
    }
}
