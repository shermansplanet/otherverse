package com.shermansplanet.otherverse.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EcstasyStrandFeature extends Feature<NoneFeatureConfiguration> {

    public EcstasyStrandFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        var level = ctx.level();
        var xVariation = level.getRandom().nextBoolean();
        var dx = xVariation ? (level.getRandom().nextFloat() - 0.5f) * 0.3f : 0;
        var dz = xVariation ? 0 : (level.getRandom().nextFloat() - 0.5f) * 0.3f;
        var canBuild = false;
        var pos = ctx.origin().below().getCenter();
        List<Vec3> positions = new ArrayList<Vec3>();
        for (var y = ctx.origin().getY() - 1; y > level.getMinBuildHeight(); y--) {
            pos = pos.add(dx, -1, dz);
            var bp = BlockPos.containing(pos);
            var isAir = level.getBlockState(bp).isAir();
            if (isAir) {
                positions.add(pos);
            } else {
                if (positions.size() > 4) {
                    positions.add(pos);
                    canBuild = true;
                    break;
                }
                positions.clear();
                positions.add(pos);
            }
        }
        if (!canBuild) return false;
        var radius = positions.size() / 5f;
        radius = constrainRadius(radius, level, positions.get(0), Direction.UP);
        if (radius < positions.size() / 20f) return false;
        var downRadius = constrainRadius(radius, level, positions.get(positions.size() - 1), Direction.DOWN);
        var isDrop = downRadius < positions.size() / 20f;
        if (isDrop) {
            positions = positions.subList(0, level.getRandom().nextInt(positions.size() / 2, positions.size()));
        }else{
            radius = downRadius;
        }
        for (int i = 0; i < positions.size(); i++) {
            var center = positions.get(i);
            var lerp = i / (positions.size() - 1f);
            var r = radius * (1 - lerp * (1 - lerp) * 3);
            if (isDrop && lerp > 0.75f) {
                var blocksBelow = (lerp - 0.75f) * (positions.size() - 1f);
                var baseR = radius * (1 - 0.75f * (1 - 0.75f) * 3);
                if (blocksBelow > baseR) break;
                r = baseR * (float) (1 - Math.pow(blocksBelow / baseR, 2));
            }
            for (var bp : getPositionsInRadius(r, center)) {
                level.setBlock(bp, level.getRandom().nextBoolean() ?
                        Blocks.HONEY_BLOCK.defaultBlockState()
                        : Blocks.HONEYCOMB_BLOCK.defaultBlockState(), 2);
            }
        }
        return true;
    }

    private ArrayList<BlockPos> getPositionsInRadius(float radius, Vec3 center) {
        var intr = Mth.ceil(radius);
        var rsquared = radius * radius;
        var positions = new ArrayList<BlockPos>();
        for (var dx = -intr; dx <= intr; dx++) {
            for (var dz = -intr; dz <= intr; dz++) {
                if (dx * dx + dz * dz <= rsquared) positions.add(BlockPos.containing(center.add(dx, 0, dz)));
            }
        }
        return positions;
    }

    private float constrainRadius(float radius, WorldGenLevel level, Vec3 center, Direction dir) {
        for (var pos : getPositionsInRadius(radius, center)) {
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.relative(dir)).isAir()) {
                radius = Math.min(radius, (float) center.distanceTo(pos.getCenter()) - 0.01f);
            }
        }
        return radius;
    }
}
