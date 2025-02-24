package com.shermansplanet.otherverse.spawning;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public class SpawnAltarBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public EntityType<? extends LivingEntity> spawnType;
    public LivingEntity displayEntity;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (spawnType != null) {
            tag.putString("spawn_altar_type", ForgeRegistries.ENTITY_TYPES.getKey(spawnType).toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        LOGGER.debug("LOADING ALTAR");
        super.load(tag);
        if (tag.contains("spawn_altar_type")) {
            LOGGER.debug(tag.getString("spawn_altar_type"));
            spawnType = (EntityType<? extends LivingEntity>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(tag.getString("spawn_altar_type")));
            if (displayEntity != null) {
                displayEntity.discard();
            }
            if (level != null) {
                displayEntity = spawnType.create(level);
            }
        }
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

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    public SpawnAltarBlockEntity(BlockPos pos, BlockState state) {
        super(Otherverse.SPAWN_ALTAR_ENTITY.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        var altar = (SpawnAltarBlockEntity) t;
        if (altar.displayEntity == null && level != null && altar.spawnType != null) {
            altar.displayEntity = altar.spawnType.create(level);
        }
    }
}
