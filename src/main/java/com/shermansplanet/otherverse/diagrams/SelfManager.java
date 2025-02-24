package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;

import java.util.UUID;

import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SelfManager {

    public static final int SELF_TOTAL = 10;
    private static final UUID selfModifierId = UUID.fromString("4fc847e9-c431-486a-9f05-3bb7fc825fcb");

    private static boolean wasDay = true;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean ChangeSelf(Player player, int selfDelta) {
        LOGGER.debug("Changing Self by " + selfDelta);
        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier prevMod = healthAttribute.getModifier(selfModifierId);
        double prevCoeff = prevMod == null ? 1 : (prevMod.getAmount() + 1);
        if (prevCoeff == 1 && selfDelta > 0) {
            return false;
        }
        double newCoeff = (Math.round(prevCoeff * SELF_TOTAL) + selfDelta) / (float) SELF_TOTAL;

        if (newCoeff <= 0) {
            return false;
        }
        healthAttribute.removeModifier(selfModifierId);
        if (newCoeff >= 0.99) {
            return true;
        }
        healthAttribute.addPermanentModifier(
                new AttributeModifier(selfModifierId, "Spent Self", newCoeff - 1, Operation.MULTIPLY_TOTAL));

        if (selfDelta < 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, selfDelta * -240));
        }

        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        return true;
    }

    @SubscribeEvent
    public static void playerDeath(PlayerEvent.Clone event) {
        AttributeInstance oldHealthAttribute = event.getOriginal().getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier prevMod = oldHealthAttribute.getModifier(selfModifierId);
        if (prevMod == null) return;
        var player = event.getEntity();
        player.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(prevMod);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEarnEvent event) {
        Player player = event.getEntity();
        var display = event.getAdvancement().getDisplay();
        if (player.level.isClientSide() || display == null || display.isHidden()) {
            return;
        }
        if (ChangeSelf(player, SELF_TOTAL)) {
            player.displayClientMessage(Component.translatable("otherverse.self.restored_advancement"), true);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent event) {
        if (event.phase == Phase.START) {
            return;
        }

        boolean isNowDay = event.getServer().overworld().isDay();
        if (isNowDay == wasDay) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            FamiliarManager.updateAbilities(player);
            if (isNowDay && ChangeSelf(player, 3)) {
                player.displayClientMessage(Component.translatable("otherverse.self.restored_dawn"), true);
            }
        }
        wasDay = isNowDay;
    }
}
