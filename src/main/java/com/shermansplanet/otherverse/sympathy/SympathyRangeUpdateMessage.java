package com.shermansplanet.otherverse.sympathy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;

public record SympathyRangeUpdateMessage(int dx, int dy, int dz, AABB withinBounds, DyeColor color, boolean visible) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dx);
        buffer.writeInt(dy);
        buffer.writeInt(dz);

        buffer.writeInt((int) Math.round(withinBounds.minX));
        buffer.writeInt((int) Math.round(withinBounds.minY));
        buffer.writeInt((int) Math.round(withinBounds.minZ));
        buffer.writeInt((int) Math.round(withinBounds.maxX));
        buffer.writeInt((int) Math.round(withinBounds.maxY));
        buffer.writeInt((int) Math.round(withinBounds.maxZ));

        buffer.writeEnum(color);

        buffer.writeBoolean(visible);
    }

    public static SympathyRangeUpdateMessage decode(FriendlyByteBuf buffer) {
        var dx = buffer.readInt();
        var dy = buffer.readInt();
        var dz = buffer.readInt();

        var minx = buffer.readInt();
        var miny = buffer.readInt();
        var minz = buffer.readInt();
        var maxx = buffer.readInt();
        var maxy = buffer.readInt();
        var maxz = buffer.readInt();

        var color = buffer.readEnum(DyeColor.class);

        var visible = buffer.readBoolean();

        return new SympathyRangeUpdateMessage(dx, dy, dz, new AABB(minx, miny, minz, maxx, maxy, maxz), color, visible);
    }
}
