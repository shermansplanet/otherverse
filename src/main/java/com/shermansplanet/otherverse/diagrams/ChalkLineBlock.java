package com.shermansplanet.otherverse.diagrams;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class ChalkLineBlock extends Block implements EntityBlock {

    public static final float[][][] selfPositions = {
            {{0, 0}},
            {{-0.16f, 0f}, {0.16f, 0f}},
            {{0f, 0.12f}, {-0.16f, -0.12f}, {0.16f, -0.12f}},
            {{-0.16f, -0.16f}, {0.16f, -0.16f}, {0.16f, 0.16f}, {-0.16f, 0.16f}},
            {{-0.2f, -0.2f}, {0.2f, -0.2f}, {0.2f, 0.2f}, {-0.2f, 0.2f}, {0, 0}},
            {{-0.16f, -0.25f}, {0.16f, -0.25f}, {0.16f, 0.25f}, {-0.16f, 0.25f}, {-0.16f, 0}, {0.16f, 0}},
            {{0, 0}, {0, 0.25f}, {0, -0.25f}, {-0.2f, -0.14f}, {0.2f, -0.14f}, {0.2f, 0.14f},
                    {-0.2f, 0.14f}}
    };

    static final BooleanProperty chalkN = BooleanProperty.create("chalk_n");
    static final BooleanProperty chalkNE = BooleanProperty.create("chalk_ne");
    static final BooleanProperty chalkE = BooleanProperty.create("chalk_e");
    static final BooleanProperty chalkSE = BooleanProperty.create("chalk_se");
    static final BooleanProperty chalkS = BooleanProperty.create("chalk_s");
    static final BooleanProperty chalkSW = BooleanProperty.create("chalk_sw");
    static final BooleanProperty chalkW = BooleanProperty.create("chalk_w");
    static final BooleanProperty chalkNW = BooleanProperty.create("chalk_nw");
    public static final BooleanProperty chalkCircle = BooleanProperty.create("chalk_circle");
    public static final EnumProperty<DyeColor> color = EnumProperty.create("color", DyeColor.class);
    public static final BooleanProperty hasScaffolding = BooleanProperty.create("has_scaffolding");

    private static final int[] stateReplacements = new int[256];

    private static boolean initialized = false;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static void initializeStateReplacements() {
        initialized = true;
        for (int n = 0; n < 256; n++) {
            stateReplacements[n] = n;
        }
        replaceState(0b00100110, 0b00100010);
        replaceState(0b00110010, 0b00100010);
        replaceState(0b00101110, 0b00100110);
        replaceState(0b00111010, 0b00110010);
        replaceState(0b11010101, 0b10010100);
        replaceState(0b01110110, 0b01100110);
        replaceState(0b00110111, 0b00110011);
        replaceState(0b00111110, 0b00101010);
        replaceState(0b00110100, 0b00100100);
        replaceState(0b00010110, 0b00010010);
        replaceState(0b00111100, 0b00110100);
        replaceState(0b00011110, 0b00010110);
        replaceState(0b10111010, 0b10010010);
        replaceState(0b11101110, 0b01000100);
        replaceState(0b00111001, 0b00101001);
        replaceState(0b00111000, 0b00101000);
        replaceState(0b01111100, 0b01010100);
        replaceState(0b11110111, 0b10010100);
        replaceState(0b11010111, 0b01010101);
        replaceState(0b10111111, 0b00011011);
        replaceState(0b11111111, 0b10101010);
        replaceState(0b11011101, 0b10001000);
        replaceState(0b10111110, 0b10011100);
        replaceState(0b00111111, 0b00100101);
        replaceState(0b01111110, 0b01010010);
        replaceState(0b01110100, 0b01100100);
        replaceState(0b00010111, 0b00010011);
        replaceState(0b10110111, 0b00110101);
        replaceState(0b11110110, 0b01010110);
        replaceState(0b10110011, 0b00110011);
        replaceState(0b11100110, 0b01100110);
    }

    private static void replaceState(int s1, int s2) {
        for (int i = 0; i < 8; i += 2) {
            int a1 = (s1 >>> i) | (s1 << (8 - i));
            int a2 = (s2 >>> i) | (s2 << (8 - i));
            stateReplacements[a1 & 255] = a2 & 255;
        }
    }

    private static BlockState GetActualState(BlockState state) {
        if (!initialized) {
            initializeStateReplacements();
        }
        int n = 0;
        for (int i = 0; i < 8; i++) {
            n = n | ((state.getValue(dirstates[7 - i]) ? 1 : 0) << i);
        }
        n = stateReplacements[n];
        for (int i = 0; i < 8; i++) {
            state = state.setValue(dirstates[7 - i], ((n >>> i) & 1) == 1);
        }
        return state;
    }

    public ChalkLineBlock() {
        super(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().instabreak());
        registerDefaultState(stateDefinition.any()
                .setValue(chalkCircle, false)
                .setValue(chalkN, false)
                .setValue(chalkNE, false)
                .setValue(chalkE, false)
                .setValue(chalkSE, false)
                .setValue(chalkS, false)
                .setValue(chalkSW, false)
                .setValue(chalkW, false)
                .setValue(chalkNW, false)
                .setValue(color, DyeColor.WHITE)
                .setValue(hasScaffolding, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(chalkN)
                .add(chalkNE)
                .add(chalkE)
                .add(chalkSE)
                .add(chalkS)
                .add(chalkSW)
                .add(chalkW)
                .add(chalkNW)
                .add(color)
                .add(chalkCircle)
                .add(hasScaffolding);
    }

    @Deprecated
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return !state.getValue(hasScaffolding);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circle) {
            if (circle.item.getItem() instanceof ChalkItem) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack og = itemEntity.getItem();
                    var newItemStack = og.split(1);
                    if (og.isEmpty()) {
                        itemEntity.discard();
                    }
                    circle.insertItem(0, newItemStack, false);
                }
            }
        }
    }

    private void updateAllNeighbors(BlockPos pos, Level level,
                                    DiagramManager.BlockUpdateType updateType) {
        level.updateNeighborsAt(pos, this);
        if (level instanceof ServerLevel sl) {
            DiagramManager.OnDiagramBlockChanged(sl, pos, updateType);
        }
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (!(blockentity instanceof ChalkCircle circle)) return InteractionResult.SUCCESS;
        ItemStack itemstack = player.getItemInHand(hand);
        if (ImplementManager.isImplement(itemstack)) {
            return InteractionResult.PASS;
        }
        if (itemstack.is(OtherverseItems.IDOL.get()) && itemstack.getTag().getString("material").equals("otherverse:cinnabar_block")) {
            return InteractionResult.PASS;
        }
        boolean hadScaffolding = state.getValue(hasScaffolding);
        if (itemstack.is(OtherverseItems.SLATE_SCAFFOLDING.get())) {
            if (hadScaffolding) {
                ItemStack drop = OtherverseItems.SLATE_SCAFFOLDING.get().getDefaultInstance();
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
            } else {
                itemstack.split(1);
            }
            level.setBlockAndUpdate(pos, state.setValue(hasScaffolding, !hadScaffolding));
            return InteractionResult.SUCCESS;
        }
        if (hadScaffolding && itemstack.getItem() instanceof ChalkItem
                && result.getDirection() == Direction.UP) {
            return InteractionResult.PASS;
        }
        boolean containsSelf = circle.item.is(OtherverseItems.SELF.get());
        boolean makeCircle = !itemstack.isEmpty() && !circle.item.is(itemstack.getItem());
        boolean fullOfSelf =
                containsSelf && circle.item.getCount() >= selfPositions.length;
        circle.isNumber = false;
        if (player.isShiftKeyDown() && itemstack.isEmpty()) {
            if (fullOfSelf) {
                return InteractionResult.SUCCESS;
            }
            if (!player.isCreative()) {
                if (!SelfManager.ChangeSelf(player, -1)) {
                    return InteractionResult.SUCCESS;
                }
            }
            makeCircle = true;
            int selfCount = 1;
            if (containsSelf) {
                selfCount += circle.item.getCount();
            }
            itemstack = new ItemStack(OtherverseItems.SELF.get(), selfCount);
        } else if (makeCircle) {
            boolean dontSpend =
                    player.getAbilities().instabuild || itemstack.getItem() instanceof ChalkItem;
            itemstack = dontSpend ? itemstack.copy() : itemstack;
            if (player.isShiftKeyDown() && itemstack.is(Tags.Items.SEEDS)) {
                player.getInventory().removeItem(itemstack);
                player.displayClientMessage(Component.literal("Count: " + itemstack.getCount()), true);
                circle.isNumber = true;
            } else {
                itemstack = itemstack.split(1);
            }
        }
        boolean leaveCircleBehind = makeCircle || !(itemstack.getItem() instanceof ChalkItem);
        state = state.setValue(chalkCircle, leaveCircleBehind);
        ItemStack drop = getDrop(circle);
        if (!drop.isEmpty()) {
            if (!player.addItem(drop)) {
                player.drop(drop, false);
            }
        }
        circle.item = makeCircle ? itemstack
                : leaveCircleBehind ? new ItemStack(OtherverseItems.CHALK.get()) : ItemStack.EMPTY;
        circle.setPlayer(player);
        level.setBlock(pos, state, 2);
        circle.markUpdated();
        updateAllNeighbors(pos, level, DiagramManager.BlockUpdateType.CHANGED);
        return InteractionResult.SUCCESS;
    }

    private static final BooleanProperty[] dirstates = {
            chalkN, chalkNE, chalkE, chalkSE, chalkS, chalkSW, chalkW, chalkNW
    };

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var item = ctx.getItemInHand();
        return getConnectionState(ctx.getLevel(), ctx.getClickedPos(),
                OtherverseBlocks.CHALK_LINE.get().defaultBlockState().setValue(color, item.is(OtherverseItems.CHALK.get())
                        ? (item.hasTag() ? DyeColor.values()[item.getTag().getInt("dye_color")] : DyeColor.WHITE)
                        : DyeColor.BLACK));
    }

    public static BlockState getConnectionState(Level level, BlockPos pos, BlockState currentState) {
        for (int i = 0; i < 8; i++) {
            int[] d = DiagramManager.dirs[i];
            BlockState bs = level.getBlockState(pos.offset(d[0], 0, d[1]));
            currentState = currentState.setValue(dirstates[i], bs.getBlock() instanceof ChalkLineBlock && bs.getValue(color) == currentState.getValue(color));
        }
        return GetActualState(currentState);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlaceBlock(EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getPlacedBlock().getBlock() instanceof ChalkLineBlock cb) {
            cb.blockWasPlacedBy(event.getEntity().getLevel(), event.getPos());
            if (event.getEntity() instanceof Player p && p.getLevel().getBlockEntity(event.getPos()) instanceof ChalkCircle cc) {
                cc.setPlayer(p);
            }
            return;
        }

    }

    public void blockWasPlacedBy(Level level, BlockPos pos) {
        refreshNeighborLines(level, pos);
        updateAllNeighbors(pos, level, DiagramManager.BlockUpdateType.ADDED);
    }

    private static ItemStack getDrop(ChalkCircle circleEntity) {
        ItemStack itemToDrop = circleEntity.item;
        if (itemToDrop.is(OtherverseItems.SELF.get()) || itemToDrop.getItem() instanceof ChalkItem) {
            return ItemStack.EMPTY;
        }
        return itemToDrop;
    }

    public void onRemove(BlockState state1, Level level, BlockPos pos, BlockState state2,
                         boolean p_55728_) {
        if (state1.is(state2.getBlock())) return;

        /*if (state1.getValue(hasScaffolding) && !state2.getFluidState().isEmpty()) {
            level.setBlock(pos, state1, 2);
            return;
        }*/

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (!level.isClientSide() && blockentity instanceof ChalkCircle circleEntity) {
            ItemStack itemToDrop = getDrop(circleEntity);
            if (!itemToDrop.isEmpty()) {
                ItemEntity itementity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                        itemToDrop);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
            if (state1.getValue(hasScaffolding)) {
                ItemEntity itementity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                        OtherverseItems.SLATE_SCAFFOLDING.get().getDefaultInstance());
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        super.onRemove(state1, level, pos, state2, p_55728_);
        refreshNeighborLines(level, pos);
        updateAllNeighbors(pos, level, DiagramManager.BlockUpdateType.REMOVED);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(hasScaffolding);
    }

    public static void refreshNeighborLines(Level level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            int[] d = DiagramManager.dirs[i];
            BlockPos newpos = pos.offset(d[0], 0, d[1]);
            BlockState bs = level.getBlockState(newpos);
            if (bs.getBlock() instanceof ChalkLineBlock) {
                level.setBlockAndUpdate(newpos, getConnectionState(level, newpos, bs));
            }
        }
    }

    public VoxelShape getShape(BlockState p_55620_, BlockGetter p_55621_, BlockPos p_55622_,
                               CollisionContext p_55623_) {
        return p_55620_.getValue(hasScaffolding)
                ? Block.box(0, 0, 0, 16, 16, 16)
                : Block.box(0, 0, 0, 16, 1, 16);
    }

    public boolean canSurvive(BlockState p_55585_, LevelReader p_55586_, BlockPos p_55587_) {
        BlockPos blockpos = p_55587_.below();
        BlockState blockstate = p_55586_.getBlockState(blockpos);
        return this.canSurviveOn(p_55586_, blockpos, blockstate);
    }

    private boolean canSurviveOn(BlockGetter p_55613_, BlockPos p_55614_, BlockState blockState) {
        return blockState.isFaceSturdy(p_55613_, p_55614_, Direction.UP) || blockState.is(Blocks.HOPPER)
                || blockState.is(OtherverseBlocks.SLATE_SCAFFOLDING.get())
                || (blockState.is(OtherverseBlocks.CHALK_LINE.get()) && blockState.getValue(hasScaffolding));
    }

    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos,
                                Block otherBlock, BlockPos otherPos, boolean p_55566_) {
        if (level.isClientSide) {
            return;
        }

        if (!blockState.canSurvive(level, blockPos)) {
            level.removeBlock(blockPos, false);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChalkCircle(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == Otherverse.CHALK_CIRCLE.get() ? ChalkCircle::tick : null;
    }
}