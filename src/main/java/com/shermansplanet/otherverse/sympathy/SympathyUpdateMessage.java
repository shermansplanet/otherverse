package com.shermansplanet.otherverse.sympathy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

public class SympathyUpdateMessage {
    public BlockPos position;
    public String key;
    public int levelValue;

    public SympathyUpdateMessage(String key, BlockPos position, int levelValue) {
        this.position = position;
        this.key = key;
        this.levelValue = levelValue;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(position);
        buffer.writeInt(levelValue);
        var bytes = key.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    public static SympathyUpdateMessage decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int lvl = buffer.readInt();
        var byteCount = buffer.readInt();
        var k = buffer.readBytes(byteCount).toString(StandardCharsets.UTF_8);
        return new SympathyUpdateMessage(k, pos, lvl);
    }
}
