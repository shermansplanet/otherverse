package com.shermansplanet.otherverse.ruins;

import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class ClaimArrow extends Arrow {
    public ClaimArrow(Level p40513, LivingEntity p40515) {
        super(p40513, p40515);
        setBaseDamage(1);
    }

    protected ItemStack getPickupItem() {
        return new ItemStack(OtherverseItems.CLAIM_ARROW.get(), 1);
    }

    protected void onHitBlock(BlockHitResult result) {
        if (getOwner() instanceof Player player) RuinsManager.claimAmounts.remove(player);
        super.onHitBlock(result);
    }
}
