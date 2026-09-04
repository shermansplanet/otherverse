package com.shermansplanet.otherverse.spirits;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramProcess;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
        if (abandoned) {
            return;
        }

        var tag = source.getItem().getTag();
        if (tag == null) {
            LOGGER.debug("abandoning - null tag");
            abandon();
            return;
        }

        var isTablet = tag.contains("linked_position_x");

        SpiritType spiritType = Spirits.spiritsByLabel.get(isTablet ? tag.getString("spirit_type") : tag.getCompound("hallow").getString("spirit_type"));

        if (spiritType != transfusion.spiritType()) {
            LOGGER.debug("abandoning - can't fulfill");
            abandon();
            return;
        }

        makeSpiritParticles(spiritType);

        if (remainingDuration > 0) {
            return;
        }

        abandon();

        if (source.drainHallow(spiritType, transfusion.price(), true, false) < transfusion.price()) return;

        if (sink.getFocusLevel() instanceof ServerLevel sl) {
            BlockPos bp = sink.getPos();
            for (int i = 0; i < 6; i++) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, sink.getItem()),
                        bp.getX() + 0.5, bp.getY() + 0.1, bp.getZ() + 0.5, 1, 0, 0, 0, 0.1D);
            }
        }

        var level = sink.getFocusLevel();
        if (sink.isBlock()) {
            var pos = sink.getPos();
            if (!SpiritTransfusions.tryReplaceBlock(level, pos, transfusion.blockOutput())) {
                level.destroyBlock(pos, false);
                ItemEntity itementity = new ItemEntity(level, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, transfusion.output());
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        } else {
            ChalkCircle targetCircle = (ChalkCircle) sink;
            targetCircle.item = transfusion.output().copy();
            targetCircle.markUpdated();
        }

        if (level instanceof ServerLevel sl) {
            var player = source.getDiagram().getOwner(sl);
            if (player != null) {
                SpiritAffinityTracker.increaseAffinity(spiritType, player);
                SpiritAffinityTracker.decreaseAffinity(SpiritTransfer.getOppositeSpiritType(spiritType), player);
            }
        }
    }
}
