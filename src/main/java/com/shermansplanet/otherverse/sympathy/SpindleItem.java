package com.shermansplanet.otherverse.sympathy;

import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
public class SpindleItem extends Item {
    public SpindleItem(Properties p_41383_) {
        super(p_41383_);
    }

    public static BlockPos getBlockPos(Player player, BlockHitResult hitResult) {
        var selectedBlockPosition = hitResult.getBlockPos();
        var block = player.level().getBlockState(selectedBlockPosition);
        if (!(block.is(OtherverseBlocks.CHALK_LINE.get()) || block.is(OtherverseBlocks.WEB_OF_FATE.get()))) {
            var dir = hitResult.getDirection();
            var vecToCam = player.getEyePosition().subtract(hitResult.getLocation()).normalize();
            if (vecToCam.dot(new Vec3(dir.step())) < 0.75f) {
                selectedBlockPosition = selectedBlockPosition.relative(dir);
            }
        }
        return selectedBlockPosition;
    }

    public static DyeColor getDyeColor(Item item) {
        return OtherverseItems.spindleColors.get(item);
    }
}
