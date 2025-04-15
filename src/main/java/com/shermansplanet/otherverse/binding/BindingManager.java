package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherverseClientPacketHandler;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.artifacts.ArtifactManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class BindingManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static long lastGiveAttempt;

    public static ItemStack getHeldItem(LivingEntity entity) {
        return entity.getMainHandItem();
    }

    public static void setHeldItem(LivingEntity entity, ItemStack stack) {
        entity.setItemSlot(EquipmentSlot.MAINHAND, stack);
        if (entity instanceof EnderMan em) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                em.setCarriedBlock(bi.getBlock().defaultBlockState());
            } else {
                em.setCarriedBlock(null);
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof IdolItem ii) {
            event.getToolTip().clear();
            var entityType = IdolItem.getType(event.getItemStack());
            if (entityType == null) return;
            event.getToolTip().add(entityType.getDescription());
            if (event.getItemStack().getTag().contains("mob_data")) {
                try {
                    int maxHp = (int) DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType).getValue(Attributes.MAX_HEALTH);
                    int hp = (int) event.getItemStack().getTag().getCompound("mob_data").getCompound("EntityTag").getFloat("Health");
                    event.getToolTip().add(Component.literal(hp + "/" + maxHp + " Health"));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && event.getEntity().getType() == EntityType.ENDERMAN && event.getEntity().getPersistentData().hasUUID("bindingId")) {
            event.setCanceled(true);
        }
    }

    public static boolean tryBindMob(Mob mob, BlockFocus focus, Level level) {
        boolean rebinding = false;
        var data = DiagramManager.getOrCreateLevelData(level);
        var bindingsByPosition = data.bindingsByPosition;
        BindingInfo oldBinding = bindingsByPosition.get(focus.getPos());
        if (FamiliarManager.isFamiliar(mob)) {
            if (oldBinding != null) {
                bindingsByPosition.remove(focus.getPos());
                data.savedData.setDirty();
            }
            return false;
        }
        if (mob == null) {
            if (oldBinding != null) {
                oldBinding.unload();
            }
            focus.mostRecentMob = null;
            return false;
        }
        if(mob.getPersistentData().contains("construct_type")){
            return false;
        }
        if (oldBinding != null) {
            if (oldBinding.mob == null || oldBinding.mob.isDeadOrDying()) {
                LOGGER.debug("FOUND OLD BINDING WITHOUT MOB");
                oldBinding.unload();
                oldBinding = null;
            }
        }
        if (oldBinding != null && mob.getId() != oldBinding.mob.getId()) {
            return false;
        }
        if (mob.getPersistentData().hasUUID("bindingId")) {
            if (oldBinding != null && mob.equals(oldBinding.mob)) {
                rebinding = true;
            } else {
                return false;
            }
        }
        List<ItemStack> influenceItems = new ArrayList<>();
        CompoundTag contract = null;
        for (var influence : focus.getDiagram().influences.entrySet()) {
            if (!influence.getValue().equals(focus.getPos())) {
                continue;
            }
            BlockPos sourcePos = influence.getKey();
            if (mob.level.getBlockEntity(sourcePos) instanceof ChalkCircle cc) {
                influenceItems.add(cc.item);
                if (cc.item.is(OtherverseItems.CONTRACT.get()) && cc.item.hasTag() && cc.item.getTag().contains("contract")) {
                    contract = cc.item.getTag().getCompound("contract");
                }
                continue;
            }
            BlockFocus bf = DiagramManager.getOrCreateLevelData(mob.level).allBlockFoci.get(sourcePos);
            if (bf != null) {
                influenceItems.add(bf.getItem());
            }
            BindingInfo sourceBinding = bindingsByPosition.get(sourcePos);
            if (sourceBinding != null && sourceBinding.mob != null) {
                ItemStack idol = MobBindingInfluenceUtils.getIdol(sourceBinding.mob.getType());
                if (idol != null) {
                    influenceItems.add(idol);
                }
            }
        }
        if (influenceItems.isEmpty() || !MobBindingInfluenceUtils.CanBeBound(mob, influenceItems, focus)) {
            if (rebinding) {
                LOGGER.debug("BREAKING BINDING BECAUSE REBINDING");
                breakBinding(oldBinding);
            }
            return false;
        }
        LOGGER.debug("BINDING");
        if (rebinding) {
            return true;
        }
        if (mob.level instanceof ServerLevel sl) {
            BindingInfo binding = new BindingInfo(UUID.randomUUID(), focus.getPos(), mob, sl, new CompoundTag(), DiagramManager.getDimensionHash(sl));
            mob.setPersistenceRequired();
            mob.getPersistentData().putUUID("bindingId", binding.bindingId);
            binding.register();
            LOGGER.debug("APPLYING BINDING");
            applyBinding(binding, mob, false);
            if (contract != null) {
                ContractManager.applyContract(contract, mob, false);
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingDamageEvent event) {
        tryRefreshDiagram(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityHeal(LivingHealEvent event) {
        tryRefreshDiagram(event.getEntity());
    }

    private static void tryRefreshDiagram(LivingEntity entity) {
        if (entity instanceof Mob mob && mob.level instanceof ServerLevel sl) {
            if (mob.getPersistentData().hasUUID("bindingId")) {
                TransientDiagramData data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
                BindingInfo binding = data.bindingsById.get(mob.getPersistentData().getUUID("bindingId"));
                if (binding.getFocus() != null) {
                    DiagramManager.markDiagramActive(sl, binding.getFocus().getDiagram());
                }
                if(mob.isDeadOrDying()){
                    binding.unload();
                    return;
                }
            }
            var focus = DiagramManager.getOrCreateLevelData(sl).allBlockFoci.get(mob.blockPosition());
            if (focus == null) return;
            DiagramManager.markDiagramActive(sl, focus.getDiagram());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (event.getLevel() instanceof ServerLevel sl) {
            if (mob.getPersistentData().hasUUID("bindingId")) {
                TransientDiagramData data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
                applyBinding(data.bindingsById.get(mob.getPersistentData().getUUID("bindingId")), mob, true);
            }
            var loyalTo = mob.getPersistentData().getString("practitioner_loyalty");
            var inSwarm = mob.getPersistentData().getBoolean("part_of_swarm");
            if (!loyalTo.isEmpty()) {
                var player = FamiliarManager.getPlayerFromName(sl, loyalTo);
                enforceLoyalty(player, mob, inSwarm);
            }
            if (mob.getPersistentData().contains("unbound_contract")) {
                applyUnboundContract(mob, false);
            }
        } else {
            var id = event.getEntity().getId();
            var msg = OtherverseClientPacketHandler.waitingMessages.get(id);
            if (msg != null) {
                for(var m : msg) {
                    BindingRenderer.updateBinding(mob, m);
                }
                OtherverseClientPacketHandler.waitingMessages.remove(id);
            }
        }
    }

    public static void applyUnboundContract(Mob mob, boolean fromClick) {
        var didFindGoal = false;
        for (var g : mob.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof BoundGoal bg) {
                bg.loadUnboundContract();
                didFindGoal = true;
                break;
            }
        }
        if(!didFindGoal){
            BoundGoal boundGoal = new BoundGoal(mob, null);
            boundGoal.loadUnboundContract();
            mob.goalSelector.addGoal(-1, boundGoal);
        }
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BindingUpdateMessage(mob, BindingUpdateMessage.BindingUpdateType.CONTRACT, mob.getPersistentData(), !fromClick));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level instanceof ServerLevel sl) {
            if (mob.getPersistentData().hasUUID("bindingId")) {
                TransientDiagramData data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
                data.bindingsById.get(mob.getPersistentData().getUUID("bindingId")).unload();

                if (!BindingManager.getHeldItem(mob).isEmpty()) {
                    Vec3 pos = mob.position();
                    ItemEntity itementity = new ItemEntity(sl, pos.x, pos.y, pos.z, BindingManager.getHeldItem(mob));
                    itementity.setDefaultPickUpDelay();
                    sl.addFreshEntity(itementity);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEnderTeleport(EnderEntity event) {
        if (event.getEntityLiving().getPersistentData().hasUUID("bindingId")) {
            event.setCanceled(true);
        }
    }

    public static void enforceLoyalty(ServerPlayer player, Mob mob, boolean isPartOfSwarm) {
        if (player != null) {
            mob.getPersistentData().putString("practitioner_loyalty", player.getGameProfile().getName());
        }
        mob.getPersistentData().putBoolean("part_of_swarm", isPartOfSwarm);
        BoundGoal boundGoal = new BoundGoal(mob, null);
        boundGoal.tetherToPlayer(player, isPartOfSwarm);
        mob.goalSelector.addGoal(-1, boundGoal);
    }

    @SubscribeEvent
    public static void onDie(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var mob = event.getSource().getEntity();
        if (mob == null) return;
        if (mob.getPersistentData().getString("last_bound_by").equals(sp.getGameProfile().getName())) {
            Otherverse.ADVANCEMENTS.trigger(sp, "karma");
        }
    }

    public static int getSpiritDrain(float maxHealth){
        return (int) Math.ceil(maxHealth / 10);
    }

    public static int getBindingWearInterval(float maxHealth) {
        return 6561 / (int) Math.ceil(maxHealth);
    }

    public static void applyBinding(BindingInfo binding, Mob mob, boolean silent) {
        if (binding == null) {
            mob.getPersistentData().remove("bindingId");
            LOGGER.error("BINDING NOT FOUND");
            return;
        }
        binding.mob = mob;
        binding.mob.setAggressive(false);
        mob.removeEffect(MobEffects.MOVEMENT_SPEED);
        mob.removeEffect(MobEffects.DAMAGE_BOOST);
        for (var goal : mob.goalSelector.getAvailableGoals()) {
            goal.stop();
        }
        BoundGoal boundGoal = new BoundGoal(mob, binding);
        mob.goalSelector.addGoal(-1, boundGoal);
        if (!binding.contract.isEmpty()) {
            boundGoal.applyContract();
        }
        if (mob.level instanceof ServerLevel sl) {
            LOGGER.debug("SENDING BINDING PACKET");
            TransientDiagramData.updateClientBinding(binding);

            var focus = binding.getFocus();
            if (focus != null) {
                var sp = focus.getDiagram().getOwner(sl);
                if (sp != null) {
                    mob.getPersistentData().putString("last_bound_by", sp.getGameProfile().getName());
                    Otherverse.ADVANCEMENTS.trigger(sp, "binding");
                    var t = mob.getType();
                    if (t.equals(EntityType.ENDER_DRAGON)) {
                        Otherverse.ADVANCEMENTS.trigger(sp, "bind_end");
                    } else if (t.equals(EntityType.WITHER)) {
                        Otherverse.ADVANCEMENTS.trigger(sp, "bind_wither");
                    } else if (t.equals(EntityType.WARDEN)) {
                        Otherverse.ADVANCEMENTS.trigger(sp, "bind_warden");
                    }
                }
            }

        } else {
            LOGGER.debug("APPLYING BINDING ON CLIENT LEVEL");
        }
    }

    public static void startAttacking(Mob mob, LivingEntity targetMob) {
        mob.setTarget(targetMob);
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, targetMob);
        mob.getBrain().setMemory(MemoryModuleType.ANGRY_AT, targetMob.getUUID());
        mob.setAggressive(true);
        if (mob instanceof Warden w) w.increaseAngerAt(targetMob);

        for (var goal : mob.goalSelector.getAvailableGoals()) {
            if (BoundGoal.isAttackGoal(goal.getGoal())) {
                if (!goal.isRunning() && goal.canUse()) {
                    goal.start();
                }
                goal.tick();
            }
        }
    }

    public static void breakBinding(BindingInfo binding) {
        Mob mob = binding.mob;
        if (mob == null) {
            binding.unload();
            return;
        }
        Player closest = mob.level.getNearestPlayer(mob, 16);
        if(closest != null) {
            mob.setLastHurtByPlayer(closest);
            mob.setLastHurtByMob(closest);
            startAttacking(mob, closest);
        }
        mob.getPersistentData().remove("bindingId");
        Goal bg = null;
        for (var goal : mob.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof BoundGoal) {
                bg = goal.getGoal();
            }
            if (goal.getGoal() instanceof PanicGoal) {
                goal.getGoal().start();
            }
        }
        if (bg != null) {
            bg.stop();
            mob.goalSelector.removeGoal(bg);
        }
        LOGGER.debug("BINDING BROKEN");
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200));
        binding.unload();
        if (mob.level instanceof ServerLevel sl) {
            LOGGER.error("SENDING BROKEN BINDING PACKET");
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new BindingUpdateMessage(mob, BindingUpdateMessage.BindingUpdateType.BREAK, false));
        } else {
            LOGGER.error("BREAKING BINDING ON CLIENT LEVEL");
        }
    }

    private static boolean isOverrideItem(ItemStack stack) {
        return stack.is(OtherverseItems.FAMILIAR_NAME_TAG.get())
                || stack.is(OtherverseItems.CONTRACT.get())
                || stack.is(Items.SADDLE);
    }

    @SubscribeEvent
    public static void tryGiveItem(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;
        var time = event.getLevel().getGameTime();
        if (time - lastGiveAttempt < 5) return;
        lastGiveAttempt = time;
        var item = event.getItemStack();

        if (item.isEmpty()) {
            FamiliarManager.onInteract(event);
        }

        var target = event.getTarget();
        if (target instanceof EnderDragonPart ep) {
            target = ep.parentMob;
        }

        if (isOverrideItem(event.getItemStack()) && target instanceof LivingEntity le) {
            InteractionResult interactionresult = event.getItemStack().interactLivingEntity(event.getEntity(), le, event.getHand());
            if (interactionresult.consumesAction()) {
                event.setCancellationResult(interactionresult);
                event.setCanceled(true);
                return;
            }
        }

        if (event.getEntity().isCrouching()) return;

        if (event.getEntity().isCreative() && item.is(OtherverseItems.SPAWN_ALTAR.get())) {
            event.setResult(Event.Result.ALLOW);
            ArtifactManager.setEntity(item, target.getType());
            return;
        }

        if (!(target.getPersistentData().hasUUID("bindingId") || target.getPersistentData().contains("unbound_contract"))
                || !(target instanceof Mob mob)) return;

        if (item.is(OtherverseItems.CINNABAR_BLOCK.get())) {
            FleshbindingManager.fleshbindMob(mob, "otherverse:cinnabar_block", (ServerLevel) event.getLevel());
            item.shrink(1);
            return;
        }

        if (ImplementManager.isImplement(item)) return;
        if (!getHeldItem(mob).isEmpty()) {
            ItemEntity itementity = new ItemEntity(mob.level,
                    mob.getX(0.5f), mob.getY(0.5f), mob.getZ(0.5f),
                    getHeldItem(mob).copy());
            itementity.setDefaultPickUpDelay();
            mob.level.addFreshEntity(itementity);
        }
        setHeldItem(mob, item.copy());
        event.getEntity().setItemInHand(event.getHand(), ItemStack.EMPTY);
        event.setResult(Event.Result.ALLOW);
    }

    public static boolean drainsBindings(EntityType<? extends LivingEntity> type) {
        return type.getCategory() == MobCategory.MONSTER && DefaultAttributes.getSupplier(type).getValue(Attributes.MAX_HEALTH) > 20;
    }
}