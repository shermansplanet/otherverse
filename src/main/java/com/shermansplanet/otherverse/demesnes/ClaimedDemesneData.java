package com.shermansplanet.otherverse.demesnes;

import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ClaimedDemesneData {
    public final BlockPos minPos, maxPos;
    public final int range, levelId;
    public final String practitioner;
    private final HashMap<DemesnesManager.DemesnePerk, Integer> perkValues = new HashMap<>();
    private final HashMap<String, boolean[]> writsByPlayer = new HashMap<>();
    private final int writOffset = DemesnesManager.DemesnePerk.SANCTION_FIGHT.ordinal();
    public final List<Block> favoredMaterials = new ArrayList<>();
    public final HashSet<String> favoredSpirits = new HashSet<>();
    public final Set<BlockPos> spawnDestinations = new HashSet<>();
    public BlockPos minChronoPos, maxChronoPos;
    private int skyColor;
    private boolean hasSkyColor;
    public long fixedTime = -1;

    public final SpiritType spiritType;

    public ClaimedDemesneData(BlockPos minPos, BlockPos maxPos, String practitioner, int range, int levelId, SpiritType spiritType) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.spiritType = spiritType;
        this.practitioner = practitioner;
        this.range = range;
        this.levelId = levelId;
        for (var perk : DemesnesManager.DemesnePerk.values()) {
            perkValues.put(perk, 0);
        }
    }

    public ClaimedDemesneData(CompoundTag tag) {
        minPos = new BlockPos(tag.getInt("min_x"),
                tag.getInt("min_y"),
                tag.getInt("min_z"));
        maxPos = new BlockPos(tag.getInt("max_x"),
                tag.getInt("max_y"),
                tag.getInt("max_z"));

        fixedTime = tag.contains("fixedTime") ? tag.getLong("fixedTime") : -1;

        if (tag.contains("chrono_min_x")) {
            minChronoPos = new BlockPos(tag.getInt("chrono_min_x"),
                    tag.getInt("chrono_min_y"),
                    tag.getInt("chrono_min_z"));
            maxChronoPos = new BlockPos(tag.getInt("chrono_max_x"),
                    tag.getInt("chrono_max_y"),
                    tag.getInt("chrono_max_z"));
        }

        if (tag.contains("skyColor")) {
            skyColor = tag.getInt("skyColor");
            hasSkyColor = true;
        }

        practitioner = tag.getString("practitioner");
        range = tag.getInt("range");
        levelId = tag.getInt("levelId");
        var vals = DemesnesManager.DemesnePerk.values();
        int[] intArray = tag.getIntArray("perks");
        if (intArray.length == 0) {
            for (var perk : DemesnesManager.DemesnePerk.values()) {
                perkValues.put(perk, 0);
            }
        } else {
            for (int i = 0; i < intArray.length; i++) {
                perkValues.put(vals[i], intArray[i]);
            }
        }

        var writsTag = tag.getCompound("writs");
        for (var playerName : writsTag.getAllKeys()) {
            var b = writsTag.getByte(playerName);
            var writVals = new boolean[DemesnesManager.DemesnePerk.values().length];
            writVals[writOffset] = (b & 1) == 1;
            writVals[writOffset + 1] = (b >> 1 & 1) == 1;
            writVals[writOffset + 2] = (b >> 2 & 1) == 1;
            writVals[writOffset + 3] = (b >> 3 & 1) == 1;
            writsByPlayer.put(playerName, writVals);
        }

        var namespaces = tag.getCompound("materialNamespaces");
        var paths = tag.getCompound("materialPaths");
        for (var key : namespaces.getAllKeys()) {
            var namespace = namespaces.getString(key);
            var path = paths.getString(key);
            favoredMaterials.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path)));
        }

        favoredSpirits.addAll(tag.getCompound("favoredSpirits").getAllKeys());

        var spawnDestinationsTag = tag.getCompound("spawnDestinations");
        for (var k : spawnDestinationsTag.getAllKeys()) {
            var dest = spawnDestinationsTag.getIntArray(k);
            spawnDestinations.add(new BlockPos(dest[0], dest[1], dest[2]));
        }

        spiritType = tag.contains("spiritType") ? Spirits.spiritsById.get(tag.getInt("spiritType")) : null;
    }

    public CompoundTag getTag() {
        var tag = new CompoundTag();
        tag.putInt("min_x", minPos.getX());
        tag.putInt("min_y", minPos.getY());
        tag.putInt("min_z", minPos.getZ());
        tag.putInt("max_x", maxPos.getX());
        tag.putInt("max_y", maxPos.getY());
        tag.putInt("max_z", maxPos.getZ());

        tag.putLong("fixedTime", fixedTime);

        if (minChronoPos != null) {
            tag.putInt("chrono_min_x", minChronoPos.getX());
            tag.putInt("chrono_min_y", minChronoPos.getY());
            tag.putInt("chrono_min_z", minChronoPos.getZ());
            tag.putInt("chrono_max_x", maxChronoPos.getX());
            tag.putInt("chrono_max_y", maxChronoPos.getY());
            tag.putInt("chrono_max_z", maxChronoPos.getZ());
        }

        tag.putString("practitioner", practitioner);
        tag.putInt("range", range);
        tag.putInt("levelId", levelId);

        if (hasSkyColor) {
            tag.putInt("skyColor", skyColor);
        }

        var vals = DemesnesManager.DemesnePerk.values();
        var ints = new int[vals.length];
        for (var i = 0; i < vals.length; i++) {
            ints[i] = perkValues.get(vals[i]);
        }
        tag.putIntArray("perks", ints);

        var writs = new CompoundTag();
        for (var writ : writsByPlayer.entrySet()) {
            var writValues = writ.getValue();
            var i = (writValues[writOffset] ? 1 : 0) | (writValues[1 + writOffset] ? 1 : 0) << 1
                    | (writValues[2 + writOffset] ? 1 : 0) << 2
                    | (writValues[3 + writOffset] ? 1 : 0) << 3;
            writs.putByte(writ.getKey(), (byte) i);
        }
        tag.put("writs", writs);

        var matTagNamespace = new CompoundTag();
        var matTagPath = new CompoundTag();
        for (int i = 0; i < favoredMaterials.size(); i++) {
            var mat = favoredMaterials.get(i);
            var key = ForgeRegistries.BLOCKS.getKey(mat);
            matTagNamespace.putString("material_" + i, key.getNamespace());
            matTagPath.putString("material_" + i, key.getPath());
        }
        tag.put("materialNamespaces", matTagNamespace);
        tag.put("materialPaths", matTagPath);

        var spiritsTag = new CompoundTag();
        for (var spirit : favoredSpirits) {
            spiritsTag.putBoolean(spirit, true);
        }
        tag.put("favoredSpirits", spiritsTag);

        var spawnDestinationsTag = new CompoundTag();
        var i = 0;
        for (var dest : spawnDestinations) {
            spawnDestinationsTag.putIntArray(String.valueOf(i), new int[]{
                    dest.getX(), dest.getY(), dest.getZ()
            });
        }
        tag.put("spawnDestinations", spawnDestinationsTag);

        if (spiritType != null) tag.putInt("spiritType", spiritType.id());

        return tag;
    }

    public void addToChunks(HashMap<ServerLevel, HashMap<ChunkPos, ClaimedDemesneData>> claimedDemesnes, ServerLevel sl) {
        if (!claimedDemesnes.containsKey(sl)) claimedDemesnes.put(sl, new HashMap<>());
        var chunksForLevel = claimedDemesnes.get(sl);
        var minChunk = sl.getChunkAt(minPos).getPos();
        var maxChunk = sl.getChunkAt(maxPos).getPos();
        for (var x = minChunk.x; x <= maxChunk.x; x++) {
            for (var z = minChunk.z; z <= maxChunk.z; z++) {
                var chunkPos = new ChunkPos(x, z);
                chunksForLevel.put(chunkPos, this);
                if (minChronoPos == null) continue;
                if (sl.getChunk(chunkPos.x, chunkPos.z) instanceof ISectionSetter ss) {
                    ss.setSections(minChronoPos.getY(), maxChronoPos.getY());
                }
                ;
            }
        }
    }

    public int getPerkLevel(DemesnesManager.DemesnePerk perk) {
        return perkValues.get(perk);
    }

    public void setPerkValue(DemesnesManager.DemesnePerk perk, int i) {
        perkValues.put(perk, i);
    }

    public boolean toggleSanction(DemesnesManager.DemesnePerk perk, ServerPlayer recipient) {
        var name = recipient.getGameProfile().getName();
        if (!writsByPlayer.containsKey(name))
            writsByPlayer.put(name, new boolean[DemesnesManager.DemesnePerk.values().length]);
        var writs = writsByPlayer.get(name);
        var oldVal = writs[perk.ordinal()];
        writs[perk.ordinal()] = !oldVal;
        return !oldVal;
    }

    public boolean hasSanction(DemesnesManager.DemesnePerk perk, ServerPlayer recipient) {
        if (recipient.getAbilities().instabuild || Objects.equals(recipient.getGameProfile().getName(), practitioner))
            return true;
        if (getPerkLevel(perk) == 0) return true;
        var name = recipient.getGameProfile().getName();
        var writs = writsByPlayer.get(name);
        if (writs == null) return false;
        return writs[perk.ordinal()];
    }

    public void addFavoredMaterial(BlockState blockstate) {
        favoredMaterials.add(blockstate.getBlock());
    }

    public void addFavoredSpirit(String spiritType) {
        favoredSpirits.add(spiritType);
    }

    public void addSpawnDestination(BlockPos pos) {
        spawnDestinations.add(pos);
    }

    public void removeSpawnDestination(BlockPos pos) {
        spawnDestinations.remove(pos);
    }

    public void refreshChrono(ServerLevel sl) {
        var minChunk = sl.getChunkAt(minPos).getPos();
        var maxChunk = sl.getChunkAt(maxPos).getPos();
        for (var x = minChunk.x; x <= maxChunk.x; x++) {
            for (var z = minChunk.z; z <= maxChunk.z; z++) {
                var chunkPos = new ChunkPos(x, z);
                if (sl.getChunk(chunkPos.x, chunkPos.z) instanceof ISectionSetter ss) {
                    if (minChronoPos == null) {
                        ss.clearSections();
                    } else {
                        ss.setSections(minChronoPos.getY(), maxChronoPos.getY());
                    }
                }
            }
        }
    }

    public void adjustSkyColor(DyeColor dyeColor) {
        if (dyeColor == null) {
            hasSkyColor = false;
            return;
        }

        if (!hasSkyColor) {
            skyColor = 0x7aa8fb;
            hasSkyColor = true;
        }

        var r1 = FastColor.ARGB32.red(skyColor);
        var g1 = FastColor.ARGB32.green(skyColor);
        var b1 = FastColor.ARGB32.blue(skyColor);

        var destColor = dyeColor.getTextColor();
        var r2 = FastColor.ARGB32.red(destColor);
        var g2 = FastColor.ARGB32.green(destColor);
        var b2 = FastColor.ARGB32.blue(destColor);

        var lerp = 0.25f;

        var r = Math.round(r1 * (1 - lerp) + r2 * lerp);
        var g = Math.round(g1 * (1 - lerp) + g2 * lerp);
        var b = Math.round(b1 * (1 - lerp) + b2 * lerp);

        skyColor = FastColor.ARGB32.color(1, r, g, b);
    }

    public final static long TIME_COEFF = 1000;

    public BlockPos getWeather() {
        return new BlockPos(
                hasSkyColor ? 1 : 0,
                skyColor,
                (int) (fixedTime < 0 ? -1 : fixedTime / TIME_COEFF)
        );
    }
}
