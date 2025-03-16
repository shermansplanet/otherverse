package com.shermansplanet.otherverse.sympathy;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SympathyManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        event.setAmount(-onHpChange(event.getEntity(), -event.getAmount(), event.getSource()));
        var src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity le)) return;
        var spindle = le.getMainHandItem();
        var bloodySpindle = OtherverseItems.SPINDLE_BLOODY.get();
        if (!(spindle.getItem() instanceof SpindleItem) && !spindle.is(bloodySpindle)) return;
        var newstack = new ItemStack(bloodySpindle, spindle.getCount(), spindle.getTag());
        newstack.getOrCreateTag().putInt("sympathy_target", event.getEntity().getId());
        le.setItemSlot(EquipmentSlot.MAINHAND, newstack);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHeal(LivingHealEvent event) {
        event.setAmount(onHpChange(event.getEntity(), event.getAmount(), DamageSource.OUT_OF_WORLD));
    }

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
        if (state.getBlock() == OtherverseBlocks.CHALK_LINE.get()) return;
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

    private static float onHpChange(LivingEntity entity, float amount, DamageSource damageSource) {
        if (!entity.getPersistentData().contains("bindingId")) return amount;
        if (!(entity.level instanceof ServerLevel sl)) return amount;
        var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
        var binding = data.bindingsById.get(entity.getPersistentData().getUUID("bindingId"));
        if (binding == null) return amount;
        amount = distributeHpChange(binding.getFocus(), (int) amount, damageSource);
        var inFocus = DiagramManager.getFocusInBoundingBox(DiagramManager.getOrCreateLevelData(sl), entity.getBoundingBox());
        if (inFocus != null) amount = distributeHpChange(inFocus, (int) amount, damageSource);
        return amount;
    }

    public static int distributeHpChange(IFocus focus, int delta, DamageSource damageSource) {
        if (Math.abs(delta) <= 1) return delta;
        var hallowPos = focus.getDiagram().influences.get(focus.getPos());
        if (hallowPos == null) return delta;
        var spindlePos = focus.getDiagram().influences.get(hallowPos);
        if (spindlePos == null) return delta;
        var level = focus.getFocusLevel();
        if (!(level.getBlockEntity(spindlePos) instanceof ChalkCircle spindleCircle)) return delta;
        if (!spindleCircle.getItem().is(OtherverseItems.SPINDLE_BLOODY.get())) return delta;
        IFocus hallowFocus = DiagramManager.getOrCreateLevelData(level).allBlockFoci.get(hallowPos);
        if (hallowFocus == null) {
            if (level.getBlockEntity(hallowPos) instanceof ChalkCircle hallowCircle) {
                hallowFocus = hallowCircle;
            } else {
                return delta;
            }
        }

        var price = Math.abs(delta) / 2;
        var drained = hallowFocus.drainHallow(Spirits.FATE, price, false, false);
        var otherDelta = Mth.sign(delta) * drained;
        if(otherDelta == 0) return delta;
        var otherEntity = level.getEntity(spindleCircle.getItem().getTag().getInt("sympathy_target"));
        if(!(otherEntity instanceof LivingEntity le)) return delta;
        if(otherDelta > 0){
            le.heal(otherDelta);
        }else{
            le.hurt(damageSource, -otherDelta);
        }
        return delta - otherDelta;
    }
}
