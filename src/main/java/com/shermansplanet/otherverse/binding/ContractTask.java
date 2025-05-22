package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.*;

public class ContractTask {
    private static final int COBWEB_DAMAGE = 3;

    private final Mob mob;
    private int countMin = 0;
    private int countMax = 64;
    public BlockPos targetPos, targetMovePos;
    public ContractManager.PositionOrSpindle spindle;
    public ItemEntity targetItem;
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
    private HashMap<ItemStack, Integer> usedIngredientsCache = new HashMap<>();
    private boolean onlyHasInventoryBlockFilters = true;
    public boolean isTakingFromGround;
    private int blockDamage = -1;
    private int doActionTicks;
    public final ContractManager.PositionOrSpindle corner0;
    public final ContractManager.PositionOrSpindle corner1;
    private Recipe<?> recipe;
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean harvesting = false;
    public int lookIndex;
    public ArrayList<BlockPos> potentialTargets = new ArrayList<>();
    public final String taskId;
    private Path directPath;

    public void getDebugMessage(StringBuilder s) {
        s.append("task: ").append(taskType).append("\n");
        s.append("my position: ").append(mob.blockPosition()).append("\n");
        s.append("task target: ").append(targetPos).append("\n");
        s.append("move target: ").append(targetMovePos).append("\n");
        s.append("phase: ").append(directPath == null ? "initial" : "final").append("\n");
        s.append("path: ").append(mob.getNavigation().getPath() == null ? "null" : "not null")
                .append(", ").append(mob.getNavigation().getPath().isDone() ? "done" : "not done");
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
        if (tag.contains("count")) {
            if (tag.contains("min")) {
                countMin = tag.getInt("min");
                countMax = tag.getInt("max");
            } else {
                countMin = tag.getInt("count");
                countMax = tag.getInt("count");
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
            recipe = mob.level.getServer().getRecipeManager().byKey(new ResourceLocation(tag.getString("recipe_id"))).get();
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
                offsets.add(new ContractManager.PositionOrSpindle(tag.getCompound(key)));
                var pos = tag.getIntArray(key + "_basis");
                offsetBases.add(new BlockPos(pos[0], pos[1], pos[2]));
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
        if (itemFilters.isEmpty() && !harvesting) return true;
        var block = blockState.getBlock();
        if (harvesting && (!(block instanceof CropBlock cb && cb.isMaxAge(blockState)))) return false;
        if (itemFilters.isEmpty()) return true;
        if (!itemFilters.contains(block.asItem())) return false;
        if (blockstateFilters.isEmpty()) return true;
        var blockstateFilter = blockstateFilters.get(block);
        if (blockstateFilter == null) return true;
        for (var prop : blockState.getValues().entrySet()) {
            if (!blockstateFilter.props.get(prop.getKey().getName()).equals(prop.getValue().toString())) return false;
        }
        return true;
    }

    public ItemEntity getClosestDroppedItem() {
        ItemEntity closest = null;
        var lookDiameter = boundGoal.range * 2;
        float closestDist = lookDiameter;
        for (Entity e : mob.level.getEntities(mob, AABB.ofSize(mob.position(), lookDiameter, lookDiameter, lookDiameter))) {
            if (e instanceof ItemEntity ie && isValidItem(ie.getItem())) {
                if (!positionFilters.isEmpty() && !positionFilters.contains(ie.blockPosition())) {
                    continue;
                }
                if (!BindingManager.getHeldItem(mob).isEmpty() && !BindingManager.getHeldItem(mob).is(ie.getItem().getItem()))
                    continue;
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    Block block = mob.level.getBlockState(e.blockPosition().subtract(processOffset(offsetIndex))).getBlock();
                    if (!blockFilters.isEmpty() && !blockFilters.contains(block.asItem()) && !blockFilters.contains(BlockFocus.blockReplacements.get(block))) {
                        continue;
                    }
                    float dist = (float) ie.position().distanceTo(mob.position());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = ie;
                    }
                }
            }
        }
        return closest;
    }

