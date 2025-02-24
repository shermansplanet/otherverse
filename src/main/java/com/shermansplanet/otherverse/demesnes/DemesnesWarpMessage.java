package com.shermansplanet.otherverse.demesnes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DemesnesWarpMessage(BlockPos beaconPosition) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(beaconPosition);
    }

    public static DemesnesWarpMessage decode(FriendlyByteBuf buffer) {
        var c = buffer.readBlockPos();
        return new DemesnesWarpMessage(c);
    }
}
