package com.shermansplanet.otherverse.implement;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.demesnes.DemesnesRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VeinMiningRenderer {
    @SubscribeEvent
    public static void renderOverlay(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var player = Minecraft.getInstance().player;
        var item = player.getMainHandItem();
        if (!(item.getItem() instanceof DiggerItem diggerItem)) return;
        var blockBreakAmount = getBlockBreakAmount(item);
        if (blockBreakAmount <= 1) return;

        var hitResult = Minecraft.getInstance().hitResult;
        if (!(hitResult instanceof BlockHitResult bh)) return;

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        BlockPos blockpos1 = bh.getBlockPos();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        BlockState blockstate = level.getBlockState(blockpos1);

        if (!diggerItem.isCorrectToolForDrops(item, blockstate)) return;

        var mode = ImplementVeinMining.getMode(item);
        if (mode == ImplementVeinMining.MiningMode.NONE) return;

        var constrainToDemesne = blockBreakAmount > 9 || !ImplementManager.isImplement(item);

        var positions = ImplementVeinMining.getPositions(mode, Minecraft.getInstance().player, bh.getBlockPos(), Minecraft.getInstance().level, blockstate.getBlock(), blockBreakAmount);

        for (var pos : positions) {
            if (constrainToDemesne && !isInDemesneClient(pos)) continue;
            event.getPoseStack().pushPose();
            var diff = pos.subtract(blockpos1);
            event.getPoseStack().translate(diff.getX(), diff.getY(), diff.getZ());
            renderHitOutline(event.getPoseStack(), buffers.getBuffer(RenderType.lines()), Minecraft.getInstance().getCameraEntity(),
                    cam.getPosition().x, cam.getPosition().y, cam.getPosition().z, blockpos1, blockstate);
            event.getPoseStack().popPose();
        }
    }

    private static int getBlockBreakAmount(ItemStack item) {
        var base = (ImplementManager.isImplement(item)) ? 9 : 1;
        if (isInDemesneClient(Minecraft.getInstance().player.blockPosition())) {
            return Math.max(base, DemesnesRenderer.demesneMineAmount);
        }
        return base;
    }

    private static boolean isInDemesneClient(BlockPos bp) {
        return DemesnesRenderer.myDemesne != null && !DemesnesRenderer.myDemesne.isEmpty
                && DemesnesRenderer.myDemesne.bounds.contains(new Vec3(bp.getX(), bp.getY(), bp.getZ()));
    }

    private static void renderHitOutline(PoseStack p_109638_, VertexConsumer p_109639_, Entity p_109640_, double p_109641_, double p_109642_, double p_109643_, BlockPos p_109644_, BlockState p_109645_) {
        renderShape(p_109638_, p_109639_, p_109645_.getShape(Minecraft.getInstance().level, p_109644_, CollisionContext.of(p_109640_)), (double) p_109644_.getX() - p_109641_, (double) p_109644_.getY() - p_109642_, (double) p_109644_.getZ() - p_109643_, 0.0F, 0.0F, 0.0F, 0.4F);
    }

    private static void renderShape(PoseStack p_109783_, VertexConsumer p_109784_, VoxelShape p_109785_, double p_109786_, double p_109787_, double p_109788_, float p_109789_, float p_109790_, float p_109791_, float p_109792_) {
        PoseStack.Pose posestack$pose = p_109783_.last();
        p_109785_.forAllEdges((p_234280_, p_234281_, p_234282_, p_234283_, p_234284_, p_234285_) -> {
            float f = (float) (p_234283_ - p_234280_);
            float f1 = (float) (p_234284_ - p_234281_);
            float f2 = (float) (p_234285_ - p_234282_);
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;
            p_109784_.vertex(posestack$pose.pose(), (float) (p_234280_ + p_109786_), (float) (p_234281_ + p_109787_), (float) (p_234282_ + p_109788_)).color(p_109789_, p_109790_, p_109791_, p_109792_).normal(posestack$pose.normal(), f, f1, f2).endVertex();
            p_109784_.vertex(posestack$pose.pose(), (float) (p_234283_ + p_109786_), (float) (p_234284_ + p_109787_), (float) (p_234285_ + p_109788_)).color(p_109789_, p_109790_, p_109791_, p_109792_).normal(posestack$pose.normal(), f, f1, f2).endVertex();
        });
    }

}
