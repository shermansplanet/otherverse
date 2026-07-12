package com.shermansplanet.otherverse.others;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.familiar.IAnchorSetter;
import com.shermansplanet.otherverse.mixin.PhantomInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public class Banshee extends Phantom {
    private int alertCooldown = 0;
    private int anvilCooldown = 0;

    public Banshee(EntityType<? extends Phantom> p_33101_, Level p_33102_) {
        super(p_33101_, p_33102_);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> p_217058_, LevelAccessor p_217059_, MobSpawnType p_217060_, BlockPos p_217061_, RandomSource p_217062_) {
        return true;
    }

    public void alert() {
        alertCooldown = 20 * 60;
    }

    public void aiStep() {
        if (alertCooldown > 0) alertCooldown--;
        super.aiStep();
    }

    public void tryAnvilDrop() {
        var targetBlockPos = getTarget().blockPosition().above(21);
        ((IAnchorSetter) this).setAnchor(targetBlockPos);
        var targetPos = targetBlockPos.getCenter();
        anvilCooldown--;
        if (anvilCooldown <= 0 && position().distanceToSqr(targetPos) < 2 * 2 && level().getBlockState(targetBlockPos).isAir()) {
            var anvil = BindingManager.getHeldItem(this).split(1);
            level().setBlock(targetBlockPos, ((BlockItem) anvil.getItem()).getBlock().defaultBlockState(), 3);
            if (BindingManager.getHeldItem(this).isEmpty()) BindingManager.setHeldItem(this, ItemStack.EMPTY);
            anvilCooldown = 60;
        }
        navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, 1);
    }

    private boolean isAnvil(ItemStack heldItem) {
        return heldItem.is(Items.ANVIL) || heldItem.is(Items.CHIPPED_ANVIL) || heldItem.is(Items.DAMAGED_ANVIL);
    }

    public void setTarget(@Nullable LivingEntity target) {
        if (!BindingManager.isBoundOrContracted(this) && alertCooldown <= 0) return;
        super.setTarget(target);
    }

    public boolean hurt(DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            if (source.getEntity() instanceof LivingEntity le) {
                alert();
                setTarget(le);
            }
            return true;
        }
        return false;
    }

    public boolean canDropAnvil() {
        return FamiliarManager.isFamiliar(this) && isAnvil(BindingManager.getHeldItem(this)) && getTarget() != null;
    }
}
