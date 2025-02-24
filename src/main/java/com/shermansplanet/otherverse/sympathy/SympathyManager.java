package com.shermansplanet.otherverse.sympathy;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SympathyManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getItemStack().getItem() instanceof SpindleItem)) {
            return;
        }
        var col = SpindleItem.getDyeColor(event.getItemStack().getItem());
        var key = getKey(event.getEntity(), col);
        var data = DiagramManager.getOrCreateLevelData(event.getLevel());
        event.getEntity().displayClientMessage(Component.translatable("otherverse.sympathy.cleared"), true);
        data.putSympathyPosition(key, null);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof SpindleItem)) {
            return;
        }
        var bindPos = SpindleItem.getBlockPos(event.getEntity(), event.getHitVec());
        var state = event.getEntity().getLevel().getBlockState(bindPos);
        if(state.getBlock() == OtherverseBlocks.CHALK_LINE.get()) return;
        var col = SpindleItem.getDyeColor(event.getItemStack().getItem());
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (state.getBlock() == OtherverseBlocks.WEB_OF_FATE.get()) {
            event.getEntity().getLevel().setBlockAndUpdate(bindPos, state.setValue(ColorableBlock.color, col));
        } else {
            event.getEntity().displayClientMessage(Component.translatable("otherverse.sympathy.bound_to_pos"), true);
            var key = getKey(event.getEntity(), col);
            var data = DiagramManager.getOrCreateLevelData(event.getLevel());
            data.putSympathyPosition(key, bindPos);
        }
    }

    public static String getKey(String playerName, DyeColor color) {
        return playerName + "_" + color.getName();
    }

    public static String getKey(Player player, DyeColor color) {
        return getKey(player.getGameProfile().getName(), color);
    }
}
