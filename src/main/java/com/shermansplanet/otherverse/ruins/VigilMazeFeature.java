package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class VigilMazeFeature extends Feature<NoneFeatureConfiguration> {

    enum ConnectorType {SOLID, PARKOUR, NONE}

    public class ConnectorData {
        public int offset;
        public ConnectorType type;
        public BlockPos pos;
        public Direction dir;

        public ConnectorData(int x, int z, Direction dir) {
            Random r = new Random(z * 10000L + x);
            offset = r.nextInt(2, 14);
            type = r.nextBoolean() ? ConnectorType.SOLID : r.nextBoolean() ? ConnectorType.PARKOUR : ConnectorType.NONE;
            this.dir = dir;
        }
    }

    public VigilMazeFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    private static boolean isAirAt(BlockPos pos, WorldGenLevel level) {
        return level.getBlockState(pos).isAir();
    }

    private static final Direction[] cardinalDirections = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final HashMap<Direction, EnumProperty<WallSide>> wallSides = new HashMap<>();

    static {
        wallSides.put(Direction.NORTH, WallBlock.NORTH_WALL);
        wallSides.put(Direction.SOUTH, WallBlock.SOUTH_WALL);
        wallSides.put(Direction.EAST, WallBlock.EAST_WALL);
        wallSides.put(Direction.WEST, WallBlock.WEST_WALL);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        ChunkPos coords = new ChunkPos(ctx.origin());
        var baseX = coords.x * 16;
        var baseZ = coords.z * 16;
        var minusXY = 0;
        var plusXY = 0;
        var minusZY = 0;
        var plusZY = 0;

        HashMap<Vector2i, Float> mazeBlocks = new HashMap<>();

        for (var mazeLayer = 0; mazeLayer <= 1; mazeLayer++) {
            var offset = mazeLayer * 100;
            var minusX = new ConnectorData(coords.x * 2 - 1 + offset, coords.z * 2, Direction.EAST);
            var plusX = new ConnectorData(coords.x * 2 + 1 + offset, coords.z * 2, Direction.WEST);
            var minusZ = new ConnectorData(coords.x * 2 + offset, coords.z * 2 - 1, Direction.SOUTH);
            var plusZ = new ConnectorData(coords.x * 2 + offset, coords.z * 2 + 1, Direction.NORTH);

            if (mazeLayer == 0) {
                minusXY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ + minusX.offset) - 1;
                plusXY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX + 15, baseZ + plusX.offset) - 1;
                minusZY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX + minusZ.offset, baseZ) - 1;
                plusZY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX + plusZ.offset, baseZ + 15) - 1;
            } else {
                minusXY += 8;
                plusXY += 8;
                minusZY += 8;
                plusZY += 8;
            }

            minusX.pos = new BlockPos(baseX, minusXY, baseZ + minusX.offset);
            plusX.pos = new BlockPos(baseX + 15, plusXY, baseZ + plusX.offset);
            minusZ.pos = new BlockPos(baseX + minusZ.offset, minusZY, baseZ);
            plusZ.pos = new BlockPos(baseX + plusZ.offset, plusZY, baseZ + 15);

            var endpoints = new ArrayList<ConnectorData>();
            if (minusX.type != ConnectorType.NONE) endpoints.add(minusX);
            if (plusX.type != ConnectorType.NONE) endpoints.add(plusX);
            if (minusZ.type != ConnectorType.NONE) endpoints.add(minusZ);
            if (plusZ.type != ConnectorType.NONE) endpoints.add(plusZ);

            var oldMazeBlocks = mazeBlocks;
            mazeBlocks = new HashMap<>();

            if (endpoints.size() > 1) {
                var shuffledEndpoints = new ArrayList<ConnectorData>();
                while (!endpoints.isEmpty()) {
                    var i = ctx.random().nextInt(endpoints.size());
                    shuffledEndpoints.add(endpoints.get(i));
                    endpoints.remove(i);
                }
                endpoints = shuffledEndpoints;
                connect(endpoints.get(0), endpoints.get(1), mazeBlocks, ctx.random());
                for (var i = 2; i < endpoints.size(); i++) {
                    connect(endpoints.get(i), mazeBlocks, ctx.random());
                }
            }

            var elevated = mazeLayer > 0;

            var centers = new HashSet<>(mazeBlocks.keySet());

            if (!elevated) {
                for (var pos : new ArrayList<>(mazeBlocks.keySet())) {
                    for (var dx = -1; dx <= 1; dx++) {
                        for (var dz = -1; dz <= 1; dz++) {
                            var newPos = new Vector2i(pos.x + dx, pos.y + dz);
                            if (mazeBlocks.containsKey(newPos)) continue;
                            mazeBlocks.put(newPos, mazeBlocks.get(pos));
                        }
                    }
                }
            }

            var brick = elevated ? Blocks.NETHER_BRICKS : Blocks.RED_NETHER_BRICKS;
            var slab = elevated ? Blocks.NETHER_BRICK_SLAB : Blocks.RED_NETHER_BRICK_SLAB;

            for (var mazeBlock : mazeBlocks.entrySet()) {
                if (mazeBlock.getKey().x < baseX || mazeBlock.getKey().x >= baseX + 16 || mazeBlock.getKey().y < baseZ || mazeBlock.getKey().y >= baseZ + 16)
                    continue;
                var y = Math.round(mazeBlock.getValue() * 2);
                var addSlab = y % 2 == 1;
                y = Mth.floor(y / 2f);
                var pos = new BlockPos(mazeBlock.getKey().x, y, mazeBlock.getKey().y);
                ctx.level().setBlock(pos, brick.defaultBlockState(), 2);
                var descent = elevated ? 1 : ctx.random().nextInt(6);
                for (var i = 0; i < descent; i++) {
                    ctx.level().setBlock(pos.below(i), brick.defaultBlockState(), 2);
                }
                if (!elevated && centers.contains(mazeBlock.getKey())) {
                    ctx.level().setBlock(pos.below(1), Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                    if (ctx.random().nextFloat() < 0.3f) {
                        ctx.level().setBlock(pos.below(2), Blocks.TNT.defaultBlockState(), 2);
                    }
                }
                if (!elevated && ctx.random().nextFloat() < 0.1f) {
                    ctx.level().setBlock(pos, OtherverseBlocks.REDSTONE_NETHER_BRICKS.get().defaultBlockState(), 2);
                    for (var dir : cardinalDirections) {
                        var sidePos = pos.relative(dir);
                        if (!mazeBlocks.containsKey(new Vector2i(sidePos.getX(), sidePos.getZ()))) continue;
                        ctx.level().setBlock(sidePos, OtherverseBlocks.REDSTONE_NETHER_BRICKS.get().defaultBlockState(), 2);
                    }
                }
                if (addSlab) {
                    pos = pos.above();
                    ctx.level().setBlock(pos, slab.defaultBlockState(), 2);
                }
                for (var i = 0; i < 12; i++) {
                    pos = pos.above();
                    if (ctx.level().getBlockState(pos).isAir()) break;
                    ctx.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }

            if (elevated) {
                var positions = mazeBlocks.keySet().stream().toList();
                if (positions.isEmpty()) continue;
                for (var i = 0; i < 2; i++) {
                    var p2d = positions.get(ctx.random().nextInt(positions.size()));
                    var y = Mth.floor(Math.round(mazeBlocks.get(p2d) * 2) / 2f);
                    var pos = new BlockPos(p2d.x, y - 1, p2d.y);
                    for (var dir : cardinalDirections) {
                        var p1 = pos.relative(dir, 1);
                        if (isAirAt(p1.above(), ctx.level()) || !isAirAt(p1, ctx.level())) continue;
                        ctx.level().setBlock(p1, Blocks.NETHER_BRICKS.defaultBlockState(), 2);
                        var p2 = pos.relative(dir, 2);
                        if (isAirAt(p2.above(), ctx.level()) || !isAirAt(p2, ctx.level())) continue;
                        ctx.level().setBlock(p2, Blocks.NETHER_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
                        var p3 = p1.below();
                        if (!isAirAt(p3, ctx.level())) continue;
                        ctx.level().setBlock(p3, Blocks.NETHER_BRICK_WALL.defaultBlockState().setValue(wallSides.get(dir.getOpposite()), WallSide.TALL), 2);
                    }
                    var mutPos = pos.mutable();
                    for (var dy = 1; dy < 32; dy++) {
                        mutPos.setY(y - dy);
                        var bs = ctx.level().getBlockState(mutPos);
                        if (bs.isCollisionShapeFullBlock(ctx.level(), mutPos)) break;
                        ctx.level().setBlock(mutPos, Blocks.NETHER_BRICKS.defaultBlockState(), 2);
                    }
                }
                var chainCount = ctx.random().nextInt(10);
                for (var i = 0; i < chainCount; i++) {
                    var p2d = positions.get(ctx.random().nextInt(positions.size()));
                    var y = Mth.floor(Math.round(mazeBlocks.get(p2d) * 2) / 2f);
                    var mutPos = new BlockPos(p2d.x, y - 1, p2d.y).mutable();
                    var chainLength = ctx.random().nextInt(1, 6);
                    var hitBlock = false;
                    for (var dy = 1; dy <= chainLength; dy++) {
                        mutPos.setY(y - dy);
                        var bs = ctx.level().getBlockState(mutPos);
                        if (!bs.isAir()) {
                            hitBlock = true;
                            break;
                        }
                        ctx.level().setBlock(mutPos, Blocks.CHAIN.defaultBlockState(), 2);
                    }
                    if (!hitBlock && oldMazeBlocks.containsKey(p2d)) {
                        var yLevel = oldMazeBlocks.get(p2d) + 4;
                        while (mutPos.getY() > yLevel) {
                            mutPos.setY(mutPos.getY() - 1);
                            ctx.level().setBlock(mutPos, Blocks.CHAIN.defaultBlockState(), 2);
                        }
                        ctx.level().setBlock(mutPos, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), 2);
                    }
                }
                for (var i = 0; i < 8; i++) {
                    var p2d = positions.get(ctx.random().nextInt(positions.size()));
                    var y = Mth.floor(Math.round(mazeBlocks.get(p2d) * 2) / 2f);
                    var pos = new BlockPos(p2d.x, y, p2d.y);
                    for (var dir : cardinalDirections) {
                        var p = pos.relative(dir);
                        if (!isAirAt(p, ctx.level())) continue;
                        ctx.level().setBlock(p, Blocks.MANGROVE_BUTTON.defaultBlockState()
                                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                                .setValue(ButtonBlock.FACING, dir), 2);
                    }
                }
                continue;
            }

            int depth = 16;

            for (var x = baseX; x < baseX + 16; x++) {
                for (var z = baseZ; z < baseZ + 16; z++) {
                    if (mazeBlocks.containsKey(new Vector2i(x, z))) continue;
                    var pos = new BlockPos(x, ctx.level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
                    var dripstoneHeight = Math.max(0, ctx.random().nextInt(-8, 6));
                    for (var i = 1; i < depth - dripstoneHeight; i++) {
                        ctx.level().setBlock(pos.below(i), Blocks.AIR.defaultBlockState(), 2);
                    }
                    for (var i = 1; i <= dripstoneHeight; i++) {
                        var bs = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                .setValue(PointedDripstoneBlock.THICKNESS,
                                        i == dripstoneHeight ? DripstoneThickness.TIP :
                                                i == dripstoneHeight - 1 ? DripstoneThickness.FRUSTUM :
                                                i == 1 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE);
                        ctx.level().setBlock(pos.below(depth - i), bs, 2);
                    }
                    ctx.level().setBlock(pos.below(depth), Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
                }
            }
        }

        if (ctx.random().nextFloat() < 0.5f) {
            makeIsland(ctx, (minusXY + plusXY + minusZY + plusZY) / 4f + 24);
        }

        return true;
    }

    private void makeIsland(FeaturePlaceContext<NoneFeatureConfiguration> ctx, float height) {
        var r = ctx.random();
        var radius = r.nextFloat() * 6 + 1;
        var margin = radius + 1;
        var range = 16 - (margin * 2);
        var center = new Vec3(Mth.floor(ctx.origin().getX() / 16f) * 16 + r.nextFloat() * range + margin,
                height + r.nextFloat() * 32,
                Mth.floor(ctx.origin().getZ() / 16f) * 16 + r.nextFloat() * range + margin);
        var maxDescentLength = 5;
        var nudgeFactor = radius * 0.25f;
        for (var x = Mth.floor(center.x - radius); x <= Mth.ceil(center.x + radius); x++) {
            var nudge1 = (r.nextFloat() - 0.5f) * nudgeFactor;
            for (var z = Mth.floor(center.z - radius); z <= Mth.ceil(center.z + radius); z++) {
                var wasFilled = false;
                var nudge2 = (r.nextFloat() - 0.5f) * nudgeFactor;
                var nudge3 = (r.nextFloat() - 0.5f) * nudgeFactor;
                for (var y = Mth.ceil(center.y + radius); y >= Mth.floor(center.y - radius - 1); y--) {
                    var pos = new BlockPos(x, y, z);
                    var dist = pos.distToCenterSqr(center.add(0, nudge3, 0));
                    var modRad = radius + nudge1 + nudge2;
                    if (dist > modRad * modRad) {
                        if (!wasFilled) continue;
                        if (r.nextFloat() < 0.5f) {
                            var isChain = r.nextFloat() < 0.3f;
                            var descentLength = r.nextInt(maxDescentLength) + (isChain ? 4 : 1);
                            for (var i = 0; i <= descentLength; i++) {
                                var bs = isChain ? (i == descentLength && r.nextFloat() < 0.4f
                                                    ? Blocks.ANVIL.defaultBlockState()
                                                    : Blocks.CHAIN.defaultBlockState()) :
                                        Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                                .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                                                .setValue(PointedDripstoneBlock.THICKNESS,
                                                        i == descentLength ? DripstoneThickness.TIP :
                                                        i == descentLength - 1 ? DripstoneThickness.FRUSTUM :
                                                        i == 1 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE);
                                ctx.level().setBlock(pos.below(i), bs, 2);
                            }
                        }
                        break;
                    }
                    wasFilled = true;
                    ctx.level().setBlock(pos, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
                }
            }
        }
    }

    private void connect(ConnectorData a, ConnectorData b, HashMap<Vector2i, Float> mazeBlocks, RandomSource r) {
        var positions = new ArrayList<Vector2i>();
        var aAxis = a.dir.getAxis();
        var bAxis = b.dir.getAxis();
        positions.add(new Vector2i(a.pos.getX(), a.pos.getZ()));
        if (aAxis != bAxis) {
            var seekPos = a.pos;
            while (seekPos.get(aAxis) != b.pos.get(aAxis)) {
                seekPos = seekPos.relative(a.dir);
                positions.add(new Vector2i(seekPos.getX(), seekPos.getZ()));
            }
            while (seekPos.get(bAxis) != b.pos.get(bAxis)) {
                seekPos = seekPos.relative(b.dir.getOpposite());
                positions.add(new Vector2i(seekPos.getX(), seekPos.getZ()));
            }
        } else {
            var midPoint = r.nextInt(2, 14);
            var seekPos = a.pos;
            for (var i = 0; i < midPoint; i++) {
                seekPos = seekPos.relative(a.dir);
                positions.add(new Vector2i(seekPos.getX(), seekPos.getZ()));
            }
            var perp = a.dir.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            var positive = seekPos.get(perp) < b.pos.get(perp);
            while (seekPos.get(perp) != b.pos.get(perp)) {
                seekPos = seekPos.relative(perp, positive ? 1 : -1);
                positions.add(new Vector2i(seekPos.getX(), seekPos.getZ()));
            }
            while (seekPos.get(bAxis) != b.pos.get(bAxis)) {
                seekPos = seekPos.relative(b.dir.getOpposite());
                positions.add(new Vector2i(seekPos.getX(), seekPos.getZ()));
            }
        }
        var startY = a.pos.getY();
        var endY = b.pos.getY();
        for (var i = 0; i < positions.size(); i++) {
            var lerp = i / (positions.size() - 1f);
            var y = startY * (1 - lerp) + endY * lerp;
            mazeBlocks.put(positions.get(i), y);
        }
    }

    private void connect(ConnectorData a, HashMap<Vector2i, Float> mazeBlocks, RandomSource r) {
        var closestDist = Integer.MAX_VALUE;
        var closestPos = new Vector2i();
        var aPos = new Vector2i(a.pos.getX(), a.pos.getZ());
        for (var pos : mazeBlocks.keySet()) {
            var dist = Mth.abs(aPos.x - pos.x) + Mth.abs(aPos.y - pos.y);
            if (dist < closestDist) {
                closestDist = dist;
                closestPos = pos;
            }
        }
        var dummyPos = new BlockPos(closestPos.x, Math.round(mazeBlocks.get(closestPos)), closestPos.y);
        var perp = a.dir.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        var positive = dummyPos.get(perp) > a.pos.get(perp);
        var dummyConnector = new ConnectorData(0, 0,
                perp == Direction.Axis.X ? (positive ? Direction.WEST : Direction.EAST) : (positive ? Direction.NORTH : Direction.SOUTH));
        dummyConnector.pos = dummyPos;
        connect(a, dummyConnector, mazeBlocks, r);
    }
}
