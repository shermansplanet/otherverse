package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.ruins.RuinsManager;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class Guest extends EnderMan {
    private int eatCountdown;

    public Guest(EntityType<? extends EnderMan> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> p_217058_, LevelAccessor p_217059_, MobSpawnType p_217060_, BlockPos p_217061_, RandomSource p_217062_) {
        BlockPos blockpos = p_217061_.below();
        return p_217060_ == MobSpawnType.SPAWNER || p_217059_.getBlockState(blockpos).isCollisionShapeFullBlock(p_217059_, p_217061_);
    }

    public void customServerAiStep() {
        if (level().getGameTime() % 20 == getId() % 20) {
            for (var player : level().players()) {
                Vec3 vec3 = player.getViewVector(1.0F).normalize();
                Vec3 vec31 = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
                double d0 = vec31.length();
                vec31 = vec31.normalize();
                double d1 = vec3.dot(vec31);
                if (d1 > 1.0D - 0.025D / d0 && player.hasLineOfSight(this)) {
                    summonTo(player);
                    break;
                }
            }
        }

        if (eatCountdown > 0) {
            eatCountdown--;
            if (eatCountdown == 0) {
                var item = getItemInHand(InteractionHand.MAIN_HAND);
                if (!item.isEmpty()) {
                    level().playSound(null, this, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1, 1);
                    RuinsManager.onFedGuest(level().getGameTime());
                    setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }

        super.customServerAiStep();
    }

    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player &&
                player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath().equals("ruins_trust")) {
            player.displayClientMessage(Component.literal("By harming a guest, you have violated Hospitality."), true);
            RuinsManager.breakHospitality(player);
        }
        return super.hurt(source, amount);
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        var item = player.getItemInHand(hand);
        if (!item.isEdible()) return InteractionResult.PASS;
        setItemInHand(InteractionHand.MAIN_HAND, item.split(1));
        if (item.getCount() == 0) player.getInventory().removeItem(item);
        eatCountdown = 20;
        return InteractionResult.CONSUME;
    }

    private void summonTo(Player player) {
        var lookAngle = player.getLookAngle();
        var horizontalLookAngle = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
        var dest = BlockPos.containing(player.position().add(horizontalLookAngle.scale(3))).above(6);
        var airCount = 0;
        var level = player.level();
        var foundGround = false;
        for (var i = 0; i < 9; i++) {
            var isEmpty = level.getBlockState(dest).getCollisionShape(level, dest).isEmpty();
            if (isEmpty) {
                airCount++;
            } else if (airCount >= 3) {
                dest = dest.above();
                foundGround = true;
                break;
            }
            dest = dest.below();
        }
        if (!foundGround) return;
        var dist = player.distanceTo(this);
        var dstPos = Vec3.atBottomCenterOf(dest);
        if (dist > 16 || !getNavigation().moveTo(dstPos.x, dstPos.y, dstPos.z, 2)) {
            teleportTo(dstPos.x, dstPos.y, dstPos.z);
            level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1, 1);
            lookAt(EntityAnchorArgument.Anchor.FEET, player.position());
        }
    }
}
