package com.shermansplanet.otherverse.capabilities;

import com.shermansplanet.otherverse.MacabreCompat;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.implement.SyncPracticeDataMessage;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;

public class PracticeCapabilityImplementation implements IPracticeCapability {

    private CompoundTag implementTag = new CompoundTag();
    private CompoundTag familiarTag = new CompoundTag();
    private MacabreCompat.QuestStage stage = MacabreCompat.QuestStage.UNAWAKENED;

    @Override
    public CompoundTag getImplement() {
        return implementTag;
    }

    @Override
    public CompoundTag getFamiliarData() {
        return familiarTag;
    }

    @Override
    public void setImplement(CompoundTag tag, ServerPlayer player) {
        implementTag = tag;
        if (player != null) {
            sync(player);
        }
    }

    @Override
    public void setFamiliar(CompoundTag tag, ServerPlayer player) {
        familiarTag = tag;
        if (player != null) {
            sync(player);
        }
    }

    @Override
    public void sync(ServerPlayer player) {
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPracticeDataMessage(serializeNBT()));
    }

    @Override
    public MacabreCompat.QuestStage getQuestStage() {
        return stage;
    }

    @Override
    public void setQuestStage(MacabreCompat.QuestStage stage, ServerPlayer player) {
        this.stage = stage;
        if (player != null) {
            sync(player);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.put("implement", implementTag);
        tag.put("familiar", familiarTag);
        tag.putInt("macabreQuestStage", stage.ordinal());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.implementTag = nbt.getCompound("implement");
        this.familiarTag = nbt.getCompound("familiar");
        this.stage = MacabreCompat.QuestStage.values()[nbt.getInt("macabreQuestStage")];
    }
}
