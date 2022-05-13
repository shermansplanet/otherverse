package com.loren.testmod.blocks;

import com.loren.testmod.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChalkCircle extends BlockEntity {

    public ItemStack item = ItemStack.EMPTY;

    public ChalkCircle(BlockPos pos, BlockState state) {
        super(BlockInit.CHALK_CIRCLE.get(), pos, state);
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.item != ItemStack.EMPTY) {
            tag.put("CircleItem", this.item.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.item = ItemStack.EMPTY;
        if (tag.contains("CircleItem")) {
            CompoundTag compoundtag = tag.getCompound("CircleItem");
            this.item = compoundtag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(compoundtag);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if(tag == null) {
            item = ItemStack.EMPTY;
        }else{
            load(tag);
        }
    }
}
