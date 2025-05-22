package com.shermansplanet.otherverse.binding;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.familiar.FaceSetter;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class BoundGoal extends Goal {

    private static final String REMEMBERED_BLOCKS = "remembered_blocks";

    private Mob mob;
    private BindingInfo binding;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final int bindingWearInterval;
    public int cooldown = 0;
    private int currentMode = 0;
    public Player practitioner;
    private boolean isAttacking;
    private boolean isPartOfSwarm;
    private boolean isLoyaltyBound;
    public int nextTick = 0;
    public boolean isTamed;
    public int range = 8;

    public BoundGoal(Mob m, BindingInfo binding) {
        mob = m;
        currentMode = mob.getPersistentData().getInt("familiar_mode");
        if (!mob.getPersistentData().contains(REMEMBERED_BLOCKS)) {
            mob.getPersistentData().put(REMEMBERED_BLOCKS, new CompoundTag());
        }
        this.binding = binding;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
        bindingWearInterval = BindingManager.getBindingWearInterval(mob.getMaxHealth());
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.targetSelector.disableControlFlag(Flag.TARGET);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public HashSet<Item> itemDropDenyList = new HashSet<>();
    public boolean takesAll;

    public void switchToDecider(ContractDecider decider) {
        currentDecider = decider;
        currentTask = null;
    }

    public void toggleFamiliarBehavior() {
        getPractitioner();
        if (rootDecider == null) {
            currentMode = (currentMode % 2) + 1;
        } else {
            currentMode = (currentMode + 1) % 3;
        }
        mob.getPersistentData().putInt("familiar_mode", currentMode);
        displayFamiliarMode();
    }

    private void getPractitioner() {
        if (practitioner != null) return;
        var playerName = isLoyaltyBound ? mob.getPersistentData().getString("practitioner_loyalty") : FamiliarManager.getPractitionerForFamiliar(mob);
        for (var player : mob.getLevel().players()) {
            if (!player.getGameProfile().getName().equals(playerName)) continue;
            practitioner = player;
            break;
        }
    }

    private void displayFamiliarMode() {
        var name = (mob.hasCustomName() ? mob.getCustomName() : mob.getType().getDescription()).copy();
        var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        var s = switch (currentMode) {
            case 0 -> " is now obeying its contract.";
            case 1 -> " is now following you"
                    + ((atk == null || atk.getValue() < 0.5f) ? "." : " and attacking your enemies.");
            case 2 -> " is now staying put.";
            default -> throw new IllegalStateException("Unexpected value: " + currentMode);
        };
        practitioner.displayClientMessage(name.append(Component.literal(s)), true);
    }

    public void tetherToPlayer(ServerPlayer player, boolean isPartOfSwarm) {
        practitioner = player;
        currentMode = 1;
        this.isPartOfSwarm = isPartOfSwarm;
        this.isLoyaltyBound = true;
        if (player == null) cooldown = 60;
    }

    public void loadUnboundContract() {
        isTamed = true;
        applyContract(mob.getPersistentData().getCompound("unbound_contract"));
        cooldown = 1060;
    }

    public class ContractDecider {
        public List<ContractTask> tasks = new ArrayList<>();
        public ContractDecider fallback = null;
    }

    private ContractDecider currentDecider = null;
    public ContractDecider rootDecider = null;
    private ContractTask currentTask = null;
    private int lookIndex;

    private IFocus GetFocus(BlockPos pos) {
        if (mob.level.getBlockEntity(pos) instanceof ChalkCircle cc) {
            return cc;
        }
        BlockFocus bf = DiagramManager.getOrCreateLevelData(mob.level).allBlockFoci.get(pos);
        if (bf != null) {
            return bf;
        }
        return null;
    }

    public void tick() {
        if (FamiliarManager.isFamiliar(mob)) {
            if (FamiliarManager.fishingMobs.contains(mob.getType())) {
                goFish();
            } else if (mob.getType() == EntityType.SHEEP) {
                makeDye();
            }
        }
        if (currentMode == 1) {
            followPlayer();
        } else if (currentMode == 2) {
            mob.getNavigation().stop();
        } else if (currentMode == 0 && currentDecider != null) {
            tickContract();
            if (isTamed && cooldown == 1000) {
                cooldown = 0;
                OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new BindingUpdateMessage(mob, BindingUpdateMessage.BindingUpdateType.CONTRACT, mob.getPersistentData(), true));
            }
        }
        if (mob.level instanceof ServerLevel sl) {
            mob.getBrain().setActiveActivityToFirstValid(ImmutableList.of(isAttacking ? Activity.FIGHT : Activity.IDLE));
        }
        if (FamiliarManager.isFamiliar(mob)) return;
        if (!BindingManager.drainsBindings((EntityType<? extends LivingEntity>) mob.getType())) {
            return;
        }
        //if (currentDecider == null) return;
        if (mob.level.getGameTime() % (20L * bindingWearInterval) != 0) {
            return;
        }
        var demesne = DemesnesManager.getData(binding.getLocalLevel(), binding.position);
        if (demesne != null && demesne.getPerkLevel(DemesnesManager.DemesnePerk.CAGE) > 0) return;
        LOGGER.debug("BINDING WEAR");
        if (!binding.getLocalLevel().isLoaded(binding.position)) {
            LOGGER.debug("BREAKING BINDING BECAUSE NOT LOADED");
            BindingManager.breakBinding(binding);
            return;
        }
        BlockFocus bindingFocus = DiagramManager.getOrCreateLevelData(binding.getLocalLevel()).allBlockFoci.get(binding.position);

        if (bindingFocus == null) {
            LOGGER.debug("FOCUS NOT FOUND");
            BindingManager.breakBinding(binding);
            return;
        }

        List<IFocus> foci = new ArrayList<>();

        int SPIRIT_DRAIN = (int) Math.ceil(mob.getMaxHealth() / 10);

        for (var influence : bindingFocus.getDiagram().influences.entrySet()) {
            if (!influence.getValue().equals(bindingFocus.getPos())) {
                continue;
            }
            IFocus focus = GetFocus(influence.getKey());
            if (focus == null) {
                continue;
            }
            ItemStack itemStack = focus.getItem();
            if (MobBindingInfluenceUtils.GetInfluence(mob, itemStack) == 0) {
                continue;
            }
            if (itemStack.hasTag() && itemStack.getTag().contains("hallow") &&
                    itemStack.getTag().getCompound("hallow").getInt("spirit_count") < SPIRIT_DRAIN) {
                continue;
            }
            foci.add(focus);
        }

        IFocus mostUniqueFocus = getMostUniqueFocus(foci);

        ItemStack mostUniqueItem = mostUniqueFocus.getItem();
        if (mostUniqueItem.hasTag() && mostUniqueItem.getTag().contains("hallow")) {
            CompoundTag hallowTag = mostUniqueItem.getTag().getCompound("hallow");
            hallowTag.putInt("spirit_count", hallowTag.getInt("spirit_count") - SPIRIT_DRAIN);
            if (mostUniqueFocus.isBlock()) {
                DiagramManager.getOrCreateLevelData(mob.level)
                        .putPlacedItemTag(mostUniqueFocus.getPos(), hallowTag);
            }
        } else if (mostUniqueItem.getItem() instanceof IdolItem) {
            BindingInfo bindingInfo = DiagramManager.getOrCreateLevelData(mob.level).bindingsByPosition.get(mostUniqueFocus.getPos());
            if (bindingInfo == null || bindingInfo.mob == null) {
                LOGGER.debug("NULL BINDING INFO");
            } else {
                bindingInfo.mob.hurt(DamageSource.OUT_OF_WORLD, SPIRIT_DRAIN);
            }
        } else {
            mostUniqueFocus.removeItem();
        }

        if (mob.level instanceof ServerLevel sl) {
            BlockPos bp = mostUniqueFocus.getPos();
            for (int i = 0; i < 10; ++i) {
                sl.sendParticles(ParticleTypes.POOF, bp.getX() + 0.5, bp.getY() + 0.25, bp.getZ() + 0.5,
                        1, 0, 0, 0, 0.15);
            }

            LOGGER.debug("ACTIVATING DIAGRAM: BINDING WEAR");
            DiagramManager.markDiagramActive(sl, bindingFocus.getDiagram());
        }
    }

    @Nullable
    private static IFocus getMostUniqueFocus(List<IFocus> foci) {
        HashMap<Item, Integer> itemCounts = new HashMap<>();

        for (IFocus focus : foci) {
            Item item = focus.getItem().getItem();
            if (!itemCounts.containsKey(item)) {
                itemCounts.put(item, 0);
            }
            itemCounts.put(item, itemCounts.get(item) + 1);
        }

        IFocus mostUniqueFocus = null;
        int lowest = Integer.MAX_VALUE;
        for (IFocus focus : foci) {
            Item item = focus.getItem().getItem();
            int count = itemCounts.get(item);
            if (count < lowest) {
                mostUniqueFocus = focus;
                lowest = count;
            }
        }
        return mostUniqueFocus;
    }

    private void makeDye() {
        if (mob.level.getGameTime() % 200 != 0 || !(mob.getLevel() instanceof ServerLevel sl)) return;
        if (!BindingManager.getHeldItem(mob).isEmpty()) return;
        var dyeItem = switch (((Sheep) mob).getColor()) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
        BindingManager.setHeldItem(mob, dyeItem.getDefaultInstance());
    }

    private void goFish() {
        if (mob.level.getGameTime() % 20 != 0 || !(mob.getLevel() instanceof ServerLevel sl)) return;
        if (!BindingManager.getHeldItem(mob).isEmpty()) return;
        var FISH_KEY = "fishing_timer";
        var data = mob.getPersistentData();
        if (!data.contains(FISH_KEY)) {
            data.putInt(FISH_KEY, 10);
            return;
        }
        var isPiglin = mob.getType() == EntityType.PIGLIN;
        if (!isPiglin && !mob.isUnderWater()) return;
        var timer = data.getInt(FISH_KEY) - 1;
        if (timer > 0) {
            data.putInt(FISH_KEY, timer);
            return;
        }
        var wait = (isPiglin) ? 20 : 60 * 5;
        data.putInt(FISH_KEY, 10 + mob.getRandom().nextInt(wait));

        List<ItemStack> list;

        if (isPiglin) {
            LootTable loottable = sl.getServer().getLootTables().get(BuiltInLootTables.PIGLIN_BARTERING);
            list = loottable.getRandomItems(new LootContext.Builder(sl).withParameter(LootContextParams.THIS_ENTITY, mob).withRandom(mob.level.random).create(LootContextParamSets.PIGLIN_BARTER));
        } else {
            var tempFishingHook = new FishingHook(null, mob.level, 0, 0);
            LootContext.Builder lootcontext$builder = (new LootContext.Builder(sl)).withParameter(LootContextParams.ORIGIN, mob.position()).withParameter(LootContextParams.TOOL, Items.FISHING_ROD.getDefaultInstance())
                    .withParameter(LootContextParams.THIS_ENTITY, tempFishingHook).withRandom(mob.getRandom()).withLuck(0);
            lootcontext$builder.withParameter(LootContextParams.KILLER_ENTITY, mob).withParameter(LootContextParams.THIS_ENTITY, tempFishingHook);
            LootTable loottable = sl.getServer().getLootTables().get(BuiltInLootTables.FISHING);
            list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));
            tempFishingHook.discard();
        }
        if (list.isEmpty()) return;
        var stack = list.get(0);
        var isFish = stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
        if (isFish != (mob.getType() == EntityType.SQUID)) return;
        BindingManager.setHeldItem(mob, stack);
    }

    private void followPlayer() {
        cooldown--;
        if (cooldown <= 0) {
            cooldown = 10;
            getPractitioner();
            if (practitioner == null) {
                if (isPartOfSwarm) poof();
                return;
            }
            var dist = mob.distanceToSqr(practitioner);
            if (isPartOfSwarm && dist > FamiliarManager.TETHER_DISTANCE * FamiliarManager.TETHER_DISTANCE) {
                poof();
                return;
            }
            if (!isAttacking) {
                var limit = isLoyaltyBound ? 6 : 2;
                if (dist > limit * limit) {
                    if (mob instanceof Shulker shulker) {
                        if (dist > 12 * 12) {
                            var pos = FamiliarManager.getSpaceAroundPlayer(practitioner, 16);
                            if (pos != null) {
                                var bp = new BlockPos(pos);
                                for (var dir : Direction.values()) {
                                    if (mob.level.loadedAndEntityCanStandOnFace(bp.relative(dir), mob, dir.getOpposite())) {
                                        shulker.teleportTo(pos.x, pos.y, pos.z);
                                        ((FaceSetter) shulker).setFace(dir);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        mob.getNavigation().moveTo(practitioner, 1);
                        mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(practitioner, 1, limit));
                    }
                } else {
                    mob.getNavigation().stop();
                    mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                }
            }
        }
        if (practitioner == null) return;
        var targetMob = practitioner.getLastHurtMob();
        if (targetMob == null || targetMob.isDeadOrDying()) {
            targetMob = practitioner.getLastHurtByMob();
        }
        if (targetMob == null || targetMob.isDeadOrDying()) {
            for (var e : mob.level.getEntities(mob, mob.getBoundingBox().inflate(16))) {
                if (!(e instanceof Mob otherMob)) continue;
                if (otherMob.getTarget() == practitioner) {
                    targetMob = otherMob;
                    break;
                }
                if (otherMob.getTarget() == mob) {
                    targetMob = otherMob;
                }
                if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                    var otherMobTarget = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
                    if (otherMobTarget.isPresent() && otherMobTarget.get() == practitioner) {
                        targetMob = otherMob;
                        break;
                    }
                    if (otherMobTarget.isPresent() && otherMobTarget.get() == mob) {
                        targetMob = otherMob;
                    }
                }
            }
        }
        var shouldAttack = !(targetMob == null || !targetMob.isAttackable() || targetMob.isDeadOrDying());

        if (!shouldAttack) {
            if (isAttacking) {
                stopAttacking();
            }
            return;
        }

        startAttacking(targetMob);
    }

    public static boolean isAttackGoal(Goal g) {
        return g instanceof MeleeAttackGoal || g instanceof RangedAttackGoal || g instanceof SwellGoal
                || g.getClass().getName().toLowerCase().contains("attack");
    }

    public void startAttacking(LivingEntity targetMob) {
        BindingManager.startAttacking(mob, targetMob);
        isAttacking = true;
    }

    public void stopAttacking() {
        isAttacking = false;
        for (var goal : mob.goalSelector.getAvailableGoals()) {
            if (isAttackGoal(goal.getGoal())) {
                if (goal.isRunning()) {
                    goal.stop();
                }
            }
        }
        if (mob.getTarget() != null && mob instanceof Warden w) w.clearAnger(mob.getTarget());
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, (LivingEntity) null);
        mob.setAggressive(false);
    }

    private void poof() {
        if (!(mob.getLevel() instanceof ServerLevel sl)) return;
        var r = mob.getLevel().random;
        for (int i = 0; i < 10; ++i) {
            double d0 = r.nextGaussian() * 0.02D;
            double d1 = r.nextGaussian() * 0.02D;
            double d2 = r.nextGaussian() * 0.02D;
            sl.sendParticles(ParticleTypes.POOF,
                    mob.getRandomX(1.0D),
                    mob.getRandomY(),
                    mob.getRandomZ(1.0D),
                    1, d0, d1, d2, 0.15f);
        }
        mob.discard();
    }

    public void sendDebugMessage(Player entity) {
        var s = new StringBuilder();
        if(currentTask == null){
            s.append("no task");
        }else{
            currentTask.getDebugMessage(s);
        }
        entity.sendSystemMessage(Component.literal(s.toString()));
    }

    private void tickContract() {
        if (cooldown > 0) cooldown--;
        if (currentTask == null) {
            if (lookAround()) {
                for (var task : currentDecider.tasks) {
                    task.resetLookIndex();
                }
            } else {
                return;
            }
        }
        if (--nextTick > 0) return;
        nextTick = mob.getRandom().nextInt(10) + 5;
        currentTask.tick();
    }

    private static final Direction[][] otherDirections = new Direction[][]{
            new Direction[]{Direction.SOUTH, Direction.EAST},
            new Direction[]{Direction.SOUTH, Direction.EAST},
            new Direction[]{Direction.EAST, Direction.UP},
            new Direction[]{Direction.EAST, Direction.UP},
            new Direction[]{Direction.SOUTH, Direction.UP},
            new Direction[]{Direction.SOUTH, Direction.UP}
    };

    private static final Direction[][] edgeDirections = new Direction[][]{
            new Direction[]{Direction.UP, Direction.SOUTH, Direction.EAST},
            new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST},
            new Direction[]{Direction.UP, Direction.EAST, Direction.SOUTH},
            new Direction[]{Direction.UP, Direction.WEST, Direction.SOUTH},
            new Direction[]{Direction.DOWN, Direction.SOUTH, Direction.EAST},
            new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST},
            new Direction[]{Direction.DOWN, Direction.EAST, Direction.SOUTH},
            new Direction[]{Direction.DOWN, Direction.WEST, Direction.SOUTH},
            new Direction[]{Direction.SOUTH, Direction.WEST, Direction.UP},
            new Direction[]{Direction.SOUTH, Direction.EAST, Direction.UP},
            new Direction[]{Direction.NORTH, Direction.WEST, Direction.UP},
            new Direction[]{Direction.NORTH, Direction.EAST, Direction.UP},
    };

    private static final Vec3i[] cornerDirections = new Vec3i[]{
            new Vec3i(1, 1, 1),
            new Vec3i(1, 1, -1),
            new Vec3i(1, -1, 1),
            new Vec3i(1, -1, -1),
            new Vec3i(-1, 1, 1),
            new Vec3i(-1, 1, -1),
            new Vec3i(-1, -1, 1),
            new Vec3i(-1, -1, -1)
    };

    private boolean lookAround() {
        BlockPos basePos = mob.blockPosition();
        for (ContractTask task : currentDecider.tasks) {
            if (!task.isPossible()) {
                continue;
            }
            for (ContractManager.PositionOrSpindle positionFilter : task.positionFilters) {
                var pos = positionFilter.getPos(mob.level);
                if (task.isAcceptableTarget(pos, true)) {
                    if (!positionFilter.isPosition) task.spindle = positionFilter;
                    currentTask = task;
                    currentTask.targetPos = pos;
                    lookIndex = 0;
                    return true;
                }
            }
            if (task.corner0 != null) {
                var eyePos = mob.getEyePosition();
                if (task.potentialTargets.isEmpty()) {
                    var corner0 = task.corner0.getPos(mob.level);
                    var corner1 = task.corner1.getPos(mob.level);
                    var minCorner = new Vec3i(Math.min(corner0.getX(), corner1.getX()), Math.min(corner0.getY(), corner1.getY()), Math.min(corner0.getZ(), corner1.getZ()));
                    var maxCorner = new Vec3i(Math.max(corner0.getX(), corner1.getX()), Math.max(corner0.getY(), corner1.getY()), Math.max(corner0.getZ(), corner1.getZ()));
                    for (int x = minCorner.getX(); x <= maxCorner.getX(); x++) {
                        for (int y = minCorner.getY(); y <= maxCorner.getY(); y++) {
                            for (int z = minCorner.getZ(); z <= maxCorner.getZ(); z++) {
                                task.potentialTargets.add(new BlockPos(x, y, z));
                            }
                        }
                    }
                    task.potentialTargets.sort(Comparator.comparingDouble(bp -> eyePos.distanceToSqr(new Vec3(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))));
                } else {
                    for (var i = 0; i < 8; i++) {
                        var index = task.lookIndex + i;
                        if (index >= task.potentialTargets.size()) {
                            task.resetLookIndex();
                            break;
                        }
                        var target = task.potentialTargets.get(index);
                        if (!task.isAcceptableTarget(target, true)) continue;
                        currentTask = task;
                        currentTask.targetPos = target;
                        return true;
                    }
                    task.lookIndex += 8;
                }
            }
            if (task.taskType == ContractTask.TaskType.TAKE) {
                ItemEntity ie = task.getClosestDroppedItem();
                if (ie != null) {
                    currentTask = task;
                    currentTask.targetItem = ie;
                    currentTask.isTakingFromGround = true;
                    lookIndex = 0;
                    return true;
                }
            } else if (task.taskType == ContractTask.TaskType.ATTACK) {
                LivingEntity le = task.getClosestValidLivingTarget();
                if (le != null) {
                    currentTask = task;
                    currentTask.targetMob = le;
                    lookIndex = 0;
                    return true;
                }
            }
            var rememberedBlocks = mob.getPersistentData().getCompound(REMEMBERED_BLOCKS);
            if (rememberedBlocks.contains(task.taskId)) {
                var ints = rememberedBlocks.getIntArray(task.taskId);
                var pos = new BlockPos(ints[0], ints[1], ints[2]);
                if (task.isAcceptableTarget(pos, false)) {
                    currentTask = task;
                    currentTask.targetPos = pos;
                    lookIndex = 0;
                }
            }
        }
        for (int i = 0; i < 64; i++) {
            int shellIndex = Mth.floor((Math.pow(lookIndex, 1f / 3f) + 1) / 2);
            if (shellIndex > range) {
                shellIndex = 0;
                lookIndex = 0;
                for (ContractTask task : currentDecider.tasks) {
                    if (task.taskType == ContractTask.TaskType.OBSERVE) {
                        task.fail();
                        return false;
                    }
                }
                ContractDecider decider = currentDecider.fallback == null ? rootDecider : currentDecider.fallback;
                switchToDecider(decider);
            }
            BlockPos lookOffset = BlockPos.ZERO;
            if (shellIndex > 0) {
                int priorSideLength = shellIndex * 2 - 1;
                int indexWithinShell = lookIndex - (int) Math.pow(priorSideLength, 3);
                int priorSquareArea = (int) Math.pow(priorSideLength, 2);
                if (indexWithinShell < priorSquareArea * 6) {
                    var squareIndex = (int) (indexWithinShell / priorSquareArea);
                    var direction = Direction.values()[squareIndex];
                    var indexWithinSquare = indexWithinShell - squareIndex * priorSquareArea;
                    lookOffset = lookOffset.relative(direction, shellIndex)
                            .relative(otherDirections[squareIndex][0], indexWithinSquare / priorSideLength - priorSideLength / 2)
                            .relative(otherDirections[squareIndex][1], indexWithinSquare % priorSideLength - priorSideLength / 2);
                } else if (indexWithinShell < priorSquareArea * 6 + priorSideLength * 12) {
                    indexWithinShell -= priorSquareArea * 6;
                    var edgeIndex = indexWithinShell / priorSideLength;
                    var edgeDirection = edgeDirections[edgeIndex];
                    lookOffset = lookOffset
                            .relative(edgeDirection[0], priorSideLength / 2 + 1)
                            .relative(edgeDirection[1], priorSideLength / 2 + 1)
                            .relative(edgeDirection[2], indexWithinShell - edgeIndex * priorSideLength - priorSideLength / 2);
                } else {
                    indexWithinShell -= priorSquareArea * 6 + priorSideLength * 12;
                    lookOffset = new BlockPos(cornerDirections[indexWithinShell].multiply(priorSideLength / 2 + 1));
                }
            }
            BlockPos pos = basePos.offset(lookOffset);
            for (ContractTask task : currentDecider.tasks) {
                if (task.positionFilters.isEmpty() && task.corner0 == null && task.isPossible() && task.isAcceptableTarget(pos, false)) {
                    currentTask = task;
                    currentTask.targetPos = pos;
                    mob.getPersistentData().getCompound(REMEMBERED_BLOCKS).putIntArray(task.taskId,
                            new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    lookIndex = 0;
                    return true;
                }
            }
            lookIndex++;
        }
        return false;
    }

    @Override
    public void stop() {
        mob.targetSelector.enableControlFlag(Flag.TARGET);
    }

    public void setContract(CompoundTag contract, boolean fromPlayerClick) {
        binding.setContract(contract);
        applyContract();

        if (fromPlayerClick && currentMode != 0) {
            currentMode = 0;
            displayFamiliarMode();
        }
    }

    public void applyContract() {
        applyContract(binding.contract);
    }

    public void applyContract(CompoundTag contractTag) {
        itemDropDenyList.clear();
        takesAll = false;
        rootDecider = makeDecider(contractTag);
        currentDecider = rootDecider;
        range = contractTag.contains("range") ? contractTag.getInt("range") : 8;
        currentTask = null;
    }

    public ContractDecider makeDecider(CompoundTag tag) {
        ContractDecider decider = new ContractDecider();
        for (String key : tag.getAllKeys()) {
            if(key.equals("range")) continue;
            if (key.equals("fallback")) {
                decider.fallback = makeDecider(tag.getCompound(key));
            } else {
                decider.tasks.add(new ContractTask(tag.getCompound(key), mob, this));
            }
        }
        for (var task : decider.tasks) {
            if (task.taskType == ContractTask.TaskType.TAKE) {
                if (task.itemFilters.isEmpty()) {
                    takesAll = true;
                    continue;
                }
                for (var item : task.itemFilters) {
                    if (item instanceof BlockItem) itemDropDenyList.add(item);
                }
            }
        }
        return decider;
    }
}
