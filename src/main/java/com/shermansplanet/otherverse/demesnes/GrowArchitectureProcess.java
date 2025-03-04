package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.DiagramProcess;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;

public class GrowArchitectureProcess extends DiagramProcess {
    private final BlockPos minCopyPos;
    private final int dx, dy, dz;
    private int currentSeekIndex;
    private final ServerLevel level;
    private final DemesnesManager.ArchitectureInventory inventory;
    private long destroyStartTime = -1;
    private boolean hasSentUpdate = false;
    private final AABB bounds;
    private static final Logger LOGGER = LogUtils.getLogger();
    private BlockPos wasDestroying;

    public GrowArchitectureProcess(BlockFocus beaconFocus, DemesnesManager.ArchitectureInventory inventory, ArrayList<BlockPos> webPositions, ClaimedDemesneData demesne) {
        super(beaconFocus, inventory.focus(), -1); // beacon sink, inventory source - because sources can only have one process
        LOGGER.debug("NEW PROCESS");
        this.inventory = inventory;
        var p1 = webPositions.get(0);
        var p2 = webPositions.get(1);
        minCopyPos = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        BlockPos maxSourcePos = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
        dx = maxSourcePos.getX() - minCopyPos.getX() + 1;
        dy = maxSourcePos.getY() - minCopyPos.getY() + 1;
        dz = maxSourcePos.getZ() - minCopyPos.getZ() + 1;
        level = (ServerLevel) beaconFocus.getFocusLevel();
        bounds = new AABB(demesne.minPos, demesne.maxPos);
    }

    @Override
    public void abandon() {
        super.abandon();
        LOGGER.debug("GROWTH ABANDONED");
        inventory.pos().trySendUpdate(level, dx, dy, dz, bounds, false);
    }

    @Override
    public void tick() {
        if (!hasSentUpdate) {
            LOGGER.debug("TRYING TO SEND UPDATE");
            hasSentUpdate = inventory.pos().trySendUpdate(level, dx, dy, dz, bounds, true);
        }
        var minPastePos = inventory.pos().getPos(level);
        if (minPastePos == null) {
            currentSeekIndex = 0;
            return;
        }
        if (wasDestroying != null) {
            level.destroyBlockProgress(inventory.hashCode(), wasDestroying, -1);
            wasDestroying = null;
        }
        for (var ignored = 0; ignored < 16; ignored++) {
            if (currentSeekIndex >= dx * dy * dz) currentSeekIndex = 0;
            var diff = new BlockPos(currentSeekIndex % dx, (currentSeekIndex / (dx * dz)) % dy, (currentSeekIndex / dx) % dz);
            var copyBlock = level.getBlockState(minCopyPos.offset(diff));
            if (copyBlock.is(OtherverseBlocks.CHALK_LINE.get())) continue;
            var pastePos = minPastePos.offset(diff);
            if (!bounds.contains(pastePos.getX(), pastePos.getY(), pastePos.getZ())) continue;

            var pasteBlock = level.getBlockState(pastePos);
            if (pasteBlock.is(OtherverseBlocks.CHALK_LINE.get())) continue;
            var sameBlocks = copyBlock.getBlock() == pasteBlock.getBlock();
            /*var sameBlocks = false;
            if (copyBlock.getBlock() == pasteBlock.getBlock()) {
                sameBlocks = true;
                for (var key : copyBlock.getValues().keySet()) {
                    if (copyBlock.getValue(key).equals(pasteBlock.getValue(key))) continue;
                    sameBlocks = false;
                    break;
                }
                if (!sameBlocks) {
                    level.setBlock(pastePos, copyBlock, 1);
                    level.destroyBlockProgress(inventory.hashCode(), pastePos, -1);
                    sameBlocks = true;
                }
            }*/
            if (sameBlocks) {
                currentSeekIndex++;
                continue;
            }

            if (pasteBlock.isAir() || pasteBlock.getBlock() instanceof LiquidBlock) {
                var hasRequiredItem = false;
                if (copyBlock.isAir()) {
                    hasRequiredItem = true;
                } else {
                    Item requiredItem;
                    try {
                        requiredItem = copyBlock.getBlock().asItem();
                    } catch (Exception e) {
                        if (copyBlock.getBlock() == Blocks.WATER) {
                            requiredItem = Items.WATER_BUCKET;
                        } else {
                            currentSeekIndex++;
                            continue;
                        }
                    }
                    for (var i = 0; i < inventory.inventory().getSlots(); i++) {
                        if (inventory.inventory().getStackInSlot(i).getItem() != requiredItem) continue;
                        var extracted = inventory.inventory().extractItem(i, 1, false);
                        if (extracted.isEmpty()) continue;
                        if (extracted.is(Items.WATER_BUCKET)) {
                            inventory.inventory().insertItem(i, new ItemStack(Items.BUCKET, 1), false);
                        }
                        hasRequiredItem = true;
                        break;
                    }
                }
                if (hasRequiredItem) {
                    level.setBlockAndUpdate(pastePos, copyBlock);
                    level.destroyBlockProgress(inventory.hashCode(), pastePos, -1);
                    currentSeekIndex++;
                    break;
                } else {
                    currentSeekIndex++;
                    continue;
                }
            } else {
                if (destroyStartTime == -1) {
                    destroyStartTime = level.getGameTime();
                }
                var blockDestroyTime = pasteBlock.getBlock().defaultDestroyTime();
                if (blockDestroyTime < 0) {
                    currentSeekIndex++;
                    continue;
                }
                var timeSinceDestroy = (level.getGameTime() - destroyStartTime) / 20f;
                var blockDamage = blockDestroyTime == 0 ? 10 : Mth.ceil(timeSinceDestroy * 10f / blockDestroyTime);
                if (blockDamage >= 10) {
                    destroyStartTime = -1;
                    wasDestroying = null;
                    level.destroyBlock(pastePos, false);
                } else {
                    wasDestroying = pastePos;
                    level.destroyBlockProgress(inventory.hashCode(), pastePos, blockDamage);
                }
            }
            break;
        }
    }
}
