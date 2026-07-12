package com.shermansplanet.otherverse.artifacts;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.core.*;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BiomeBrazierBlockEntity extends BlockEntity {

    private static final int MAX_ACTIVATIONS = 16;
    private static final Logger LOGGER = LogUtils.getLogger();
    public CompoundTag biomeTag = null;
    public HashMap<SpiritType, Pair<Integer, Integer>> spiritCounts = null;
    public List<SpiritType> spiritCode = null;
    private int activations = 0;
    public MutableComponent[] labels = null;
    private String originalBiomeString = null;
    private Vec3 direction = Vec3.ZERO;
    private static boolean wasDay = true;

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

        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biomeTag.getString("location")));
        var targetBiome = registry.getHolderOrThrow(key);

        activations++;
        var radius = Math.min(activations * 3, MAX_ACTIVATIONS);
        BoundingBox biomeConversionBox = new BoundingBox(this.getBlockPos().getX() - radius, this.getBlockPos().getY() - radius, this.getBlockPos().getZ() - radius,
                this.getBlockPos().getX() + radius, this.getBlockPos().getY() + radius, this.getBlockPos().getZ() + radius);

        var blockReplacements = new HashMap<Block, Block>();
        if (originalBiomeString == null) {
            originalBiomeString = registry.getKey(level.getBiome(getBlockPos()).get()).toString();
        }
        var originalBiomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(originalBiomeString));
        var originalBiome = registry.getHolderOrThrow(originalBiomeKey);
        addIfNotNull(blockReplacements, getBlockStone(originalBiome), getBlockStone(targetBiome));
        addIfNotNull(blockReplacements, getBlockDirt(originalBiome), getBlockDirt(targetBiome));
        addIfNotNull(blockReplacements, getBlockSurface(originalBiome), getBlockSurface(targetBiome));
        var biomeKeys = MobBindingInfluenceUtils.getBiomeKeys(targetBiome, level);
        var biomeMod = biomeKeys.getNamespace();
        var biomeName = biomeKeys.getPath();
        var isPlaces = biomeMod.equals("places");

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
                    if (isPlaces && (bs.is(BlockTags.PLANKS) || bs.is(BlockTags.LOGS))) {
                        if (biomeName.startsWith("room_0")) {
                            replacement = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "yellow_wallpaper_arrow_destructable"));
                        } else if (biomeName.equals("manilla_room")) {
                            replacement = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "manila_walpaper_destructable"));
                        } else if (biomeName.equals("the_end_room")) {
                            replacement = Blocks.BOOKSHELF;
                        }
                    }
                    if (replacement == null) continue;
                    level.setBlock(pos, replacement.defaultBlockState(), 2);
                }
            }
        }

        var makingFeatures = activations > MAX_ACTIVATIONS;

        var feature_range = 64;

        if (makingFeatures) {
            biomeConversionBox = biomeConversionBox.inflatedBy(feature_range);
        }

        List<ChunkAccess> list = new ArrayList<>();
        var minK = SectionPos.blockToSectionCoord(biomeConversionBox.minZ());
        var minL = SectionPos.blockToSectionCoord(biomeConversionBox.minX());
        var maxK = SectionPos.blockToSectionCoord(biomeConversionBox.maxZ());
        var maxL = makingFeatures ? (minL + (maxK - minK)) : SectionPos.blockToSectionCoord(biomeConversionBox.maxX());
        var hasNullChunks = false;
        for (int k = minK; k <= maxK; ++k) {
            for (int l = minL; l <= maxL; ++l) {
                ChunkAccess chunkaccess = serverLevel.getChunk(l, k, ChunkStatus.FULL, false);
                if (chunkaccess == null) {
                    hasNullChunks = true;
                    break;
                } else {
                    list.add(chunkaccess);
                }
            }
        }

        if (hasNullChunks) {
            return;
        }

        if (makingFeatures) {
            for (var featureSet : targetBiome.get().getGenerationSettings().features()) {
                for (var feature : featureSet) {
                    if (r.nextBoolean()) continue;
                    var x = getBlockPos().getX() + serverLevel.getRandom().nextInt(-radius, radius);
                    var z = getBlockPos().getZ() + serverLevel.getRandom().nextInt(-radius, radius);
                    var pos = new BlockPos.MutableBlockPos(x, worldPosition.getY(), z);
                    for (var i = 0; i < radius; i++) {
                        if (serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos).isEmpty()) break;
                        pos.setY(pos.getY() + 1);
                    }
                    for (var i = 0; i < radius; i++) {
                        if (!serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos).isEmpty()) break;
                        pos.setY(pos.getY() - 1);
                    }
                    try {
                        var thisFeature = feature.get();
                        var ctx = new PlacementContext(serverLevel, serverLevel.getChunkSource().getGenerator(), Optional.of(thisFeature));

                        Stream<BlockPos> stream = Stream.of(pos);
                        for (PlacementModifier placementmodifier : thisFeature.placement()) {
                            stream = stream.flatMap((p_226376_) -> placementmodifier.getPositions(ctx, serverLevel.getRandom(), p_226376_));
                        }

                        ConfiguredFeature<?, ?> configuredfeature = thisFeature.feature().value();
                        MutableBoolean mutableboolean = new MutableBoolean();
                        stream.forEach((placePos) -> {
                            if (placePos.distSqr(worldPosition) <= radiusSqr) {
                                if (configuredfeature.place(ctx.getLevel(), ctx.generator(), serverLevel.getRandom(), placePos)) {
                                    mutableboolean.setTrue();
                                }
                            }
                        });

                    } catch (Exception e) {
                        LOGGER.warn("Error when generating " + feature.get());
                    }
                }
            }
        }

        MutableInt mutableint = new MutableInt(0);
        for (ChunkAccess chunkaccess1 : list) {
            if (!makingFeatures) {
                chunkaccess1.fillBiomesFromNoise(makeResolver(mutableint, chunkaccess1, biomeConversionBox, radius * radius, targetBiome, getBlockPos()), serverLevel.getChunkSource().randomState().sampler());
            }
            chunkaccess1.setUnsaved(true);
            for (var player : serverLevel.getServer().getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundLevelChunkWithLightPacket((LevelChunk) chunkaccess1, level.getLightEngine(),
                        new BitSet(), new BitSet()));
            }
        }

        level.playSound(null, getBlockPos(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1, 1);
    }

    private void addIfNotNull(HashMap<Block, Block> blockReplacements, Block b1, Block b2) {
        if (b1 != null && b2 != null) blockReplacements.put(b1, b2);
    }

    private Block getBlockStone(Holder<Biome> biome) {
        var biomeKeys = MobBindingInfluenceUtils.getBiomeKeys(biome, level);
        var biomeMod = biomeKeys.getNamespace();
        var biomeName = biomeKeys.getPath();

        if (biomeMod.equals("biomesoplenty")) {
            if (biomeName.equals("visceral_heap"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("biomesoplenty", "flesh"));
            if (biomeName.equals("erupting_inferno"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("biomesoplenty", "brimstone"));
            if (biomeName.equals("withered_abyss"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("biomesoplenty", "blackstone"));
        } else if (biomeMod.equals("alexscaves")) {
            if (biomeName.equals("magnetic_caves"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "galena"));
            if (biomeName.equals("primordial_caves"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "limestone"));
            if (biomeName.equals("toxic_caves"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "radrock"));
            if (biomeName.equals("abyssal_chasm"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "abyssmarine"));
            if (biomeName.equals("forlorn_hollows"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "coprolith"));
            if (biomeName.equals("candy_cavity"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "cake_layer"));
        } else if (biomeMod.equals("macabre")) {
            if (biomeName.equals("mountain_maw"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "petrified_rock"));
            return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "weak_stone"));
        } else if (biomeMod.equals("places")) {
            if (biomeName.equals("structure_bridge")) {
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "structure_block"));
            }
            return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "rot_fountain"));
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return Blocks.WATER;
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) return Blocks.SANDSTONE;

        if (biome.is(BiomeTags.IS_OVERWORLD)) return Blocks.STONE;
        if (biome.is(BiomeTags.IS_NETHER)) return Blocks.NETHERRACK;
        if (biome.is(BiomeTags.IS_END)) return Blocks.END_STONE;
        return null;
    }

    private Block getBlockDirt(Holder<Biome> biome) {
        if (biome.is(Biomes.SOUL_SAND_VALLEY)) return Blocks.SOUL_SOIL;

        var biomeKeys = MobBindingInfluenceUtils.getBiomeKeys(biome, level);
        var biomeMod = biomeKeys.getNamespace();
        var biomeName = biomeKeys.getPath();

        if (biomeMod.equals("alexscaves")) {
            if (biomeName.equals("candy_cavity"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "block_of_chocolate"));
            if (biomeName.equals("forlorn_hollows"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "guanostone"));
            if (biomeName.equals("primordial_caves")) return Blocks.DIRT;
        } else if (biomeMod.equals("macabre")) {
            if (biomeName.equals("deathless_valley") || biomeName.equals("lifeless_pits"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "gore_undersand"));
            if (biomeName.equals("mountain_maw"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "dr_dirt"));
            return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "damp_dirt"));
        } else if (biomeMod.equals("places")) {
            if (biomeName.startsWith("room_0") || biomeName.equals("manilla_room") || biomeName.equals("the_end_room")) {
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "woven_brown_carpet"));
            } else if (biomeName.startsWith("alpha")) {
                if (biomeName.equals("alpha_beach"))
                    return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "polymer_sand"));
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "plastistone"));
            } else if (biomeName.equals("aquarium_tunnels")) {
                return Blocks.WATER;
            } else if (biomeName.equals("red_road") || biomeName.equals("structure_bridge")) {
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "weathered_cement"));
            }
        }

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

        var biomeKeys = MobBindingInfluenceUtils.getBiomeKeys(biome, level);
        var biomeMod = biomeKeys.getNamespace();
        var biomeName = biomeKeys.getPath();

        if (biomeMod.equals("alexscaves")) {
            if (biomeName.equals("candy_cavity"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "block_of_frosted_chocolate"));
            if (biomeName.equals("forlorn_hollows")) return Blocks.PACKED_MUD;
            if (biomeName.equals("primordial_caves")) return Blocks.GRASS_BLOCK;
        } else if (biomeMod.equals("macabre")) {
            if (biomeName.equals("mortem_swamp"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "mortem_dirt"));
            if (biomeName.equals("mortem_marsh"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "mortem_dirt"));
            if (biomeName.equals("gloom_forest"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "living_dirt"));
            if (biomeName.equals("cracked_cliffs"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "living_dirt"));
            if (biomeName.equals("rotting_field"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "living_dirt"));
            if (biomeName.equals("teething_forest"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "gums"));
            if (biomeName.equals("crawling_field"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "crawling_dirt"));
            if (biomeName.equals("valley_of_eyes"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "veins"));
            if (biomeName.equals("deathless_valley"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "gore_sand"));
            if (biomeName.equals("decaying_meadow"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "necrosis_flesh"));
            if (biomeName.equals("lifeless_pits"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "lifeless_sand"));
            if (biomeName.equals("mountain_maw"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("macabre", "tumor_dirt"));
        } else if (biomeMod.equals("places")) {
            if (biomeName.startsWith("room_0") || biomeName.equals("manilla_room") || biomeName.equals("the_end_room"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "woven_brown_carpet"));
            if (biomeName.equals("alpha_plains"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "astroturf_grass"));
            if (biomeName.equals("alpha_beach"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "polymer_sand"));
            if (biomeName.equals("aquarium_tunnels"))
                return Blocks.WATER;
            if (biomeName.equals("red_road") || biomeName.equals("structure_bridge"))
                return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("places", "asphalt_destructable"));
        }

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
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        originalBiomeString = registry.getKey(level.getBiome(getBlockPos()).get()).toString();
        markUpdated();
    }

    private void resetSpiritCounts() {
        spiritCounts = new HashMap<>();
        spiritCode = new ArrayList<>();
        if (biomeTag == null) {
            for (var st : Spirits.allSpiritTypes) {
                spiritCounts.put(st, new Pair<>(0, 1));
            }
            return;
        }
        var spiritTag = biomeTag.getCompound("spirits");
        for (var st : spiritTag.getAllKeys()) {
            spiritCounts.put(Spirits.spiritsByLabel.get(st), new Pair<>(0, spiritTag.getInt(st)));
        }
        markUpdated();
    }

    public void setLabels() {
        if (biomeTag == null) {
            if (spiritCode != null && !spiritCode.isEmpty()) {
                labels = new MutableComponent[spiritCode.size()];
                for (var i = 0; i < spiritCode.size(); i++) {
                    labels[i] = Component.literal(spiritCode.get(i).label());
                }
                return;
            }
            labels = null;
            return;
        }
        labels = new MutableComponent[spiritCounts.size() + 1];
        var location = biomeTag.getString("location");
        labels[0] = Component.translatable("biome." + location.replace(":", "."))
                .withStyle(Style.EMPTY.withBold(true));
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
        var isLit = state.getValue(BlockStateProperties.LIT);
        var isScrying = state.getValue(BiomeBrazierBlock.SCRY);
        if (isLit && isScrying) {
            DiagramManager.getOrCreateLevelData(sl).addChunkloader(pos);
            boolean isNowDay = sl.getServer().overworld().isDay();
            if (isNowDay == wasDay) return;
            if (isNowDay && t instanceof BiomeBrazierBlockEntity brazier) {
                brazier.refreshChunkloading(sl, null);
            }
            wasDay = isNowDay;
            return;
        }
        if (isLit || isScrying) {
            sl.sendParticles(new ItemParticleOption(OtherverseParticles.HALLOW_PARTICLE_TYPE,
                            isLit ? isScrying ? Items.LIME_DYE.getDefaultInstance()
                                    : OtherverseItems.REALM_WRACKED_COAL.get().getDefaultInstance()
                                    : OtherverseItems.SCRYING_POWDER.get().getDefaultInstance()),
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
        if (isScrying && t instanceof BiomeBrazierBlockEntity brazier && brazier.direction != Vec3.ZERO) {
            var dir = brazier.direction.scale(3);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + r.nextFloat() * 0.8f + 0.1f,
                    pos.getY() + r.nextFloat() * 0.5f + 0.5f,
                    pos.getZ() + r.nextFloat() * 0.8f + 0.1f,
                    0, dir.x, dir.y, dir.z, 0.15f);
        }
    }

    private void refreshChunkloading(ServerLevel sl, ServerPlayer player) {
        var shouldLoad = tryDrain(sl);
        var chunkPos = Otherverse.chunkAt(getBlockPos());
        sl.setChunkForced(chunkPos.x, chunkPos.z, shouldLoad);
        if (shouldLoad) {
            DiagramManager.getOrCreateLevelData(sl).addChunkloader(getBlockPos());
        } else {
            level.setBlockAndUpdate(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get().defaultBlockState());
            level.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
            markUpdated();
            if (player != null)
                player.displayClientMessage(Component.literal("No shrine nearby with enough spirits."), true);
            DiagramManager.getOrCreateLevelData(sl).removeChunkloader(getBlockPos());
        }
    }

    private boolean tryDrain(ServerLevel sl) {
        var levelShrines = ShrineHelper.shrinesByChunk.get(sl);
        if (levelShrines == null) return false;
        var shrines = levelShrines.get(Otherverse.chunkAt(getBlockPos()));
        if (shrines == null || shrines.isEmpty()) return false;
        var name = sl.dimensionTypeId().location().getPath();
        var spiritType = switch (name) {
            case "overworld" -> Spirits.OVERWORLD;
            case "the_nether" -> Spirits.NETHER;
            case "the_end" -> Spirits.END;
            default -> Spirits.OVERWORLD;
        };
        for (var shrine : shrines) {
            if (shrine.st != spiritType) continue;
            if (!shrine.isInRange(null, getBlockPos())) continue;
            return shrine.tryDrain(9);
        }
        return false;
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
        if (spiritCode != null) {
            var spiritCodeTag = new IntArrayTag(spiritCode.stream().map(SpiritType::id).toList());
            tag.put("spiritCode", spiritCodeTag);
        }
        tag.putInt("activations", activations);
        tag.putFloat("dirX", (float) direction.x);
        tag.putFloat("dirY", (float) direction.y);
        tag.putFloat("dirZ", (float) direction.z);
        if (originalBiomeString != null) tag.putString("originalBiomeString", originalBiomeString);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        biomeTag = null;
        spiritCounts = null;
        spiritCode = null;
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
        if (tag.contains("spiritCode")) {
            spiritCode = new ArrayList<>(Arrays.stream(tag.getIntArray("spiritCode")).mapToObj(Spirits.spiritsById::get).toList());
        }
        direction = new Vec3(
                tag.getFloat("dirX"), tag.getFloat("dirY"), tag.getFloat("dirZ"));
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

    public void markUpdated() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
    }

    public void fuelForScrying() {
        level.setBlockAndUpdate(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get().defaultBlockState().setValue(BiomeBrazierBlock.SCRY, true));
        level.playSound(null, getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1, 1);
        biomeTag = null;
        resetSpiritCounts();
        activations = 0;
        setLabels();
        markUpdated();
    }

    public void tryScry(SpiritType spiritType, ServerLevel sl) {
        if (!spiritCode.isEmpty() && spiritType == spiritCode.get(spiritCode.size() - 1)) return;
        if (spiritCode.size() >= BiomeCodeAssigner.SPIRIT_CODE_COUNT) resetSpiritCounts();
        spiritCode.add(spiritType);

        if (spiritCode.size() == BiomeCodeAssigner.SPIRIT_CODE_COUNT) {
            var pos = getBlockPos();
            var players = sl.getEntitiesOfClass(ServerPlayer.class, AABB.ofSize(pos.getCenter(), 16, 16, 16));
            var biomeName = BiomeCodeAssigner.getBiomeFor(spiritCode);
            if (biomeName == null) {
                for (var player : players)
                    player.displayClientMessage(Component.literal("Spirit sequence does not correspond to any known biome"), false);
            } else {
                var biomeComponent = Component.translatable("biome." + biomeName.toString().replace(':', '.'));
                var registry = sl.registryAccess().registry(Registries.BIOME).get();
                var biome = registry.getHolderOrThrow(registry.getResourceKey(registry.get(biomeName)).get());
                for (var player : players) {
                    player.displayClientMessage(Component.literal("Attempting to locate ").append(biomeComponent), false);
                }
                new Thread(() -> {
                    var result = sl.findClosestBiome3d(Predicate.isEqual(biome), getBlockPos(), 6400, 32, 64);
                    if (result == null || result.getFirst() == null) {
                        for (var player : players)
                            player.displayClientMessage(Component.literal("Could not find ").append(biomeComponent), false);
                    } else {
                        for (var i = 0; i < 8; i++) {
                            var r = sl.getRandom();
                            var dir = result.getFirst().getCenter().subtract(pos.getCenter()).normalize();
                            sl.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1, 1);
                            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    pos.getX() + r.nextFloat() * 3f - 1f,
                                    pos.getY() + r.nextFloat() * 3f - 1f,
                                    pos.getZ() + r.nextFloat() * 3f - 1f,
                                    0, 0, 0.25, 0, 0.15f);
                            direction = dir;
                        }
                        for (var player : players)
                            player.displayClientMessage(Component.literal("Now scrying towards ").append(biomeComponent), false);
                    }
                }).start();
            }
        }
        fuelEffects();
        setLabels();
        markUpdated();
    }

    public void fuelEffects() {
        if (!(level instanceof ServerLevel sl)) return;
        var pos = getBlockPos();
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1);
        var r = level.getRandom();
        for (var i = 0; i < 8; i++) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + r.nextFloat() * 3f - 1f,
                    pos.getY() + r.nextFloat() * 3f - 1f,
                    pos.getZ() + r.nextFloat() * 3f - 1f,
                    0, 0, 0.2f, 0, 0.15f);
        }
    }

    public void fuelForChunkloading(Player player) {
        level.setBlockAndUpdate(getBlockPos(), OtherverseBlocks.BIOME_BRAZIER.get().defaultBlockState().setValue(BiomeBrazierBlock.SCRY, true).setValue(BlockStateProperties.LIT, true));
        level.playSound(null, getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1, 1);
        biomeTag = null;
        resetSpiritCounts();
        activations = 0;
        setLabels();
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) refreshChunkloading(sl, sp);
        markUpdated();
    }
}
