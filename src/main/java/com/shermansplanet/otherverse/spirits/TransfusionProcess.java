package com.shermansplanet.otherverse.spirits;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.DiagramProcess;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.implement.ImplementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class TransfusionProcess extends DiagramProcess {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final SpiritTransfusions.SpiritTransfusionData transfusion;
    public TransfusionProcess(IFocus sink, IFocus source, int duration, SpiritTransfusions.SpiritTransfusionData t) {
        super(sink, source, duration);
        transfusion = t;
    }

    public void tick() {
        super.tick();
        if(abandoned){
            return;
        }

        var tag = source.getItem().getTag();
        if(tag == null){
            LOGGER.debug("abandoning - null tag");
            abandon();
            return;
        }
        CompoundTag hallowTag = tag.getCompound("hallow");
        SpiritType spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
        int spiritCount = hallowTag.getInt("spirit_count");

        if(spiritCount < transfusion.price() || spiritType != transfusion.spiritType()){
            LOGGER.debug("abandoning - can't fulfill");
            abandon();
            return;
        }

        makeSpiritParticles(spiritType);

        if (remainingDuration > 0) {
            return;
        }

        abandon();

        if (sink.getFocusLevel() instanceof ServerLevel sl) {
            BlockPos bp = sink.getPos();
            for (int i = 0; i < 6; i++) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, sink.getItem()),
                        bp.getX() + 0.5, bp.getY() + 0.1, bp.getZ() + 0.5, 1, 0, 0, 0, 0.1D);
            }
        }

        var level = sink.getFocusLevel();
        if (sink.isBlock()) {
            level.setBlockAndUpdate(sink.getPos(), transfusion.blockOutput().defaultBlockState());
        } else {
            ChalkCircle targetCircle = (ChalkCircle) sink;
            targetCircle.item = transfusion.output().copy();
            targetCircle.markUpdated();
        }

        hallowTag.putInt("spirit_count", spiritCount - transfusion.price());
        if (source.isBlock()) {
            DiagramManager.getOrCreateLevelData(level).putPlacedItemTag(source.getPos(), hallowTag);
        }

        if(level instanceof ServerLevel sl) {
            var player = source.getDiagram().getOwner(sl);
            if(player != null){
                var cap = player.getCapability(ImplementManager.PRACTICE_HANDLER).resolve();
                cap.ifPresent(practice -> {
                    SpiritAffinityTracker.increaseAffinity(spiritType, player);
                    SpiritAffinityTracker.decreaseAffinity(SpiritTransfer.getOppositeSpiritType(spiritType), player);
                });
            }
        }

        if (sink.getFocusLevel() instanceof ServerLevel sl) {
            LOGGER.debug("ACTIVATING DIAGRAM: TRANSFUSION COMPLETE");
            DiagramManager.markDiagramActive(sl, sink.getDiagram());
        }
    }
}
