package com.shermansplanet.otherverse.spirits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record OverflowMessage(int dimension, BlockPos focusPos, SpiritType spiritType, int amount, boolean overdraw) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeBlockPos(focusPos);
        buffer.writeByte(spiritType.id());
        buffer.writeInt(amount);
        buffer.writeBoolean(overdraw);
    }

    public static OverflowMessage decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readInt();
        var focusPos = buffer.readBlockPos();
        var spiritType = Spirits.spiritsById.get((int)buffer.readByte());
        var amount = buffer.readInt();
        var overdraw = buffer.readBoolean();
        return new OverflowMessage(dimension, focusPos, spiritType, amount, overdraw);
    }
}
