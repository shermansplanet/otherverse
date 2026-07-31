package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DiagramProcess {

    private static final Logger LOGGER = LogUtils.getLogger();

    protected int totalDuration;
    protected int remainingDuration;
    protected IFocus source;
    protected IFocus sink;
    protected boolean abandoned;
    protected final ItemStack sourceStack, sinkStack;

    public DiagramProcess(IFocus sink, IFocus source, int duration) {
        this.source = source;
        this.sink = sink;
        this.sourceStack = source.getItem();
        this.sinkStack = sink.getItem();
        this.totalDuration = duration;
        this.remainingDuration = duration;
        source.setProcess(this);
        source.getDiagram().processes.add(this);
        if (this.source instanceof ChalkCircle cc) {
            cc.setAnimTime();
        }
    }

    public void tick() {
        remainingDuration--;
        if (source.getProcess() != this) {
            LOGGER.debug("abandoning - wrong process");
            abandon();
            return;
        }
        if (!(source.getItem().getItem() == sourceStack.getItem())) {
            LOGGER.debug("abandoning - source mismatch {} {}", source.getItem().getItem(), sourceStack.getItem());
            abandon();
            return;
        }
        if (!(sink.getItem().getItem() == sinkStack.getItem())) {
            LOGGER.debug("abandoning - sink mismatch {} {}", sink.getItem().getItem(), sinkStack.getItem());
            abandon();
        }
    }

    public void abandon() {
        var diagram = source.getDiagram();
        if (diagram != null) {
            diagram.processes.remove(this);
        }
        source.setProcess(null);
        if (source instanceof ChalkCircle cc) {
            cc.clearAnimTime();
        }
        abandoned = true;
    }

    protected void makeSpiritParticles(SpiritType spiritType) {
        if ((source.isBlock() || (totalDuration - remainingDuration > 20) || (remainingDuration < 20))
                && remainingDuration % 4 == 0 && sink.getFocusLevel() instanceof ServerLevel sl) {
            ItemStack sourceItem = source.getItem();
            BlockPos sourcePos = source.getPos();
            var uplerp = source.isBlock() ? 1 : Mth.clamp((totalDuration - remainingDuration) / 20f, 0, 1);
            Vec3 center = new Vec3(sourcePos.getX() + 0.5, sourcePos.getY() + 0.5f * uplerp, sourcePos.getZ() + 0.5);

            if (!sourceItem.is(Items.AIR) && !sourceItem.is(OtherverseItems.IDOL.get())) {
                var particleItem = sourceItem;
                if(sourceItem.hasTag() && sourceItem.getTag().contains("hallow")){
                    var spiritName = sourceItem.getTag().getCompound("hallow").getString("spirit_type");
                    particleItem = Spirits.spiritItems.get(Spirits.spiritsByLabel.get(spiritName)).get().getDefaultInstance();
                }
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleItem),
                        center.x, center.y, center.z, 1, 0, 0, 0, 0.05D);
            }

            BlockPos sinkPos = sink.getPos();
            Vec3 diff = new Vec3(sinkPos.getX() + 0.5, sinkPos.getY() + 0.5, sinkPos.getZ() + 0.5).subtract(0, 0.5, 0).subtract(center);
            sl.sendParticles(new ItemParticleOption(OtherverseParticles.SPIRIT_PARTICLE_TYPE,
                            Spirits.spiritItems.get(spiritType).get().getDefaultInstance()),
                    center.x, center.y, center.z, 0, diff.x, diff.y, diff.z, 0.15D);
        }
    }
}
