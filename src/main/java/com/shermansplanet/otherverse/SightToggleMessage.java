package com.shermansplanet.otherverse;

import net.minecraft.network.FriendlyByteBuf;

public record SightToggleMessage(boolean isOn) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isOn);
    }

    public static SightToggleMessage decode(FriendlyByteBuf buffer) {
        return new SightToggleMessage(buffer.readBoolean());
    }
}
