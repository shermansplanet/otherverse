package com.shermansplanet.otherverse.sympathy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SympathySelectRenderer {

    public static Vec3 renderPosition;
    private static final HashMap<DyeColor, Vec3i> colorSizes = new HashMap<>();
    private static final HashMap<DyeColor, AABB> colorBounds = new HashMap<>();
    private static final Vec3i defaultSize = new Vec3i(1, 1, 1);

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event){
        renderPosition = null;
        colorSizes.clear();
        colorBounds.clear();
    }

    @SubscribeEvent
    public static void renderTick(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        var pose = event.getPoseStack();
        var cam = event.getCamera();

        if (SightManager.shouldRenderSight()) {
            var data = DiagramManager.getOrCreateLevelData(player.level());
            for (var color : DyeColor.values()) {
                var key = SympathyManager.getKey(player, color);
                var pos = data.getSympathyPosition(key);
                if (pos == null) continue;
                var savedPosition = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                pose.pushPose();
                transformPose(pose, cam, savedPosition, color);
                mc.getBlockRenderer().renderSingleBlock(
                        OtherverseBlocks.SELECTOR.get().defaultBlockState()
                                .setValue(ColorableBlock.color, color)
                                .setValue(SelectorBlock.filled, true),
                        pose, mc.renderBuffers().bufferSource(),
                        255, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
                pose.popPose();
            }
        }

        if (!(player.getMainHandItem().getItem() instanceof SpindleItem)) {
            return;
        }

        HitResult hitresult = Minecraft.getInstance().hitResult;
        BlockPos selectedBlockPosition = null;
        if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            var blockHit = ((BlockHitResult) hitresult);
            selectedBlockPosition = SpindleItem.getBlockPos(player, blockHit);
        }

        if (selectedBlockPosition == null) {
            renderPosition = null;
            return;
        }
        var blockvec = new Vec3(selectedBlockPosition.getX(), selectedBlockPosition.getY(), selectedBlockPosition.getZ());
        if (renderPosition == null) {
            renderPosition = blockvec;
        } else {
            renderPosition = renderPosition.lerp(blockvec, 0.2f);
        }

        var color = SpindleItem.getDyeColor(player.getMainHandItem().getItem());

        pose.pushPose();
        transformPose(pose, cam, renderPosition, color);
        mc.getBlockRenderer().renderSingleBlock(
                OtherverseBlocks.SELECTOR.get().defaultBlockState()
                        .setValue(ColorableBlock.color, color)
                        .setValue(SelectorBlock.filled, false),
                pose, mc.renderBuffers().bufferSource(),
                255, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    private static void transformPose(PoseStack pose, Camera cam, Vec3 position, DyeColor color) {
        pose.translate(-cam.getPosition().x(), -cam.getPosition().y(), -cam.getPosition().z());
        var size = defaultSize;
        var bounds = colorBounds.get(color);
        if (bounds != null && bounds.contains(position)) {
            size = colorSizes.get(color);
        }
        pose.translate(position.x, position.y, position.z);
        pose.translate(-0.051f, -0.05f, -0.05f);
        pose.scale(size.getX() + 0.1f, size.getY() + 0.1f, size.getZ() + 0.1f);
    }

    public static void updateRange(SympathyRangeUpdateMessage msg, Supplier<NetworkEvent.Context> ctx) {
        colorSizes.put(msg.color(), msg.visible() ? new Vec3i(msg.dx(), msg.dy(), msg.dz()) : defaultSize);
        colorBounds.put(msg.color(), msg.visible() ? msg.withinBounds() : null);
    }
}
