package com.shermansplanet.otherverse.demesnes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DemesnesClaimStartMessage(BlockPos centerPos, int range) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(centerPos);
        buffer.writeInt(range);
    }

    public static DemesnesClaimStartMessage decode(FriendlyByteBuf buffer) {
        var c = buffer.readBlockPos();
        var r = buffer.readInt();
        return new DemesnesClaimStartMessage(c, r);
    }
}
