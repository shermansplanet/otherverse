package com.loren.testmod.blocks;

import com.loren.testmod.init.BlockInit;
import com.loren.testmod.init.ItemInit;
import com.loren.testmod.rendering.ChalkCircleRenderer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChalkLineBlock extends Block implements EntityBlock {
    static final BooleanProperty chalkN = BooleanProperty.create("chalk_n");
    static final BooleanProperty chalkNE = BooleanProperty.create("chalk_ne");
    static final BooleanProperty chalkE = BooleanProperty.create("chalk_e");
    static final BooleanProperty chalkSE = BooleanProperty.create("chalk_se");
    static final BooleanProperty chalkS = BooleanProperty.create("chalk_s");
    static final BooleanProperty chalkSW = BooleanProperty.create("chalk_sw");
    static final BooleanProperty chalkW = BooleanProperty.create("chalk_w");
    static final BooleanProperty chalkNW = BooleanProperty.create("chalk_nw");
    public static final BooleanProperty chalkCircle = BooleanProperty.create("chalk_circle");

    private static final int[] stateReplacements = new int[256];

    private static boolean initialized = false;

    private static final DamageSource SELF_LOSS = (new DamageSource("testmod_self")).bypassArmor().bypassInvul();

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
        if (!initialized) initializeStateReplacements();
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

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circleEntity) {
            if (circleEntity.item.isEmpty() || circleEntity.item.is(ItemInit.CHALK.get())) return 0;
            Item item = circleEntity.item.getItem();
            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                return block.getLightEmission(block.defaultBlockState(), level, pos);
            }
        }
        return 0;
    }

    public ChalkLineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(chalkCircle, false)
                .setValue(chalkN, false)
                .setValue(chalkNE, false)
                .setValue(chalkE, false)
                .setValue(chalkSE, false)
                .setValue(chalkS, false)
                .setValue(chalkSW, false)
                .setValue(chalkW, false)
                .setValue(chalkNW, false));
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
                .add(chalkCircle);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if(level.isClientSide){
            return;
        }
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circle) {
            if (circle.item.is(ItemInit.CHALK.get())) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack og = itemEntity.getItem();
                    circle.item = og.split(1);
                    circle.markUpdated();
                    updateAllNeighbors(pos, level);
                }
            }
        }
    }

    private void updateAllNeighbors(BlockPos pos, Level level) {
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public boolean isSignalSource(BlockState p_60571_) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction dir) {
        BlockEntity blockentity = getter.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circle) {
            if (circle.item.is(ItemInit.CHALK.get())) {
                return 15;
            }
        }
        return 0;
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circle) {
            ItemStack itemstack = player.getItemInHand(hand);
            boolean containsSelf = circle.item.is(ItemInit.SELF.get());
            boolean makeCircle = !itemstack.isEmpty() && !circle.item.is(itemstack.getItem());
            boolean fullOfSelf = containsSelf && circle.item.getCount() >= ChalkCircleRenderer.selfPositions.length;
            if (player.isShiftKeyDown()) {
                if (fullOfSelf) {
                    return InteractionResult.SUCCESS;
                }
                player.hurt(SELF_LOSS, 2);
                makeCircle = true;
                int selfCount = 1;
                if (containsSelf) {
                    selfCount += circle.item.getCount();
                }
                itemstack = new ItemStack(ItemInit.SELF.get(), selfCount);
            } else if (makeCircle) {
                boolean dontSpend = player.getAbilities().instabuild || itemstack.is(ItemInit.CHALK.get());
                itemstack = dontSpend ? itemstack.copy() : itemstack;
                itemstack = itemstack.split(1);
            }
            state = state.setValue(chalkCircle, makeCircle);
            ItemStack drop = getDrop(circle);
            if (!drop.isEmpty()) {
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
            }
            circle.item = makeCircle ? itemstack : ItemStack.EMPTY;
            level.setBlock(pos, state, 2);
            circle.markUpdated();
            updateAllNeighbors(pos, level);
        }
        return InteractionResult.SUCCESS;
    }

    private static final int[][] dirs = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    private static final BooleanProperty[] dirstates = {
            chalkN, chalkNE, chalkE, chalkSE, chalkS, chalkSW, chalkW, chalkNW
    };

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return getConnectionState(ctx.getLevel(), ctx.getClickedPos(), BlockInit.CHALK_LINE.get().defaultBlockState());
    }

    private static BlockState getConnectionState(Level level, BlockPos pos, BlockState currentState) {
        for (int i = 0; i < 8; i++) {
            int[] d = dirs[i];
            BlockState bs = level.getBlockState(pos.offset(d[0], 0, d[1]));
            currentState = currentState.setValue(dirstates[i], bs.getBlock() instanceof ChalkLineBlock);
        }
        return GetActualState(currentState);
    }

    public void onPlace(BlockState state1, Level level, BlockPos pos, BlockState state2, boolean p_55728_) {
        super.onPlace(state1, level, pos, state2, p_55728_);
        if (!state1.is(state2.getBlock())) {
            refreshNeighbors(level, pos);
        }
    }

    private static ItemStack getDrop(ChalkCircle circleEntity) {
        ItemStack itemToDrop = circleEntity.item;
        if (itemToDrop.is(ItemInit.SELF.get()) || itemToDrop.is(ItemInit.CHALK.get())) {
            return ItemStack.EMPTY;
        }
        return itemToDrop;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ChalkCircle circleEntity) {
            ItemStack itemToDrop = getDrop(circleEntity);
            if (!level.isClientSide && !itemToDrop.isEmpty()) {
                ItemEntity itementity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), itemToDrop);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }
    }

    public void onRemove(BlockState state1, Level level, BlockPos pos, BlockState state2, boolean p_55728_) {
        super.onRemove(state1, level, pos, state2, p_55728_);
        if (!p_55728_ && !state1.is(state2.getBlock())) {
            refreshNeighbors(level, pos);
            updateAllNeighbors(pos, level);
        }
    }

    private void refreshNeighbors(Level level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            int[] d = dirs[i];
            BlockPos newpos = pos.offset(d[0], 0, d[1]);
            BlockState bs = level.getBlockState(newpos);
            if (bs.getBlock() instanceof ChalkLineBlock) {
                level.setBlockAndUpdate(newpos, getConnectionState(level, newpos, bs));
            }
        }
    }

    public VoxelShape getShape(BlockState p_55620_, BlockGetter p_55621_, BlockPos p_55622_, CollisionContext p_55623_) {
        return Block.box(0, 0, 0, 16, 1, 16);
    }

    public boolean canSurvive(BlockState p_55585_, LevelReader p_55586_, BlockPos p_55587_) {
        BlockPos blockpos = p_55587_.below();
        BlockState blockstate = p_55586_.getBlockState(blockpos);
        return this.canSurviveOn(p_55586_, blockpos, blockstate);
    }

    public boolean isFaceSturdy(BlockGetter p_60784_, BlockPos p_60785_, Direction p_60786_) {
        return false;
    }

    private boolean canSurviveOn(BlockGetter p_55613_, BlockPos p_55614_, BlockState p_55615_) {
        return p_55615_.isFaceSturdy(p_55613_, p_55614_, Direction.UP);
    }

    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean p_55566_) {
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
}
