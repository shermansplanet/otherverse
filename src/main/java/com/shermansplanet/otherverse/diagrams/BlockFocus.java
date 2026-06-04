package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingInfo;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;

public class BlockFocus implements IFocus {

    private final Level level;
    private final BlockPos blockPos;
    private final Diagram diagram;
    private DiagramProcess activeProcess;

    public Mob mostRecentMob;
    public int mostRecentMobHealth;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static HashMap<Block, Item> blockReplacements = new HashMap<>();

    static {
        blockReplacements.put(Blocks.TWISTING_VINES_PLANT, Items.TWISTING_VINES);
        blockReplacements.put(Blocks.WEEPING_VINES_PLANT, Items.WEEPING_VINES);
        blockReplacements.put(Blocks.KELP_PLANT, Items.KELP);
    }

    public BlockFocus(Level level, BlockPos blockPos, Diagram diagram) {
        this.level = level;
        this.blockPos = blockPos;
        this.diagram = diagram;
        BindingInfo binding = DiagramManager.getOrCreateLevelData(level).bindingsByPosition.get(blockPos);
        if (binding != null) {
            mostRecentMob = binding.mob;
        }
    }

    @Override
    public ItemStack getItem() {
        if (level instanceof ServerLevel sl) {
            BindingInfo binding = DiagramManager.getBindingOrBoundMobAt(sl, blockPos);
            if (binding != null && binding.mob != null) {
                return MobBindingInfluenceUtils.getIdol(binding.mob.getType());
            }
        }
        return getItemNotMob();
    }

    @Override
    public ItemStack getItemNotMob() {
        var levelData = DiagramManager.getOrCreateLevelData(level);
        BlockState blockstate = level.getBlockState(blockPos);
        Item item = blockReplacements.getOrDefault(blockstate.getBlock(), blockstate.getBlock().asItem());
        ItemStack stack = new ItemStack(item);
        if (blockstate.is(Blocks.NETHER_PORTAL)) {
            return Spirits.spiritItems.get(Spirits.NETHER).get().getDefaultInstance();
        } else if (blockstate.is(Blocks.FIRE) || blockstate.is(Blocks.SOUL_FIRE)) {
            return Spirits.spiritItems.get(Spirits.FIRE).get().getDefaultInstance();
        }
        CompoundTag hallowTag = levelData.getPlacedItemTag(blockPos);
        if (hallowTag != null) {
            var newHallowTag = hallowTag.copy();
            var spiritType = Spirits.spiritsByLabel.get(newHallowTag.getString("spirit_type"));
            var countcap = HallowHelper.getShrineSpiritCountAndCapacity(level, blockPos, spiritType);
            newHallowTag.putInt("spirit_count", countcap.getFirst());
            newHallowTag.putInt("capacity", countcap.getSecond());
            stack.getOrCreateTag().put("hallow", newHallowTag);
            HallowHelper.addFakeEnchantment(stack.getTag());
        }
        if (stack.is(Items.AIR)) {
            if (level.isWaterAt(blockPos)) {
                return Items.WATER_BUCKET.getDefaultInstance();
            }
            return Items.AIR.getDefaultInstance();
        }
        return stack;
    }

    @Override
    public Diagram getDiagram() {
        return diagram;
    }

    @Override
    public BlockPos getPos() {
        return blockPos;
    }

    @Override
    public void removeItem() {
        var bs = level.getBlockState(blockPos);
        if (bs.getBlock() instanceof BucketPickup bp && level.isWaterAt(blockPos)) {
            bp.pickupBlock(level, blockPos, bs);
            bp.getPickupSound(bs).ifPresent((soundEvent) ->
                    level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1, 1));
        } else {
            level.destroyBlock(blockPos, false);
        }
        DiagramManager.getOrCreateLevelData(level).removePlacedItemTag(blockPos);
    }

    @Override
    public Level getFocusLevel() {
        return level;
    }

    @Override
    public void drainItem() {
        removeItem();
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public void setProcess(DiagramProcess process) {
        activeProcess = process;
    }

    @Override
    public DiagramProcess getProcess() {
        return activeProcess;
    }

    @Override
    public int drainHallow(SpiritType spiritType, int price, boolean mustMeetFullPrice, boolean simulate) {
        return HallowHelper.drainBlockHallow(level, blockPos, spiritType, price, mustMeetFullPrice, simulate);
    }

    @Override
    public int fillHallow(SpiritType spiritType, int amount, boolean mustAcceptAll, boolean simulate) {
        return HallowHelper.fillBlockHallow(level, blockPos, spiritType, amount, mustAcceptAll, simulate);
    }

    @Override
    public int getHallowCapacity(SpiritType spiritType) {
        var data = DiagramManager.getOrCreateLevelData(level);
        var hallowPositions = ShrineHelper.getAllHallows(getPos(), spiritType, data);
        if (hallowPositions.isEmpty()) return 0;

        var capacity = 0;
        for (BlockPos sourcePos : hallowPositions) {
            var ht = data.getPlacedItemTag(sourcePos);
            if(spiritType == Spirits.TECH && ht.contains("shrine") && level.hasNeighborSignal(blockPos)){
                return 0;
            }
            capacity += Math.max(0, ht.getInt("capacity") - ht.getInt("spirit_count"));
        }
        return capacity;
    }

    public Vec3 getCenter() {
        return new Vec3(blockPos.getX() + 0.5f,
                blockPos.getY() + 0.5f,
                blockPos.getZ() + 0.5f);
    }

    public BlockState getBlock() {
        return level.getBlockState(blockPos);
    }
}
