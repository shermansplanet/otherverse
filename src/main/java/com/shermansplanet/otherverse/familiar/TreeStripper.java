package com.shermansplanet.otherverse.familiar;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.implement.ImplementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TreeStripper {

    private static boolean stripping = false;

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (stripping || !(event.getLevel() instanceof ServerLevel sl)) return;
        var tool = event.getPlayer().getMainHandItem();
        var state = sl.getBlockState(event.getPos());
        if (!(state.is(BlockTags.LEAVES)) && !(state.is(BlockTags.LOGS))) return;
        var isLeaves = state.is(BlockTags.LEAVES);
        if (!FamiliarManager.hasFamiliarType(event.getPlayer(), EntityType.PANDA)
                && !(ImplementManager.isImplement(tool) && tool.is(isLeaves ? Tags.Items.SHEARS : Tags.Items.TOOLS_AXES)))
            return;
        stripping = true;
        for (var pos : getConnectedTree(event, sl, isLeaves ? BlockTags.LEAVES : BlockTags.LOGS)) {
            ((ServerPlayer) event.getPlayer()).gameMode.destroyBlock(pos);
            if (event.getPlayer().getMainHandItem().isEmpty()) break;
        }
        event.setCanceled(true);
        stripping = false;
    }

    private static HashSet<BlockPos> getConnectedTree(BlockEvent.BreakEvent event, ServerLevel sl, TagKey<Block> tag) {
        var searched = new HashSet<BlockPos>();
        var toNotSearch = new HashSet<BlockPos>();
        searched.add(event.getPos());
        var toSearch = new ArrayDeque<BlockPos>();
        toSearch.add(event.getPos());
        while (searched.size() < 64 && !toSearch.isEmpty()) {
            var pos = toSearch.pop();
            for (var dir : Direction.values()) {
                var newpos = pos.relative(dir);
                if (searched.contains(newpos) || toNotSearch.contains(newpos) || toSearch.contains(newpos)) continue;
                var s = sl.getBlockState(newpos);
                if (s.is(tag)) {
                    toSearch.add(newpos);
                    searched.add(newpos);
                } else {
                    toNotSearch.add(newpos);
                }
            }
        }
        return searched;
    }
}
