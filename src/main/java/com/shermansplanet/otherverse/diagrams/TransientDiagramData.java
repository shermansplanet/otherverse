package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.binding.BindingInfo;
import com.shermansplanet.otherverse.binding.BindingUpdateMessage;
import com.shermansplanet.otherverse.demesnes.ClaimedDemesneData;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.spirits.HallowUpdateMessage;
import com.shermansplanet.otherverse.spirits.SpiritAffinityTracker;
import com.shermansplanet.otherverse.sympathy.SympathyUpdateMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.*;

public class TransientDiagramData {

    private static final Logger LOGGER = LogUtils.getLogger();

    public Map<BlockPos, BlockFocus> allBlockFoci = new HashMap<>();
    public Map<BlockPos, Diagram> diagramsByPrimary = new HashMap<>();
    public HashSet<Diagram> diagramsToActivate = new HashSet<>();
    public HashMap<BlockPos, BindingInfo> bindingsByPosition = new HashMap<>();
    public HashMap<UUID, BindingInfo> bindingsById = new HashMap<>();
    public SavedData savedData;
    public Level level = null;

    private final HashMap<BlockPos, CompoundTag> placedItemTags = new HashMap<>();
    private final HashMap<String, BlockPos> sympathyPositions = new HashMap<>();

    public HashMap<String, ClaimedDemesneData> claimedDemesnes = new HashMap<>();

    public final int levelId;

    public TransientDiagramData(int levelId) {
        this.levelId = levelId;
    }

    public void putPlacedItemTag(BlockPos pos, CompoundTag tag) {
        placedItemTags.put(pos, tag);
        if (savedData != null) {
            savedData.setDirty();
        }
        if (!level.isClientSide()) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new HallowUpdateMessage(pos, tag, levelId));
        }
    }

    public void retryUpdateClient() {
        for (var tag : placedItemTags.entrySet()) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new HallowUpdateMessage(tag.getKey(), tag.getValue(), levelId));
        }
        for (var binding : bindingsById.values()) {
            updateClientBinding(binding);
        }
        for (var tag : sympathyPositions.entrySet()) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new SympathyUpdateMessage(tag.getKey(), tag.getValue(), levelId));
        }
    }

    public static void updateClientBinding(BindingInfo binding) {
        if (binding.mob == null) {
            return;
        }
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BindingUpdateMessage(binding.mob, BindingUpdateMessage.BindingUpdateType.BIND, true));
        if (!binding.contract.isEmpty()) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new BindingUpdateMessage(binding.mob, BindingUpdateMessage.BindingUpdateType.CONTRACT, true));
        }
        var pract = FamiliarManager.getPractitionerForFamiliar(binding.mob);
        if (!pract.isEmpty()) {
            var tag = new CompoundTag();
            tag.putString("practitioner", pract);
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new BindingUpdateMessage(binding.mob, BindingUpdateMessage.BindingUpdateType.FAMILIAR, tag, true));
        }
    }

    public CompoundTag getPlacedItemTag(BlockPos pos) {
        return placedItemTags.get(pos);
    }

    public void removePlacedItemTag(BlockPos pos) {
        if (placedItemTags.remove(pos) == null) {
            return;
        }
        if (savedData != null) {
            savedData.setDirty();
        }
        if (!level.isClientSide()) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new HallowUpdateMessage(pos, null, levelId));
        }
    }

    public void save(CompoundTag tag) {
        CompoundTag placedTags = new CompoundTag();
        CompoundTag placedTagPositions = new CompoundTag();
        for (BlockPos pos : placedItemTags.keySet()) {
            placedTags.put(pos.toString(), placedItemTags.get(pos));
            placedTagPositions.putInt(pos + "_x", pos.getX());
            placedTagPositions.putInt(pos + "_y", pos.getY());
            placedTagPositions.putInt(pos + "_z", pos.getZ());
        }
        tag.put("placedTags", placedTags);
        tag.put("placedTagPositions", placedTagPositions);

        CompoundTag bindings = new CompoundTag();
        for (UUID id : bindingsById.keySet()) {
            bindings.put(id.toString(), bindingsById.get(id).encode());
        }
        tag.put("bindings", bindings);

        CompoundTag sympathyPositionsTag = new CompoundTag();
        for (var sympathyPos : sympathyPositions.entrySet()) {
            var pos = sympathyPos.getValue();
            sympathyPositionsTag.putIntArray(sympathyPos.getKey(), new int[]{
                    pos.getX(), pos.getY(), pos.getZ()
            });
        }
        tag.put("sympathyPositions", sympathyPositionsTag);

        CompoundTag demesnesTag = new CompoundTag();
        for (var demesne : claimedDemesnes.entrySet()) {
            LOGGER.debug("SAVING DEMESNE: " + demesne.getKey());
            demesnesTag.put(demesne.getKey(), demesne.getValue().getTag());
        }
        tag.put("demesnes", demesnesTag);
        tag.put("spiritAffinities", SpiritAffinityTracker.save());
    }

    public Set<BlockPos> getAllPlacedItemPositions() {
        return placedItemTags.keySet();
    }

    public void putSympathyPosition(String key, BlockPos bindPos) {
        if (bindPos == null) sympathyPositions.remove(key);
        else sympathyPositions.put(key, bindPos);
        if (savedData != null) savedData.setDirty();
    }

    public BlockPos getSympathyPosition(String key) {
        return sympathyPositions.get(key);
    }

    public void registerClaimedDemesne(ClaimedDemesneData demesne) {
        claimedDemesnes.put(demesne.practitioner, demesne);
        if (savedData != null) savedData.setDirty();
    }
}
