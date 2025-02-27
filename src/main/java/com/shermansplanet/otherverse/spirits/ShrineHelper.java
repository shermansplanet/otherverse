package com.shermansplanet.otherverse.spirits;

import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collection;
import java.util.HashSet;

public class ShrineHelper {

    public static Collection<BlockPos> getAllHallows(BlockPos pos, SpiritType spiritType, TransientDiagramData data) {
        var set = new HashSet<BlockPos>();
        getHallowsRecursive(pos, set, data, spiritType.label());
        return set;
    }

    private static void getHallowsRecursive(BlockPos pos, HashSet<BlockPos> set, TransientDiagramData data, String label) {
        if (set.contains(pos)) return;
        var tag = data.getPlacedItemTag(pos);
        if (tag == null || !tag.getString("spirit_type").equals(label)) return;

        set.add(pos);
        for (var dir : Direction.values()) getHallowsRecursive(pos.relative(dir), set, data, label);
    }
}
