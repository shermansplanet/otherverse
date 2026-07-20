package com.shermansplanet.otherverse.artifacts;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConnectionBlockManager {
    public static boolean tryMakeBlocker(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        if (!circle.getItem().is(Items.PAPER)) return false;
        var targetPos = circle.getPos();
        var hasCarrot = false;
        var hasEye = false;
        for (var influence : diagram.influences.entrySet()) {
            if (!targetPos.equals(influence.getValue())) continue;
            if (!(level.getBlockEntity(influence.getKey()) instanceof ChalkCircle cc)) continue;
            var item = cc.getItem();
            if (item.is(Items.GOLDEN_CARROT)) hasCarrot = true;
            else if (item.is(Items.FERMENTED_SPIDER_EYE)) hasEye = true;
        }
        if (!hasCarrot || !hasEye) return false;
        var powerSpent = diagram.getPowerSpent(level, targetPos, -1, new HashSet<>());
        if (powerSpent == 0) return false;
        circle.item = new ItemStack(OtherverseItems.CONNECTION_BLOCKER.get(), 1);
        var total = powerSpent * 20;
        circle.item.getOrCreateTag().putInt("connection_blocker_total", total);
        circle.item.getOrCreateTag().putInt("connection_blocker_remaining", total);
        return true;
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        var level = event.getEntity().level();
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return;
        var blockedSeconds = event.getEntity().getPersistentData().getInt("connection_blocked_seconds");
        var isHoldingBlocker = event.getEntity().isHolding(OtherverseItems.CONNECTION_BLOCKER.get());
        if (isHoldingBlocker) {
            blockedSeconds++;
        }
        if (blockedSeconds == 0) return;
        if (!isHoldingBlocker) {
            event.getEntity().addEffect(new MobEffectInstance(OtherversePotions.REBOUND_EFFECT.get(),
                    blockedSeconds * 20, 0, false, false, true));
            blockedSeconds = 0;
        }
        event.getEntity().getPersistentData().putInt("connection_blocked_seconds", blockedSeconds);
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffect() == OtherversePotions.REBOUND_EFFECT.get()) {
            event.setCanceled(true);
        }
    }

    public static boolean isBlocked(LivingEntity target) {
        if (target == null) return false;
        if (target.hasEffect(OtherversePotions.REBOUND_EFFECT.get())) return false;
        var blocker = target.getMainHandItem();
        var hand = InteractionHand.MAIN_HAND;
        if (!blocker.is(OtherverseItems.CONNECTION_BLOCKER.get())) {
            blocker = target.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (!blocker.is(OtherverseItems.CONNECTION_BLOCKER.get())) return false;
        if (!blocker.hasTag() || !blocker.getTag().contains("connection_blocker_total")) return false;
        var remaining = blocker.getTag().getInt("connection_blocker_remaining") - 1;
        if (remaining == 0) {
            target.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            blocker.getTag().putInt("connection_blocker_remaining", remaining);
        }
        return true;
    }
}