    private Vec3i processOffset(int offsetIndex) {
        return offsets.get(offsetIndex).getPos(mob.level).subtract(offsetBases.get(offsetIndex));
    }

    public LivingEntity getClosestValidLivingTarget() {
        LivingEntity closest = null;
        var lookDiameter = boundGoal.range * 2;
        float closestDist = lookDiameter;
        for (Entity e : mob.level.getEntities(mob, AABB.ofSize(mob.position(), lookDiameter, lookDiameter, lookDiameter))) {
            if (e == mob) {
                continue;
            }
            if (e instanceof LivingEntity le) { //  && mob.canAttack(le)
                if (entityFilters.isEmpty() && blockFilters.isEmpty() && e.getType() == EntityType.PLAYER) continue;
                if (!entityFilters.isEmpty() && !entityFilters.contains(e.getType())) {
                    continue;
                }
                if (!blockFilters.isEmpty() && !blockFilters.contains(Items.CHAIN)
                        && e.getPersistentData().hasUUID("bindingId")) {
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

    public boolean isAcceptableTarget(BlockPos pos, boolean inPositionFilter) {
        if (pos == null) return false;
        if (!isBlockAcceptableTarget(pos, inPositionFilter)) return false;

        var h = mob.getBbHeight();
        var potentialMovePositions = new ArrayList<BlockPos>();
        var mobPos = mob.blockPosition();
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
        potentialMovePositions.sort(Comparator.comparingDouble(mobPos::distSqr));
        for (var potentialPos : potentialMovePositions) {
            if (!canReachBlock(potentialPos)) continue;
            targetMovePos = potentialPos;
            return true;
        }
        return false;
        //return internalAcceptableTarget(pos, inPositionFilter) && canReachBlock(pos);
    }

    private boolean isBlockAcceptableTarget(BlockPos pos, boolean inPositionFilter) {
        switch (taskType) {
            case BREAK:
                if (!blockFilters.isEmpty()) {
                    var anyMatches = false;
                    for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                        Block block = mob.level.getBlockState(pos.subtract(processOffset(offsetIndex))).getBlock();
                        if (blockFilters.contains(block.asItem()) || blockFilters.contains(BlockFocus.blockReplacements.get(block))) {
                            anyMatches = true;
                        }
                    }
                    if (!anyMatches) {
                        return false;
                    }
                }

                if (mob.level.isEmptyBlock(pos)) {
                    return false;
                }
                // fall back to MOVE
            case MOVE:
                return isValidBlock(mob.level.getBlockState(pos));
            case PUT:
                if (blockFilters.isEmpty() && inPositionFilter && mob.level.isEmptyBlock(pos)) {
                    return true;
                }
                ItemStack heldItem = BindingManager.getHeldItem(mob);
                BlockEntity putTarget = mob.level.getBlockEntity(pos);
                boolean validPutBlockBelow = false;
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    var otherpos = pos.subtract(processOffset(offsetIndex));
                    validPutBlockBelow = blockFilters.contains(mob.level.getBlockState(otherpos).getBlock().asItem());
                    if (validPutBlockBelow) {
                        BlockEntity beBelow = mob.level.getBlockEntity(otherpos);
                        if (beBelow != null && beBelow.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                            validPutBlockBelow = false;
                        }
                    }
                    if (validPutBlockBelow) {
                        break;
                    }
                }
                BlockState blockState = mob.level.getBlockState(pos);
                if (!inPositionFilter && !validPutBlockBelow && !blockFilters.contains(blockState.getBlock().asItem())) {
                    return false;
                }

                if (isPlacingCobweb()) {
                    return validPutBlockBelow && mob.level.isEmptyBlock(pos);
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
                if (canDrop && !blockState.isCollisionShapeFullBlock(mob.level, pos)) {
                    return true;
                }
                return tryPlaceBlock(pos, heldItem, false, false);
            case OBSERVE:
            case TAKE:
                BlockEntity takeTarget = mob.level.getBlockEntity(pos);
                if (takeTarget == null) {
                    return false;
                }
                boolean validTakeBlockBelow = false;
                for (var offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
                    if (blockFilters.contains(mob.level.getBlockState(pos.subtract(processOffset(offsetIndex))).getBlock().asItem())) {
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
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.isEmpty()) continue;
                        if (isValidTakeItem(stack)) {
                            return true;
                        }
                    }
                }
                return false;
            case ATTACK:
                return canAttackPosition(mob.getType());
            case CRAFT:
                if (mob.level.getBlockState(pos).getBlock() != Blocks.CRAFTING_TABLE) return false;
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
        if (stack.getCount() < countMin) return false;
        var held = BindingManager.getHeldItem(mob);
        if (held.isEmpty()) return true;
        return held.is(stack.getItem()) && Objects.equals(held.getTag(), stack.getTag());
    }

    private boolean canReachBlock(BlockPos pos) {
        if (taskType == TaskType.ATTACK || taskType == TaskType.OBSERVE) return true;
        if (mob.blockPosition().distSqr(pos) > 16 * 16) return true;
        if (mob.getType() == EntityType.ENDERMAN || mob.getType() == EntityType.SHULKER) {
            if (mob.level.getBlockState(pos.below()).getCollisionShape(mob.level, pos.below()).isEmpty()) return false;
        } else {
            var path = mob.getNavigation().createPath(pos, 0);
            if (path == null || !path.getEndNode().asBlockPos().equals(pos)) return false;
        }
        return !mob.level.collidesWithSuffocatingBlock(mob, new AABB(
                pos.getX() + 0.1f, pos.getY() + 0.5f, pos.getZ() + 0.1f,
                pos.getX() + 0.9f, pos.getY() + mob.getBbHeight(), pos.getZ() + 0.0f
        ));
    }

    private boolean findCraftingIngredient(Ingredient ingredient, BlockPos pos, HashMap<ItemStack, Integer> usedIngredients) {
        if (ingredient.isEmpty()) return true;
        for (var dir : Direction.values()) {
            var checkTarget = mob.level.getBlockEntity(pos.relative(dir));
            if (checkTarget instanceof IItemHandler inventory) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!matchesIngredient(ingredient, stack, usedIngredients)) continue;
                    return true;
                }
            } else if (checkTarget instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!matchesIngredient(ingredient, stack, usedIngredients)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesIngredient(Ingredient ingredient, ItemStack stack, HashMap<ItemStack, Integer> usedIngredients) {
        if (stack.isEmpty() || !ingredient.test(stack)) return false;
        if (!usedIngredients.containsKey(stack)) {
            usedIngredients.put(stack, 1);
            return true;
        }
        var usedCount = usedIngredients.get(stack);
        if (usedCount >= stack.getCount()) return false;
        usedIngredients.put(stack, usedCount + 1);
        return true;
    }

    public boolean isPossible() {
        for (var offset : offsets) {
            if (!offset.isPosition && offset.getPos(mob.level) == null) return false;
        }
        if (corner0 != null && !corner0.isPosition && corner0.getPos(mob.level) == null) return false;
        if (corner0 != null && !corner1.isPosition && corner1.getPos(mob.level) == null) return false;

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
            targetPos = spindle.getPos(mob.level);
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

        if (taskType == TaskType.TAKE) {
            if (BindingManager.getHeldItem(mob).getCount() + countMin >= BindingManager.getHeldItem(mob).getMaxStackSize()) {
                return fail();
            }
            if (isTakingFromGround) {
                if ((targetItem == null || targetItem.isRemoved() || targetItem.getItem().isEmpty())) {
                    return fail();
                }
                targetPos = targetItem.blockPosition();
                targetMovePos = targetPos;
            } else if (!isAcceptableTarget(targetPos, true)) {
                return fail();
            }
        } else if (!isAcceptableTarget(targetPos, true)) {
            return fail();
        }

//        LOGGER.debug(mob.blockPosition() + " -> " + targetMovePos + " (" + targetPos + ")");

        if (directPath == null) {
            if (moveTo(targetMovePos.getX(), targetMovePos.getY(), targetMovePos.getZ())) {
                return false;
            }
        }
        if (mob.getNavigation().isStuck()) {
            return fail();
        }
        if (mob.getNavigation().getPath() == null || !mob.getNavigation().getPath().isDone()) {
            return false;
        }

        if (directPath == null && !mob.blockPosition().equals(targetMovePos)) {
            directPath = new Path(List.of(
//                    new Node(mob.blockPosition().getX(), mob.blockPosition().getY(), mob.blockPosition().getZ()),
                    new Node(targetMovePos.getX(), targetMovePos.getY(), targetMovePos.getZ())
            ), targetMovePos, false);
            mob.getNavigation().moveTo(directPath, 1);
            return false;
        }

        mob.moveTo(targetMovePos.getX() + 0.5f, mob.position().y, targetMovePos.getZ() + 0.5f);

        if (taskType == TaskType.TAKE) {
            if (targetItem == null) {
                int maxTransferAmount = BindingManager.getHeldItem(mob).getMaxStackSize() - BindingManager.getHeldItem(mob).getCount();
                maxTransferAmount = Math.min(maxTransferAmount, countMax);
                var transferredItems = ItemStack.EMPTY;

                BlockEntity be = mob.level.getBlockEntity(targetPos);

                if (be != null) {
                    var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                    if (cap.isPresent()) {
                        var inventory = cap.get();
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack stack = inventory.getStackInSlot(i);
                            if (stack.isEmpty() || !isValidTakeItem(stack)) continue;
                            transferredItems = inventory.extractItem(i, Math.min(maxTransferAmount, stack.getCount()), false);
                            if (!transferredItems.isEmpty()) break;
                        }
                    }
                }

                if (transferredItems.isEmpty()) return fail();

                BindingManager.setHeldItem(mob, new ItemStack(transferredItems.getItem(),
                        BindingManager.getHeldItem(mob).getCount() + transferredItems.getCount()));

            } else {
                int maxStack = targetItem.getItem().getMaxStackSize();
                int transferAmount = Math.min(maxStack - BindingManager.getHeldItem(mob).getCount(), targetItem.getItem().getCount());
                transferAmount = Math.min(transferAmount, countMax);
                if (transferAmount < countMin) return fail();
                Item itemType = targetItem.getItem().getItem();
                BindingManager.setHeldItem(mob,
                        new ItemStack(itemType, BindingManager.getHeldItem(mob).getCount() + transferAmount));
                targetItem.setItem(
                        new ItemStack(itemType, targetItem.getItem().getCount() - transferAmount));
                if (targetItem.getItem().isEmpty()) {
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
            BlockEntity be = mob.level.getBlockEntity(targetPos);
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
            BlockState blockstate = mob.level.getBlockState(targetPos);
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
                    mob.level.playSound(null, targetPos, blockstate.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
                    this.mob.level.destroyBlock(targetPos, false, mob);
                    if (tool.isEmpty() && mob instanceof EnderMan em) {
                        BindingManager.setHeldItem(mob, blockstate.getBlock().asItem().getDefaultInstance());
                    } else {
                        Block.dropResources(blockstate, mob.level, targetPos, mob.level.getBlockEntity(targetPos), mob, tool);
                    }
                    return succeed();
                }
                mob.level.playSound(null, targetPos, blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1f, 1f);
                blockDamage = newBlockDamage;
                mob.level.destroyBlockProgress(this.mob.getId(), targetPos, blockDamage);
            }
            return false;
        } else if (taskType == TaskType.CRAFT) {
            if (!mob.swinging) {
                mob.swing(InteractionHand.MAIN_HAND);
            }
            doActionTicks++;
            if (doActionTicks < 60) return false;
            for (var dir : Direction.values()) {
                var be = mob.level.getBlockEntity(targetPos.relative(dir));
                if (be instanceof IItemHandler inventory) {
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.isEmpty()) continue;
                        var amount = usedIngredientsCache.get(stack);
                        if (amount == null) continue;
                        if (stack.hasCraftingRemainingItem()) {
                            spawnAtTargetPos(stack.getCraftingRemainingItem());
                        }
                        inventory.extractItem(i, amount, false);
                    }
                } else if (be instanceof Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack stack = container.getItem(i);
                        if (stack.isEmpty()) continue;
                        var amount = usedIngredientsCache.get(stack);
                        if (amount == null) continue;
                        if (stack.hasCraftingRemainingItem()) {
                            spawnAtTargetPos(stack.getCraftingRemainingItem());
                        }
                        container.removeItem(i, amount);
                    }
                }
            }
            spawnAtTargetPos(recipe.getResultItem().copy());
        }
        return succeed();
    }

