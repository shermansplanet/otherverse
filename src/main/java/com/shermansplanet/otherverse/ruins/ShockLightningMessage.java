package com.shermansplanet.otherverse.ruins;

import net.minecraft.network.FriendlyByteBuf;

public class ShockLightningMessage {
    public final float distance;

    public ShockLightningMessage(float dist) {
        distance=dist;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(distance);
    }

    public static ShockLightningMessage decode(FriendlyByteBuf buffer) {
        return new ShockLightningMessage(buffer.readFloat());
    }
}
