package com.shermansplanet.otherverse.spirits;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingInfo;
import com.shermansplanet.otherverse.demesnes.ClaimedDemesneData;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class SavedPracticeData extends SavedData {

    public Level level;

    private static final Logger LOGGER = LogUtils.getLogger();

    public SavedPracticeData(Level l) {
        level = l;
    }

    public void load(CompoundTag tag) {
        LOGGER.debug("LOADING LEVEL DATA " + (level.isClientSide() ? "client" : "server"));
        if (tag.contains("placedTags")) {
            CompoundTag positions = tag.getCompound("placedTagPositions");
            CompoundTag placedTags = tag.getCompound("placedTags");
            TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(level);
            for (String key : placedTags.getAllKeys()) {
                LOGGER.debug(key);
                CompoundTag itemTag = placedTags.getCompound(key);
                BlockPos pos = new BlockPos(
                        positions.getInt(key + "_x"),
                        positions.getInt(key + "_y"),
                        positions.getInt(key + "_z"));
                diagramData.putPlacedItemTag(pos, itemTag);
            }
        }
        if (tag.contains("bindings")) {
            CompoundTag bindingTags = tag.getCompound("bindings");
            if (level instanceof ServerLevel sl) {
                for (String key : bindingTags.getAllKeys()) {
                    BindingInfo binding = BindingInfo.decode(bindingTags.getCompound(key), sl);
                    binding.register();
                    LOGGER.debug("REGISTERED BINDING " + binding.bindingId);
                }
            }
        }
        if (tag.contains("sympathyPositions")) {
            TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(level);
            CompoundTag positions = tag.getCompound("sympathyPositions");
            for (String key : positions.getAllKeys()) {
                var ints = positions.getIntArray(key);
                diagramData.putSympathyPosition(key, new BlockPos(ints[0], ints[1], ints[2]));
            }
        }
        if (tag.contains("demesnes")) {
            if (level instanceof ServerLevel sl) {
                var overworld = sl.getServer().overworld();
                TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(overworld);
                CompoundTag demesnes = tag.getCompound("demesnes");
                for (String key : demesnes.getAllKeys()) {
                    LOGGER.debug("LOADING DEMESNE: " + key);
                    var demesneData = new ClaimedDemesneData(demesnes.getCompound(key));
                    diagramData.registerClaimedDemesne(demesneData);
                    DemesnesManager.tryLoadDemesne(demesneData, overworld);
                }
            }
        }
        if(tag.contains("spiritAffinities")){
            SpiritAffinityTracker.load(tag.getCompound("spiritAffinities"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        LOGGER.debug("SAVING PRACTICE DATA");
        TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(level);
        diagramData.save(tag);
        return tag;
    }
}