    private boolean harvestBlock() {
        BlockState blockstate = mob.level.getBlockState(targetPos);
        if (!(mob.level instanceof ServerLevel sl) || !(blockstate.getBlock() instanceof CropBlock cb)) return fail();
        MutableBoolean hasTaken = new MutableBoolean(false);
        Item blockItem = blockstate.getBlock().asItem();
        Block.getDrops(blockstate, sl, targetPos, mob.level.getBlockEntity(targetPos), mob, mob.getMainHandItem().copy())
                .forEach((stack) -> {
                    if (stack.getItem() == blockItem && !hasTaken.getValue()) {
                        stack.shrink(1);
                        hasTaken.setValue(true);
                    }

                    if (!stack.isEmpty())
                        Block.popResource(sl, targetPos, stack);
                });
        if (hasTaken.isFalse()) {
            mob.level.playSound(null, targetPos, blockstate.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
            this.mob.level.destroyBlock(targetPos, false, mob);
        } else {
            var prop = cb.getAgeProperty();
            blockstate = blockstate.setValue(prop, cb.defaultBlockState().getValue(prop));
            mob.level.setBlockAndUpdate(targetPos, blockstate);
        }
        return succeed();
    }

    private void spawnAtTargetPos(ItemStack stackToPlace) {
        ItemEntity itementity = new ItemEntity(mob.level,
                targetPos.getX() + 0.5f, targetPos.getY() + 0.5f, targetPos.getZ() + 0.5f,
                stackToPlace);
        itementity.setDefaultPickUpDelay();
        mob.level.addFreshEntity(itementity);
    }

    private boolean canAttackPosition(EntityType<?> type) {
        return type == EntityType.BLAZE || type == EntityType.GHAST || type == EntityType.SKELETON
                || type == EntityType.WITHER || type == EntityType.STRAY;
    }

    private void attackPosition(Vec3 target) {
        var type = mob.getType();
        var source = new Vec3(mob.getX(0.5f), mob.getY(0.5f), mob.getZ(0.5f));
        var diff = target.subtract(source);
        Level level = mob.getLevel();
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
        if (!(stack.getItem() instanceof BlockItem bi) || !mob.level.isEmptyBlock(placePos)) {
            return false;
        }
        BlockPlaceContext ctx = new BlockPlaceContext(mob.level, null, InteractionHand.MAIN_HAND, stack,
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
            mob.hurt(DamageSource.OUT_OF_WORLD, COBWEB_DAMAGE);
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
                mob.level.destroyBlockProgress(mob.getId(), targetPos, -1);
            }
        }
        resetLookIndex();
        isTakingFromGround = false;
        targetItem = null;
        targetPos = null;
        targetMob = null;
        directPath = null;
        boundGoal.stopAttacking();
        decider = decider != null ? decider : onEither != null ? onEither : boundGoal.rootDecider;
        boundGoal.switchToDecider(decider);
    }

    public void resetLookIndex() {
        lookIndex = 0;
        potentialTargets.clear();
    }

    private boolean moveTo(int x, int y, int z) {
        if (mob instanceof EnderMan em && boundGoal.cooldown == 0 && mob.position().distanceToSqr(new Vec3(x, y, z)) > 9
                && mob.level.isEmptyBlock(new BlockPos(x, y + 1, z))
                && mob.level.isEmptyBlock(new BlockPos(x, y + 2, z))
                && mob.level.isEmptyBlock(new BlockPos(x, y + 3, z))) {
            em.teleportTo(x + 0.5, y + 1, z + 0.5);
            boundGoal.cooldown = 200;
        }
        return mob.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1);
    }
}
