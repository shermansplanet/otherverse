package com.shermansplanet.otherverse.implement;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ImplementVeinMining {
    private static boolean currentlyVeinMining = false;

    public enum MiningMode {NONE, SQUARE, TUNNEL, ANY}

    private static final Vec3i[] alldirs = new Vec3i[26];

    private static HashMap<Direction.Axis, Vec3i[]> squareOffsets = new HashMap<>();

    static {
        squareOffsets.put(Direction.Axis.X, new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, 1), new Vec3i(0, 0, -1),
                new Vec3i(0, 1, 1), new Vec3i(0, 1, -1),
                new Vec3i(0, -1, 1), new Vec3i(0, -1, -1),
                new Vec3i(0, 1, 0), new Vec3i(0, -1, 0)
        });
        squareOffsets.put(Direction.Axis.Y, new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, 1), new Vec3i(0, 0, -1),
                new Vec3i(1, 0, 1), new Vec3i(1, 0, -1),
                new Vec3i(-1, 0, 1), new Vec3i(-1, 0, -1),
                new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0)
        });
        squareOffsets.put(Direction.Axis.Z, new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0, 1, 0), new Vec3i(0, -1, 0),
                new Vec3i(1, 1, 0), new Vec3i(1, -1, 0),
                new Vec3i(-1, 1, 0), new Vec3i(-1, -1, 0),
                new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0)
        });

        var i = 0;
        for (var x = -1; x <= 1; x++) {
            for (var y = -1; y <= 1; y++) {
                for (var z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    alldirs[i] = new Vec3i(x, y, z);
                    i++;
                }
            }
        }
    }

    public static MiningMode getMode(ItemStack stack) {
        return MiningMode.values()[stack.getTag().getInt("implement_mode")];
    }

    private static int getBlockBreakAmount(ItemStack item, ServerPlayer sp) {
        var base = (ImplementManager.isImplement(item)) ? 9 : 1;
        if (isInDemesneServer(sp, sp.blockPosition())) {
            return Math.max(base, DemesnesManager.getMiningLevel(DemesnesManager.getData(sp).getPerkLevel(DemesnesManager.DemesnePerk.VEIN_MINE)));
        }
        return base;
    }

    private static boolean isInDemesneServer(ServerPlayer sp, BlockPos bp) {
        var demesne = DemesnesManager.getData(sp);
        return demesne != null && demesne == DemesnesManager.getData(sp.serverLevel(), bp);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (currentlyVeinMining || !(event.getPlayer() instanceof ServerPlayer sp)) return;
        var tool = event.getPlayer().getMainHandItem();
        if (!(tool.getItem() instanceof DiggerItem diggerItem)) return;
        var blockBreakAmount = getBlockBreakAmount(tool, sp);
        if (blockBreakAmount <= 1) return;
        var state = sp.serverLevel().getBlockState(event.getPos());
        if (!diggerItem.isCorrectToolForDrops(tool, state)) return;
        var demesne = DemesnesManager.getData(event.getPlayer());

        var mode = getMode(tool);
        if (mode == MiningMode.NONE) return;

        var positions = getPositions(mode, event.getPlayer(), event.getPos(), event.getLevel(), state.getBlock(), blockBreakAmount);
        var constrainToDemesne = blockBreakAmount > 9 || !ImplementManager.isImplement(tool);

        currentlyVeinMining = true;
        for (var pos : positions) {
            if(constrainToDemesne && !isInDemesneServer(sp, pos)) continue;
            ((ServerPlayer) event.getPlayer()).gameMode.destroyBlock(pos);
            if (event.getPlayer().getMainHandItem().isEmpty()) break;
        }
        currentlyVeinMining = false;
        event.setCanceled(true);
    }

    public static Collection<BlockPos> getPositions(MiningMode mode, Player p, BlockPos pos, LevelAccessor level, Block block, int blockBreakAmount) {
        return switch (mode) {
            case SQUARE -> getSquare(p, pos, level, block, blockBreakAmount);
            case TUNNEL -> getTunnel(p, pos, level, block, blockBreakAmount);
            case ANY -> getConnectedTree(p, pos, level, block, blockBreakAmount);
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        };
    }

    private static Collection<BlockPos> getTunnel(Player p, BlockPos pos, LevelAccessor level, Block block, int blockBreakAmount) {
        var positions = new LinkedList<BlockPos>();
        BlockHitResult hitResult = level.clip(new ClipContext(
                p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(10)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        var dir = hitResult.getDirection().getOpposite();

        positions.add(pos);
        for (var i = 1; i < blockBreakAmount; i++) {
            pos = pos.relative(dir);
            if (!level.getBlockState(pos).is(block)) break;
            positions.add(pos);
        }
        return positions;
    }

    private static Collection<BlockPos> getSquare(Player p, BlockPos pos, LevelAccessor level, Block block, int blockBreakAmount) {
        BlockHitResult hitResult = level.clip(new ClipContext(
                p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(10)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        var dir = hitResult.getDirection().getOpposite();
        var positions = new LinkedList<BlockPos>();
        for (var i = 0; i < Mth.floor(blockBreakAmount / 9f); i++) {
            for (var offset : squareOffsets.get(hitResult.getDirection().getAxis())) {
                var bp = pos.offset(offset).relative(dir, i);
                if (!level.getBlockState(bp).is(block)) continue;
                positions.add(bp);
            }
        }
        return positions;
    }

    private static Collection<BlockPos> getConnectedTree(Player player, BlockPos pos, LevelAccessor level, Block block, int blockBreakAmount) {
        var searched = new HashSet<BlockPos>();
        var toNotSearch = new HashSet<BlockPos>();
        searched.add(pos);
        var toSearch = new ArrayDeque<BlockPos>();
        toSearch.add(pos);
        while (searched.size() < blockBreakAmount && !toSearch.isEmpty()) {
            var p = toSearch.pop();
            for (var dir : alldirs) {
                var newpos = p.offset(dir);
                if (searched.contains(newpos) || toNotSearch.contains(newpos) || toSearch.contains(newpos)) continue;
                var s = level.getBlockState(newpos);
                if (s.is(block)) {
                    toSearch.add(newpos);
                    searched.add(newpos);
                    if (searched.size() == blockBreakAmount) return searched;
                } else {
                    toNotSearch.add(newpos);
                }
            }
        }
        return searched;
    }
}
