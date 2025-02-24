package com.shermansplanet.otherverse.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

@AutoRegisterCapability
public interface IPracticeCapability extends INBTSerializable<CompoundTag> {
    CompoundTag getImplement();
    void setImplement(CompoundTag tag, ServerPlayer player);

    CompoundTag getFamiliarData();
    void setFamiliar(CompoundTag tag, ServerPlayer player);

    void sync(ServerPlayer player);
}
