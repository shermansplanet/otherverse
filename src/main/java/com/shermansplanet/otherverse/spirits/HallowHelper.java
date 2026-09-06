package com.shermansplanet.otherverse.spirits;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherverseConfig;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.BindingOrFleshbinding;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class HallowHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Dictionary<Block, Float> hallowMultipliers = new Hashtable<>();

    static {
        hallowMultipliers.put(Blocks.IRON_BLOCK, 0.333f);
        hallowMultipliers.put(Blocks.GOLD_BLOCK, 0.666f);
        hallowMultipliers.put(Blocks.DIAMOND_BLOCK, 1f);
        hallowMultipliers.put(Blocks.NETHERITE_BLOCK, 3f);
    }

    public static SavedPracticeData createPracticeData(Level level) {
        return new SavedPracticeData(level);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HallowCommand.register(event.getDispatcher());
    }

    public static SavedPracticeData loadPracticeData(CompoundTag tag, Level level) {
        SavedPracticeData data = createPracticeData(level);
        data.load(tag);
        return data;
    }

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event) {
        ShrineHelper.onStartup();
    }

    @SubscribeEvent
    public static void onGrindstoneChange(GrindstoneEvent.OnPlaceItem event) {
        var newstack = event.getTopItem().copy();
        if (newstack.getItem() instanceof SpiritItem) {
            event.setOutput(new ItemStack(OtherverseItems.SPIRIT_TABLET.get(), newstack.getCount()));
            return;
        }
        if (!event.getTopItem().hasTag() || !event.getTopItem().getTag().contains("hallow")) return;
        newstack.removeTagKey("hallow");
        event.setOutput(newstack);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            DiagramManager.tryLoadOverworld(sl);

            SavedPracticeData data = sl.getDataStorage().computeIfAbsent(
                    tag -> loadPracticeData(tag, sl),
                    () -> createPracticeData(sl), "practice");
            DiagramManager.getOrCreateLevelData(sl).savedData = data;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DiagramManager.updatePlayer(sp);
            for (var pos : DiagramManager.getOrCreateLevelData(sp.serverLevel()).getAllPlacedItemPositions()) {
                ShrineHelper.getShrine(sp.serverLevel(), pos);
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DiagramManager.updatePlayer(sp);
        }
    }

    @SubscribeEvent
    static void onGrief(EntityMobGriefingEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le) || BindingManager.isBoundOrContracted(le)) return;
        for (var shrine : ShrineHelper.getShrinesFor(event.getEntity(), Spirits.PROTECTION)) {
            if (!shrine.tryDrain(9)) continue;
            event.setResult(Event.Result.DENY);
            return;
        }
    }

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof LivingEntity le) || BindingManager.isBoundOrContracted(le))
            return;
        for (var shrine : ShrineHelper.getShrinesFor(le, Spirits.PROTECTION)) {
            if (!shrine.tryDrain(9)) continue;
            event.getAffectedBlocks().clear();
            return;
        }
    }

    @SubscribeEvent
    static void onAttack(LivingHurtEvent event) {
        if (event.getEntity() == null) return;
        if (isPlayerOrTamedOrBound(event.getEntity())) {
            for (var shrine : ShrineHelper.getShrinesFor(event.getEntity(), Spirits.PROTECTION)) {
                if (!shrine.tryDrain(Mth.ceil(event.getAmount() * 3))) continue;
                event.setAmount(event.getAmount() / 2);
            }
        }
        var source = event.getSource().getEntity();
        if (source instanceof LivingEntity le && isPlayerOrTamedOrBound(le)) {
            for (var shrine : ShrineHelper.getShrinesFor(le, Spirits.WAR)) {
                if (!shrine.tryDrain(Mth.ceil(event.getAmount() * 3))) continue;
                event.setAmount(event.getAmount() * 2);
            }
        }
    }

    @SubscribeEvent
    static void onDie(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!isPlayerOrTamedOrBound(entity)) return;
        for (var shrine : ShrineHelper.getShrinesFor(entity, Spirits.DEATH)) {
            if (!shrine.tryDrain(444)) continue;
            entity.setHealth(entity.getMaxHealth() / 2);
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    static void onTryMultipleHallow(PlayerInteractEvent.RightClickBlock event) {
        var item = event.getItemStack();
        if (!item.hasTag() || !item.getTag().contains("hallow")) return;
        var pos = event.getPos();
        for (var i = 0; i < 2; i++) {
            if (i == 1 && event.getFace() != null) pos = pos.relative(event.getFace());
            var bs = event.getLevel().getBlockState(pos);
            if (!item.is(bs.getBlock().asItem())) continue;
            if (!(bs.getBlock() instanceof CandleBlock) && !(bs.getBlock() instanceof SlabBlock) && !item.is(Items.TURTLE_EGG) && !item.is(Items.SEA_PICKLE))
                continue;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            sp.sendSystemMessage(Component.literal("Sorry, you can't put hallows in a block that already contains something."));
            return;
        }
    }

    private static boolean isPlayerOrTamedOrBound(LivingEntity entity) {
        return entity != null && (entity.getType() == EntityType.PLAYER || (entity instanceof TamableAnimal ta && ta.isTame()) || BindingManager.isBoundOrContracted(entity));
    }

    @SubscribeEvent
    static void onPreventDamage(ShieldBlockEvent event) {
        var item = event.getEntity().getMainHandItem();
        if (!(item.getItem() instanceof ShieldItem)) {
            item = event.getEntity().getOffhandItem();
        }
        if (item.isEmpty() || !item.hasTag() || !item.getTag().contains("hallow")) return;
        var hallowTag = item.getTag().getCompound("hallow");
        if (!hallowTag.getString("spirit_type").equals("protection")) return;
        int capacity = hallowTag.getInt("capacity");
        int count = hallowTag.getInt("spirit_count");
        if (count >= capacity) return;
        var blocked = (int) event.getBlockedDamage();
        hallowTag.putInt("spirit_count", Math.min(capacity, count + blocked));
    }

    @SubscribeEvent
    static void onPush(PistonEvent.Pre event) {
        if (event.getLevel().isClientSide()) return;
        LOGGER.debug(event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND ? "EXTENDING" : "RETRACTING");
        var structureHelper = event.getStructureHelper();
        if (structureHelper == null) return;
        structureHelper.resolve();
        var data = DiagramManager.getOrCreateLevelData((Level) event.getLevel());
        for (var pos : structureHelper.getToDestroy()) {
            data.removePlacedItemTag(pos);
        }
        var tagsByPosition = new HashMap<BlockPos, CompoundTag>();
        //LOGGER.debug("toPush: " + structureHelper.getToPush().size());
        for (var pos : structureHelper.getToPush()) {
            //LOGGER.debug("pushing: " + pos);
            var tag = data.getPlacedItemTag(pos);
            if (tag == null) continue;
            tagsByPosition.put(pos, tag);
            data.removePlacedItemTag(pos);
        }
        //LOGGER.debug("positions: " + tagsByPosition.size());
        for (var pos : tagsByPosition.keySet()) {
            var dest = pos.relative(structureHelper.getPushDirection());
            data.putPlacedItemTag(dest, tagsByPosition.get(pos));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void trample(BlockEvent.FarmlandTrampleEvent event) {
        var entity = event.getEntity();
        if (entity == null) return;
        for (var spiritType : List.of(Spirits.PROTECTION, Spirits.NATURE)) {
            if (ShrineHelper.getShrinesFor(entity, spiritType).isEmpty()) continue;
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void warDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ItemStack item = attacker.getMainHandItem();
        if (item.isEmpty() || !item.hasTag() || !item.getTag().contains("hallow")) return;
        var hallowTag = item.getTag().getCompound("hallow");
        if (!hallowTag.getString("spirit_type").equals("war")) return;
        int capacity = hallowTag.getInt("capacity");
        int count = hallowTag.getInt("spirit_count");
        if (count >= capacity) return;

        var initialHp = (int) event.getEntity().getHealth();
        var finalHp = (int) Math.max(0, initialHp - event.getAmount());
        var hpDelta = initialHp - finalHp;
        hallowTag.putInt("spirit_count", Math.min(capacity, count + hpDelta));
    }

    private static final float SpiritTabletCutoff = 16f;
    private static final float SpiritTabletHalfway = 555f;
    private static final float SpiritTabletMinEfficiency = 0.1f;
    private static final float SpiritTabletCoeff = SpiritTabletCutoff / (SpiritTabletHalfway - SpiritTabletCutoff);

    public static float getEfficiency(BlockPos tabletPos, BlockPos shrinePos, boolean sameDimension) {
        if (!sameDimension) return SpiritTabletMinEfficiency;
        var dist = tabletPos.getCenter().distanceTo(shrinePos.getCenter());
        dist = (dist - SpiritTabletCutoff) * SpiritTabletCoeff + SpiritTabletCutoff;
        return (float) Math.max(SpiritTabletMinEfficiency, Math.min(1f, SpiritTabletCutoff / dist));
    }

    public static ShrineHelper.Shrine shrineFromTablet(ItemStack stack, SpiritType spiritType) {
        var tag = stack.getTag();
        var pos = new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z"));
        var linkedDimension = tag.getInt("linked_dimension");
        var data = DiagramManager.getOrCreateLevelData(linkedDimension, false);
        return ShrineHelper.getShrine(data.level, pos);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().hasTag()) {
            return;
        }
        CompoundTag tag = event.getItemStack().getTag();

        if (event.getEntity() != null && tag.contains("linked_position_x")) {
            addTabletInfo(event.getToolTip(), event.getEntity().blockPosition(), tag, event.getEntity().level());
            return;
        }

        var entityData = BlockItem.getBlockEntityData(event.getItemStack());
        if (entityData != null && entityData.contains("spawn_altar_type")) {
            var entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityData.getString("spawn_altar_type")));
            event.getToolTip().add(Component.literal("Spawn type: ")
                    .append(entityType.getDescription()));
        }

        if (tag == null || !tag.contains("hallow")) {
            return;
        }
        CompoundTag hallowTag = tag.getCompound("hallow");
        int capacity = hallowTag.getInt("capacity");
        int count = hallowTag.getInt("spirit_count");
        event.getToolTip().add(Component.literal(count + "/" + capacity + " "
                + hallowTag.getString("spirit_type").replace("_", " ")));
    }

    public static void addTabletInfo(List<Component> toolTip, BlockPos referencePos, CompoundTag tag, Level level) {
        var pos = new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z"));
        var linkedDimension = tag.getInt("linked_dimension");
        var data = DiagramManager.getOrCreateLevelData(linkedDimension, true);
        if (data.getPlacedItemTag(pos) == null) {
            toolTip.add(Component.literal("Shrine not found!").withStyle(Style.EMPTY.withColor(0xaa4444)));
            return;
        }
        var spiritLabel = tag.getString("spirit_type");
        var cc = getShrineSpiritCountAndCapacity(data, pos, Spirits.spiritsByLabel.get(spiritLabel));
        var inThisDimension = DiagramManager.getDimensionHash(level) == linkedDimension;
        toolTip.add(Component.literal("Linked to a shrine at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + (inThisDimension ? " in this dimension" : " in another dimension")));
        var spiritCount = Component.literal(String.valueOf(cc.getFirst()));
        var efficiency = getEfficiency(referencePos, pos, inThisDimension);
        var defaultStyle = Style.EMPTY.withStrikethrough(false).withColor(0xffffff);
        if (efficiency < 1) {
            spiritCount = spiritCount.withStyle(Style.EMPTY.withStrikethrough(true).withColor(0xaa4444))
                    .append(Component.literal(String.valueOf(Math.round(efficiency * cc.getFirst()))).withStyle(defaultStyle));
        }
        toolTip.add(spiritCount.append(Component.literal("/" + cc.getSecond() + " " + spiritLabel + " spirits").withStyle(defaultStyle)));
        toolTip.add(Component.literal(Math.round(efficiency * 100) + "% efficiency"));
    }

    public static boolean tryHallow(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        BlockState blockBelow = level.getBlockState(circle.getBlockPos().below());
        Float coeff = hallowMultipliers.get(blockBelow.getBlock());
        if (coeff == null) {
            return false;
        }
        if (!circle.item.hasTag() || !circle.item.getTag().contains("hallow")) {
            var spirits = SpiritLabeler.getSpiritsFor(circle.item.getItem());
            if (spirits == null) {
                return false;
            }
            if (diagram.trySpendPower(level, circle.getBlockPos(), (int) (9 * coeff), new HashSet<>())) {
                CompoundTag tag = circle.item.getOrCreateTag();
                CompoundTag hallowTag = new CompoundTag();
                float capacity = 0f;
                for (int spiritAmount : spirits.values()) {
                    capacity += coeff * spiritAmount;
                }

                var implementData = ImplementManager.getImplementData(circle);
                if (!implementData.isEmpty()
                        && ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(implementData.getString("item"))) == Items.BUCKET) {
                    capacity *= ImplementManager.BUCKET_BONUS;
                }

                hallowTag.putInt("capacity", (int) capacity);
                hallowTag.putInt("spirit_count", 0);
                tag.put("hallow", hallowTag);
                addFakeEnchantment(tag);
                tryFillHallow(level, circle, diagram);
                circle.markUpdated();
                Otherverse.ADVANCEMENTS.trigger(diagram.getOwner(level), "hallow");
                return true;
            }
        }
        return false;
    }

    private static long lastClick;

    @SubscribeEvent
    public static void makeShrine(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var time = event.getLevel().getGameTime();
        if (time - lastClick < 5) return;
        lastClick = time;
        if (!event.getEntity().isShiftKeyDown()) return;
        if (!event.getEntity().getMainHandItem().isEmpty() || !event.getEntity().getOffhandItem().isEmpty()) return;
        var data = DiagramManager.getOrCreateLevelData(event.getLevel());
        var tag = data.getPlacedItemTag(event.getPos());
        if (tag == null) return;
        var isShrine = tag.contains("shrine");
        if (!event.getEntity().getAbilities().instabuild)
            event.getEntity().hurt(event.getEntity().damageSources().magic(), 3);
        for (var pos : ShrineHelper.getAllHallows(event.getPos(), tag.getString("spirit_type"), data)) {
            tag = data.getPlacedItemTag(pos);
            if (isShrine) {
                tag.remove("shrine");
            } else {
                tag.putBoolean("shrine", true);
            }
            data.putPlacedItemTag(pos, tag);
            var focus = data.allBlockFoci.get(pos);
            if (focus != null && event.getLevel() instanceof ServerLevel sl)
                DiagramManager.markDiagramActive(sl, focus.getDiagram());
        }
        if (isShrine) {
            var st = Spirits.spiritsByLabel.get(tag.getString("spirit_type"));
            for (var i = 0; i < 10; i++) {
                SpiritAffinityTracker.decreaseAffinity(st, sp);
            }
        }
        if (event.getLevel() instanceof ServerLevel sl) {
            ShrineHelper.getShrine(sl, event.getPos());
            var r = sl.getRandom();
            var v1 = event.getHitVec().getLocation();
            for (var i = 0; i < 8; i++) {
                sl.sendParticles(ParticleTypes.INSTANT_EFFECT,
                        v1.x, v1.y, v1.z, 1,
                        r.nextFloat() - 0.5f,
                        r.nextFloat() - 0.5f,
                        r.nextFloat() - 0.5f,
                        0.15);
            }
        }
        event.setUseBlock(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void spiritTransferClick(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        if (!(event.getLevel() instanceof ServerLevel sl) || stack.getItem() instanceof BlockItem) return;
        var practiceData = DiagramManager.getOrCreateLevelData(sl);
        var blockTag = practiceData.getPlacedItemTag(event.getPos());
        if (blockTag == null || !blockTag.contains("spirit_type")) return;
        var blockSpiritType = Spirits.spiritsByLabel.get(blockTag.getString("spirit_type"));
        var isBucketImplement = stack.is(Items.BUCKET) && ImplementManager.isImplement(stack);
        if (stack.is(OtherverseItems.SPIRIT_TABLET.get()) && blockTag.contains("shrine")) {
            stack.shrink(1);
            var newstack = new ItemStack(Spirits.spiritItems.get(blockSpiritType).get(), 1);
            newstack.getOrCreateTag().putInt("linked_position_x", event.getPos().getX());
            newstack.getOrCreateTag().putInt("linked_position_y", event.getPos().getY());
            newstack.getOrCreateTag().putInt("linked_position_z", event.getPos().getZ());
            newstack.getOrCreateTag().putInt("linked_dimension", DiagramManager.getDimensionHash(event.getLevel()));
            newstack.getOrCreateTag().putString("spirit_type", blockSpiritType.label());
            var player = event.getEntity();
            if (stack.getCount() == 0) {
                player.getInventory().removeItem(stack);
            }
            if (!player.addItem(newstack)) {
                player.drop(newstack, false);
            }
            return;
        }
        var itemSpiritType = HallowHelper.getSpiritType(stack);
        if (itemSpiritType == null && !isBucketImplement) return;
        var itemTag = stack.getTag();
        if (itemSpiritType == null && blockSpiritType != null) {
            itemSpiritType = blockSpiritType;
            HallowHelper.addFakeEnchantment(itemTag);
            var hallowTag = new CompoundTag();
            hallowTag.putInt("capacity", ImplementManager.BUCKET_CAPACITY);
            hallowTag.putInt("spirit_count", 0);
            hallowTag.putString("spirit_type", blockSpiritType.label());
            itemTag.put("hallow", hallowTag);
        }
        if (itemSpiritType == blockSpiritType) {
            var hallowTag = itemTag.getCompound("hallow");
            var spiritCount = hallowTag.getInt("spirit_count");
            var spiritCapacity = hallowTag.getInt("capacity");

            var amountAndCapacity = getShrineSpiritCountAndCapacity(sl, event.getPos(), blockSpiritType);
            var otherAmount = amountAndCapacity.getFirst();
            var otherCapacity = amountAndCapacity.getSecond();
            var depositing = otherAmount == 0 || spiritCount == spiritCapacity || event.getEntity().isShiftKeyDown();
            if (depositing) {
                spiritCount -= fillBlockHallow(sl, event.getPos(), itemSpiritType,
                        Math.min(otherCapacity, spiritCount), false, false);
            } else {
                spiritCount += drainBlockHallow(sl, event.getPos(), itemSpiritType,
                        Math.min(spiritCapacity - spiritCount, otherAmount), false, false);
            }
            hallowTag.putInt("spirit_count", spiritCount);
            if (spiritCount == 0 && isBucketImplement) {
                itemTag.remove("hallow");
                ListTag listtag = itemTag.getList("Enchantments", 10);
                listtag.removeIf(tag -> {
                    if (!(tag instanceof CompoundTag ct)) return false;
                    return ct.getString("id").equals("Hallow");
                });
            }
            practiceData.putPlacedItemTag(event.getPos(), blockTag);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /*@SubscribeEvent
    public static void useHallow(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getEntity().isShiftKeyDown()) return;
        if(!event.getItemStack().hasTag()) return;
        var hallowTag = event.getItemStack().getTag().getCompound("hallow");
        if(hallowTag.isEmpty()) return;
    }*/

    @SubscribeEvent
    public static void recolorChalk(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var itemstack = event.getItemStack();
        var state = player.level().getBlockState(event.getPos());
        if (state.is(OtherverseBlocks.CHALK_LINE.get()) && player.isShiftKeyDown()
                && itemstack.hasTag() && itemstack.getTag().contains("hallow")) {
            var hallowTag = itemstack.getTag().getCompound("hallow");
            var spiritCount = hallowTag.getInt("spirit_count");
            if (spiritCount <= 0) return;
            var spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
            for (var dyeColor : Spirits.colorsByDye.entrySet()) {
                if (dyeColor.getValue() != spiritType) continue;
                var newstate = ChalkLineBlock.getConnectionState(player.level(), event.getPos(), state.setValue(ChalkLineBlock.color, dyeColor.getKey()));
                player.level().setBlockAndUpdate(event.getPos(), newstate);
                ChalkLineBlock.refreshNeighborLines(player.level(), event.getPos());
                if (player.level() instanceof ServerLevel sl) {
                    DiagramManager.OnDiagramBlockChanged(sl, event.getPos(), DiagramManager.BlockUpdateType.ADDED);
                }
                hallowTag.putInt("spirit_count", spiritCount - 1);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void mobDie(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        EntityType<? extends LivingEntity> type = (EntityType<? extends LivingEntity>) event.getEntity().getType();
        var ct = event.getEntity().getPersistentData().getString("construct_type");
        var baseType = MobBindingInfluenceUtils.mobSpirits.get(type);
        var spiritType = ct.isEmpty() ? baseType == null ? null : baseType.label() : ct;
        if (spiritType == null) return;
        var data = DiagramManager.getOrCreateLevelData(sl);
        for (var offset : new float[]{0f, -1f}) {
            var pos = BlockPos.containing(event.getEntity().position().add(0, offset, 0));
            var tag = data.getPlacedItemTag(pos);
            if (tag == null) continue;
            if (!tag.getString("spirit_type").equals(spiritType)) continue;
            int hp = Math.round((float) DefaultAttributes.getSupplier(type).getValue(Attributes.MAX_HEALTH) / 3f);
            tag.putInt("capacity", tag.getInt("capacity") + hp);
            data.putPlacedItemTag(pos, tag);
            return;
        }
    }

    public static void addFakeEnchantment(CompoundTag tag) {
        /*if (!tag.contains("Enchantments", 9)) {
            tag.put("Enchantments", new ListTag());
        }
        ListTag listtag = tag.getList("Enchantments", 10);
        CompoundTag fakeEnchantment = new CompoundTag();
        fakeEnchantment.putString("id", "Hallow");
        fakeEnchantment.putInt("lvl", 1);
        listtag.add(fakeEnchantment);*/
    }

    public static void applySpiritType(Level level, IFocus focus, Diagram diagram,
                                       CompoundTag hallowTag, List<IFocus> influences) {
        List<SpiritType> possibleSpirits = new ArrayList<>(
                SpiritLabeler.getSpiritsFor(focus.getItem().getItem()).keySet());
        Set<SpiritType> originalSpirits = new HashSet<>(possibleSpirits);
        for (IFocus sourceFocus : influences) {
            Item item = sourceFocus.getItem().getItem();
            if (sourceFocus.getItem().isEmpty() || item == Items.AIR || item == OtherverseItems.CHALK.get() || item == OtherverseItems.SPINDLE_BLOODY.get()) {
                continue;
            }
            var spiritCount = SpiritLabeler.getSpiritsFor(item);
            if (spiritCount == null) {
                continue;
            }
            Set<SpiritType> otherSpirits = spiritCount.keySet();
            if (otherSpirits.isEmpty()) {
                continue;
            }
            for (SpiritType spiritType : originalSpirits) {
                if (!otherSpirits.contains(spiritType)) {
                    possibleSpirits.remove(spiritType);
                }
            }
            if (possibleSpirits.isEmpty()) {
                possibleSpirits = new ArrayList<>(originalSpirits);
                break;
            }
        }

        var colorSet = new HashSet<>(Arrays.stream(Spirits.colorSpiritTypes).toList());
        var newPossibleSpirits = new ArrayList<SpiritType>();
        for (var spirit : possibleSpirits) {
            if (!colorSet.contains(spirit)) {
                newPossibleSpirits.add(spirit);
            }
        }
        if (!newPossibleSpirits.isEmpty()) possibleSpirits = newPossibleSpirits;

        hallowTag.putString("spirit_type",
                possibleSpirits.get(level.getRandom().nextInt(possibleSpirits.size())).label());
    }

    public static boolean canFill(IFocus sink, IFocus source, SpiritType spiritType) {
        ItemStack sourceItem = source.getItem();
        boolean sourceIsHallow = sourceItem.hasTag() && sourceItem.getTag().contains("hallow");
        var isOverflowable = ShrineHelper.isOverflowable(spiritType);

        if (spiritType == null) {
            LOGGER.error("NULL SPIRIT TYPE");
            return false;
        }

        var sinkIsTablet = sink.getItem().getItem() instanceof SpiritItem && sink.getItem().getTag().contains("linked_position_x");
        var sourceIsTablet = sourceItem.getItem() instanceof SpiritItem && sourceItem.hasTag() && sourceItem.getTag().contains("linked_position_x");

        var hallowTag = sinkIsTablet
                ? tabletToHallow(sink.getItem().getTag())
                : sink.getItem().getTag().getCompound("hallow");

        if (hallowTag == null) return false;

        if (!isOverflowable && sink.getHallowCapacity(spiritType) <= 0) {
            return false;
        }

        if (sourceItem.is(OtherverseItems.DEMESNE_BEACON.get())) {
            var demesne = DemesnesManager.getData((ServerLevel) source.getFocusLevel(), source.getPos());
            if (demesne != null && demesne.favoredSpirits.contains(hallowTag.getString("spirit_type"))) {
                return true;
            }
        }

        if (sourceIsHallow || sourceIsTablet) {
            var sourceTag = sourceIsTablet ? tabletToHallow(sourceItem.getTag()) : sourceItem.getTag().getCompound("hallow");
            if (sourceTag == null) return false;
            if (!sourceTag.getString("spirit_type").equals(spiritType.label()))
                return false;
            if (sourceIsTablet || source.isBlock()) {
                return isOverflowable || getShrineSpiritCount(source, spiritType, sourceIsTablet) > 0;
            } else {
                return sourceTag.getInt("spirit_count") > 0;
            }
        }

        if (source.getFocusLevel() instanceof ServerLevel sl) {
            if (sourceItem.is(OtherverseItems.IDOL.get())) {
                var et = IdolItem.getType(sourceItem);
                if (MobBindingInfluenceUtils.mobSpirits.get(et) != spiritType) return false;
                var binding = BindingOrFleshbinding.getFromPosition(sl, source.getPos());
                if (binding == null) return false;
                if (hallowTag.contains("shrine") && (spiritType == Spirits.FLESH || spiritType == Spirits.TECH))
                    return false;
                return binding.getHealth() > 1;
            }

            if (source instanceof ChalkCircle cc && sourceItem.is(OtherverseItems.SPINDLE_BLOODY.get())) {
                var binding = BindingOrFleshbinding.getFromSpindle(cc);
                if (binding == null) return false;
                if (binding.mob == null) return false;
                if (MobBindingInfluenceUtils.mobSpirits.get(binding.mob.getType()) != spiritType) return false;
                return binding.mob.getHealth() > 1;
            }
        }

        var spirits = SpiritLabeler.getSpiritsFor(sourceItem.getItem());
        return spirits != null && spirits.containsKey(spiritType);
    }

    public static CompoundTag tabletToHallow(CompoundTag tag) {
        return DiagramManager.getOrCreateLevelData(tag.getInt("linked_dimension"), false)
                .getPlacedItemTag(new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z")));
    }

    public static int getShrineSpiritCount(IFocus source, SpiritType spiritType, boolean sourceIsTablet) {
        var tag = source.getItem().getTag();
        var data = sourceIsTablet ? DiagramManager.getOrCreateLevelData(tag.getInt("linked_dimension"), false) : DiagramManager.getOrCreateLevelData(source.getFocusLevel());
        var total = 0;
        for (BlockPos sourcePos : ShrineHelper.getAllHallows(sourceIsTablet
                ? new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z"))
                : source.getPos(), spiritType, data)) {
            var ht = data.getPlacedItemTag(sourcePos);
            total += ht.getInt("spirit_count");
        }
        return total;
    }

    public static int getShrineSpiritCountFromTablet(CompoundTag tag, SpiritType spiritType) {
        var data = DiagramManager.getOrCreateLevelData(tag.getInt("linked_dimension"), false);
        var total = 0;
        for (BlockPos sourcePos : ShrineHelper.getAllHallows(
                new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z")),
                spiritType, data)) {
            var ht = data.getPlacedItemTag(sourcePos);
            total += ht.getInt("spirit_count");
        }
        return total;
    }

    public static Pair<Integer, Integer> getShrineSpiritCountAndCapacity(Level level, BlockPos pos, SpiritType spiritType) {
        return getShrineSpiritCountAndCapacity(DiagramManager.getOrCreateLevelData(level), pos, spiritType);
    }

    public static Pair<Integer, Integer> getShrineSpiritCountAndCapacity(TransientDiagramData data, BlockPos pos, SpiritType spiritType) {
        var count = 0;
        var cap = 0;
        for (BlockPos sourcePos : ShrineHelper.getAllHallows(pos, spiritType, data)) {
            var ht = data.getPlacedItemTag(sourcePos);
            count += ht.getInt("spirit_count");
            cap += ht.getInt("capacity");
        }
        return Pair.of(count, cap);
    }

    public static void tryFillHallow(ServerLevel level, IFocus focus, Diagram diagram) {
        ItemStack item = focus.getItem();
        if (!item.hasTag()) return;
        var isTablet = item.getItem() instanceof SpiritItem && item.getTag().contains("linked_position_x");
        SpiritType spiritType;
        int spiritCount;
        int capacity;
        List<IFocus> influences = new ArrayList<>();
        BlockPos targetPos = focus.getPos();
        for (BlockPos pos : diagram.itemFocusPositions) {
            if (targetPos.equals(diagram.influences.get(pos))
                    && level.getBlockEntity(pos) instanceof ChalkCircle cc) {
                influences.add(cc);
            }
        }
        for (BlockPos pos : diagram.blockFocusPositions) {
            if (targetPos.equals(diagram.influences.get(pos))) {
                influences.add(DiagramManager.getOrCreateLevelData(level).allBlockFoci.get(pos));
            }
        }
        if (!isTablet) {
            if (!item.getTag().contains("hallow")) {
                return;
            }
            CompoundTag hallowTag = item.getTag().getCompound("hallow");
            if (!hallowTag.contains("spirit_type")) {
                applySpiritType(level, focus, diagram, hallowTag, influences);
            }
            var spiritTypeString = hallowTag.getString("spirit_type");
            spiritType = Spirits.spiritsByLabel.get(spiritTypeString);
            spiritCount = hallowTag.getInt("spirit_count");
            capacity = hallowTag.getInt("capacity");
        } else {
            var tag = item.getTag();
            spiritType = Spirits.spiritsByLabel.get(tag.getString("spirit_type"));
            var pos = new BlockPos(tag.getInt("linked_position_x"), tag.getInt("linked_position_y"), tag.getInt("linked_position_z"));
            var linkedDimension = tag.getInt("linked_dimension");
            var data = DiagramManager.getOrCreateLevelData(linkedDimension, true);
            if (data.getPlacedItemTag(pos) == null) {
                return;
            }
            var spiritLabel = tag.getString("spirit_type");
            var cc = getShrineSpiritCountAndCapacity(data, pos, Spirits.spiritsByLabel.get(spiritLabel));
            spiritCount = cc.getFirst();
            capacity = cc.getSecond();
        }

        var willOverflow = false;
        if (spiritCount >= capacity) {
            if (ShrineHelper.isOverflowable(spiritType) && (isTablet || focus.isBlock())) {
                willOverflow = true;
            } else {
                return;
            }
        }

        for (IFocus sourceFocus : influences) {
            if (sourceFocus.getProcess() != null) continue;
            if (!canFill(focus, sourceFocus, spiritType)) continue;
            var sourceItem = sourceFocus.getItem();
            var sourceIsTablet = sourceItem.getItem() instanceof SpiritItem && sourceItem.hasTag() && sourceItem.getTag().contains("linked_position_x");
            if (willOverflow && sourceItem.hasTag()) {
                if (sourceIsTablet) {
                    if (getShrineSpiritCount(sourceFocus, spiritType, true) <= 0) {
                        continue;
                    }
                } else if (sourceItem.getTag().contains("hallow")) {
                    var hallowTag = sourceItem.getTag().getCompound("hallow");
                    if (hallowTag.getInt("spirit_count") <= 0) {
                        continue;
                    }
                }
            }
            if (sourceFocus.getItem().is(Items.BEDROCK) && !OtherverseConfig.BEDROCK_REMOVAL.get()) continue;

            new SpiritTransfer(focus, sourceFocus, SpiritAffinityTracker.getTransferDuration(focus.getDiagram().getOwnerName(), spiritType));
        }
    }

    public static SpiritType getSpiritType(ItemStack item) {
        if (!item.hasTag() || !item.getTag().contains("hallow")) return null;
        CompoundTag hallowTag = item.getTag().getCompound("hallow");
        return Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
    }

    public static boolean tryDrainHallow(ItemStack item, Player player, SpiritType spiritType, int spiritAmount) {
        if (!item.hasTag() || !item.getTag().contains("hallow")) return false;
        CompoundTag hallowTag = item.getTag().getCompound("hallow");
        if (spiritType != Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"))) return false;
        int count = hallowTag.getInt("spirit_count");
        if (count < spiritAmount) return false;
        if (item.getCount() == 1) {
            hallowTag.putInt("spirit_count", count - spiritAmount);
        } else {
            var newItem = item.split(1);
            if (!player.addItem(newItem)) {
                player.drop(newItem, false);
            }
            newItem.getTag().getCompound("hallow").putInt("spirit_count", count - spiritAmount);
        }
        return true;
    }

    public static int drainBlockHallow(Level level, BlockPos pos, SpiritType spiritType, int price, boolean mustMeetFullPrice, boolean simulate) {
        var data = DiagramManager.getOrCreateLevelData(level);
        var hallowPositions = ShrineHelper.getAllHallows(pos, spiritType, data);
        if (hallowPositions.isEmpty()) return 0;

        var drainPositions = new HashMap<BlockPos, Integer>();

        var remainingPrice = price;
        for (BlockPos sourcePos : hallowPositions) {
            if (remainingPrice > 0) {
                var ht = data.getPlacedItemTag(sourcePos);
                var count = ht.getInt("spirit_count");
                count = Math.min(count, remainingPrice);
                if (count == 0) continue;
                drainPositions.put(sourcePos, count);
                remainingPrice -= count;
            }
        }

        if (remainingPrice == price) {
            if (price == Integer.MAX_VALUE) {
                return 0;
            }
            return ShrineHelper.onOverdrawOrOverflow(level, pos, spiritType, price, true, simulate);
        }

        if (mustMeetFullPrice && remainingPrice > 0) {
            return 0;
        }

        if (!simulate) {
            for (var drainPosition : drainPositions.entrySet()) {
                var shrineTag = data.getPlacedItemTag(drainPosition.getKey());
                shrineTag.putInt("spirit_count", shrineTag.getInt("spirit_count") - drainPosition.getValue());
                data.putPlacedItemTag(drainPosition.getKey(), shrineTag);
                var otherFocus = data.allBlockFoci.get(drainPosition.getKey());
                if (otherFocus == null || !(level instanceof ServerLevel sl)) continue;
                DiagramManager.markDiagramActive(sl, otherFocus.getDiagram());
            }
        }

        return price - remainingPrice;
    }

    public static int fillBlockHallow(Level level, BlockPos blockPos, SpiritType spiritType, int amount, boolean mustAcceptAll, boolean simulate) {
        var data = DiagramManager.getOrCreateLevelData(level);
        var hallowPositions = ShrineHelper.getAllHallows(blockPos, spiritType, data);
        if (hallowPositions.isEmpty()) return 0;

        var drainPositions = new HashMap<BlockPos, Integer>();

        var remainingAmount = amount;
        for (BlockPos sourcePos : hallowPositions) {
            if (remainingAmount > 0) {
                var ht = data.getPlacedItemTag(sourcePos);
                var remainingCapacity = Math.max(0, ht.getInt("capacity") - ht.getInt("spirit_count"));
                var transferAmount = Math.min(remainingCapacity, remainingAmount);
                drainPositions.put(sourcePos, transferAmount);
                remainingAmount -= transferAmount;
                if (spiritType == Spirits.TECH && ht.contains("shrine") && level.hasNeighborSignal(blockPos)) {
                    return 0;
                }
            }
        }

        if (!simulate && remainingAmount < amount) {
            for (BlockPos sourcePos : hallowPositions) {
                var otherFocus = data.allBlockFoci.get(sourcePos);
                if (otherFocus == null || !(level instanceof ServerLevel sl)) continue;
                DiagramManager.markDiagramActive(sl, otherFocus.getDiagram());
            }
        }

        if (mustAcceptAll && remainingAmount > 0) {
            return 0;
        }

        if (!simulate) {
            for (var drainPosition : drainPositions.entrySet()) {
                var shrineTag = data.getPlacedItemTag(drainPosition.getKey());
                shrineTag.putInt("spirit_count", shrineTag.getInt("spirit_count") + drainPosition.getValue());
                data.putPlacedItemTag(drainPosition.getKey(), shrineTag);
            }
        }

        return amount - remainingAmount;
    }
}
