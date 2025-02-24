package com.shermansplanet.otherverse.implement;

import net.minecraft.network.FriendlyByteBuf;

public class FetchImplementMessage {
    public final boolean hasPrice;

    public FetchImplementMessage(boolean hasPrice) {
        this.hasPrice = hasPrice;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(hasPrice);
    }

    public static FetchImplementMessage decode(FriendlyByteBuf buffer) {
        return new FetchImplementMessage(buffer.readBoolean());
    }
}
