package com.shermansplanet.otherverse.implement;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramProcess;
import com.shermansplanet.otherverse.diagrams.IFocus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ImplementumProcess extends DiagramProcess {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerPlayer player;

    public ImplementumProcess(IFocus focus, int duration, ServerPlayer player) {
        super(focus, focus, duration);
        if (this.source instanceof ChalkCircle cc) {
            cc.setAnimTime(1);
        }
        this.player = player;
        var sourcePos = source.getPos();
        Vec3 center = new Vec3(sourcePos.getX() + 0.5,
                sourcePos.getY() + 2.5f,
                sourcePos.getZ() + 0.5);
        player.level().playSound(null, center.x, center.y, center.z,
                Otherverse.IMPLEMENT_RITUAL.get(),
                SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!player.isCreative() && !ImplementManager.getImplementData(player).isEmpty()) {
            LOGGER.debug("already has implement");
            abandon();
        }
        if (abandoned) {
            return;
        }

        doEffects();

        if (remainingDuration > 0) {
            return;
        }
        abandon();

        ImplementManager.makeImplement(player, source.getItem());
        var newstack = source.getItem().copy();
        LOGGER.debug("adding " + newstack + " to " + player.getInventory().getFreeSlot());
        player.getInventory().add(newstack);
        source.removeItem();

        var sourcePos = source.getPos();
        Vec3 center = new Vec3(sourcePos.getX() + 0.5,
                sourcePos.getY() + 2.5f,
                sourcePos.getZ() + 0.5);
        var r = player.serverLevel().random;

        for (var i = 0; i < 8; i++) {
            player.serverLevel().sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 1,
                    (r.nextFloat() - 0.5f) * 0.2f,
                    (r.nextFloat() - 0.5f) * 0.2f,
                    (r.nextFloat() - 0.5f) * 0.2f,
                    0.02D);
        }

        player.serverLevel().sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1,
                0, 0, 0, 0.02D);
    }

    private void doEffects() {
        int interval = remainingDuration < 20 ? 1
                : remainingDuration < 60 ? 2
                : remainingDuration < 120 ? 6
                : 10;
        if (remainingDuration % interval != 0) return;
        var sourcePos = source.getPos();
        Vec3 center = new Vec3(sourcePos.getX() + 0.5,
                sourcePos.getY() + 2.5f - remainingDuration * 2f / totalDuration,
                sourcePos.getZ() + 0.5);
        var r = player.serverLevel().random;
        player.serverLevel().sendParticles(ParticleTypes.GLOW, center.x, center.y, center.z, 1,
                (r.nextFloat() - 0.5f) * 0.2f,
                (r.nextFloat() - 0.5f) * 0.2f,
                (r.nextFloat() - 0.5f) * 0.2f,
                0.02D);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 1,
                (r.nextFloat() - 0.5f) * 0.2f,
                (r.nextFloat() - 0.5f) * 0.2f,
                (r.nextFloat() - 0.5f) * 0.2f,
                0.1D);
    }
}
