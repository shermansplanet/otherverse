package com.shermansplanet.otherverse.ruins;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class MemorySnareBlockEntity extends BlockEntity {
    public int storedExperience;

    public MemorySnareBlockEntity(BlockPos pos, BlockState state) {
        super(Otherverse.MEMORY_SNARE_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("storedExperience", storedExperience);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("storedExperience");
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
        if (tag != null) {
            load(tag);
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState blockState, T t) {
        var snare = (MemorySnareBlockEntity) t;
        if (level.hasNeighborSignal(pos)) {
            snare.dropExperience();
            return;
        }
        if (level.getGameTime() % 20 != Math.abs(pos.getX()) % 20) return;
        var shouldUpdate = false;
        for (var e : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(4))) {
            var tag = new CompoundTag();
            e.addAdditionalSaveData(tag);
            snare.storedExperience += e.value * tag.getInt("Count");
            shouldUpdate = true;
            e.discard();
        }
        if (shouldUpdate) {
            snare.markUpdated();
        }
    }

    private void markUpdated() {
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
    }

    public void dropExperience() {
        if (storedExperience == 0 || !(level instanceof ServerLevel sl)) return;
        ExperienceOrb.award(sl, getBlockPos().getCenter(), storedExperience);
        storedExperience = 0;
        markUpdated();
    }
}
