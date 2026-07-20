package com.shermansplanet.otherverse.demesnes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.SightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DemesnesBeaconRenderer implements BlockEntityRenderer<DemesnesBeacon> {
    public static final float LOOK_RADIUS = 0.98f;
    public static DemesnesBeacon clientLookingAt;

    public DemesnesBeaconRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public boolean shouldRender(DemesnesBeacon beacon, Vec3 pos) {
        return SightManager.shouldRenderSight();
    }

    @Override
    public boolean shouldRenderOffScreen(DemesnesBeacon beacon) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(DemesnesBeacon beacon, float p_112308_, PoseStack pose, MultiBufferSource buffers, int p_112311_, int p_112312_) {
        var player = Minecraft.getInstance().player;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (beacon.hoverName != null
                && beacon.inDemesneOf.equals(player.getGameProfile().getName())
                && beacon.claimedDemesneBounds.contains(player.position())) {
            var playerPos = camera.getPosition();
            var beaconPos = new Vec3(
                    beacon.getBlockPos().getX() + 0.5f,
                    beacon.getBlockPos().getY() + 0.5f,
                    beacon.getBlockPos().getZ() + 0.5f
            );
            var dist = (float) playerPos.distanceTo(beaconPos);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            var scale = 0.005f * Mth.clamp(dist, 2, 100);
            pose.scale(-scale, -scale, scale);
            var matrix4f = pose.last().pose();
            Font font = Minecraft.getInstance().font;
            var hoverName = beacon.hoverName;
            float dx = (float) (-font.width(hoverName) / 2);
            float dy = (float) (-font.lineHeight / 2);
            var dot = player.getForward().dot(beaconPos.subtract(playerPos).normalize());
            var isLookingAt = dot > LOOK_RADIUS;
            if (isLookingAt) clientLookingAt = beacon;
            var tint = isLookingAt ? -1 : 0x80ffffff;
            if (isLookingAt) {
                pose.pushPose();
                pose.scale((dx - 4) / dx, (dy - 4) / dy, 1);
                pose.translate(0, 0, 5);
                var newMatrix = pose.last().pose();
                hoverName = hoverName.copy().withStyle(Style.EMPTY.withColor(0));
                font.drawInBatch(hoverName, dx, dy, 0xaa000000, false, newMatrix, buffers, Font.DisplayMode.SEE_THROUGH, 0xff000000, 0);
                hoverName = hoverName.copy().withStyle(Style.EMPTY.withColor(0xbcff00));
                pose.popPose();
            }
            font.drawInBatch(hoverName, dx, dy, tint, false, matrix4f, buffers, Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
            pose.popPose();
        }
        if (beacon.clientData == null) beacon.recalculatePositions();
        if (beacon.clientData.isEmpty) return;
        for (var otherRitual : DemesnesRenderer.ongoingRituals) {
            if (otherRitual.minPos.equals(beacon.clientData.minPos)
                    && otherRitual.levelId == beacon.clientData.levelId) return;
        }
        pose.pushPose();
        var p = beacon.getBlockPos();
        pose.translate(-p.getX(), -p.getY(), -p.getZ());
        beacon.clientData.render(Minecraft.getInstance(), pose, buffers, Minecraft.getInstance().getPartialTick(), 0);
        pose.popPose();
    }
}
