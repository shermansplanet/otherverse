package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

import java.nio.charset.Charset;

public record DemesnesClientboundMessage(EventType eventType, BlockPos minPos, BlockPos maxPos, int levelId, String playerName) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public enum EventType {
        START, LOAD_CLAIMED, LOAD_RITUAL, SUCCEED, ABANDON, CHRONO_SET, CHRONO_UNSET, MINING_SET, COLOR_SET
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(eventType);
        buffer.writeBlockPos(minPos);
        buffer.writeInt(maxPos.getX());
        buffer.writeInt(maxPos.getY());
        buffer.writeInt(maxPos.getZ());
        buffer.writeInt(levelId);
        buffer.writeInt(playerName.length());
        buffer.writeCharSequence(playerName, Charset.defaultCharset());
    }

    public static DemesnesClientboundMessage decode(FriendlyByteBuf buffer) {
        var e = buffer.readEnum(EventType.class);
        var min = buffer.readBlockPos();
        var maxx = buffer.readInt();
        var maxy = buffer.readInt();
        var maxz = buffer.readInt();
        var max = new BlockPos(maxx, maxy, maxz);
        var levelId = buffer.readInt();
        var nameLength = buffer.readInt();
        var playerName = buffer.readCharSequence(nameLength, Charset.defaultCharset());
        return new DemesnesClientboundMessage(e, min, max, levelId, playerName.toString());
    }
}
