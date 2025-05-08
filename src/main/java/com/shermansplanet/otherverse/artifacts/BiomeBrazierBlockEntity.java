package com.shermansplanet.otherverse.artifacts;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.core.*;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class BiomeBrazierBlockEntity extends BlockEntity {

    private static final int MAX_ACTIVATIONS = 16;
    private static final Logger LOGGER = LogUtils.getLogger();
    private CompoundTag biomeTag = null;
    public HashMap<SpiritType, Pair<Integer, Integer>> spiritCounts = null;
    private int activations = 0;
    public MutableComponent[] labels = null;
    private String originalBiomeString = null;

    public BiomeBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(Otherverse.BIOME_BRAZIER_ENTITY.get(), pos, state);
    }

    private static BiomeResolver makeResolver(MutableInt biomeCounter, ChunkAccess chunkAccess, BoundingBox boundingBox, int widthSqr, Holder<Biome> biomeHolder, Vec3i pos) {
        return (quartX, quartY, quartZ, sampler) -> {
            int i = QuartPos.toBlock(quartX);
            int j = QuartPos.toBlock(quartY);
            int k = QuartPos.toBlock(quartZ);
            Holder<Biome> holder = chunkAccess.getNoiseBiome(quartX, quartY, quartZ);
            var blockPos = new Vec3i(i, j, k);
            if (boundingBox.isInside(blockPos) && pos.distSqr(blockPos) < widthSqr) {
                biomeCounter.increment();
                return biomeHolder;
            } else {
                return holder;
            }
        };
    }


    public void activate(ServerLevel serverLevel) {
        resetSpiritCounts();

        var registry = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        var key = ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biomeTag.getString("location")));
        var targetBiome = registry.getOrCreateHolderOrThrow(key);

        activations++;
        var radius = Math.min(activations, MAX_ACTIVATIONS);
        BoundingBox biomeConversionBox = new BoundingBox(this.getBlockPos().getX() - radius, this.getBlockPos().getY() - radius, this.getBlockPos().getZ() - radius,
                this.getBlockPos().getX() + radius, this.getBlockPos().getY() + radius, this.getBlockPos().getZ() + radius);

        var blockReplacements = new HashMap<Block, Block>();
        if(originalBiomeString == null){
            originalBiomeString = registry.getKey(level.getBiome(getBlockPos()).get()).toString();
        }
        var originalBiomeKey = ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(originalBiomeString));
        var originalBiome = registry.getOrCreateHolderOrThrow(originalBiomeKey);
        addIfNotNull(blockReplacements, getBlockStone(originalBiome), getBlockStone(targetBiome));
        addIfNotNull(blockReplacements, getBlockDirt(originalBiome), getBlockDirt(targetBiome));
        addIfNotNull(blockReplacements, getBlockSurface(originalBiome), getBlockSurface(targetBiome));

        var radiusSqr = radius * radius;
        var r = level.getRandom();
        for (var x = biomeConversionBox.minX(); x <= biomeConversionBox.maxX(); x++) {
            for (var z = biomeConversionBox.minZ(); z <= biomeConversionBox.maxZ(); z++) {
                var h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (var y = biomeConversionBox.minY(); y <= h; y++) {
                    var pos = new BlockPos(x, y, z);
                    if (pos.distSqr(getBlockPos()) > radiusSqr || r.nextInt(3) > 0) continue;
                    var bs = level.getBlockState(pos);
                    var replacement = blockReplacements.get(bs.getBlock());
                    if (replacement == null) continue;
                    level.setBlock(pos, replacement.defaultBlockState(), 2);
                }
            }
        }

        if (activations > MAX_ACTIVATIONS) {
            for (var featureSet : targetBiome.get().getGenerationSettings().features()) {
                for (var feature : featureSet) {
                    if(r.nextBoolean()) continue;
                    var x = getBlockPos().getX() + serverLevel.getRandom().nextInt(-radius, radius);
                    var z = getBlockPos().getZ() + serverLevel.getRandom().nextInt(-radius, radius);
                    var y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    feature.get().placeWithBiomeCheck(serverLevel, serverLevel.getChunkSource().getGenerator(),
                            serverLevel.getRandom(), new BlockPos(x, y - 1, z));
                }
            }
            return;
        }

        List<ChunkAccess> list = new ArrayList<>();
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
            chunkaccess1.fillBiomesFromNoise(makeResolver(mutableint, chunkaccess1, biomeConversionBox, radius * radius, targetBiome, getBlockPos()), serverLevel.getChunkSource().randomState().sampler());
            chunkaccess1.setUnsaved(true);
            for (var player : serverLevel.getServer().getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundLevelChunkWithLightPacket((LevelChunk) chunkaccess1, level.getLightEngine(),
                        new BitSet(), new BitSet(), false));
            }
        }
    }

    private void addIfNotNull(HashMap<Block, Block> blockReplacements, Block b1, Block b2) {
        if (b1 != null && b2 != null) blockReplacements.put(b1, b2);
    }

    private Block getBlockStone(Holder<Biome> biome) {
        var biomeName = MobBindingInfluenceUtils.getBiomeName(biome, level);
        if (biomeName.equals("visceral_heap")) return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomesoplenty","flesh"));
        if (biomeName.equals("erupting_inferno")) return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomesoplenty","brimstone"));
        if (biomeName.equals("withered_abyss")) return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomesoplenty","blackstone"));

        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return Blocks.WATER;
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) return Blocks.SANDSTONE;

        if (biome.is(BiomeTags.IS_OVERWORLD)) return Blocks.STONE;
        if (biome.is(BiomeTags.IS_NETHER)) return Blocks.NETHERRACK;
        if (biome.is(BiomeTags.IS_END)) return Blocks.END_STONE;
        return null;
    }

    private Block getBlockDirt(Holder<Biome> biome) {
        if (biome.is(Biomes.SOUL_SAND_VALLEY)) return Blocks.SOUL_SOIL;

        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return Blocks.WATER;
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) return Blocks.SAND;

        if (biome.is(BiomeTags.IS_OVERWORLD)) return Blocks.DIRT;
        return getBlockStone(biome);
    }

    private Block getBlockSurface(Holder<Biome> biome) {
        if (biome.is(Biomes.SOUL_SAND_VALLEY)) return Blocks.SOUL_SAND;
        if (biome.is(Biomes.WARPED_FOREST)) return Blocks.WARPED_NYLIUM;
        if (biome.is(Biomes.CRIMSON_FOREST)) return Blocks.CRIMSON_NYLIUM;
        if (biome.is(Biomes.DEEP_DARK)) return Blocks.SCULK;
        if (biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.BAMBOO_JUNGLE)) return Blocks.PODZOL;
        if (biome.is(Biomes.MUSHROOM_FIELDS)) return Blocks.MYCELIUM;

        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return Blocks.WATER;
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) return Blocks.SAND;

        if (biome.is(BiomeTags.IS_OVERWORLD)) return Blocks.GRASS_BLOCK;
        return getBlockDirt(biome);
    }

    public void fuel(CompoundTag newTag) {
        if (biomeTag == null) {
            level.setBlockAndUpdate(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get().defaultBlockState().setValue(BlockStateProperties.LIT, true));
            level.scheduleTick(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get(), 4);
        }
        level.playSound(null, getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1, 1);
        biomeTag = newTag;
        resetSpiritCounts();
        activations = 0;
        setLabels();
        var registry = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        originalBiomeString = registry.getKey(level.getBiome(getBlockPos()).get()).toString();
        setChanged();
    }

    private void resetSpiritCounts() {
        spiritCounts = new HashMap<>();
        var spiritTag = biomeTag.getCompound("spirits");
        for (var st : spiritTag.getAllKeys()) {
            spiritCounts.put(Spirits.spiritsByLabel.get(st), new Pair<>(0, spiritTag.getInt(st)));
        }
    }

    public void setLabels() {
        if (biomeTag == null) {
            labels = null;
            return;
        }
        labels = new MutableComponent[spiritCounts.size() + 1];
        var location = biomeTag.getString("location");
        labels[0] = Component.translatable("biome." + location.replace(":", "."))
                .withStyle(Style.EMPTY.withUnderlined(true));
        var i = 1;
        for (var spiritCount : spiritCounts.entrySet()) {
            labels[i] = Component.literal(
                    spiritCount.getValue().getFirst() + "/" + spiritCount.getValue().getSecond() + " " + spiritCount.getKey().label());
            i++;
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if (level.getGameTime() % 5 != 0) return;
        if (!(level instanceof ServerLevel sl)) return;
        var r = level.getRandom();
        if (state.getValue(BlockStateProperties.LIT)) {
            sl.sendParticles(new ItemParticleOption(OtherverseParticles.HALLOW_PARTICLE_TYPE,
                            OtherverseItems.REALM_WRACKED_COAL.get().getDefaultInstance()),
                    pos.getX() + r.nextFloat() * 0.8f + 0.1f,
                    pos.getY() + r.nextFloat() * 0.5f + 0.5f,
                    pos.getZ() + r.nextFloat() * 0.8f + 0.1f,
                    0, 0, 0.4f, 0, 0.15f);

            sl.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + r.nextFloat() * 0.8f + 0.1f,
                    pos.getY() + r.nextFloat() * 0.5f + 0.5f,
                    pos.getZ() + r.nextFloat() * 0.8f + 0.1f,
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
        if (biomeTag != null) tag.put("biomeTag", biomeTag);
        if (spiritCounts != null) {
            var spiritCountTag = new CompoundTag();
            var spiritNeedsTag = new CompoundTag();
            for (var sc : spiritCounts.entrySet()) {
                spiritCountTag.putInt(sc.getKey().label(), sc.getValue().getFirst());
                spiritNeedsTag.putInt(sc.getKey().label(), sc.getValue().getSecond());
            }
            tag.put("spiritCounts", spiritCountTag);
            tag.put("spiritNeeds", spiritNeedsTag);
        }
        tag.putInt("activations", activations);
        if (originalBiomeString != null) tag.putString("originalBiomeString", originalBiomeString);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        biomeTag = null;
        spiritCounts = null;
        if (tag.contains("biomeTag")) biomeTag = tag.getCompound("biomeTag");
        if (tag.contains("spiritCounts")) {
            spiritCounts = new HashMap<>();
            var spiritCountTag = tag.getCompound("spiritCounts");
            var spiritNeedsTag = tag.getCompound("spiritNeeds");
            for (var k : spiritCountTag.getAllKeys()) {
                spiritCounts.put(Spirits.spiritsByLabel.get(k),
                        new Pair<>(spiritCountTag.getInt(k), spiritNeedsTag.getInt(k)));
            }
        }
        activations = tag.getInt("activations");
        if (tag.contains("originalBiomeString")) originalBiomeString = tag.getString("originalBiomeString");
        setLabels();
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
