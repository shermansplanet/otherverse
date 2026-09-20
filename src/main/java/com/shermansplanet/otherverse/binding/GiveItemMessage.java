package com.shermansplanet.otherverse.binding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;

public class GiveItemMessage {
    public final int mobId;

    public GiveItemMessage(int targetId) {
        this.mobId = targetId;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(mobId);
    }

    public static GiveItemMessage decode(FriendlyByteBuf buffer) {
        int id = buffer.readInt();
        return new GiveItemMessage(id);
    }
}
