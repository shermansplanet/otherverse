package com.shermansplanet.otherverse.binding;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.familiar.TreeStripper;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.*;

public class ContractTask {
    private static final int COBWEB_DAMAGE = 3;

    private final Mob mob;
    private int countMin = 1;
    private int countMax = Integer.MAX_VALUE;
    public BlockPos targetPos, targetMovePos;
    public ContractManager.PositionOrSpindle spindle;
    public Entity targetItem;
    public LivingEntity targetMob;
    public BoundGoal boundGoal;
    public BoundGoal.ContractDecider onSuccess, onFailure, onEither;
    public final TaskType taskType;
    public HashSet<Item> itemFilters = new HashSet<>();
    public HashSet<Item> blockFilters = new HashSet<>();
    public HashSet<ContractManager.PositionOrSpindle> positionFilters = new HashSet<>();
    public HashSet<EntityType<?>> entityFilters = new HashSet<>();
    public ArrayList<ContractManager.PositionOrSpindle> offsets = new ArrayList<>();
    public ArrayList<BlockPos> offsetBases = new ArrayList<>();
    private HashMap<Block, BlockstateMatch> blockstateFilters = new HashMap<>();
    private HashMap<Item, String> tagFilters = new HashMap<>();
    private HashMap<Pair<IItemHandler, Integer>, Integer> usedIngredientsCache = new HashMap<>();
    private boolean onlyHasInventoryBlockFilters = true;
    public boolean isTakingFromGround;
    private int blockDamage = -1;
    private int doActionTicks;
    public final ContractManager.PositionOrSpindle corner0;
    public final ContractManager.PositionOrSpindle corner1;
    private Recipe<?> recipe;
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean harvesting = false;
    private boolean felling = false;
    public int lookIndex;
    public ArrayList<BlockPos> potentialTargets = new ArrayList<>();
    public final String taskId;
    private Path directPath;
    private int ticksInSameBlock = 0;
    private BlockPos lastPos = null;

    public void getDebugMessage(StringBuilder s) {
        s.append("task: ").append(taskType).append("\n");
        s.append("my position: ").append(mob.blockPosition()).append("\n");
        s.append("task target: ").append(targetPos).append("\n");
        s.append("move target: ").append(targetMovePos).append("\n");
        s.append("phase: ").append(directPath == null ? "initial" : "final").append("\n");
        var nullPath = mob.getNavigation().getPath() == null;
        s.append("path: ").append(nullPath ? "null" : "not null");
        if (!nullPath) s.append(", ").append(mob.getNavigation().getPath().isDone() ? "done" : "not done");
    }

    public enum TaskType {
        MOVE, TAKE, PUT, BREAK, ATTACK, CRAFT, OBSERVE
    }

    private static record BlockstateMatch(HashMap<String, String> props) {
    }

    public ContractTask(CompoundTag tag, Mob mob, BoundGoal boundGoal) {
        this.mob = mob;
        this.boundGoal = boundGoal;
        StringBuilder id = new StringBuilder(tag.getString("type"));
        switch (id.toString()) {
            case "go" -> taskType = TaskType.MOVE;
            case "take" -> taskType = TaskType.TAKE;
            case "put" -> taskType = TaskType.PUT;
            case "break" -> taskType = TaskType.BREAK;
            case "harvest" -> {
                taskType = TaskType.BREAK;
                harvesting = true;
            }
            case "fell" -> {
                taskType = TaskType.BREAK;
                felling = true;
            }
            case "attack" -> taskType = TaskType.ATTACK;
            case "craft" -> taskType = TaskType.CRAFT;
            case "observe" -> taskType = TaskType.OBSERVE;
            default -> taskType = null;
        }
        if (tag.contains("success")) {
            onSuccess = boundGoal.makeDecider(tag.getCompound("success"));
        }
        if (tag.contains("failure")) {
            onFailure = boundGoal.makeDecider(tag.getCompound("failure"));
        }
        if (tag.contains("either")) {
            onEither = boundGoal.makeDecider(tag.getCompound("either"));
        }
        if (tag.contains("inscription")) {
            var inscription = tag.getString("inscription");
            if (inscription.contains("-")) {
                var parts = inscription.split("-");
                countMin = Integer.parseInt(parts[0]);
                countMax = Integer.parseInt(parts[1]);
            } else if (inscription.startsWith("<")) {
                countMax = Integer.parseInt(inscription.substring(1)) - 1;
                countMin = 0;
            } else if (inscription.startsWith(">")) {
                countMin = Integer.parseInt(inscription.substring(1)) + 1;
            } else {
                var val = Integer.parseInt(inscription);
                countMin = val;
                countMax = val;
            }
            if (countMax < countMin) {
                countMax = countMin;
            }
        }
        if (tag.contains("corner_0")) {
            corner0 = new ContractManager.PositionOrSpindle(tag.getCompound("corner_0"));
            corner1 = new ContractManager.PositionOrSpindle(tag.getCompound("corner_1"));
        } else {
            corner0 = null;
            corner1 = null;
        }
        if (tag.contains("recipe_id")) {
            recipe = mob.level().getServer().getRecipeManager().byKey(ResourceLocation.parse(tag.getString("recipe_id"))).get();
        }
        for (String key : tag.getAllKeys()) {
            if (key.startsWith("filter")) {
                if (taskType == TaskType.ATTACK) {
                    var entities = LootHelper.entitiesThatDropItem.get(Item.byId(tag.getInt(key)));
                    if (entities != null) {
                        entityFilters.addAll(entities);
                    }
                } else {
                    var item = Item.byId(tag.getInt(key));
                    itemFilters.add(item);
                    var index = key.substring(7);
                    var tagitemkey = "tag_item_" + index;
                    if (tag.contains(tagitemkey)) {
                        tagFilters.put(item, tag.getCompound(tagitemkey).toString());
                    }
                    var tagblockkey = "tag_block_" + index;
                    if (tag.contains(tagblockkey)) {
                        var compound = tag.getCompound(tagblockkey);
                        HashMap<String, String> props = new HashMap<>();
                        for (var k : compound.getAllKeys()) {
                            props.put(k, compound.getString(k));
                        }
                        if (item instanceof BlockItem bi) {
                            blockstateFilters.put(bi.getBlock(), new BlockstateMatch(props));
                        }
                    }
                }
                id.append(tag.getInt(key));
            } else if (key.startsWith("block") || key.startsWith("inventory")) {
                var filterList = (taskType == TaskType.MOVE) ? itemFilters : blockFilters;
                filterList.add(Item.byId(tag.getInt(key)));
                if (key.startsWith("block")) onlyHasInventoryBlockFilters = false;
                id.append(tag.getInt(key));
            } else if (key.startsWith("position")) {
                positionFilters.add(new ContractManager.PositionOrSpindle(tag.getCompound(key)));
            } else if (key.startsWith("offset")) {
                var pos = tag.getIntArray(key + "_basis");
                if (pos.length == 3) {
                    offsets.add(new ContractManager.PositionOrSpindle(tag.getCompound(key)));
                    offsetBases.add(new BlockPos(pos[0], pos[1], pos[2]));
                }
            }
        }
        if (offsets.isEmpty()) {
            offsets.add(new ContractManager.PositionOrSpindle(new BlockPos(0, -127, 0)));
            offsetBases.add(new BlockPos(0, -128, 0));
        }
        taskId = id.toString();
    }

