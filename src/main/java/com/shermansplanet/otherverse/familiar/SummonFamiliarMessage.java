package com.shermansplanet.otherverse.familiar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class SummonFamiliarMessage {
    public final boolean hasPrice;
    public BlockHitResult hitResult;

    public SummonFamiliarMessage(boolean hasPrice, HitResult hitResult) {
        this.hasPrice = hasPrice;
        if (hitResult instanceof BlockHitResult bh) {
            this.hitResult = bh;
        } else {
            this.hitResult = null;
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(hasPrice);
        if (hitResult != null) buffer.writeBlockHitResult(hitResult);
    }

    public static SummonFamiliarMessage decode(FriendlyByteBuf buffer) {
        var hp = buffer.readBoolean();
        BlockHitResult hr = null;
        try {
            hr = buffer.readBlockHitResult();
        } catch (IndexOutOfBoundsException ignored) {

        }
        return new SummonFamiliarMessage(hp, hr);
    }
}
