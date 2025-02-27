package com.shermansplanet.otherverse.spirits;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingOrFleshbinding;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.DiagramProcess;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.HashMap;

public class SpiritTransfer extends DiagramProcess {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SpiritTransfer(IFocus sink, IFocus source, int duration) {
        super(sink, source, duration);
    }

    private static HashMap<String, String> oppositeSpirits = new HashMap<>();

    static {
        declareOpposites(Spirits.EARTH, Spirits.AIR);
        declareOpposites(Spirits.FIRE, Spirits.COLD);
        declareOpposites(Spirits.PHLOGISTON, Spirits.WATER);
        declareOpposites(Spirits.LIGHT, Spirits.DARK);
        declareOpposites(Spirits.PROTECTION, Spirits.WAR);
        declareOpposites(Spirits.FOOD, Spirits.DEATH);
        declareOpposites(Spirits.FLESH, Spirits.TECH);
        declareOpposites(Spirits.NATURE, Spirits.FORTUNE);
        declareOpposites(Spirits.FATE, Spirits.TIME);
        declareOpposites(Spirits.COLOR_WHITE, Spirits.COLOR_BLACK);
        declareOpposites(Spirits.COLOR_LIGHT_GRAY, Spirits.COLOR_GRAY);
        declareOpposites(Spirits.COLOR_LIME, Spirits.COLOR_PURPLE);
        declareOpposites(Spirits.COLOR_BROWN, Spirits.COLOR_PINK);
        declareOpposites(Spirits.COLOR_ORANGE, Spirits.COLOR_LIGHT_BLUE);
        declareOpposites(Spirits.COLOR_RED, Spirits.COLOR_CYAN);
        declareOpposites(Spirits.COLOR_GREEN, Spirits.COLOR_MAGENTA);
        declareOpposites(Spirits.COLOR_BLUE, Spirits.COLOR_YELLOW);

        oppositeSpirits.put(Spirits.OVERWORLD.label(), Spirits.NETHER.label());
        oppositeSpirits.put(Spirits.NETHER.label(), Spirits.END.label());
        oppositeSpirits.put(Spirits.END.label(), Spirits.OVERWORLD.label());
    }

    private static void declareOpposites(SpiritType a, SpiritType b) {
        oppositeSpirits.put(a.label(), b.label());
        oppositeSpirits.put(b.label(), a.label());
    }

    public static SpiritType getOppositeSpiritType(SpiritType spiritType) {
        if (spiritType == null) return null;
        return Spirits.spiritsByLabel.get(oppositeSpirits.getOrDefault(spiritType.label(), ""));
    }

    public void tick() {
        super.tick();
        if (abandoned) {
            return;
        }
        var tag = sink.getItem().getTag();
        if (tag == null) {
            LOGGER.debug("abandoning - null tag");
            abandon();
            return;
        }

        SpiritType spiritType = Spirits.spiritsByLabel.get(tag.getCompound("hallow").getString("spirit_type"));
        makeSpiritParticles(spiritType);

        if (remainingDuration > 0) {
            return;
        }

        abandon();

        ItemStack sourceItem = source.getItem();
        boolean sourceIsHallow = sourceItem.hasTag() && sourceItem.getTag().contains("hallow");
        LOGGER.debug("DRAINING SOURCE ITEM: " + sourceItem);

        var remainingCapacity = sink.getHallowCapacity(spiritType);

        if (remainingCapacity <= 0) {
            return;
        }

        if (!HallowHelper.canFill(sink, source, spiritType)) {
            return;
        }

        int transferAmount = 0;
        float coeff = SpiritAffinityTracker.getSpiritYieldCoeff(sink.getDiagram().getOwnerName(), spiritType);
        var isDemesne = false;
        if (sourceItem.is(OtherverseItems.DEMESNE_BEACON.get())) {
            var demesne = DemesnesManager.getData((ServerLevel) source.getFocusLevel(), source.getPos());
            if (demesne != null && demesne.favoredSpirits.contains(spiritType.label())) {
                transferAmount = Math.min((int) (3 * coeff), remainingCapacity);
                isDemesne = true;
            }
        }

        if (!(source.getFocusLevel() instanceof ServerLevel sl)) return;

        if (!isDemesne) {
            if (sourceIsHallow) {
                transferAmount = source.drainHallow(spiritType, remainingCapacity, false);
            } else {
                if (sourceItem.is(OtherverseItems.IDOL.get())) {
                    var binding = BindingOrFleshbinding.getFromPosition(sl, source.getPos());
                    if (binding == null) return;
                    transferAmount = Math.min(remainingCapacity, binding.getHealth() - 1);
                    binding.changeHealth(-transferAmount, sl);
                } else {
                    transferAmount = Math.round(SpiritLabeler.getSpiritsFor(sourceItem.getItem()).get(spiritType) * coeff);
                    transferAmount = Math.min(remainingCapacity, transferAmount);
                    source.removeItem();
                    if (!sourceItem.is(Items.AIR)) {
                        BlockPos bp = source.getPos();
                        for (int i = 0; i < 6; i++) {
                            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, sourceItem),
                                    bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 1, 0, 0, 0, 0.1D);
                        }
                    }
                }
            }
        }

        var remainingAfterFill = sink.fillHallow(spiritType, transferAmount, false);
        if (remainingAfterFill > 0) {
            LOGGER.error("HALLOW OVERFILL");
        }

        var player = source.getDiagram().getOwner(sl);
        if (player != null) {
            SpiritAffinityTracker.increaseAffinity(spiritType, player);
            SpiritAffinityTracker.decreaseAffinity(getOppositeSpiritType(spiritType), player);
        }
    }
}
