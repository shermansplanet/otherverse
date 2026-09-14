package com.shermansplanet.otherverse.sympathy;

import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BloodySpindleItem extends Item {
    public BloodySpindleItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var tag = stack.getOrCreateTag();
        if (!tag.getBoolean("can_summon")) return InteractionResultHolder.pass(stack);
        putSummonLoc(tag, player.getEyePosition().add(player.getLookAngle().scale(2)));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var tag = ctx.getItemInHand().getOrCreateTag();
        if (!tag.getBoolean("can_summon")) return InteractionResult.PASS;
        putSummonLoc(tag, ctx.getClickLocation());
        ctx.getPlayer().startUsingItem(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    private void putSummonLoc(CompoundTag tag, Vec3 loc) {
        tag.putFloat("summon_x", (float) loc.x);
        tag.putFloat("summon_y", (float) loc.y);
        tag.putFloat("summon_z", (float) loc.z);
    }

    private Vec3 getSummonLoc(CompoundTag tag) {
        return new Vec3(
                tag.getFloat("summon_x"),
                tag.getFloat("summon_y"),
                tag.getFloat("summon_z")
        );
    }

    @Override
    public int getUseDuration(ItemStack p_41454_) {
        return 25;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_41452_) {
        return UseAnim.BOW;
    }

    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity player) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return stack;
        var tag = stack.getTag();
        var entity = SympathyManager.getEntityByUniqueId(tag.getString("sympathy_target"), sl);
        if (entity == null) return stack;
        var position = getSummonLoc(tag);
        entity.moveTo(position);
        stack.shrink(1);
        if (stack.isEmpty()) sp.getInventory().removeItem(stack);
        return stack;
    }


    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int ticks) {
        if(ticks % 4 != 0 || !level.isClientSide()) return;
        var tag = stack.getTag();
        level.addParticle(ParticleTypes.SOUL, tag.getFloat("summon_x"),
                tag.getFloat("summon_y"),
                tag.getFloat("summon_z"),
                0, 0.1f, 0);
    }

//    if (event.getItemStack().is(OtherverseItems.SPINDLE_BLOODY.get()))
//    trySpindleSummon(event.getEntity(), event.getItemStack(), event.getHitVec().getLocation());
//    if (event.getItemStack().is(OtherverseItems.SPINDLE_BLOODY.get()))
//    trySpindleSummon(event.getEntity(), event.getItemStack(), event.getEntity().getEyePosition().add(event.getEntity().getLookAngle().scale(2)));
}
