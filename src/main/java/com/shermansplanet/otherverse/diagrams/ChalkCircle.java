package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.diagrams.DiagramManager.BlockUpdateType;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class ChalkCircle extends BlockEntity implements IItemHandler, IFocus, IForgeBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemStack item = ItemStack.EMPTY;
    public boolean isNumber = false;

    public Diagram diagram = null;
    public BlockPos diagramPrimary = null;
    public double angleDegrees = 0;
    private DiagramProcess activeProcess;
    public long animationTime = -1;
    public int cooldownTicks = 0;
    public String player = "";

    LazyOptional<IItemHandler> inventoryHandlerLazyOptional = LazyOptional.of(() -> this);

    public ChalkCircle(BlockPos pos, BlockState state) {
        super(Otherverse.CHALK_CIRCLE.get(), pos, state);
    }

    public void setAnimTime() {
        setAnimTime(0);
    }

    public void setAnimTime(int offset) {
        animationTime = offset * ChalkCircleRenderer.ANIM_CUTOFF;
        markUpdated();
    }

    public void clearAnimTime() {
        animationTime = -1;
        markUpdated();
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

    public void markUpdated(BlockState state) {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), state, 2);
    }

    public void markUpdated() {
        markUpdated(this.getBlockState());
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (diagram != null) {
            LOGGER.debug("unloading diagram because chunk unloaded");
            DiagramManager.unloadDiagram(diagram, level, true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (item != ItemStack.EMPTY) {
            tag.put("CircleItem", item.save(new CompoundTag()));
            tag.putDouble("Angle", angleDegrees);
        }
        if (diagram != null) {
            tag.put("Diagram", diagram.save(new CompoundTag()));
        }
        if (diagramPrimary != null) {
            tag.putInt("DiagramPrimaryX", diagramPrimary.getX());
            tag.putInt("DiagramPrimaryY", diagramPrimary.getY());
            tag.putInt("DiagramPrimaryZ", diagramPrimary.getZ());
        }
        if (player != null) {
            tag.putString("player", player);
        }
        tag.putBoolean("IsNumber", isNumber);
        tag.putLong("AnimTime", animationTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        item = ItemStack.EMPTY;
        if (tag.contains("DiagramPrimaryX")) {
            diagramPrimary = new BlockPos(
                    tag.getInt("DiagramPrimaryX"),
                    tag.getInt("DiagramPrimaryY"),
                    tag.getInt("DiagramPrimaryZ"));
        }
        if (tag.contains("AnimTime")) {
            animationTime = tag.getLong("AnimTime");
        }
        if (tag.contains("CircleItem")) {
            CompoundTag compoundtag = tag.getCompound("CircleItem");
            item = compoundtag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(compoundtag);
            angleDegrees = tag.getDouble("Angle");
        }
        if (diagram != null && level != null && level.isClientSide()) {
            DiagramManager.unloadDiagram(diagram, level);
        }
        if (tag.contains("player")) {
            player = tag.getString("player");
        }
        if (tag.contains("Diagram")) {
            diagram = new Diagram(level);
            diagram.load(tag.getCompound("Diagram"));
            if (level != null && level.isClientSide()) {
                diagram.calculateItemFociAndInfluences(level, false);
                DiagramManager.diagramLoaded(diagram, level, true);
            }
        }
        isNumber = tag.getBoolean("IsNumber");
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
        if (tag == null) {
            item = ItemStack.EMPTY;
            diagramPrimary = null;
            diagram = null;
            player = "";
            isNumber = false;
        } else {
            load(tag);
        }
    }

    public boolean isEmpty() {
        return item.getItem() instanceof ChalkItem;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return isEmpty() ? ItemStack.EMPTY : item;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isEmpty() || stack.isEmpty()) {
            return stack;
        }
        if (!simulate) {
            if (stack.is(OtherverseItems.IDOL.get()) && stack.getTag().getString("material").equals("otherverse:cinnabar_block") && level instanceof ServerLevel sl) {
                FamiliarManager.makeMobFromTag(IdolItem.getType(stack), stack.getTag(), getCenter(), sl);
                item = new ItemStack(OtherverseItems.CINNABAR_BLOCK.get(), 1);
            } else {
                item = stack.copy();
                item.setCount(1);
            }
            markUpdated();
            if (level instanceof ServerLevel sl) {
                DiagramManager.OnDiagramBlockChanged(sl, getBlockPos(), BlockUpdateType.CHANGED);
            }
        }
        int count = stack.getCount();
        ItemStack newstack = stack.copy();
        newstack.setCount(count - 1);
        return count == 1 ? ItemStack.EMPTY : newstack;
    }

    private Vec3 getCenter() {
        var pos = getPos();
        return new Vec3(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
    }

    private boolean isLocked() {
        var diagram = getDiagram();
        return diagram == null || level == null || !diagram.processes.isEmpty() || !diagram.mobsOnCooldown.isEmpty()
                || DiagramManager.getOrCreateLevelData(level).diagramsToActivate.contains(diagram);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isLocked() || amount == 0 || isEmpty() || item.is(OtherverseItems.SELF.get())) {
            return ItemStack.EMPTY;
        }
        ItemStack cachedItem = item;
        if (!simulate) {
            removeItem();
        }
        return cachedItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public Diagram getDiagram() {
        return DiagramManager.getOrCreateLevelData(level).diagramsByPrimary.get(diagramPrimary);
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public void removeItem() {
        if (item.hasCraftingRemainingItem()) {
            item = item.getCraftingRemainingItem();
        } else if (item.getItem() instanceof BowlFoodItem) {
            item = new ItemStack(Items.BOWL, 1);
        } else {
            item = new ItemStack(OtherverseItems.CHALK.get(), 1);
        }
        markUpdated();
        if (level instanceof ServerLevel sl) {
            DiagramManager.OnDiagramBlockChanged(sl, getBlockPos(), BlockUpdateType.CHANGED);
            if (sl.getBlockEntity(getPos().above()) instanceof ChalkCircle cc
                    && !DiagramManager.getOrCreateLevelData(sl).diagramsToActivate.contains(cc.getDiagram())) {
                Diagram.tryTransferDown(sl, cc);
            }
        }
    }

    @Override
    public boolean isBlock() {
        return false;
    }

    @Override
    public void setProcess(DiagramProcess process) {
        activeProcess = process;
    }

    @Override
    public Level getFocusLevel() {
        return level;
    }

    @Override
    public DiagramProcess getProcess() {
        return activeProcess;
    }

    @Override
    public int drainHallow(SpiritType spiritType, int price, boolean mustMeetFullPrice, boolean simulate) {
        if (isEmpty()) return 0;
        if (!getItem().hasTag() || !getItem().getTag().contains("hallow")) return 0;
        var hallowTag = getItem().getTag().getCompound("hallow");
        if (!hallowTag.getString("spirit_type").equals(spiritType.label())) return 0;
        var spiritCount = hallowTag.getInt("spirit_count");
        if (mustMeetFullPrice && spiritCount < price) return 0;
        var drained = Math.min(price, spiritCount);
        if (!simulate) {
            hallowTag.putInt("spirit_count", spiritCount - drained);
            if (getLevel() instanceof ServerLevel sl) DiagramManager.markDiagramActive(sl, getDiagram());
        }
        return drained;
    }

    @Override
    public int fillHallow(SpiritType spiritType, int amount, boolean mustAcceptAll, boolean simulate) {
        if (isEmpty()) return 0;
        if (!getItem().hasTag() || !getItem().getTag().contains("hallow")) return 0;
        var hallowTag = getItem().getTag().getCompound("hallow");
        if (!hallowTag.getString("spirit_type").equals(spiritType.label())) return 0;
        var spiritCount = hallowTag.getInt("spirit_count");
        var capacity = hallowTag.getInt("capacity");
        var transferAmount = Math.min(amount, Math.max(0, capacity - spiritCount));
        if (mustAcceptAll && transferAmount < amount) return 0;
        if (!simulate) hallowTag.putInt("spirit_count", spiritCount + transferAmount);
        if (getLevel() instanceof ServerLevel sl) DiagramManager.markDiagramActive(sl, getDiagram());
        return transferAmount;
    }

    @Override
    public int getHallowCapacity(SpiritType spiritType) {
        if (isEmpty()) return 0;
        if (!getItem().hasTag() || !getItem().getTag().contains("hallow")) return 0;
        var hallowTag = getItem().getTag().getCompound("hallow");
        if (!hallowTag.getString("spirit_type").equals(spiritType.label())) return 0;
        var spiritCount = hallowTag.getInt("spirit_count");
        var capacity = hallowTag.getInt("capacity");
        return Math.max(0, capacity - spiritCount);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {

        if (!(level instanceof ServerLevel sl)) return;
        if (sl.players().isEmpty()) return;
        if (!sl.players().get(0).isAddedToWorld()) return;

        var cc = (ChalkCircle) t;
        if (cc.cooldownTicks > 0) {
            cc.cooldownTicks--;
            if (cc.cooldownTicks == 0) {
                DiagramManager.markDiagramActive(sl, cc.getDiagram());
            }
        }
        if (cc.diagram == null || cc.diagram.isInitialized) {
            return;
        }

        for (var pos : cc.diagram.blockFocusPositions) {
            if (!level.isLoaded(pos)) {
                return;
            }
        }

        for (var pos : cc.diagram.itemFocusPositions) {
            if (!level.isLoaded(pos)) {
                return;
            }
        }

        cc.diagram.calculateItemFociAndInfluences(sl, false);
        DiagramManager.diagramLoaded(cc.diagram, sl);
    }

    public void setPlayer(Player p) {
        if (player.isEmpty()) {
            player = p.getGameProfile().getName();
        }
    }
}
