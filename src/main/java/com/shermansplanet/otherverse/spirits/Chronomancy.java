package com.shermansplanet.otherverse.spirits;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Chronomancy {

    public static HashSet<Entity> frozenEntitiesForClient = new HashSet<>();
    public static HashSet<Entity> frozenEntitiesForServer = new HashSet<>();

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        tryUseClock(event);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        tryUseClock(event);
    }

    private static void tryUseClock(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        if (!stack.is(Items.CLOCK) || !ImplementManager.isImplement(stack)) return;
        var player = event.getEntity();
        for (var other : player.level().getEntities(player, player.getBoundingBox().inflate(16))) {
            other.getPersistentData().putInt("chronomancy_ticks", 20 * 10);
        }
        player.getInventory().removeItem(stack);
    }

    public record ChronomancyMessage(int entityId, boolean shouldTick) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeBoolean(shouldTick);
        }

        public static ChronomancyMessage decode(FriendlyByteBuf buffer) {
            var entityId = buffer.readInt();
            var shouldTick = buffer.readBoolean();
            return new ChronomancyMessage(entityId, shouldTick);
        }
    }

    public static boolean doesEntityTick(Entity entity) {
        var doesTick = doesEntityTickInternal(entity);
        var didTick = !frozenEntitiesForServer.contains(entity);
        if (doesTick && !didTick) {
            frozenEntitiesForServer.remove(entity);
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ChronomancyMessage(entity.getId(), true));
        } else if (didTick && !doesTick) {
            frozenEntitiesForServer.add(entity);
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ChronomancyMessage(entity.getId(), false));
        }
        return doesTick;
    }

    private static boolean doesEntityTickInternal(Entity entity) {
        if (entity instanceof Player) return true;
        var chronoTicks = entity.getPersistentData().getInt("chronomancy_ticks");
        if (chronoTicks > 0) {
            entity.getPersistentData().putInt("chronomancy_ticks", chronoTicks - 1);
            return false;
        }
        var blockPositions = new HashSet<BlockPos>();
        blockPositions.add(entity.blockPosition());
        var vel = entity.getDeltaMovement();
        var speedSqr = vel.lengthSqr();
        if (speedSqr > 1) {
            var speed = (float) Math.sqrt(speedSqr);
            var steps = Math.ceil(speed);
            for (var i = 1; i <= steps; i++) {
                var pos = entity.position().subtract(vel.scale(i / steps));
                blockPositions.add(BlockPos.containing(pos));
            }
        }
        for (var blockPos : blockPositions) {
            var level = entity.level();
            var data = DiagramManager.getOrCreateLevelData(level);
            var blockFocus = data.allBlockFoci.get(blockPos);
            if (blockFocus == null) continue;
            for (var influence : blockFocus.getDiagram().influences.entrySet()) {
                if (!blockPos.equals(influence.getValue())) continue;
                IFocus sourceFocus = data.allBlockFoci.get(influence.getKey());
                if (sourceFocus == null && level.getBlockEntity(influence.getKey()) instanceof ChalkCircle cc) {
                    sourceFocus = cc;
                }
                if (sourceFocus == null || sourceFocus.drainHallow(Spirits.TIME, 1, true, false) < 1) continue;
                entity.getPersistentData().putInt("chronomancy_ticks", 19);
                return false;
            }
        }
        return true;
    }

    public static boolean doesBlockTick(ServerLevel lvl, BlockPos pos, RandomSource r) {
        var allBlockFoci = DiagramManager.getOrCreateLevelData(lvl).allBlockFoci;
        var focus = allBlockFoci.get(pos);
        if (focus == null) return true;
        var targetPos = focus.getDiagram().influences.get(pos);
        if (targetPos == null) return true;
        IFocus targetFocus = allBlockFoci.get(targetPos);
        if (targetFocus == null && lvl.getBlockEntity(targetPos) instanceof ChalkCircle cc) {
            targetFocus = cc;
        }
        if (targetFocus == null) return true;
        if (targetFocus.fillHallow(Spirits.TIME, 9, true, false) < 9) return true;
        for (var i = 0; i < 9; i++) {
            var center = new Vec3(pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat());
            Vec3 diff = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5).subtract(0, 0.5, 0).subtract(center);
            lvl.sendParticles(new ItemParticleOption(OtherverseParticles.SPIRIT_PARTICLE_TYPE,
                            Spirits.spiritItems.get(Spirits.TIME).get().getDefaultInstance()),
                    center.x, center.y, center.z, 0, diff.x, diff.y, diff.z, 0.15D);
        }
        return false;
    }
}
