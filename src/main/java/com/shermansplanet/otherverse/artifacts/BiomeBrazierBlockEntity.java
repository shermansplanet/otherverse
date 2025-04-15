package com.shermansplanet.otherverse.artifacts;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class BiomeBrazierBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int range = 0;
    private boolean lit = false;

    public BiomeBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(Otherverse.BIOME_BRAZIER_ENTITY.get(), pos, state);
    }

    private static BiomeResolver makeResolver(MutableInt biomeCounter, ChunkAccess chunkAccess, BoundingBox boundingBox, int width, Holder<Biome> biomeHolder) {
        return (quartX, quartY, quartZ, sampler) -> {
            int i = QuartPos.toBlock(quartX);
            int j = QuartPos.toBlock(quartY);
            int k = QuartPos.toBlock(quartZ);
            Holder<Biome> holder = chunkAccess.getNoiseBiome(quartX, quartY, quartZ);
            if (boundingBox.isInside(new Vec3i(i, j, k))) {
                biomeCounter.increment();
                return biomeHolder;
            } else {
                return holder;
            }
        };
    }

    public void convertBiome() {
        AABB aabb = new AABB(this.getBlockPos().offset(-32, -32, -32), this.getBlockPos().offset(32, 32, 32));
        int width = 7;
        var registry = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        var biome = registry.getOrCreateHolderOrThrow(Biomes.CRIMSON_FOREST);

        List<ChunkAccess> list = new ArrayList<>();
        BoundingBox biomeConversionBox = new BoundingBox(this.getBlockPos().getX() - width, this.getBlockPos().getY() - width, this.getBlockPos().getZ() - width, this.getBlockPos().getX() + width, this.getBlockPos().getY() + width, this.getBlockPos().getZ() + width);
        if (level instanceof ServerLevel serverLevel) {
            for (int k = SectionPos.blockToSectionCoord(biomeConversionBox.minZ()); k <= SectionPos.blockToSectionCoord(biomeConversionBox.maxZ()); ++k) {
                for (int l = SectionPos.blockToSectionCoord(biomeConversionBox.minX()); l <= SectionPos.blockToSectionCoord(biomeConversionBox.maxX()); ++l) {
                    ChunkAccess chunkaccess = serverLevel.getChunk(l, k, ChunkStatus.FULL, false);
                    if (chunkaccess != null) {
                        list.add(chunkaccess);
                    }
                }
            }
            MutableInt mutableint = new MutableInt(0);
            for (ChunkAccess chunkaccess1 : list) {
                chunkaccess1.fillBiomesFromNoise(makeResolver(mutableint, chunkaccess1, biomeConversionBox, width, biome), serverLevel.getChunkSource().randomState().sampler());
                chunkaccess1.setUnsaved(true);
                for (var player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket((LevelChunk) chunkaccess1, level.getLightEngine(),
                            new BitSet(), new BitSet(), false));
                }
            }
        }
    }

    public void fuel(int amount) {
        range += amount;
        if (!lit) {
            lit = true;
            level.setBlockAndUpdate(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get().defaultBlockState().setValue(BlockStateProperties.LIT, true));
            level.scheduleTick(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get(), 4);
        }
        level.playSound(null, getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1, 1);
        setChanged();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if (level.getGameTime() % 5 != 0) return;
        if (!(level instanceof ServerLevel sl)) return;
        var r = level.getRandom();
        if (state.getValue(BlockStateProperties.LIT)) {
            sl.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + r.nextFloat() * 0.6f + 0.2f,
                    pos.getY() + r.nextFloat() * 0.5f + 0.5f,
                    pos.getZ() + r.nextFloat() * 0.6f + 0.2f,
                    0, 0, 0.2f, 0, 0.15f);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("range", range);
        tag.putBoolean("lit", lit);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        range = tag.getInt("range");
        lit = tag.getBoolean("lit");
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
}
