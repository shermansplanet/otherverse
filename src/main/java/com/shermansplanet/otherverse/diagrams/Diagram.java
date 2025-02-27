package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.*;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spawning.SpawnAltarManager;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.SpiritTransfusions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

public class Diagram {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BlockPos[] blockFocusPositions;
    public BlockPos[] itemFocusPositions;
    public HashSet<BlockPos> allFocusPositions = new HashSet<>();
    public BlockPos[] chalkPositions;
    public BlockPos primaryBlockPos;
    public boolean isInitialized = false;
    public ArrayList<DiagramProcess> processes = new ArrayList<>();
    public Level level;

    public HashMap<BlockPos, BlockPos[]> rawInfluences = new HashMap<>();
    public HashMap<BlockPos, BlockPos> influences = new HashMap<>();

    private HashMap<BlockPos, HashMap<HashSet<EntityType<?>>, List<PowerSource>>> powerSourceCache = new HashMap<>();

    public HashSet<Mob> mobsOnCooldown = new HashSet<>();

    public Diagram(Level level) {
        this.level = level;
    }

    public ServerPlayer getOwner(ServerLevel level) {
        if (level.getBlockEntity(primaryBlockPos) instanceof ChalkCircle cc
                && cc.player != null) {
            for (var player : level.players()) {
                if (player.getGameProfile().getName().equals(cc.player)) {
                    return player;
                }
            }
        }
        return null;
    }

    private void savePositionArray(CompoundTag compoundTag, BlockPos[] positions, String label) {
        var xs = new int[positions.length];
        var ys = new int[positions.length];
        var zs = new int[positions.length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = positions[i].getX();
            ys[i] = positions[i].getY();
            zs[i] = positions[i].getZ();
        }
        compoundTag.putIntArray(label + "x", xs);
        compoundTag.putIntArray(label + "y", ys);
        compoundTag.putIntArray(label + "z", zs);
    }

    public BlockPos[] loadPositionArray(CompoundTag tag, String label) {
        var xs = tag.getIntArray(label + "x");
        var ys = tag.getIntArray(label + "y");
        var zs = tag.getIntArray(label + "z");
        var positions = new BlockPos[xs.length];
        for (int i = 0; i < xs.length; i++) {
            positions[i] = new BlockPos(xs[i], ys[i], zs[i]);
        }
        return positions;
    }

    public CompoundTag save(CompoundTag compoundTag) {
        compoundTag.putInt("primaryx", primaryBlockPos.getX());
        compoundTag.putInt("primaryy", primaryBlockPos.getY());
        compoundTag.putInt("primaryz", primaryBlockPos.getZ());
        savePositionArray(compoundTag, blockFocusPositions, "blockFoci");
        savePositionArray(compoundTag, itemFocusPositions, "itemFoci");
        savePositionArray(compoundTag, chalkPositions, "chalkPositions");
        return compoundTag;
    }

    public void load(CompoundTag tag) {
        primaryBlockPos = new BlockPos(tag.getInt("primaryx"), tag.getInt("primaryy"),
                tag.getInt("primaryz"));
        blockFocusPositions = loadPositionArray(tag, "blockFoci");
        itemFocusPositions = loadPositionArray(tag, "itemFoci");
        chalkPositions = loadPositionArray(tag, "chalkPositions");
    }

