package com.shermansplanet.otherverse.implement;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SyncPracticeDataMessage {
    public final CompoundTag nbt;

    public SyncPracticeDataMessage(CompoundTag tag) {
        this.nbt = tag;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(nbt);
    }

    public static SyncPracticeDataMessage decode(FriendlyByteBuf buffer) {
        return new SyncPracticeDataMessage(buffer.readNbt());
    }
}
