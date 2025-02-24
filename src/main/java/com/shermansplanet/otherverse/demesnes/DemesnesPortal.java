package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.common.util.ITeleporter;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.function.Function;

public class DemesnesPortal extends BlockEntity implements IForgeBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public int height, radius;
    public float[] color;
    public BlockPos destinationPosition;
    public int destinationLevel;

    private DyeColor dyeColor;
    private AABB bounds;
    private HashSet<Entity> entitiesToIgnore = new HashSet<>();

    public DemesnesPortal(BlockPos p_155229_, BlockState p_155230_) {
        super(Otherverse.DEMESNES_PORTAL_ENTITY.get(), p_155229_, p_155230_);
    }

    public static <T extends BlockEntity> void tick(Level tickLevel, BlockPos pos, BlockState state, T t) {
        var portal = (DemesnesPortal) t;
        pos = portal.getBlockPos();

        var dyeColor = state.getValue(DemesnesPortalBlock.color);
        if (portal.dyeColor != dyeColor) {
            portal.dyeColor = dyeColor;
            portal.color = dyeColor.getTextureDiffuseColors();
        }

        if (tickLevel == null) return;

        if (portal.bounds != null && tickLevel instanceof ServerLevel sl) {
            portal.checkForPlayer(sl);
        }

        var originalPos = pos;

        if (tickLevel.getGameTime() % 10 != Mth.abs(pos.getX()) % 10) return;
        var h = 0;
        while (h < 16 && tickLevel.getBlockState(pos).getCollisionShape(tickLevel, pos).isEmpty()) {
            h++;
            pos = pos.above();
        }
        pos = pos.below();
        portal.height = h;
        portal.radius = 0;
        for (var w = 1; w <= Math.min(3, (h - 1) / 2); w++) {
            if (anyBlockersInRadius(tickLevel, pos, w)) break;
            portal.radius = w;
        }
        portal.bounds = new AABB(originalPos, originalPos.offset(1, h, 1));
    }

    private void checkForPlayer(ServerLevel level) {
        var entitiesInBeam = level.getEntities(null, bounds);
        for (var entity : entitiesInBeam) {
            if (entitiesToIgnore.contains(entity)) continue;
            teleportEntity(level, entity);
        }
        var newEntitiesToIgnore = new HashSet<Entity>();
        for (var entity : entitiesToIgnore) {
            if (entitiesInBeam.contains(entity)) newEntitiesToIgnore.add(entity);
        }
        entitiesToIgnore = newEntitiesToIgnore;
    }

    private void teleportEntity(ServerLevel sl, Entity entity) {
        LOGGER.debug("TELEPORTING " + entity.getType());
        var sourceLevelId = DiagramManager.getDimensionHash(sl);
        var destLevel = DiagramManager.levelFromHash(sl, destinationLevel);
        var destPos = new Vec3(destinationPosition.getX() + 0.5f, destinationPosition.getY(), destinationPosition.getZ());
        if (sourceLevelId != destinationLevel) {
            LOGGER.debug("TO OTHER LEVEL");
            entity = entity.changeDimension(destLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    return repositionEntity.apply(false);
                }

                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    return new PortalInfo(destPos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }
            });
        } else {
            entity.teleportTo(destPos.x, destPos.y, destPos.z);
            if (entity.getType() == EntityType.PLAYER) {
                sl.playSound(null, entity, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1, 1);
            }
        }
        if (!destLevel.getBlockState(destinationPosition).is(OtherverseBlocks.DEMESNE_PORTAL.get())) {
            LOGGER.debug("CREATING OTHER PORTAL");
            destLevel.setBlockAndUpdate(destinationPosition,
                    OtherverseBlocks.DEMESNE_PORTAL.get().defaultBlockState().setValue(DemesnesPortalBlock.color, dyeColor));
        }
        if (destLevel.getBlockEntity(destinationPosition) instanceof DemesnesPortal otherPortal) {
            otherPortal.destinationPosition = getBlockPos();
            otherPortal.destinationLevel = DiagramManager.getDimensionHash(getLevel());
            otherPortal.entitiesToIgnore.add(entity);
            otherPortal.markUpdated();
        } else {
            LOGGER.debug("COULDN'T FIND OTHER PORTAL");
        }
    }

    private static boolean anyBlockersInRadius(Level level, BlockPos pos, int w) {
        var p = pos;
        for (var i = 0; i < w * 2; i++) {
            p = pos.offset(-w, 0, -w + i);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
            p = pos.offset(w, 0, -w + i);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
            p = pos.offset(-w + i, 0, -w);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
            p = pos.offset(-w + i, 0, w);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
        }
        return false;
    }

    public void markUpdated(BlockState state) {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), state, 2);
    }

    public void markUpdated() {
        markUpdated(this.getBlockState());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (destinationPosition == null) return;
        tag.putIntArray("destination", new int[]{destinationPosition.getX(), destinationPosition.getY(), destinationPosition.getZ()});
        tag.putInt("destinationLevel", destinationLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        var pos = tag.getIntArray("destination");
        if (pos.length == 0) return;
        destinationPosition = new BlockPos(pos[0], pos[1], pos[2]);
        destinationLevel = tag.getInt("destinationLevel");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