    public boolean tryActivateDiagram(ServerLevel level) {
        LOGGER.debug("activating diagram");
        powerSourceCache = new HashMap<>();
        for (BlockPos pos : itemFocusPositions) {
            if (level.getBlockEntity(pos) instanceof ChalkCircle circle && !circle.item.isEmpty()
                    && !circle.item.is(OtherverseItems.SELF.get())) {
                if (tryActivateCircle(level, circle)) {
                    return true;
                }
            }
        }
        TransientDiagramData data = DiagramManager.getOrCreateLevelData(level);
        for (BlockPos pos : blockFocusPositions) {
            BlockFocus focus = data.allBlockFoci.get(pos);
            if (focus == null) {
                LOGGER.error("No data for block focus at " + pos);
                return false;
            }
            BindingInfo bindingInfo = data.bindingsByPosition.get(pos);
            if (bindingInfo != null && bindingInfo.mob != null) {
                if (!BindingManager.tryBindMob(bindingInfo.mob, focus, level) ||
                        MobTransfusions.tryFeedFromMob(new BindingOrFleshbinding(data.bindingsByPosition.get(pos)), pos, focus.getDiagram())) {
                    return true;
                }
                continue;
            }
            if (tryActivateBlockFocus(level, focus)) {
                return true;
            }
            if (focus.mostRecentMob == null) {
                continue;
            }
            if (!isEntityAtPosition(focus.mostRecentMob, pos)) {
                focus.mostRecentMob = null;
                continue;
            }
            if (processMobInFocus(level, focus.mostRecentMob, focus)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEntityAtPosition(Entity e, BlockPos pos){
        var bb = e.getBoundingBox();
        return (pos.getX() + 1 >= bb.minX && pos.getX() <= bb.maxX && pos.getZ() + 1 >= bb.minZ && pos.getZ() <= bb.maxZ);
    }

    public static boolean tryTransferDown(Level level, ChalkCircle circle) {
        if (level.getBlockEntity(circle.getPos().below()) instanceof ChalkCircle circleBelow
                && circleBelow.item.getItem() instanceof ChalkItem
                && circleBelow.getBlockState().getValue(ChalkLineBlock.hasScaffolding)) {
            ItemStack item = circle.extractItem(0, 1, false);
            if (item.isEmpty()) {
                return false;
            }
            circleBelow.insertItem(0, item, false);
            return true;
        }
        return false;
    }

    private boolean tryActivateCircle(ServerLevel level, ChalkCircle circle) {
        var needsReactivation = MobTransfusions.tryTransfuse(level, circle, this)
                || MobTransfusions.tryFeed(level, circle, this)
                || HallowHelper.tryHallow(level, circle, this)
                || MobTransfusions.tryFeedFromMob(circle, this)
                || ContractManager.tryMakeContract(level, circle, this)
                || FleshbindingManager.tryCinnabind(level, circle, this)
                || DemesnesManager.tryMakePortal(level, circle, this);

        if (!needsReactivation) {
            HallowHelper.tryFillHallow(level, circle, this);
            SpiritTransfusions.tryTransfuse(level, circle, this);
        }
        return needsReactivation;
    }

    private boolean tryActivateBlockFocus(ServerLevel level, BlockFocus focus) {
        var needsReactivation = MobTransfusions.tryLightningTransform(level, focus, this)
                || trySummonDragon(level, focus, this)
                || MobTransfusions.tryFeed(level, focus, this);

        if (!needsReactivation) {
            HallowHelper.tryFillHallow(level, focus, this);
            SpiritTransfusions.tryTransfuse(level, focus, this);
            DemesnesManager.tryGrowArchitecture(level, focus, this);

            needsReactivation = SpawnAltarManager.trySpawn(level, focus, this);
        }
        return needsReactivation;
    }

    private boolean trySummonDragon(ServerLevel level, BlockFocus focus, Diagram diagram) {
        if (!focus.getItem().is(Items.DRAGON_EGG)) return false;
        var crystalCount = 0;
        for (var influencePos : diagram.blockFocusPositions) {
            if (!focus.getPos().equals(diagram.influences.get(influencePos))) continue;
            for (var e : level.getEntities(null, AABB.unitCubeFromLowerCorner(new Vec3(
                    influencePos.getX(), influencePos.getY(), influencePos.getZ()
            )))) {
                if (!e.getType().equals(EntityType.END_CRYSTAL)) continue;
                crystalCount++;
                break;
            }
        }
        if (crystalCount < 8) return false;
        focus.removeItem();
        var e = EntityType.ENDER_DRAGON.create(level);
        e.setPos(focus.getCenter());
        level.addFreshEntityWithPassengers(e);

        BindingManager.tryBindMob(e, focus, level);
        return true;
    }

    public boolean trySpendPower(ServerLevel level, BlockPos targetPos, int requiredPower,
                                 HashSet<EntityType<?>> requiredEntities) {
        List<PowerSource> powerSources =
                powerSourceCache.containsKey(targetPos) ? powerSourceCache.get(targetPos).get(requiredEntities) : null;
        int availablePower = 0;
        if (powerSources == null) {
            powerSources = new ArrayList<>();
            availablePower = getPowerSources(level, targetPos, powerSources, requiredEntities);
            if (!powerSourceCache.containsKey(targetPos)) {
                powerSourceCache.put(targetPos, new HashMap<>());
            }
            powerSourceCache.get(targetPos).put(requiredEntities, powerSources);
        } else {
            for (PowerSource powerSource : powerSources) {
                LOGGER.debug(powerSource.out());
                availablePower += powerSource.totalPower();
            }
        }

        if (requiredPower > availablePower) {
            LOGGER.debug("need " + requiredPower + ", only have " + availablePower);
            return false;
        }

        if (requiredEntities.size() > 1 || requiredPower == 0) {
            for (EntityType<?> requiredEntity : requiredEntities) {
                boolean hasCorrectEntity = false;
                for (PowerSource powerSource : powerSources) {
                    if (powerSource.entityType() == requiredEntity) {
                        hasCorrectEntity = true;
                        break;
                    }
                }
                if (!hasCorrectEntity) {
                    return false;
                }
            }
        }

        powerSources.sort((a, b) -> (b.totalPower().compareTo(a.totalPower())));

        float contributionPerSource = (float) requiredPower / availablePower;

        for (PowerSource powerSource : powerSources) {
            int unitsToDrain = (int) Math.ceil(powerSource.unitsOfPower() * contributionPerSource);
            if (unitsToDrain * powerSource.powerPerUnit() > requiredPower) {
                unitsToDrain = (int) Math.ceil((float) requiredPower / powerSource.powerPerUnit());
            }
            requiredPower -= unitsToDrain * powerSource.powerPerUnit();
            powerSource.drainPower().accept(unitsToDrain);
            if (requiredPower <= 0) {
                break;
            }
        }

        LOGGER.debug("power spent");
        return true;
    }

    private int getPowerSources(ServerLevel level, BlockPos targetPos, List<PowerSource> powerSources,
                                HashSet<EntityType<?>> requiredEntities) {
        LOGGER.debug("Getting power sources for " + String.join(", ",
                requiredEntities.stream().map(EntityType::toString).toList()));
        int totalPower = 0;
        for (BlockPos pos : influences.keySet()) {
            if (!influences.get(pos).equals(targetPos)) {
                continue;
            }
            BindingInfo binding = DiagramManager.getBindingOrBoundMobAt(level, pos);
            if (binding != null) {
                if (binding.mob == null || (!requiredEntities.isEmpty()
                        && !requiredEntities.contains(binding.mob.getType()))) {
                    continue;
                }
                if (binding.mob.invulnerableTime > 0) {
                    mobsOnCooldown.add(binding.mob);
                    LOGGER.debug(binding.mob.getType() + " on cooldown (" + binding.mob.invulnerableTime + ")");
                    continue;
                }
                Mob mob = binding.mob;
                totalPower += (int) mob.getHealth() - 1;
                LOGGER.debug(totalPower + " from " + mob.getType());
                powerSources.add(new PowerSource((int) mob.getHealth() - 1, 1,
                        i -> {
                            if (i > 0) {
                                mob.hurt(DamageSource.OUT_OF_WORLD, i);
                                mob.invulnerableTime = MobTransfusions.MOB_DRAIN_COOLDOWN;
                                mobsOnCooldown.add(mob);
                            }
                        }, mob.getType()));
                continue;
            }
            if (level.getBlockEntity(pos) instanceof ChalkCircle circle) {
                if ((requiredEntities.contains(EntityType.PLAYER) || requiredEntities.isEmpty())
                        && circle.item.is(OtherverseItems.SELF.get())) {
                    int selfCount = circle.item.getCount();
                    totalPower += PowerSource.POWER_FROM_SELF * selfCount;
                    powerSources.add(new PowerSource(selfCount, PowerSource.POWER_FROM_SELF, i -> {
                        if (i > circle.item.getCount()) {
                            return;
                        }
                        circle.item.setCount(circle.item.getCount() - i);
                        if (circle.item.isEmpty()) {
                            circle.item = new ItemStack(OtherverseItems.CHALK.get(), 1);
                        }
                        circle.markUpdated();
                        BlockPos bp = circle.getBlockPos();
                        for (int ii = 0; ii < 6; ii++) {
                            level.sendParticles(ParticleTypes.POOF, bp.getX() + 0.5, bp.getY() + 0.25, bp.getZ() + 0.5,
                                    1, 0, 0, 0, 0.15);
                        }
                    }, EntityType.PLAYER));
                } else if (circle.item.is(OtherverseItems.IDOL.get())) {
                    BindingOrFleshbinding fleshbinding = new BindingOrFleshbinding(circle);
                    if (!requiredEntities.contains(fleshbinding.entityType) && !requiredEntities.isEmpty()) continue;
                    int hp = fleshbinding.getHealth();
                    int availablePower = (hp - 1) / fleshbinding.efficiencyReduction;
                    totalPower += availablePower;
                    powerSources.add(new PowerSource(availablePower, 1,
                            i -> {
                                if (i > 0) {
                                    fleshbinding.changeHealth(-i * fleshbinding.efficiencyReduction, (ServerLevel) level);
                                }
                            }, fleshbinding.entityType));
                }
            }
        }
        LOGGER.debug("total power " + totalPower);
        return totalPower;
    }

    public void calculateBlockFoci() {
        int minx = Integer.MAX_VALUE;
        int maxx = Integer.MIN_VALUE;
        int minz = Integer.MAX_VALUE;
        int maxz = Integer.MIN_VALUE;
        for (BlockPos chalkPos : chalkPositions) {
            minx = Math.min(minx, chalkPos.getX());
            maxx = Math.max(maxx, chalkPos.getX());
            minz = Math.min(minz, chalkPos.getZ());
            maxz = Math.max(maxz, chalkPos.getZ());
        }
        HashSet<BlockPos> chalk = new HashSet<>(Arrays.asList(chalkPositions));
        HashSet<BlockPos> foci = new HashSet<>();
        HashSet<BlockPos> invalid = new HashSet<>();
        for (int x = minx + 1; x < maxx; x++) {
            for (int z = minz + 1; z < maxz; z++) {
                BlockPos pos = new BlockPos(x, primaryBlockPos.getY(), z);
                if (foci.contains(pos) || chalk.contains(pos) || invalid.contains(pos)) {
                    continue;
                }
                Queue<BlockPos> q = new ArrayDeque<>();
                HashSet<BlockPos> visited = new HashSet<>();
                visited.add(pos);
                q.add(pos);
                boolean isValid = true;
                while (!q.isEmpty()) {
                    BlockPos curPos = q.remove();
                    for (int[] offset : DiagramManager.cardinals) {
                        BlockPos newPos = curPos.offset(offset[0], 0, offset[1]);
                        if (chalk.contains(newPos) || visited.contains(newPos)) {
                            continue;
                        }
                        if (newPos.getX() <= minx || newPos.getX() >= maxx
                                || newPos.getZ() <= minz || newPos.getZ() >= maxz) {
                            isValid = false;
                            continue;
                        }
                        q.add(newPos);
                        visited.add(newPos);
                    }
                }
                if (isValid) {
                    foci.addAll(visited);
                } else {
                    invalid.addAll(visited);
                }
            }
        }
        blockFocusPositions = foci.toArray(new BlockPos[0]);
    }

    public void calculateItemFociAndInfluences(Level level) {
        calculateItemFociAndInfluences(level, true);
    }

    public void calculateItemFociAndInfluences(Level level, boolean refreshFoci) {
        if (refreshFoci) {
            List<BlockPos> itemFocusList = new ArrayList<>();
            for (BlockPos bp : chalkPositions) {
                if (level.getBlockEntity(bp) instanceof ChalkCircle cc && !cc.item.isEmpty()) {
                    itemFocusList.add(bp);
                }
            }
            itemFocusPositions = itemFocusList.toArray(new BlockPos[0]);
        }
        calculateAllInfluences();
        if (refreshFoci) {
            for (BlockPos bp : influences.keySet()) {
                if (level.getBlockEntity(bp) instanceof ChalkCircle cc && !cc.item.isEmpty()) {
                    BlockPos bp2 = influences.get(bp);
                    cc.angleDegrees = Math.toDegrees(
                            Math.atan2(bp2.getX() - bp.getX(), bp2.getZ() - bp.getZ()));
                    cc.markUpdated();
                }
            }
        }
        isInitialized = true;
    }

    private void calculateAllInfluences() {
        influences = new HashMap<>();
        rawInfluences = new HashMap<>();
        allFocusPositions = new HashSet<>();
        allFocusPositions.addAll(Arrays.asList(blockFocusPositions));
        allFocusPositions.addAll(Arrays.asList(itemFocusPositions));
        HashSet<BlockPos> chalkPositionSet = new HashSet<>(Arrays.asList(chalkPositions));
        HashSet<BlockPos> nonChalkPositionSet = new HashSet<>(Arrays.asList(blockFocusPositions));
        HashMap<BlockPos, Integer> scores = new HashMap<>();
        for (BlockPos bp : chalkPositions) {
            for (int[] dir : DiagramManager.cardinals) {
                BlockPos pos = bp.offset(dir[0], 0, dir[1]);
                if (!chalkPositionSet.contains(pos)) {
                    nonChalkPositionSet.add(pos);
                }
            }
        }
        for (BlockPos p1 : allFocusPositions) {
            scores.put(p1, 0);
        }
        for (BlockPos p1 : allFocusPositions) {
            boolean isChalk = chalkPositionSet.contains(p1);
            List<BlockPos> targets = new ArrayList<>();
            for (BlockPos p2 : allFocusPositions) {
                if (p1.equals(p2)) {
                    continue;
                }
                BlockPos diff = p1.subtract(p2);
                boolean isValid = true;
                boolean needsMirror =
                        diff.getX() != 0 && diff.getZ() != 0 && Math.abs(diff.getX()) != Math.abs(diff.getZ());
                for (var mirrored = 0; mirrored < (needsMirror ? 2 : 1); mirrored++) {
                    for (var i = 0; i < 3 + mirrored; i++) {
                        diff = new BlockPos(diff.getZ(), 0, -diff.getX());
                        BlockPos newPos = p2.offset(diff);
                        if (isChalk != chalkPositionSet.contains(newPos)
                                || (!isChalk && !nonChalkPositionSet.contains(newPos))) {
                            isValid = false;
                            break;
                        }
                    }
                    if (!isValid) {
                        break;
                    }
                    diff = new BlockPos(diff.getX(), 0, -diff.getZ());
                }
                if (isValid) {
                    targets.add(p2);
                    scores.put(p1, scores.get(p1) + 100);
                    scores.put(p2, scores.get(p2) - 101);
                }
            }
            rawInfluences.put(p1, targets.toArray(new BlockPos[0]));
        }

        for (BlockPos p1 : allFocusPositions) {
            BlockPos[] influenceChoices = rawInfluences.get(p1);
            if (influenceChoices == null) {
                continue;
            }
            if (influenceChoices.length == 1) {
                influences.put(p1, influenceChoices[0]);
                continue;
            }

            HashSet<Integer> allScores = new HashSet<>();
            HashSet<Integer> collisions = new HashSet<>();
            for (BlockPos influenceChoice : influenceChoices) {
                int score = scores.get(influenceChoice);
                if (!allScores.add(score)) {
                    collisions.add(score);
                }
            }

            int highestScore = Integer.MIN_VALUE;
            BlockPos bestChoice = null;
            for (BlockPos influenceChoice : influenceChoices) {
                int score = scores.get(influenceChoice);
                if (collisions.contains(score)) {
                    continue;
                }
                if (score > highestScore) {
                    highestScore = score;
                    bestChoice = influenceChoice;
                }
            }

            if (bestChoice == null) {
                continue;
            }

            influences.put(p1, bestChoice);

        }
    }

    public boolean processMobInFocus(ServerLevel sl, Mob mob, BlockFocus focus) {
        if (mob.getPersistentData().hasUUID("bindingId")) {
            var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
            var binding = data.bindingsById.get(mob.getPersistentData().getUUID("bindingId"));
            return MobTransfusions.tryFeedFromMob(new BindingOrFleshbinding(binding), focus.getPos(), this);
        }
        return BindingManager.tryBindMob(mob, focus, sl) || FleshbindingManager.tryFleshbindMob(mob, focus, sl);
    }

    public String getOwnerName() {
        return level.getBlockEntity(primaryBlockPos) instanceof ChalkCircle cc ? cc.player : null;
    }
}