    private boolean isValidItem(ItemStack stack) {
        if (itemFilters.isEmpty()) return true;
        if (!itemFilters.contains(stack.getItem())) return false;
        if (tagFilters.isEmpty()) return true;
        var tagFilter = tagFilters.get(stack.getItem());
        if (tagFilter == null) return true;
        if (!stack.hasTag()) return false;
        return tagFilter.equals(stack.getTag().toString());
    }

    private boolean isValidBlock(BlockState blockState) {
        var block = blockState.getBlock();
        if (harvesting && (!(block instanceof CropBlock cb && cb.isMaxAge(blockState)))) return false;
        if (itemFilters.isEmpty()) {
            return !felling || (blockState.is(BlockTags.LOGS));
        }
        if (!itemFilters.contains(block.asItem())) return false;
        if (blockstateFilters.isEmpty()) return true;
        var blockstateFilter = blockstateFilters.get(block);
        if (blockstateFilter == null) return true;
        for (var prop : blockState.getValues().entrySet()) {
            if (!blockstateFilter.props.get(prop.getKey().getName()).equals(prop.getValue().toString())) return false;
        }
        return true;
    }

    public Pair<Entity, BlockPos> getClosestReachableDroppedItem() {
        var lookDiameter = boundGoal.range * 2;
        var possibleEntities = new ArrayList<Entity>();
        for (Entity e : mob.level().getEntities(mob, AABB.ofSize(mob.position(), lookDiameter, lookDiameter, lookDiameter))) {
            if ((e instanceof ItemEntity ie && isValidTakeItem(ie.getItem()))
                    || (mob.getType() == EntityType.PILLAGER && e instanceof AbstractArrow arrow && isValidTakeItem(getArrowItem(arrow)))) {
                if (!matchesPositionFilter(e.blockPosition())) {
                    continue;
                }
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    Block block = mob.level().getBlockState(e.blockPosition().subtract(processOffset(offsetIndex))).getBlock();
                    if (!blockFilters.isEmpty() && !blockFilters.contains(block.asItem()) && !blockFilters.contains(BlockFocus.blockReplacements.get(block))) {
                        continue;
                    }
                    possibleEntities.add(e);
                    break;
                }
            }
        }
        possibleEntities.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(mob.position())));
        for (var e : possibleEntities) {
            var reachFrom = getAccessibleBlock(e.blockPosition());
            if (reachFrom != null) return new Pair<>(e, reachFrom);
        }
        return new Pair<>(null, null);
    }

    private ItemStack getArrowItem(AbstractArrow abstractArrow) {
        if (abstractArrow instanceof Arrow arrow) {
            return new ItemStack(Items.ARROW);
        }
        return ItemStack.EMPTY;
    }

    private boolean matchesPositionFilter(BlockPos blockPos) {
        if (positionFilters.isEmpty()) return true;
        for (var filter : positionFilters) {
            var pos = filter.getPos(mob.level());
            if (pos != null && pos.equals(blockPos)) return true;
        }
        return false;
    }

    private Vec3i processOffset(int offsetIndex) {
        return offsets.get(offsetIndex).getPos(mob.level()).subtract(offsetBases.get(offsetIndex));
    }

    public LivingEntity getClosestValidLivingTarget() {
        LivingEntity closest = null;
        var lookDiameter = boundGoal.range * 2;
        float closestDist = lookDiameter * 2;
        for (Entity e : mob.level().getEntities(mob, AABB.ofSize(mob.position(), lookDiameter, lookDiameter, lookDiameter))) {
            if (e == mob) {
                continue;
            }
            if (e instanceof LivingEntity le) { //  && mob.canAttack(le)
                if (entityFilters.isEmpty() && blockFilters.isEmpty() && e.getType() == EntityType.PLAYER) continue;
                if (!entityFilters.isEmpty() && !entityFilters.contains(e.getType())) {
                    continue;
                }
                if (!blockFilters.isEmpty() && !blockFilters.contains(Items.CHAIN)
                        && BindingManager.isBoundOrContracted(le)) {
                    continue;
                }
                if (!blockFilters.isEmpty()) {
                    if (e.getType() == EntityType.PLAYER) {
                        if (!blockFilters.contains(Items.CRAFTING_TABLE)) {
                            continue;
                        }
                    } else {
                        boolean isMonster = le.getType().getCategory() == MobCategory.MONSTER;
                        if ((isMonster && !blockFilters.contains(Items.TARGET))
                                || (!isMonster && !blockFilters.contains(Items.HAY_BLOCK))) {
                            continue;
                        }
                    }
                }
                float dist = (float) le.position().distanceTo(mob.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = le;
                }
            }
        }
        return closest;
    }

    private BlockPos getAccessibleBlock(BlockPos pos) {
        if (pos == null) return null;
        var h = mob.getBbHeight();
        var potentialMovePositions = new ArrayList<BlockPos>();
        if (taskType == TaskType.MOVE) {
            potentialMovePositions.add(pos);
            potentialMovePositions.add(pos.above());
        } else {
            var canDoCenter = taskType != TaskType.PUT;
            for (var i = -1; i <= h + 1; i++) {
                var verticalOffset = pos.offset(0, -i, 0);
                if (canDoCenter) potentialMovePositions.add(verticalOffset);
                for (var dir : Direction.values()) {
                    if (dir == Direction.UP) continue;
                    if (dir == Direction.DOWN) continue;
                    var offset = verticalOffset.relative(dir);
                    potentialMovePositions.add(offset);
                }
            }
        }
        var mobPos = mob.blockPosition();
        potentialMovePositions.sort(Comparator.comparingDouble(mobPos::distSqr));
        for (var potentialPos : potentialMovePositions) {
            if (!canReachBlock(potentialPos)) continue;
            return potentialPos;
        }
        return null;
    }

    public BlockPos isAcceptableTarget(BlockPos pos, boolean inPositionFilter) {
        if (pos == null) return null;
        if (!isBlockAcceptableTarget(pos, inPositionFilter)) return null;
        return getAccessibleBlock(pos);
        //return internalAcceptableTarget(pos, inPositionFilter) && canReachBlock(pos);
    }

    private boolean isBlockAcceptableTarget(BlockPos pos, boolean inPositionFilter) {
        switch (taskType) {
            case BREAK:
                if (!blockFilters.isEmpty()) {
                    var anyMatches = false;
                    for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                        Block block = mob.level().getBlockState(pos.subtract(processOffset(offsetIndex))).getBlock();
                        if (blockFilters.contains(block.asItem()) || blockFilters.contains(BlockFocus.blockReplacements.get(block))) {
                            anyMatches = true;
                        }
                    }
                    if (!anyMatches) {
                        return false;
                    }
                }

                if (mob.level().isEmptyBlock(pos)) {
                    return false;
                }
                // fall back to MOVE
            case MOVE:
                return isValidBlock(mob.level().getBlockState(pos));
            case PUT:
                if (blockFilters.isEmpty() && inPositionFilter && mob.level().isEmptyBlock(pos)) {
                    return true;
                }
                ItemStack heldItem = BindingManager.getHeldItem(mob);
                BlockEntity putTarget = mob.level().getBlockEntity(pos);
                boolean validPutBlockBelow = false;
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    var otherpos = pos.subtract(processOffset(offsetIndex));
                    validPutBlockBelow = blockFilters.contains(mob.level().getBlockState(otherpos).getBlock().asItem());
                    if (validPutBlockBelow) {
                        BlockEntity beBelow = mob.level().getBlockEntity(otherpos);
                        if (beBelow != null && beBelow.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                            validPutBlockBelow = false;
                        }
                    }
                    if (validPutBlockBelow) {
                        break;
                    }
                }
                BlockState blockState = mob.level().getBlockState(pos);
                if (!inPositionFilter && !validPutBlockBelow && !blockFilters.contains(blockState.getBlock().asItem())) {
                    return false;
                }

                if (isPlacingCobweb()) {
                    return validPutBlockBelow && mob.level().isEmptyBlock(pos);
                }

                if (putTarget != null) {
                    var cap = putTarget.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                    if (cap.isPresent() && (validPutBlockBelow || onlyHasInventoryBlockFilters)) {
                        var inventory = cap.get();
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            var itemSubset = heldItem.copy();
                            itemSubset.setCount(Math.min(heldItem.getCount(), countMax));
                            var resultStack = inventory.insertItem(i, itemSubset, true);
                            var insertedAmount = itemSubset.getCount() - resultStack.getCount();
                            if (insertedAmount >= countMin && insertedAmount <= countMax) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
                if (!validPutBlockBelow) return false;
                var canDrop = boundGoal.takesAll ? !(heldItem.getItem() instanceof BlockItem)
                        : !boundGoal.itemDropDenyList.contains(heldItem.getItem());
                if (canDrop && !blockState.isCollisionShapeFullBlock(mob.level(), pos)) {
                    return true;
                }
                return tryPlaceBlock(pos, heldItem, false, false);
            case OBSERVE:
                if (countMin <= 1 && itemFilters.contains(mob.level().getBlockState(pos).getBlock().asItem())) {
                    return true;
                }
            case TAKE:
                BlockEntity takeTarget = mob.level().getBlockEntity(pos);
                if (takeTarget == null) {
                    return false;
                }
                boolean validTakeBlockBelow = false;
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    if (blockFilters.contains(mob.level().getBlockState(pos.subtract(processOffset(offsetIndex))).getBlock().asItem())) {
                        validTakeBlockBelow = true;
                        break;
                    }
                }
                if (!inPositionFilter && !validTakeBlockBelow && !blockFilters.contains(takeTarget.getBlockState().getBlock().asItem())) {
                    return false;
                }
                var cap = takeTarget.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if (cap.isPresent() && (validTakeBlockBelow || onlyHasInventoryBlockFilters)) {
                    var inventory = cap.get();
                    var runningTotals = new HashMap<Item, Integer>();
                    if (takeTarget instanceof ChalkCircle cc) {
                        cc.overrideLock = true;
                    }
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.isEmpty() || !isValidTakeItem(stack)) continue;
                        var key = stack.getItem();
                        runningTotals.put(key, runningTotals.getOrDefault(key, 0) + stack.getCount());
                    }
                    if (takeTarget instanceof ChalkCircle cc) {
                        cc.overrideLock = false;
                    }
                    for (var itemFilter : itemFilters) {
                        var total = runningTotals.getOrDefault(itemFilter, 0);
                        if (total >= countMin && (taskType == TaskType.TAKE || total <= countMax)) {
                            return true;
                        }
                    }
                }
                return false;
            case ATTACK:
                return canAttackPosition(mob.getType());
            case CRAFT:
                if (mob.level().getBlockState(pos).getBlock() != Blocks.CRAFTING_TABLE) return false;
                usedIngredientsCache.clear();
                for (var ingredient : recipe.getIngredients()) {
                    if (!findCraftingIngredient(ingredient, pos, usedIngredientsCache)) return false;
                }
                return true;
            default:
                return false;
        }
    }

    private boolean isValidTakeItem(ItemStack stack) {
        if (!isValidItem(stack)) return false;
        if (taskType == TaskType.OBSERVE) return true;
        var held = BindingManager.getHeldItem(mob);
        if (held.isEmpty()) return true;
        return held.is(stack.getItem()) && Objects.equals(held.getTag(), stack.getTag());
    }

    private boolean canReachBlock(BlockPos pos) {
        if (taskType == TaskType.ATTACK || taskType == TaskType.OBSERVE) return true;
        if (mob.getPersistentData().contains("construct_type")) {
            if (mob.getPersistentData().getString("construct_type").equals(Spirits.FLESH.label())) {
                if (ShrineHelper.getShrinesFor(mob, pos, Spirits.FLESH).isEmpty()) return false;
            } else {
                if (ShrineHelper.getShrinesFor(mob, pos, Spirits.TECH).isEmpty()) return false;
            }
        }
        if (mob.blockPosition().distSqr(pos) > 16 * 16) return true;
        if (mob.getType() == EntityType.ENDERMAN || mob.getType() == EntityType.SHULKER) {
            if (mob.level().getBlockState(pos.below()).getCollisionShape(mob.level(), pos.below()).isEmpty())
                return false;
        } else {
            var path = mob.getNavigation().createPath(pos, 0);
            if (path == null || !path.getEndNode().asBlockPos().equals(pos)) return false;
        }
        return !mob.level().collidesWithSuffocatingBlock(mob, new AABB(
                pos.getX() + 0.1f, pos.getY() + 0.5f, pos.getZ() + 0.1f,
                pos.getX() + 0.9f, pos.getY() + mob.getBbHeight(), pos.getZ() + 0.9f
        ));
    }

    private boolean findCraftingIngredient(Ingredient ingredient, BlockPos pos, HashMap<Pair<IItemHandler, Integer>, Integer> usedIngredients) {
        if (ingredient.isEmpty()) return true;
        for (var dir : Direction.values()) {
            var be = mob.level().getBlockEntity(pos.relative(dir));
            if (be != null) {
                var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if (cap.isPresent()) {
                    var inventory = cap.get();
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.isEmpty() || !ingredient.test(stack)) continue;
                        var pair = new Pair<>(inventory, i);
                        if (!usedIngredients.containsKey(pair)) {
                            usedIngredients.put(pair, 1);
                            return true;
                        }
                        var usedCount = usedIngredients.get(pair);
                        if (usedCount >= stack.getCount()) continue;
                        usedIngredients.put(pair, usedCount + 1);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isPossible() {
        for (var offset : offsets) {
            if (!offset.isPosition && offset.getPos(mob.level()) == null) return false;
        }
        if (corner0 != null && !corner0.isPosition && corner0.getPos(mob.level()) == null) return false;
        if (corner0 != null && !corner1.isPosition && corner1.getPos(mob.level()) == null) return false;

        var heldItem = BindingManager.getHeldItem(mob);
        if (taskType == TaskType.TAKE) {
            return heldItem.isEmpty() || (isValidItem(heldItem) &&
                    heldItem.getCount() + countMin < heldItem.getMaxStackSize());
        } else if (taskType == TaskType.PUT) {
            if (isPlacingCobweb()) {
                return true;
            }
            return !heldItem.isEmpty() && isValidItem(heldItem) && heldItem.getCount() >= countMin;
        }
        return true;
    }

    private boolean isPlacingCobweb() {
        return boundGoal.cooldown == 0 && mob.getType() == EntityType.SPIDER && mob.getHealth() > COBWEB_DAMAGE && itemFilters.contains(Items.COBWEB);
    }

    public boolean fail() {
        registerSuccessOrFailure(onFailure);
        return true;
    }

    public boolean succeed() {
        registerSuccessOrFailure(onSuccess);
        return true;
    }

    public boolean tick() {
        if (spindle != null) {
            targetPos = spindle.getPos(mob.level());
            if (targetPos == null) return fail();
        }
        if (!isPossible()) return fail();

        if (taskType == TaskType.OBSERVE) {
            return succeed();
        }

        if (taskType == TaskType.ATTACK) {
            if (targetMob == null) {
                var target = new Vec3(targetPos.getX() + 0.5f, targetPos.getY() + 0.5f, targetPos.getZ() + 0.5f);
                mob.lookAt(EntityAnchorArgument.Anchor.FEET, target);
                if (boundGoal.cooldown > 0) return false;
                attackPosition(target);
                return succeed();
            }
            if (targetMob.isDeadOrDying()) {
                return succeed();
            }
            if (targetMob == null || !targetMob.isAttackable()) {
                return fail();
            }
            boundGoal.startAttacking(targetMob);
            return false;
        }

        var priorTargetPos = targetPos;

        if (harvesting && !isValidBlock(mob.level().getBlockState(targetPos))) return fail();

        if (taskType == TaskType.TAKE) {
            if (BindingManager.getHeldItem(mob).getCount() + countMin > BindingManager.getHeldItem(mob).getMaxStackSize()) {
                return fail();
            }
            if (isTakingFromGround) {
                if (targetItem == null || targetItem.isRemoved() || getPickupItemStack() == null || getPickupItemStack().isEmpty()) {
                    return fail();
                }
                targetPos = targetItem.blockPosition();
            }
        }

        if (mob.level().getGameTime() % 20 == mob.getId() % 20 || targetPos != priorTargetPos) {
            targetMovePos = getAccessibleBlock(targetPos);
        }
        if (targetMovePos == null) {
            return fail();
        }

        var isNotor = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getPath().equals("notor");

        if (mob.blockPosition().equals(lastPos)) {
            ticksInSameBlock++;
            if (ticksInSameBlock == 60) {
                var r = mob.getRandom();
                var scale = 1;
                mob.push((r.nextDouble() - 0.5) * scale, r.nextDouble() * scale, (r.nextDouble() - 0.5) * scale);
            }
            if (ticksInSameBlock > 100) return fail();
        } else {
            ticksInSameBlock = 0;
        }
        lastPos = mob.blockPosition();

//        LOGGER.debug(mob.blockPosition() + " -> " + targetMovePos + " (" + targetPos + ")");

        if (!isAtTarget()) {
            if (isNotor && !mob.level().getBlockState(mob.blockPosition()).isAir()) {
                mob.push(0, 0.07, 0);
            }
            if (mob.getNavigation().getPath() == null || mob.getNavigation().getTargetPos() != targetMovePos) {
                if (moveTo(targetMovePos.getX(), targetMovePos.getY() + (isNotor ? 1 : 0), targetMovePos.getZ())) {
                    return false;
                }
            }
            if (mob.getNavigation().isStuck()) {
                return fail();
            }
            if (mob.getNavigation().getPath() == null || !mob.getNavigation().getPath().isDone()) {
                return false;
            }
            var diff = targetMovePos.getCenter().subtract(0, 0.5f, 0).subtract(mob.position()).normalize();
            var adjustedPos = targetMovePos.getCenter().add(diff.scale(ticksInSameBlock / 20f));
            mob.getNavigation().moveTo(adjustedPos.x, adjustedPos.y + (isNotor ? 1 : 0), adjustedPos.z, 1);
            return false;
        }
        mob.getNavigation().moveTo(targetMovePos.getX(), targetMovePos.getY(), targetMovePos.getZ(), 1);

        ticksInSameBlock = 0;

        if (taskType == TaskType.MOVE) {
            mob.moveTo(targetMovePos.getX() + 0.5f, mob.position().y, targetMovePos.getZ() + 0.5f);
        }

        if (taskType == TaskType.TAKE) {
            if (targetItem == null) {
                int maxTransferAmount = BindingManager.getHeldItem(mob).getMaxStackSize() - BindingManager.getHeldItem(mob).getCount();
                maxTransferAmount = Math.min(maxTransferAmount, countMax);
                var transferredItems = ItemStack.EMPTY;

                BlockEntity be = mob.level().getBlockEntity(targetPos);

                if (be != null) {
                    var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                    if (cap.isPresent()) {
                        if (be instanceof ChalkCircle cc) {
                            cc.overrideLock = true;
                        }
                        var inventory = cap.get();
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack stack = inventory.getStackInSlot(i);
                            if (stack.isEmpty() || !isValidTakeItem(stack)) continue;
                            transferredItems = inventory.extractItem(i, Math.min(maxTransferAmount, stack.getCount()), false);
                            if (!transferredItems.isEmpty()) break;
                        }
                        if (be instanceof ChalkCircle cc) {
                            cc.overrideLock = false;
                        }
                    }
                }

                if (transferredItems.isEmpty()) return fail();

                BindingManager.setHeldItem(mob, new ItemStack(transferredItems.getItem(),
                        BindingManager.getHeldItem(mob).getCount() + transferredItems.getCount()));

            } else {
                var item = getPickupItemStack();
                int maxStack = item.getMaxStackSize();
                int transferAmount = Math.min(maxStack - BindingManager.getHeldItem(mob).getCount(), item.getCount());
                transferAmount = Math.min(transferAmount, countMax);
                if (transferAmount < countMin) return fail();
                Item itemType = item.getItem();
                var newItemStack = item.copyWithCount(BindingManager.getHeldItem(mob).getCount() + transferAmount);
                BindingManager.setHeldItem(mob, newItemStack);
                if (targetItem instanceof ItemEntity ie) {
                    ie.setItem(
                            new ItemStack(itemType, ie.getItem().getCount() - transferAmount));
                    if (ie.getItem().isEmpty()) {
                        targetItem.discard();
                    }
                } else {
                    targetItem.discard();
                }
            }
            return succeed();
        } else if (taskType == TaskType.PUT) {
            int inititalCount = BindingManager.getHeldItem(mob).getCount();
            boolean isContainer = false;
            var itemsToInsert = BindingManager.getHeldItem(mob).copy();
            if (itemsToInsert.getCount() < countMin) {
                LOGGER.debug("COUNT LESS THAN " + countMin);
                return fail();
            }
            var insertAttemptCount = Math.min(itemsToInsert.getCount(), countMax);
            itemsToInsert.setCount(insertAttemptCount);
            BlockEntity be = mob.level().getBlockEntity(targetPos);
            LOGGER.debug("insertAttemptCount: " + insertAttemptCount);
            if (be != null) {
                var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if (cap.isPresent()) {
                    var inventory = cap.get();
                    isContainer = true;
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        itemsToInsert = inventory.insertItem(i, itemsToInsert, false);
                        LOGGER.debug("COUNT: " + itemsToInsert.getCount());
                        if (itemsToInsert.isEmpty()) break;
                    }
                }
            }
            if (isContainer) {
                LOGGER.debug("IS CONTAINER");
                BindingManager.getHeldItem(mob).setCount(itemsToInsert.getCount() + inititalCount - insertAttemptCount);
                return BindingManager.getHeldItem(mob).getCount() < inititalCount ? succeed() : fail();
            }
            var stackToPlace = isPlacingCobweb() ? new ItemStack(Items.COBWEB, 1) : BindingManager.getHeldItem(mob);
            if (tryPlaceBlock(targetPos, stackToPlace, !isPlacingCobweb(), true)) {
                mob.moveTo(targetMovePos.getX() + 0.5f, mob.position().y, targetMovePos.getZ() + 0.5f);
                return succeed();
            }
            spawnAtTargetPos(stackToPlace.split(countMax));
            BindingManager.setHeldItem(mob, stackToPlace.isEmpty() ? ItemStack.EMPTY : stackToPlace);
            return succeed();
        } else if (taskType == TaskType.BREAK) {
            Vec3 lookPos = new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, lookPos);
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, lookPos);
            if (!mob.swinging) {
                mob.swing(InteractionHand.MAIN_HAND);
            }
            if (harvesting) {
                return harvestBlock();
            }
            var tool = BindingManager.getHeldItem(mob);
            BlockState blockstate = mob.level().getBlockState(targetPos);
            float destroyTime = blockstate.getBlock().defaultDestroyTime();
            AttributeInstance attackDamageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            float attackDamage = attackDamageAttr == null ? 0.5f : (float) attackDamageAttr.getValue();
            if (mob instanceof WitherBoss) {
                attackDamage = 50;
            }
            attackDamage = Math.max(attackDamage, 0.5f);
            boundGoal.nextTick = 0;
            doActionTicks++;
            if (tool.getItem() instanceof DiggerItem di) {
                if (di.isCorrectToolForDrops(tool, blockstate)) {
                    attackDamage *= di.getDestroySpeed(tool, blockstate);
                }
            }
            int newBlockDamage = destroyTime < 0 ? -1 : destroyTime == 0 ? 10
                                                        : (int) Math.floor(doActionTicks * attackDamage / destroyTime / 20);
            if (newBlockDamage != blockDamage) {
                if (newBlockDamage >= 10) {
                    if (felling && blockstate.is(BlockTags.LOGS)) {
                        targetPos = tryGetBranch(targetPos);
                    }
                    mob.level().playSound(null, targetPos, blockstate.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
                    this.mob.level().destroyBlock(targetPos, false, mob);
                    if (tool.isEmpty() && mob.getType() == EntityType.ENDERMAN) {
                        var tag = DiagramManager.getOrCreateLevelData(mob.level()).getPlacedItemTag(targetPos);
                        var item = blockstate.getBlock().asItem().getDefaultInstance();
                        item.setTag(tag);
                        BindingManager.setHeldItem(mob, item);
                    } else {
                        Block.dropResources(blockstate, mob.level(), targetPos, mob.level().getBlockEntity(targetPos), mob, tool);
                    }
                    return succeed();
                }
                mob.level().playSound(null, targetPos, blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1f, 1f);
                blockDamage = newBlockDamage;
                mob.level().destroyBlockProgress(this.mob.getId(), targetPos, blockDamage);
            }
            return false;
        } else if (taskType == TaskType.CRAFT) {
            if (doActionTicks == 0 && !isBlockAcceptableTarget(targetPos, false)) return fail();
            if (!mob.swinging) {
                mob.swing(InteractionHand.MAIN_HAND);
            }
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, targetPos.getCenter());
            var resultItem = recipe.getResultItem(mob.level().registryAccess());
            if (mob.level() instanceof ServerLevel sl && doActionTicks % 10 == 0) {
                sl.playSound(null, targetPos,
                        doActionTicks == 40 ? SoundEvents.LANTERN_BREAK : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
                var r = sl.random;
                for (var i = 0; i < 8; i++) {
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, resultItem),
                            targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
                            1, (r.nextFloat() - 0.5f) * 0.5f, r.nextFloat() * 0.5f, (r.nextFloat() - 0.5f) * 0.5f, 0.05);
                }
            }
            doActionTicks++;
            if (doActionTicks <= 40) return false;
            if (!isBlockAcceptableTarget(targetPos, false)) return fail();
            var craftAmount = resultItem.getMaxStackSize();
            for (var ingredient : usedIngredientsCache.entrySet()) {
                var amount = ingredient.getKey().getFirst().getStackInSlot(ingredient.getKey().getSecond()).getCount();
                craftAmount = Math.min(craftAmount, amount / ingredient.getValue());
                if (craftAmount == 0) return fail();
            }
            for (var ingredient : usedIngredientsCache.entrySet()) {
                var amount = ingredient.getKey().getFirst().extractItem(ingredient.getKey().getSecond(), craftAmount * ingredient.getValue(), true).getCount();
                if (amount / ingredient.getValue() != craftAmount) return fail();
            }
            for (var ingredient : usedIngredientsCache.entrySet()) {
                ingredient.getKey().getFirst().extractItem(ingredient.getKey().getSecond(), craftAmount * ingredient.getValue(), false);
            }
            var held = BindingManager.getHeldItem(mob);
            var stack = recipe.getResultItem(mob.level().registryAccess()).copyWithCount(craftAmount);
            if (held.isEmpty() || (held.is(stack.getItem()) &&
                    Objects.equals(held.getTag(), stack.getTag()) &&
                    held.getCount() + stack.getCount() <= stack.getMaxStackSize())) {
                BindingManager.setHeldItem(mob, stack);
            } else {
                spawnAtTargetPos(stack);
            }
        }
        return succeed();
    }

    private ItemStack getPickupItemStack() {
        return (targetItem instanceof AbstractArrow arrow ? getArrowItem(arrow) : ((ItemEntity) targetItem).getItem());
    }

    private boolean isAtTarget() {
        var bb = mob.getBoundingBox();
        for (var y = Mth.floor(bb.minY); y <= Mth.floor(bb.maxY); y++) {
            for (var x = Mth.floor(bb.minX); x <= Mth.floor(bb.maxX); x++) {
                for (var z = Mth.floor(bb.minZ); z <= Mth.floor(bb.maxZ); z++) {
                    if (new BlockPos(x, y, z).equals(targetMovePos)) return true;
                }
            }
        }
        return false;
    }

    private BlockPos tryGetBranch(BlockPos startPos) {
        var level = mob.level();
        var toMatch = level.getBlockState(startPos);
        var searched = new HashSet<BlockPos>();
        var toNotSearch = new HashSet<BlockPos>();
        searched.add(startPos);
        var toSearch = new ArrayDeque<BlockPos>();
        toSearch.add(startPos);
        var pos = startPos;
        while (searched.size() < 64 && !toSearch.isEmpty()) {
            pos = toSearch.pop();
            for (var dir : TreeStripper.directionsAndDiagonals) {
                var newpos = pos.offset(dir);
                if (searched.contains(newpos) || toNotSearch.contains(newpos) || toSearch.contains(newpos)) continue;
                var s = level.getBlockState(newpos);
                if (s.is(toMatch.getBlock())) {
                    toSearch.add(newpos);
                    searched.add(newpos);
                } else {
                    toNotSearch.add(newpos);
                }
            }
        }
        return pos;
    }

    private boolean harvestBlock() {
        BlockState blockstate = mob.level().getBlockState(targetPos);
        if (!(mob.level() instanceof ServerLevel sl) || !(blockstate.getBlock() instanceof CropBlock cb)) return fail();
        MutableBoolean hasTaken = new MutableBoolean(false);
        Item blockItem = blockstate.getBlock().asItem();
        Block.getDrops(blockstate, sl, targetPos, mob.level().getBlockEntity(targetPos), mob, mob.getMainHandItem().copy())
                .forEach((stack) -> {
                    if (stack.getItem() == blockItem && !hasTaken.getValue()) {
                        stack.shrink(1);
                        hasTaken.setValue(true);
                    }

                    if (!stack.isEmpty())
                        Block.popResource(sl, targetPos, stack);
                });
        if (hasTaken.isFalse()) {
            mob.level().playSound(null, targetPos, blockstate.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
            this.mob.level().destroyBlock(targetPos, false, mob);
        } else {
            mob.level().setBlockAndUpdate(targetPos, cb.defaultBlockState());
        }
        return succeed();
    }

    private void spawnAtTargetPos(ItemStack stackToPlace) {
        ItemEntity itementity = new ItemEntity(mob.level(),
                targetPos.getX() + 0.5f, targetPos.getY() + 0.5f, targetPos.getZ() + 0.5f,
                stackToPlace);
        itementity.setDefaultPickUpDelay();
        mob.level().addFreshEntity(itementity);
    }

    private boolean canAttackPosition(EntityType<?> type) {
        return type == EntityType.BLAZE || type == EntityType.GHAST || type == EntityType.SKELETON
                || type == EntityType.WITHER || type == EntityType.STRAY;
    }

    private void attackPosition(Vec3 target) {
        var type = mob.getType();
        var source = new Vec3(mob.getX(0.5f), mob.getY(0.5f), mob.getZ(0.5f));
        var diff = target.subtract(source);
        Level level = mob.level();
        if (type == EntityType.BLAZE) {
            SmallFireball smallfireball = new SmallFireball(level, mob, diff.x, diff.y, diff.z);
            smallfireball.setPos(source.x, source.y, source.z);
            level.addFreshEntity(smallfireball);
            boundGoal.cooldown = 90;
        } else if (type == EntityType.GHAST) {
            LargeFireball largeFireball = new LargeFireball(level, mob, diff.x, diff.y, diff.z, 1);
            largeFireball.setPos(source.x, source.y, source.z);
            level.addFreshEntity(largeFireball);
            boundGoal.cooldown = 120;
        } else if (type == EntityType.SKELETON || type == EntityType.STRAY) {
            Arrow projectile = new Arrow(level, source.x, source.y, source.z);
            if (type == EntityType.STRAY) {
                projectile.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            }
            projectile.pickup = AbstractArrow.Pickup.DISALLOWED;
            projectile.shoot(diff.x, diff.y, diff.z, 1.1f, 6.0f);
            level.addFreshEntity(projectile);
            boundGoal.cooldown = 40;
        }
    }

    private boolean tryPlaceBlock(BlockPos placePos, ItemStack stack, boolean depleteHand, boolean actuallyPlace) {
        if (!(stack.getItem() instanceof BlockItem bi) || !mob.level().isEmptyBlock(placePos)) {
            return false;
        }
        BlockPlaceContext ctx = new BlockPlaceContext(mob.level(), null, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(
                        new Vec3(placePos.getX() + 0.5f, placePos.getY(), placePos.getZ() + 0.5f),
                        Direction.UP, placePos, false));
        if (!ctx.canPlace()) {
            return false;
        }
        BlockState blockstate = bi.getBlock().getStateForPlacement(ctx);
        if (blockstate == null) {
            return false;
        }
        if (!actuallyPlace) {
            return true;
        }
        if (!this.placeBlock(ctx, blockstate)) {
            return false;
        }
        Level level = ctx.getLevel();
        level.gameEvent(null, GameEvent.BLOCK_PLACE, placePos);
        stack.setCount(stack.getCount() - 1);
        if (depleteHand) {
            BindingManager.setHeldItem(mob, stack);
        } else {
            boundGoal.cooldown = 20 * 2;
            mob.hurt(mob.level().damageSources().fellOutOfWorld(), COBWEB_DAMAGE);
        }
        return true;
    }

    protected boolean placeBlock(BlockPlaceContext p_40578_, BlockState p_40579_) {
        return p_40578_.getLevel().setBlock(p_40578_.getClickedPos(), p_40579_, 11);
    }

    private void registerSuccessOrFailure(BoundGoal.ContractDecider decider) {
        if (doActionTicks > 0) {
            doActionTicks = 0;
            if (taskType == TaskType.BREAK) {
                blockDamage = -1;
                mob.level().destroyBlockProgress(mob.getId(), targetPos, -1);
            }
        }
        resetLookIndex();
        isTakingFromGround = false;
        targetItem = null;
        targetPos = null;
        targetMob = null;
        directPath = null;
        ticksInSameBlock = 0;
        lastPos = null;
        BindingManager.stopAttacking(mob);
        boundGoal.isAttacking = false;
        decider = decider != null ? decider : onEither != null ? onEither : boundGoal.rootDecider;
        boundGoal.switchToDecider(decider);
    }

    public void resetLookIndex() {
        lookIndex = 0;
        potentialTargets.clear();
    }

    private boolean moveTo(int x, int y, int z) {
        if (mob instanceof EnderMan em && boundGoal.cooldown == 0 && mob.position().distanceToSqr(new Vec3(x, y, z)) > 9
                && mob.level().isEmptyBlock(new BlockPos(x, y + 1, z))
                && mob.level().isEmptyBlock(new BlockPos(x, y + 2, z))
                && mob.level().isEmptyBlock(new BlockPos(x, y + 3, z))) {
            em.teleportTo(x + 0.5, y + 1, z + 0.5);
            boundGoal.cooldown = 200;
        }
        return mob.getNavigation().moveTo(mob.getNavigation().createPath(new BlockPos(x, y, z), 0), 1);
    }
}
