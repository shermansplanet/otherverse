package com.shermansplanet.otherverse.sympathy;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;

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
        newstack.getOrCreateTag().putUUID("sympathy_target", event.getEntity().getUUID());
        newstack.getOrCreateTag().putString("sympathy_label", event.getEntity().getType().getDescriptionId());
        le.setItemSlot(EquipmentSlot.MAINHAND, newstack);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHeal(LivingHealEvent event) {
        event.setAmount(onHpChange(event.getEntity(), event.getAmount(), event.getEntity().level().damageSources().fellOutOfWorld()));
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
        var state = event.getEntity().level().getBlockState(bindPos);
        if (state.getBlock() == OtherverseBlocks.CHALK_LINE.get()) return;
        var col = SpindleItem.getDyeColor(event.getItemStack().getItem());
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (state.getBlock() == OtherverseBlocks.WEB_OF_FATE.get()) {
            if (event.getEntity().isCrouching()) {
                var key = getKey(event.getEntity(), col);
                var data = DiagramManager.getOrCreateLevelData(event.getLevel());
                var targetPos = data.getSympathyPosition(key);
                if (targetPos != null) {
                    data.putSympathyPosition(bindPos.toString(), targetPos);
                }
                event.getEntity().displayClientMessage(Component.literal("Web of Fate bound to " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), true);
                col = DyeColor.WHITE;
            }
            event.getEntity().level().setBlockAndUpdate(bindPos, state.setValue(ColorableBlock.color, col));
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
        if (!(entity.level() instanceof ServerLevel sl)) return amount;
        var inFocus = DiagramManager.getFocusInBoundingBox(DiagramManager.getOrCreateLevelData(sl), entity.getBoundingBox());
        if (inFocus != null) amount = distributeHpChange(inFocus, (int) amount, damageSource);
        if (entity.getPersistentData().contains("bindingId")) {
            var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
            var binding = data.bindingsById.get(entity.getPersistentData().getUUID("bindingId"));
            if (binding == null || binding.getFocus() == null) return amount;
            amount = distributeHpChange(binding.getFocus(), (int) amount, damageSource);
        }

        for (var cc : getSympatheticLinks(entity, sl)) {
            amount = distributeHpChange(cc, (int) amount, damageSource);
        }

        return amount;
    }

    public static List<ChalkCircle> getSympatheticLinks(LivingEntity entity, ServerLevel sl) {
        var allCircles = new ArrayList<ChalkCircle>();
        var key = getKeyFor(entity);
        for (var level : sl.getServer().getAllLevels()) {
            var data = DiagramManager.getOrCreateLevelData(level);
            if (!data.selfPositions.containsKey(key)) continue;
            var allPositions = data.selfPositions.get(key);
            var validPositions = new HashSet<BlockPos>();
            for (var pos : allPositions) {
                if (!(level.getBlockEntity(pos) instanceof ChalkCircle cc)) continue;
                if (entity instanceof Player) {
                    if (!cc.getItem().is(OtherverseItems.SELF.get()) || !Objects.equals(cc.player, key)) continue;
                } else {
                    if (!cc.getItem().is(OtherverseItems.SPINDLE_BLOODY.get())
                            || !Objects.equals(cc.getItem().getTag().getUUID("sympathy_target").toString(), key))
                        continue;
                }
                validPositions.add(pos);
                allCircles.add(cc);
            }
            data.selfPositions.put(key, validPositions);
            data.setDirty();
        }
        return allCircles;
    }

    private static String getKeyFor(LivingEntity entity) {
        return (entity instanceof Player player) ? player.getGameProfile().getName() : entity.getUUID().toString();
    }

    public static int distributeHpChange(IFocus focus, int delta, DamageSource damageSource) {
        if (Math.abs(delta) == 0) return delta;
        var diagram = focus.getDiagram();
        if (diagram == null) return delta;
        var hallowPos = diagram.influences.get(focus.getPos());
        if (hallowPos == null) return delta;
        var spindlePos = diagram.influences.get(hallowPos);
        if (spindlePos == null) return delta;
        var level = focus.getFocusLevel();
        if (!(level instanceof ServerLevel sl)) return delta;
        if (!(level.getBlockEntity(spindlePos) instanceof ChalkCircle spindleCircle)) return delta;
        var isSpindle = spindleCircle.getItem().is(OtherverseItems.SPINDLE_BLOODY.get());
        var isSelf = spindleCircle.getItem().is(OtherverseItems.SELF.get());
        if (!isSelf && !isSpindle) return delta;
        IFocus hallowFocus = DiagramManager.getOrCreateLevelData(level).allBlockFoci.get(hallowPos);
        if (hallowFocus == null) {
            if (level.getBlockEntity(hallowPos) instanceof ChalkCircle hallowCircle) {
                hallowFocus = hallowCircle;
            } else {
                return delta;
            }
        }

        var price = Math.abs(delta) / 2;
        if (Math.abs(delta) % 2 == 1 && focus.getFocusLevel().getRandom().nextBoolean()) {
            price += 1;
        }
        if (price == 0) return delta;
        var drained = hallowFocus.drainHallow(Spirits.FATE, price, false, false);
        var otherDelta = Mth.sign(delta) * drained;
        if (otherDelta == 0) return delta;
        Entity otherEntity = null;
        if (isSpindle) {
            otherEntity = getEntityByUniqueId(spindleCircle.getItem().getTag().getUUID("sympathy_target"), sl);
        } else {
            for (var player : level.players()) {
                if (player.getGameProfile().getName().equals(spindleCircle.player)) {
                    otherEntity = player;
                    break;
                }
            }
        }
        if (!(otherEntity instanceof LivingEntity le)) return delta;
        if (otherDelta > 0) {
            le.heal(otherDelta);
        } else {
            le.hurt(damageSource, -otherDelta);
        }
        return delta - otherDelta;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(OtherverseItems.SPINDLE_BLOODY.get())) return;
        if (!event.getItemStack().hasTag() || !event.getItemStack().getTag().contains("sympathy_label")) return;
        var spindleTag = event.getItemStack().getTag();
        var label = spindleTag.getString("sympathy_label");
        event.getToolTip().add(Component.literal("Sympathy target: ")
                .append(Component.translatable(label))
                .withStyle(Style.EMPTY.withColor(0xaaaaaa).withItalic(true)));
    }

    private static HashMap<UUID, Entity> entitiesByUUID = new HashMap<>();

    @SubscribeEvent
    public static void onStart(ServerStartedEvent event) {
        entitiesByUUID.clear();
    }

    public static Entity getEntityByUniqueId(UUID uniqueId, ServerLevel sl) {
        if (entitiesByUUID.containsKey(uniqueId)) return entitiesByUUID.get(uniqueId);
        for (ServerLevel level : sl.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isAddedToWorld() && entity.getUUID().equals(uniqueId)) {
                    entitiesByUUID.put(uniqueId, entity);
                    return entity;
                }
            }
        }

        return null;
    }
}
