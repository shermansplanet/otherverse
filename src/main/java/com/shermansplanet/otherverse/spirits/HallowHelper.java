package com.shermansplanet.otherverse.spirits;

import com.ibm.icu.impl.Pair;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
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
        if (event.getEntity().getLevel() instanceof ServerLevel sl) {
            DiagramManager.getOrCreateLevelData(sl).retryUpdateClient();
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity().getLevel() instanceof ServerLevel sl) {
            DiagramManager.getOrCreateLevelData(sl).retryUpdateClient();
        }
    }

    @SubscribeEvent
    static void onGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() == null) return;
        var data = DiagramManager.getOrCreateLevelData(event.getEntity().level);
        var mobPos = event.getEntity().blockPosition();
        for (var pos : data.getAllPlacedItemPositions()) {
            var tag = data.getPlacedItemTag(pos);
            if (!tag.getString("spirit_type").equals("protection")) continue;
            var radius = tag.getInt("capacity") / 2;
            var dist = mobPos.distSqr(pos);
            if (dist > radius * radius) continue;
            var count = tag.getInt("spirit_count");
            if (count < 9) continue;
            tag.putInt("spirit_count", count - 9);
            event.setCanceled(true);
            break;
        }
    }

    @SubscribeEvent
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
        LOGGER.debug("Current health: " + event.getEntity().getHealth() + ", -" + hpDelta);
        hallowTag.putInt("spirit_count", count + (int) hpDelta);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().hasTag()) {
            return;
        }
        CompoundTag tag = event.getItemStack().getTag();

        var entityData = BlockItem.getBlockEntityData(event.getItemStack());
        if (entityData != null && entityData.contains("spawn_altar_type")) {
            var entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityData.getString("spawn_altar_type")));
            event.getToolTip().add(Component.literal("Spawn type: ")
                    .append(entityType.getDescription()));
        }

        if (tag == null || !tag.contains("hallow")) {
            return;
        }
        CompoundTag hallowTag = tag.getCompound("hallow");
        int capacity = hallowTag.getInt("capacity");
        int count = hallowTag.getInt("spirit_count");
        event.getToolTip().add(Component.literal("Hallow: " + count + "/" + capacity + " "
                + hallowTag.getString("spirit_type").replace("_", " ") + " spirits"));
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
                        && ForgeRegistries.ITEMS.getValue(new ResourceLocation(implementData.getString("item"))) == Items.BUCKET) {
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
    public static void recolorChalk(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var itemstack = event.getItemStack();
        var state = player.level.getBlockState(event.getPos());
        if (state.is(OtherverseBlocks.CHALK_LINE.get()) && player.isShiftKeyDown()
                && itemstack.hasTag() && itemstack.getTag().contains("hallow")) {
            var hallowTag = itemstack.getTag().getCompound("hallow");
            var spiritCount = hallowTag.getInt("spirit_count");
            if (spiritCount <= 0) return;
            var spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
            for (var dyeColor : Spirits.colorsByDye.entrySet()) {
                if (dyeColor.getValue() != spiritType) continue;
                var newstate = ChalkLineBlock.getConnectionState(player.level, event.getPos(), state.setValue(ChalkLineBlock.color, dyeColor.getKey()));
                player.level.setBlockAndUpdate(event.getPos(), newstate);
                ChalkLineBlock.refreshNeighborLines(player.level, event.getPos());
                if (player.level instanceof ServerLevel sl) {
                    DiagramManager.OnDiagramBlockChanged(sl, event.getPos(), DiagramManager.BlockUpdateType.ADDED);
                }
                hallowTag.putInt("spirit_count", spiritCount - 1);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
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
            if (sourceFocus.getItem().isEmpty() || item == Items.AIR || item == OtherverseItems.CHALK.get()) {
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

        if (spiritType == null) {
            if (sourceIsHallow) {
                CompoundTag ht = sourceItem.getTag().getCompound("hallow");
                return ht.getInt("spirit_count") > 0;
            } else {
                return false;
            }
        }

        var hallowTag = sink.getItem().getTag().getCompound("hallow");

        if (sink.getHallowCapacity(spiritType) <= 0) {
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
                return getShrineSpiritCount(source, spiritType) > 0;
            } else {
                return sourceTag.getInt("spirit_count") > 0;
            }
        }

        if (sourceItem.is(OtherverseItems.IDOL.get()) && source.getFocusLevel() instanceof ServerLevel sl) {
            var et = IdolItem.getType(sourceItem);
            if (MobBindingInfluenceUtils.mobSpirits.get(et) != spiritType) return false;
            var binding = BindingOrFleshbinding.getFromPosition(sl, source.getPos());
            if (binding == null) return false;
            return binding.getHealth() > 1;
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

    public static Pair<Integer, Integer> getShrineSpiritCountAndCapacity(BlockFocus source, SpiritType spiritType) {
        var level = source.getFocusLevel();
        var data = DiagramManager.getOrCreateLevelData(level);
        var count = 0;
        var cap = 0;
        for (BlockPos sourcePos : ShrineHelper.getAllHallows(source.getPos(), spiritType, data)) {
            var ht = data.getPlacedItemTag(sourcePos);
            count += ht.getInt("spirit_count");
            cap += ht.getInt("capacity");
        }
        return Pair.of(count, cap);
    }

    public static void tryFillHallow(ServerLevel level, IFocus focus, Diagram diagram) {
        ItemStack item = focus.getItem();
        var isDemesneBeacon = item.is(OtherverseItems.DEMESNE_BEACON.get()) && focus.isBlock();
        CompoundTag hallowTag = new CompoundTag();
        if (!isDemesneBeacon) {
            if (!item.hasTag() || !item.getTag().contains("hallow")) {
                return;
            }
            hallowTag = item.getTag().getCompound("hallow");
            int spiritCount = hallowTag.getInt("spirit_count");
            int capacity = hallowTag.getInt("capacity");
            if (spiritCount == capacity) {
                return;
            }
        }

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
        if (!isDemesneBeacon && !hallowTag.contains("spirit_type")) {
            applySpiritType(level, focus, diagram, hallowTag, influences);
        }
        SpiritType spiritType = Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
        for (IFocus sourceFocus : influences) {
            if (sourceFocus.getProcess() != null || !canFill(focus, sourceFocus, spiritType)) {
                continue;
            }
            if (isDemesneBeacon) {
                var sourceHallowTag = sourceFocus.getItem().getTag().getCompound("hallow");
                spiritType = Spirits.spiritsByLabel.get(sourceHallowTag.getString("spirit_type"));
            }
            new SpiritTransfer(focus, sourceFocus, SpiritAffinityTracker.getTransferDuration(focus.getDiagram().getOwnerName(), spiritType));
        }
    }

    public static SpiritType getSpiritType(ItemStack item) {
        if (!item.hasTag() || !item.getTag().contains("hallow")) return null;
        CompoundTag hallowTag = item.getTag().getCompound("hallow");
        return Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"));
    }

    public static boolean tryDrainHallow(ItemStack item, SpiritType spiritType, int spiritAmount) {
        if (!item.hasTag() || !item.getTag().contains("hallow")) return false;
        CompoundTag hallowTag = item.getTag().getCompound("hallow");
        if (spiritType != Spirits.spiritsByLabel.get(hallowTag.getString("spirit_type"))) return false;
        int count = hallowTag.getInt("spirit_count");
        if (count < spiritAmount) return false;
        hallowTag.putInt("spirit_count", count - spiritAmount);
        return true;
    }
}
