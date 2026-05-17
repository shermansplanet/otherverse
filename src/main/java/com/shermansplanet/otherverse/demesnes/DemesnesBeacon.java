package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Structures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class DemesnesBeacon extends BlockEntity implements MenuProvider, IItemHandler, IForgeBlockEntity {

    public int range;
    public SpiritType spiritType;
    public BlockPos minBlock, maxBlock;
    public DemesnesRenderer.ClientDemesnesData clientData = null;
    public Component hoverName;
    public String inDemesneOf = "";
    public AABB claimedDemesneBounds;
    private ClaimedDemesneData demesneData;

    LazyOptional<IItemHandler> inventoryHandlerLazyOptional = LazyOptional.of(() -> this);

    private static final Logger LOGGER = LogUtils.getLogger();

    public DemesnesBeacon(BlockPos pos, BlockState state) {
        super(Otherverse.DEMESNES_BEACON_ENTITY.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel sl) {
            var biome = level.getBiome(getBlockPos());
            var levelName = level.dimensionTypeId().location().getPath();

            var depthCutoff = biome.is(BiomeTags.IS_OCEAN) ? 30 : 60;
            var blockBelow = level.getBlockState(getBlockPos().below());

            var structures = sl.structureManager().getAllStructuresAt(getBlockPos()).keySet();
            var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            if (structures.contains(registry.getHolderOrThrow(BuiltinStructures.BASTION_REMNANT).get()) || structures.contains(registry.getHolderOrThrow(BuiltinStructures.PILLAGER_OUTPOST).get()))
                spiritType = Spirits.WAR;
            else if (structures.contains(registry.getHolderOrThrow(BuiltinStructures.STRONGHOLD).get()) || structures.contains(registry.getHolderOrThrow(BuiltinStructures.FORTRESS).get()) || isInDragonArena(biome))
                spiritType = Spirits.FATE;
            else if (isSurroundedBy(sl, getBlockPos(), Blocks.FARMLAND)) spiritType = Spirits.FOOD;
            else if (blockBelow.is(Blocks.GOLD_BLOCK) || blockBelow.is(Blocks.EMERALD_BLOCK))
                spiritType = Spirits.FORTUNE;
            else if (isSurroundedBy(sl, getBlockPos(), Blocks.GLOWSTONE)) spiritType = Spirits.LIGHT;
            else if (levelName.equals("overworld") && getBlockPos().getY() < depthCutoff) spiritType = Spirits.EARTH;
            else if (levelName.equals("overworld") && getBlockPos().getY() > 190) spiritType = Spirits.AIR;
            else spiritType = MobBindingInfluenceUtils.getSpiritTypes(biome, sl).get(0);

            LOGGER.debug("GETTING DATA FROM DEMESNE BEACON SET LEVEL");
            demesneData = DemesnesManager.getData(sl, getBlockPos());
            if (demesneData != null) {
                inDemesneOf = demesneData.practitioner;
                claimedDemesneBounds = new AABB(demesneData.minPos, demesneData.maxPos);
                if (trySetChronoPos(demesneData, sl, getBlockPos(), 1) || trySetChronoPos(demesneData, sl, getBlockPos(), -1)) {
                    OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new DemesnesClientboundMessage(
                            DemesnesClientboundMessage.EventType.CHRONO_SET, demesneData.minChronoPos, demesneData.maxChronoPos, demesneData.levelId, demesneData.practitioner));
                }
                markUpdated();
            }
        }
    }

    private boolean isInDragonArena(Holder<Biome> biome) {
        var x = getBlockPos().getX();
        var z = getBlockPos().getZ();
        return biome.is(Biomes.THE_END) && (x * x + z * z) < (100 * 100);
    }

    private boolean isSurroundedBy(ServerLevel sl, BlockPos blockPos, Block block) {
        for (var dx = -1; dx <= 1; dx++) {
            for (var dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!sl.getBlockState(blockPos.offset(dx, 0, dz)).is(block)) return false;
            }
        }
        return true;
    }

    private boolean trySetChronoPos(ClaimedDemesneData demesneData, ServerLevel sl, BlockPos originalPos, int dy) {
        if (demesneData.getPerkLevel(DemesnesManager.DemesnePerk.TIME) == 0) return false;
        var topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, originalPos.getX(), originalPos.getZ());
        var bottomY = sl.getMinBuildHeight();
        var startY = originalPos.getY() + dy;
        for (var y = startY; dy > 0 ? (y <= topY) : (y >= bottomY); y += dy) {
            var pos = new BlockPos(originalPos.getX(), y, originalPos.getZ());
            if (sl.isEmptyBlock(pos)) continue;
            if (y == startY) return false;
            if (sl.getBlockState(pos).is(OtherverseBlocks.DEMESNE_BEACON.get())) {
                demesneData.minChronoPos = (dy > 0 ? originalPos : pos).above();
                demesneData.maxChronoPos = (dy > 0 ? pos : originalPos).below();
                demesneData.refreshChrono(sl);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (clientData == null) recalculatePositions();
        return clientData.isEmpty ? super.getRenderBoundingBox() : clientData.bounds;
    }

    public void recalculatePositions() {
        var centerPos = this.getBlockPos();
        range = DemesnesManager.getDemesnesRange(getLevel(), centerPos);
        if (range < 0) {
            clientData = new DemesnesRenderer.ClientDemesnesData();
            return;
        }
        var minx = centerPos.getX();
        var minz = centerPos.getZ();
        var maxx = centerPos.getX();
        var maxz = centerPos.getZ();
        var centerChunk = getLevel().getChunkAt(centerPos);
        for (var x = -range; x <= range; x++) {
            for (var z = -range; z <= range; z++) {
                var chunkPos = new ChunkPos(centerChunk.getPos().x + x, centerChunk.getPos().z + z);
                minx = Math.min(minx, chunkPos.getMinBlockX());
                minz = Math.min(minz, chunkPos.getMinBlockZ());
                maxx = Math.max(maxx, chunkPos.getMaxBlockX());
                maxz = Math.max(maxz, chunkPos.getMaxBlockZ());
            }
        }
        minBlock = new BlockPos(minx, centerChunk.getMinBuildHeight(), minz);
        maxBlock = new BlockPos(maxx, centerChunk.getMaxBuildHeight(), maxz);
        var bounds = new AABB(minBlock, maxBlock);
        if (getLevel().isClientSide()) {
            clientData = new DemesnesRenderer.ClientDemesnesData(minBlock, bounds, DiagramManager.getDimensionHash(getLevel()));
            clientData.timeRendered = DemesnesClaimRitual.INTRO_TIME_TICKS * 10;
        }
    }

    @Override
    public Component getDisplayName() {
        return hoverName == null ? Component.literal("Demesnes Beacon") : hoverName;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        recalculatePositions();
        var data = DemesnesManager.getData(player);
        if (data != null) {
            return new DemesnesMenu(i, data, getBlockPos());
        }

        return new DemesnesClaimMenu(i, inventory, ContainerLevelAccess.create(level, getBlockPos()));
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
        tag.putString("hoverName", hoverName == null ? "" : hoverName.getString());
        tag.putString("inDemesneOf", inDemesneOf);
        if (claimedDemesneBounds != null) {
            tag.putIntArray("claimedDemesneBounds", new int[]{
                    Math.round((float) claimedDemesneBounds.minX),
                    Math.round((float) claimedDemesneBounds.minY),
                    Math.round((float) claimedDemesneBounds.minZ),
                    Math.round((float) claimedDemesneBounds.maxX),
                    Math.round((float) claimedDemesneBounds.maxY),
                    Math.round((float) claimedDemesneBounds.maxZ),
            });
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        var hn = tag.getString("hoverName");
        hoverName = hn.isEmpty() ? null : Component.literal(tag.getString("hoverName"));
        inDemesneOf = tag.getString("inDemesneOf");
        var bounds = tag.getIntArray("claimedDemesneBounds");
        if (bounds.length > 0) {
            claimedDemesneBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
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

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, inventoryHandlerLazyOptional);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryHandlerLazyOptional.invalidate();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (level == null || demesneData == null) return ItemStack.EMPTY;
        var mats = demesneData.favoredMaterials;
        if (mats.isEmpty()) return ItemStack.EMPTY;
        var mat = mats.get((int) ((level.getGameTime() / 19) % mats.size()));
        return new ItemStack(mat.asItem(), 1);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return getStackInSlot(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return false;
    }
}
