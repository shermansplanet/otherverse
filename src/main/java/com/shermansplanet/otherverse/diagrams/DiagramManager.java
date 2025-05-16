package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherverseClientPacketHandler;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.binding.BindingInfo;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.SavedPracticeData;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class DiagramManager {

    public static final int[][] dirs = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    public static final int[][] cardinals = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}
    };

    public static final int[][] dirsPlusSelf = {
            {0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    private static final HashMap<Integer, TransientDiagramData> serverDiagramDataByLevel = new HashMap<>();
    private static final HashMap<Integer, TransientDiagramData> clientDiagramDataByLevel = new HashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static ServerLevel levelFromHash(ServerLevel sl, int dimensionHash) {
        for (var level : sl.getServer().getAllLevels()) {
            if (DiagramManager.getDimensionHash(level) == dimensionHash) return level;
        }
        return null;
    }

    public static int getDimensionHash(Level level) {
        return getDimensionHash(level.dimension());
    }

    public static int getDimensionHash(ResourceKey<Level> dimension) {
        return dimension.toString().hashCode();
    }

    public static TransientDiagramData getOrCreateLevelData(Level level) {
        TransientDiagramData data = getOrCreateLevelData(DiagramManager.getDimensionHash(level),
                level.isClientSide());
        if (data.level == null) {
            data.level = level;
            if (data.level.isClientSide) {
                OtherverseClientPacketHandler.tryDisplayBindings(data.level);
            }
        }
        return data;
    }

    public static TransientDiagramData getOrCreateLevelData(int levelId, boolean isClient) {
        var diagramDataByLevel = isClient ? clientDiagramDataByLevel : serverDiagramDataByLevel;
        TransientDiagramData levelData = diagramDataByLevel.get(levelId);
        if (levelData == null) {
            levelData = new TransientDiagramData(levelId);
            diagramDataByLevel.put(levelId, levelData);
        }
        return levelData;
    }

    public static void markDiagramActive(ServerLevel level, Diagram diagram) {
        getOrCreateLevelData(level).diagramsToActivate.add(diagram);
    }

    public static void OnDiagramBlockChanged(ServerLevel level, BlockPos changedPosition,
                                             BlockUpdateType updateType) {
        HashSet<Diagram> diagramsToRecalculate = new HashSet<>();
        TransientDiagramData levelData = getOrCreateLevelData(level);
        List<ChalkCircle> newCircles = new ArrayList<>();
        var changedState = level.getBlockState(changedPosition);
        for (int[] i : dirsPlusSelf) {
            int dx = i[0];
            int dz = i[1];
            BlockPos newpos = changedPosition.offset(dx, 0, dz);
            if (level.getBlockEntity(newpos) instanceof ChalkCircle cc) {
                ;
                if (updateType != BlockUpdateType.REMOVED
                        && changedState.getValue(ChalkLineBlock.color) != level.getBlockState(newpos).getValue(ChalkLineBlock.color))
                    continue;
                Diagram d = levelData.diagramsByPrimary.get(cc.diagramPrimary);
                if (d != null) {
                    diagramsToRecalculate.add(d);
                }
                newCircles.add(cc);
            }
        }
        if (updateType == BlockUpdateType.CHANGED) {
            for (Diagram d : diagramsToRecalculate) {
                d.calculateItemFociAndInfluences(level);
                if (level.getBlockEntity(d.primaryBlockPos) instanceof ChalkCircle cc) {
                    cc.markUpdated();
                }
                markDiagramActive(level, d);
            }
            return;
        }
        for (Diagram d : diagramsToRecalculate) {
            if (level.getBlockEntity(d.primaryBlockPos) instanceof ChalkCircle cc) {
                cc.diagram = null;
                cc.markUpdated();
            }
            unloadDiagram(d, level);
        }
        HashSet<ChalkCircle> visited = new HashSet<>();
        List<Diagram> diagramsToUpdate = new ArrayList<>();
        for (ChalkCircle originalChalkCircle : newCircles) {
            if (visited.contains(originalChalkCircle)) {
                continue;
            }
            Queue<ChalkCircle> q = new ArrayDeque<>();
            q.add(originalChalkCircle);
            visited.add(originalChalkCircle);

            BlockPos primaryPos = originalChalkCircle.getBlockPos();
            var color = level.getBlockState(primaryPos).getValue(ChalkLineBlock.color);
            Diagram diagram = new Diagram(level);
            diagram.primaryBlockPos = primaryPos;
            originalChalkCircle.diagram = diagram;
            originalChalkCircle.diagramPrimary = primaryPos;
            List<BlockPos> chalkPositions = new ArrayList<>();
            chalkPositions.add(primaryPos);

            while (!q.isEmpty()) {
                ChalkCircle cc = q.remove();
                BlockPos ccpos = cc.getBlockPos();
                for (int[] i : dirs) {
                    int dx = i[0];
                    int dz = i[1];
                    BlockPos newpos = ccpos.offset(dx, 0, dz);
                    var state = level.getBlockState(newpos);
                    if (!state.is(OtherverseBlocks.CHALK_LINE.get()) || state.getValue(ChalkLineBlock.color) != color)
                        continue;
                    if (!(level.getBlockEntity(newpos) instanceof ChalkCircle neighbor)) continue;
                    if (visited.contains(neighbor)) continue;
                    visited.add(neighbor);
                    q.add(neighbor);
                    neighbor.diagramPrimary = primaryPos;
                    chalkPositions.add(newpos);
                }
            }

            diagram.chalkPositions = chalkPositions.toArray(new BlockPos[0]);
            diagram.calculateBlockFoci();
            diagram.calculateItemFociAndInfluences(level);
            diagramLoaded(diagram, level);
            diagramsToUpdate.add(diagram);
        }

        for (Diagram d : diagramsToUpdate) {
            markDiagramActive(level, d);
        }
    }

    public static void diagramLoaded(Diagram diagram, Level level) {
        diagramLoaded(diagram, level, false);
    }

    public static void diagramLoaded(Diagram diagram, Level level, boolean fromLoad) {
        if (level == null) {
            return;
        }
        if (diagram.level == null) {
            diagram.level = level;
        } else if (diagram.level != level) {
            LOGGER.debug("LOADING DIAGRAM FROM WRONG LEVEL (DIAGRAM "
                    + (diagram.level.isClientSide() ? "CLIENT" : "SERVER") + ", LEVEL "
                    + (level.isClientSide() ? "CLIENT" : "SERVER"));
        }
        TransientDiagramData levelData = getOrCreateLevelData(level);
        if (!fromLoad && level.isClientSide()) {
            for (BlockPos pos : levelData.diagramsByPrimary.keySet()) {
                Diagram d = levelData.diagramsByPrimary.get(pos);
                if (d != null && (!(level.getBlockEntity(pos) instanceof ChalkCircle cc)
                        || cc.diagram != d)) {
                    LOGGER.debug("unloading diagram because subsumed");
                    unloadDiagram(d, level);
                }
            }
        }
        levelData.diagramsByPrimary.put(diagram.primaryBlockPos, diagram);
        for (BlockPos blockFocus : diagram.blockFocusPositions) {
            levelData.allBlockFoci.put(blockFocus, new BlockFocus(level, blockFocus, diagram));
        }
        if (level instanceof ServerLevel sl) {
            markDiagramActive(sl, diagram);
        }
    }

    public static void unloadDiagram(Diagram diagram, Level level) {
        unloadDiagram(diagram, level, false);
    }

    public static void unloadDiagram(Diagram diagram, Level level, boolean unloadingChunk) {
        if (diagram.level != level && diagram.level != null) {
            LOGGER.debug("UNLOADING DIAGRAM FROM WRONG LEVEL (DIAGRAM "
                    + (diagram.level.isClientSide() ? "CLIENT" : "SERVER") + ", LEVEL "
                    + (level.isClientSide() ? "CLIENT" : "SERVER"));
        }
        TransientDiagramData levelData = getOrCreateLevelData(level);
        levelData.diagramsByPrimary.remove(diagram.primaryBlockPos);
        levelData.diagramsToActivate.remove(diagram);
        for (var process : new ArrayList<>(diagram.processes)) {
            process.abandon();
        }
        for (BlockPos pos : diagram.blockFocusPositions) {
            BindingInfo binding = getOrCreateLevelData(level).bindingsByPosition.get(pos);
            if (!level.isClientSide() && binding != null && !unloadingChunk && !FamiliarManager.isFamiliar(binding.mob)) {
                BindingManager.breakBinding(binding);
            }
            BlockFocus bf = levelData.allBlockFoci.get(pos);
            if (bf == null || bf.getDiagram() != diagram) {
                continue;
            }
            levelData.allBlockFoci.remove(pos);
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase != Phase.START) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (!PracticeWorldManager.worldSetUp) return;

        TransientDiagramData levelData = getOrCreateLevelData(sl);
        for (var diagram : levelData.diagramsByPrimary.values()) {
            for (var process : new ArrayList<>(diagram.processes)) {
                process.tick();
            }
        }
        HashSet<Diagram> diagrams = levelData.diagramsToActivate;
        if (diagrams == null || diagrams.isEmpty()) {
            return;
        }
        HashSet<Diagram> oldDiagrams = new HashSet<>(diagrams);
        diagrams.clear();
        for (Diagram d : oldDiagrams) {
            var diagramInWorld = levelData.diagramsByPrimary.get(d.primaryBlockPos);
            if (diagramInWorld != d) {
                LOGGER.debug("WRONG DIAGRAM IN MANAGER");
                continue;
            }
            if (d.tryActivateDiagram(sl)) {
//                LOGGER.debug("reactivating diagram");
                d.calculateItemFociAndInfluences(sl);
                diagrams.add(d);
            } else {
//                LOGGER.debug("deactivating diagram");
                if (!d.processes.isEmpty()) {
//                    LOGGER.debug("...but it still has processes");
                    continue;
                }
                for (BlockPos pos : d.itemFocusPositions) {
                    if (sl.getBlockEntity(pos) instanceof ChalkCircle circle) {
                        if (d.tryTransferDown(sl, circle)) {
                            d.calculateItemFociAndInfluences(sl);
                            diagrams.add(d);
                        }
                    }
                }
            }
        }
    }

    public static void blockChanged(BlockPos pos, ServerLevel sl) {
        var data = getOrCreateLevelData(sl);
        var tag = data.getPlacedItemTag(pos);
        if (tag == null) {
            var blockFocus = data.allBlockFoci.get(pos);
            if (blockFocus != null) markDiagramActive(sl, blockFocus.getDiagram());
            return;
        }

        for (var hallowPos : ShrineHelper.getAllHallows(pos, Spirits.spiritsByLabel.get(tag.getString("spirit_type")), data)) {
            var blockFocus = data.allBlockFoci.get(hallowPos);
            if (blockFocus != null) markDiagramActive(sl, blockFocus.getDiagram());
        }
    }

    private static void BlockBreak(Level level, BlockPos pos) {
        TransientDiagramData diagramData = getOrCreateLevelData(level);
        if (level instanceof ServerLevel sl) {
            blockChanged(pos, sl);
            if (sl.getBlockState(pos).is(OtherverseBlocks.DEMESNE_BEACON.get())) {
                DemesnesManager.onBeaconBroken(sl, pos);
            }
            return;
        }
        Diagram d = diagramData.diagramsByPrimary.get(pos);
        if (d != null) {
            unloadDiagram(d, level);
        }
    }

    @SubscribeEvent
    public static void waterproofChalk(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getOriginalState().is(OtherverseBlocks.CHALK_LINE.get())){
            event.setCanceled(true);
            return;
        }
        if (event.getLevel() instanceof ServerLevel sl) blockChanged(event.getPos(), sl);
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel sl) blockChanged(event.getPos(), sl);
    }

    @SubscribeEvent
    public static void onBlockBroken(BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            BlockState bs = sl.getBlockState(event.getPos());
            if (bs.is(OtherverseBlocks.CHALK_LINE.get()) && bs.getValue(ChalkLineBlock.hasScaffolding)) {
                event.setCanceled(true);
                BlockState newState = bs.setValue(ChalkLineBlock.hasScaffolding, false);
                sl.setBlockAndUpdate(event.getPos(), newState);
                ItemStack drop = OtherverseItems.SLATE_SCAFFOLDING.get().getDefaultInstance();
                if (!event.getPlayer().addItem(drop)) {
                    event.getPlayer().drop(drop, false);
                }
                return;
            }
        }
        Level level = event.getPlayer().getLevel();
        BlockBreak(level, event.getPos());
        if (event.getPlayer().isCreative()) {
            getOrCreateLevelData(level).removePlacedItemTag(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockDestroyed(LivingDestroyBlockEvent event) {
        BlockBreak(event.getEntity().getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlaced(VanillaGameEvent event) {
        if (event.getVanillaEvent() != GameEvent.BLOCK_PLACE
                || event.getContext().affectedState() == null
                || event.getCause() == null) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel sl) {
            Block block = event.getContext().affectedState().getBlock();
            ItemStack item = null;
            BlockPos pos = new BlockPos(event.getEventPosition());
            if (event.getContext().sourceEntity() instanceof Player player) {
                ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
                if (mainHandItem.is(offHandItem.getItem())) {
                    blockChanged(pos, sl);
                    return;
                }
                if (mainHandItem.is(block.asItem())) {
                    item = mainHandItem;
                } else if (offHandItem.is(block.asItem())) {
                    item = offHandItem;
                }
            }
            if (item != null && item.hasTag() && item.getTag().contains("hallow")) {
                CompoundTag tag = item.getTag().getCompound("hallow").copy();
                TransientDiagramData levelData = DiagramManager
                        .getOrCreateLevelData(event.getLevel());
                levelData.putPlacedItemTag(pos, tag);
            }
            blockChanged(pos, sl);
        }
    }

    @SubscribeEvent
    public static void levelLoad(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level)) return;
        var diagramDataByLevel = event.getLevel().isClientSide() ? clientDiagramDataByLevel : serverDiagramDataByLevel;
        diagramDataByLevel.remove(DiagramManager.getDimensionHash(level));
    }

    @SubscribeEvent
    public static void entityUpdates(LivingEvent.LivingTickEvent event) {
        Level level = event.getEntity().getLevel();
        if (level.getGameTime() % 10 != 0 || !(level instanceof ServerLevel sl) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        CompoundTag entityData = event.getEntity().getPersistentData();
        if (entityData.contains("bindingId")) {
            var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
            BindingInfo binding = data.bindingsById.get(entityData.getUUID("bindingId"));
            if (binding == null) {
                LOGGER.debug("REMOVING BINDING BECAUSE NULL ON UPDATE");
                mob.getPersistentData().remove("bindingId");
                return;
            }
            var focus = binding.getFocus();
            if (focus == null) {
                return;
            }
            Diagram diagram = focus.getDiagram();
            if (diagram.mobsOnCooldown.contains(mob) && mob.invulnerableTime <= 0) {
                diagram.mobsOnCooldown.remove(mob);
                LOGGER.debug("ACTIVATING DIAGRAM: MOB COOLED DOWN");
                DiagramManager.markDiagramActive(sl, diagram);
                return;
            }
        }

        BlockFocus focus = getFocusInBoundingBox(getOrCreateLevelData(sl), event.getEntity().getBoundingBox());
        if (focus == null) {
            return;
        }
        var hp = Math.round(mob.getHealth());
        if (focus.mostRecentMob != null && mob.getId() == focus.mostRecentMob.getId() && hp == focus.mostRecentMobHealth) {
            return;
        }
        focus.mostRecentMob = mob;
        focus.mostRecentMobHealth = hp;
        if (!PracticeWorldManager.worldSetUp) {
            DiagramManager.markDiagramActive(sl, focus.getDiagram());
            return;
        }
        focus.getDiagram().processMobInFocus(sl, mob, focus);
        if (mob.getPersistentData().contains("bindingId")) DiagramManager.markDiagramActive(sl, focus.getDiagram());
    }

    public static BlockFocus getFocusInBoundingBox(TransientDiagramData data, AABB bb) {
        for (var y = Mth.floor(bb.minY); y <= Mth.floor(bb.maxY); y++) {
            for (var x = Mth.floor(bb.minX); x <= Mth.floor(bb.maxX); x++) {
                for (var z = Mth.floor(bb.minZ); z <= Mth.floor(bb.maxZ); z++) {
                    var focus = data.allBlockFoci.get(new BlockPos(x, y, z));
                    if (focus != null) return focus;
                }
            }
        }
        return null;
    }


    public static void tryLoadOverworld(ServerLevel sl) {
        var overworld = sl.getServer().overworld();
        var id = getDimensionHash(overworld);
        if (serverDiagramDataByLevel.containsKey(id)) return;

        SavedPracticeData data = overworld.getDataStorage().computeIfAbsent(
                tag -> HallowHelper.loadPracticeData(tag, overworld),
                () -> HallowHelper.createPracticeData(overworld), "practice");

        DiagramManager.getOrCreateLevelData(overworld).savedData = data;
    }

    public static BindingInfo getBindingOrBoundMobAt(ServerLevel level, BlockPos pos) {
        var levelData = getOrCreateLevelData(level);
        var binding = levelData.bindingsByPosition.get(pos);
        if (binding != null) {
            return binding;
        }
        var focus = levelData.allBlockFoci.get(pos);
        if (focus != null && focus.mostRecentMob != null && focus.mostRecentMob.getPersistentData().hasUUID("bindingId")) {
            if (!Diagram.isEntityAtPosition(focus.mostRecentMob, pos)) {
                focus.mostRecentMob = null;
                return null;
            }
            return getOrCreateLevelData(level.getServer().overworld()).bindingsById.get(
                    focus.mostRecentMob.getPersistentData().getUUID("bindingId"));
        }
        return null;
    }

    public enum BlockUpdateType {ADDED, REMOVED, CHANGED}
}
