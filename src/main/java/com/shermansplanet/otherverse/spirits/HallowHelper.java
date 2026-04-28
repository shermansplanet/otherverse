package com.shermansplanet.otherverse.spirits;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
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
        if (!event.getTopItem().hasTag() || !event.getTopItem().getTag().contains("hallow")) return;
        var newstack = event.getTopItem().copy();
        newstack.getTag().remove("hallow");
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
        if (event.getEntity().level() instanceof ServerLevel sl) {
            DiagramManager.getOrCreateLevelData(sl).retryUpdateClient();
            for (var pos : DiagramManager.getOrCreateLevelData(sl).getAllPlacedItemPositions()) {
                ShrineHelper.getShrine(sl, pos);
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity().level() instanceof ServerLevel sl) {
            DiagramManager.getOrCreateLevelData(sl).retryUpdateClient();
        }
    }

    @SubscribeEvent
    static void onGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.CREEPER) return;
        var levelData = DiagramManager.getOrCreateLevelData(event.getEntity().level());
        for (var shrine : ShrineHelper.getShrinesFor(event.getEntity(), Spirits.PROTECTION)) {
            if (!shrine.tryDrain(9, levelData)) continue;
            event.setResult(Event.Result.DENY);
            return;
        }
    }

    @SubscribeEvent
    static void onAttack(LivingHurtEvent event) {
        if (event.getEntity() == null) return;
        var levelData = DiagramManager.getOrCreateLevelData(event.getEntity().level());
        for (var shrine : ShrineHelper.getShrinesFor(event.getEntity(), Spirits.PROTECTION)) {
            if (!shrine.tryDrain(Mth.ceil(event.getAmount() * 3), levelData)) continue;
            event.setAmount(event.getAmount() / 2);
        }
        for (var shrine : ShrineHelper.getShrinesFor(event.getEntity(), Spirits.WAR)) {
            if (!shrine.tryDrain(Mth.ceil(event.getAmount() * 3), levelData)) continue;
            event.setAmount(event.getAmount() * 2);
        }
    }

    @SubscribeEvent
    static void onDie(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (entity == null) return;
        var levelData = DiagramManager.getOrCreateLevelData(entity.level());
        for (var shrine : ShrineHelper.getShrinesFor(entity, Spirits.DEATH)) {
            if (!shrine.tryDrain(444, levelData)) continue;
            entity.setHealth(1);
            entity.removeAllEffects();
            event.setCanceled(true);
            return;
        }
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

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().hasTag()) {
            return;
        }
        CompoundTag tag = event.getItemStack().getTag();

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
            if (diagram.trySpendPower(level, circle.getBlockPos(), (int) (10 * coeff), new HashSet<>())) {
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
                LOGGER.debug("DIAGRAM SUCCESS: HALLOWING");
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void makeShrine(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().isEmpty() || !event.getEntity().isShiftKeyDown()) return;
        var data = DiagramManager.getOrCreateLevelData(event.getLevel());
        var tag = data.getPlacedItemTag(event.getPos());
        if (tag == null || tag.contains("shrine")) return;
        if (!event.getEntity().getAbilities().instabuild) event.getEntity().hurt(event.getEntity().damageSources().magic(), 3);
        for (var pos : ShrineHelper.getAllHallows(event.getPos(), tag.getString("spirit_type"), data)) {
            tag = data.getPlacedItemTag(pos);
            tag.putBoolean("shrine", true);
            data.putPlacedItemTag(pos, tag);
            var focus = data.allBlockFoci.get(pos);
            if (focus != null && event.getLevel() instanceof ServerLevel sl)
                DiagramManager.markDiagramActive(sl, focus.getDiagram());
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
        var itemSpiritType = HallowHelper.getSpiritType(stack);
        var isBucketImplement = stack.is(Items.BUCKET) && ImplementManager.isImplement(stack);
        var isSpiritTablet = stack.is(OtherverseItems.SPIRIT_TABLET.get()) && blockTag.contains("shrine");
        if (itemSpiritType == null && !isBucketImplement && !isSpiritTablet) return;
        var blockSpiritType = Spirits.spiritsByLabel.get(blockTag.getString("spirit_type"));
        var itemTag = stack.getTag();
        if (itemSpiritType == null && blockSpiritType != null) {
            itemSpiritType = blockSpiritType;
            HallowHelper.addFakeEnchantment(itemTag);
            var hallowTag = new CompoundTag();
            hallowTag.putInt("capacity", ImplementManager.BUCKET_CAPACITY);
            hallowTag.putInt("spirit_count", 0);
            hallowTag.putString("spirit_type", blockSpiritType.label());
            if (isSpiritTablet) {
                var drained = drainBlockHallow(sl, event.getPos(), itemSpiritType, Integer.MAX_VALUE, false, false);
                if (drained == 0) return;
                hallowTag.putInt("capacity", drained);
                stack.shrink(1);
                var newstack = new ItemStack(Spirits.spiritItems.get(itemSpiritType).get(), 1);
                newstack.getOrCreateTag().put("hallow", hallowTag);
                var player = event.getEntity();
                if (stack.getCount() == 0) {
                    player.getInventory().removeItem(stack);
                }
                if (!player.addItem(newstack)) {
                    player.drop(newstack, false);
                }
                return;
            }
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
        for(var offset : new float[]{0f,-1f}) {
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

        var hallowTag = sink.getItem().getTag().getCompound("hallow");

        if (!isOverflowable && sink.getHallowCapacity(spiritType) <= 0) {
            return false;
        }

        if (sourceItem.is(OtherverseItems.DEMESNE_BEACON.get())) {
            var demesne = DemesnesManager.getData((ServerLevel) source.getFocusLevel(), source.getPos());
            if (demesne != null && demesne.favoredSpirits.contains(hallowTag.getString("spirit_type"))) {
                return true;
            }
        }

        if (sourceIsHallow) {
            var sourceTag = sourceItem.getTag().getCompound("hallow");
            if (!sourceTag.getString("spirit_type").equals(spiritType.label()))
                return false;
            if (source.isBlock()) {
                return isOverflowable || getShrineSpiritCount(source, spiritType) > 0;
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

    public static int getShrineSpiritCount(IFocus source, SpiritType spiritType) {
        var level = source.getFocusLevel();
        var data = DiagramManager.getOrCreateLevelData(level);
        var total = 0;
        for (BlockPos sourcePos : ShrineHelper.getAllHallows(source.getPos(), spiritType, data)) {
            var ht = data.getPlacedItemTag(sourcePos);
            total += ht.getInt("spirit_count");
        }
        return total;
    }

    public static Pair<Integer, Integer> getShrineSpiritCountAndCapacity(Level level, BlockPos pos, SpiritType spiritType) {
        var data = DiagramManager.getOrCreateLevelData(level);
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
        if (!item.hasTag() || !item.getTag().contains("hallow")) {
            return;
        }
        CompoundTag hallowTag = item.getTag().getCompound("hallow");

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

        if (!hallowTag.contains("spirit_type")) {
            applySpiritType(level, focus, diagram, hallowTag, influences);
        }

        var spiritTypeString = hallowTag.getString("spirit_type");
        SpiritType spiritType = Spirits.spiritsByLabel.get(spiritTypeString);

        var willOverflow = false;
        int spiritCount = hallowTag.getInt("spirit_count");
        int capacity = hallowTag.getInt("capacity");
        if (spiritCount >= capacity) {
            if (ShrineHelper.isOverflowable(spiritType) && focus.isBlock()) {
                willOverflow = true;
            } else {
                return;
            }
        }

        for (IFocus sourceFocus : influences) {
            if (sourceFocus.getProcess() != null) continue;
            if (!canFill(focus, sourceFocus, spiritType)) continue;
            var sourceItem = sourceFocus.getItem();
            if (willOverflow && sourceItem.hasTag() && sourceItem.getTag().contains("hallow")
                    && sourceItem.getTag().getCompound("hallow").getInt("spirit_count") <= 0) continue;
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

        var newItem = item.split(1);
        if (item.getCount() == 0) {
            player.getInventory().removeItem(item);
        }
        if (!player.addItem(newItem)) {
            player.drop(newItem, false);
        }
        hallowTag = newItem.getTag().getCompound("hallow");
        hallowTag.putInt("spirit_count", count - spiritAmount);
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
