package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.DiagramManager;

import java.util.UUID;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

public class BindingInfo {
    private static final Logger LOGGER = LogUtils.getLogger();

    public UUID bindingId;
    public BlockPos position;
    public Mob mob;
    public CompoundTag contract;
    public int dimensionHash;
    public boolean isCinnabar;
    public boolean isPositive;

    private ServerLevel overworldServer;

    public BindingInfo(UUID bindingId, BlockPos position, Mob mob, ServerLevel sl, CompoundTag contract, int dimensionHash, boolean isCinnabar, boolean isPositive) {
        this.bindingId = bindingId;
        this.position = position;
        this.mob = mob;
        this.overworldServer = sl.getServer().overworld();
        this.contract = contract;
        this.dimensionHash = dimensionHash;
        this.isCinnabar = isCinnabar;
        this.isPositive = isPositive;
    }

    public ServerLevel getLocalLevel() {
        for (var level : overworldServer.getServer().getAllLevels()) {
            if (DiagramManager.getDimensionHash(level) == dimensionHash) return level;
        }
        return null;
    }

    public static BindingInfo decode(CompoundTag tag, ServerLevel sl) {
        LOGGER.debug("DECODING DIMENSION HASH " + tag.getInt("dimensionHash"));
        return new BindingInfo(tag.getUUID("bindingId"), new BlockPos(
                tag.getInt("x"),
                tag.getInt("y"),
                tag.getInt("z")),
                null, sl, tag.getCompound("contract"), tag.getInt("dimensionHash"),
                tag.getBoolean("isCinnabar"),
                tag.getBoolean("isPositive")
        );
    }

    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("bindingId", bindingId);
        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());
        tag.put("contract", contract);
        tag.putInt("dimensionHash", dimensionHash);
        tag.putBoolean("isCinnabar", isCinnabar);
        tag.putBoolean("isPositive", isPositive);
        return tag;
    }

    public void register() {
        var localData = DiagramManager.getOrCreateLevelData(dimensionHash, false);
        localData.bindingsByPosition.put(position, this);

        var data = DiagramManager.getOrCreateLevelData(overworldServer);
        var priorBinding = data.bindingsById.get(bindingId);
        if(priorBinding != null){
            localData.bindingsByPosition.remove(priorBinding.position);
        }
        data.bindingsById.put(bindingId, this);
        if (data.savedData != null) {
            data.savedData.setDirty();
        }
        if (localData.savedData != null) {
            localData.savedData.setDirty();
        }
    }

    public void unload() {
        var localData = DiagramManager.getOrCreateLevelData(dimensionHash, false);
        localData.bindingsByPosition.remove(position);

        var data = DiagramManager.getOrCreateLevelData(overworldServer);
        if (mob != null && FamiliarManager.isFamiliar(mob)) {
            return;
        }
        data.bindingsById.remove(bindingId);
        if (data.savedData != null) {
            data.savedData.setDirty();
        }
        if (localData.savedData != null) {
            localData.savedData.setDirty();
        }
        if (mob != null) {
            mob.getPersistentData().remove("bindingId");
        }
    }

    public void setContract(CompoundTag contract) {
        DiagramManager.getOrCreateLevelData(overworldServer).savedData.setDirty();
        this.contract = contract;
    }

    public BlockFocus getFocus() {
        return DiagramManager.getOrCreateLevelData(dimensionHash, false).allBlockFoci.get(position);
    }
}
