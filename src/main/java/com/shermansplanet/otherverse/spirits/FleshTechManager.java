package com.shermansplanet.otherverse.spirits;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FleshTechManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID constructModifier = UUID.fromString("1a6f782e-bb88-474f-ac7c-0ef82e244080");

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        var le = event.getEntity();
        if (le.level.isClientSide()) return;
        if (le.level.getGameTime() % 20 != 0) return;
        var ct = le.getPersistentData().getString("construct_type");
        if (ct.isEmpty()) return;
        if (!ShrineHelper.getShrinesFor(le, Spirits.spiritsByLabel.get(ct)).isEmpty()) return;
        if (getClosestHeart(le) != null) return;
        le.hurt(DamageSource.MAGIC, 7);
    }

    @SubscribeEvent
    public static void onTick(LivingDamageEvent event) {
        if (!event.getEntity().getPersistentData().getString("construct_type").equals("flesh")) return;
        var playerHeart = getClosestHeart(event.getEntity());
        if (playerHeart == null) return;
        var heart = playerHeart.getSecond();
        var transferredDamage = Math.min(heart.getMaxDamage() - heart.getDamageValue(), Math.round(event.getAmount()));
        heart.setDamageValue(heart.getDamageValue() + transferredDamage);
        if (heart.getDamageValue() >= heart.getMaxDamage()) {
            playerHeart.getFirst().getInventory().removeItem(heart);
        }
        var newAmount = event.getAmount() - transferredDamage;
        if (newAmount <= 0) event.setCanceled(true);
        event.setAmount(newAmount);
    }

    private static Pair<Player, ItemStack> getClosestHeart(LivingEntity le) {
        for (var p : le.getLevel().getEntities(EntityType.PLAYER, le.getBoundingBox().inflate(16), (Player p) -> true)) {
            for (var hand : InteractionHand.values()) {
                var item = p.getItemInHand(hand);
                if (!item.is(OtherverseItems.HOMUNCULUS_HEART.get())) continue;
                if (!ImplementManager.isImplement(item)) continue;
                return new Pair<>(p, item);
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onKill(LivingDropsEvent e) {
        var entity = e.getEntity();
        if (entity.level.isClientSide) return;
        if (entity.getPersistentData().contains("construct_type")) e.setCanceled(true);
    }

    public static boolean tryMakeConstruct(ServerLevel level, BlockFocus focus, Diagram diagram) {
        var target = diagram.influences.get(focus.getPos());
        if (target == null) return false;
        var shrine = ShrineHelper.getShrine(level, target);
        if (shrine == null) return false;
        if (shrine.st != Spirits.TECH && shrine.st != Spirits.FLESH) return false;
        var binding = DiagramManager.getBindingOrBoundMobAt(level, focus.getPos());
        if (binding == null) return false;
        var mob = binding.mob;
        if (mob == null) return false;
        var levelData = DiagramManager.getOrCreateLevelData(level);
        if (!shrine.tryDrain(Math.round(mob.getMaxHealth() * 3), levelData)) return false;
        var spawnPos = target;
        while (!level.getBlockState(spawnPos).getCollisionShape(level, spawnPos).isEmpty()) {
            spawnPos = spawnPos.above();
        }
        var tag = new CompoundTag();
        binding.mob.save(tag);
        var mobData = tag.getCompound("ForgeData");
        mobData.putString("construct_type", shrine.st.label());
        mobData.remove("bindingId");
        mobData.put("unbound_contract", binding.contract);
        tag.remove("UUID");

        var e = (LivingEntity) binding.mob.getType().create(level, tag, null, null,
                spawnPos, MobSpawnType.SPAWN_EGG, false, false);
        e.load(tag);
        e.setPos(new Vec3(
                spawnPos.getX() + 0.4f + level.random.nextFloat() * 0.2f,
                spawnPos.getY() + 0.25f,
                spawnPos.getZ() + 0.4f + level.random.nextFloat() * 0.2f));

        level.addFreshEntityWithPassengers(e);
        if (shrine.st == Spirits.FLESH) {
            var attr = e.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null) attr.addPermanentModifier(
                    new AttributeModifier(constructModifier, "homunculus", -0.5f, AttributeModifier.Operation.MULTIPLY_BASE));
        } else {
            var attr = e.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.addPermanentModifier(
                    new AttributeModifier(constructModifier, "golem", -0.5f, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        BindingManager.applyUnboundContract((Mob) e, false);
        return true;
    }
}
