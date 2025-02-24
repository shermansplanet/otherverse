package com.shermansplanet.otherverse.demesnes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class DemesnesPortalRenderer implements BlockEntityRenderer<DemesnesPortal> {
    private static final ResourceLocation BEAM_LOCATION = new ResourceLocation(Otherverse.MODID, "textures/portal/beam.png");
    private static final ResourceLocation[] portalLocations = new ResourceLocation[]{
            new ResourceLocation(Otherverse.MODID, "textures/portal/portal_0.png"),
            new ResourceLocation(Otherverse.MODID, "textures/portal/portal_1.png"),
            new ResourceLocation(Otherverse.MODID, "textures/portal/portal_2.png"),
            new ResourceLocation(Otherverse.MODID, "textures/portal/portal_3.png"),
            new ResourceLocation(Otherverse.MODID, "textures/portal/portal_4.png")
    };

    public DemesnesPortalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DemesnesPortal portal, float p_112308_, PoseStack pose, MultiBufferSource buffers, int p_112311_, int p_112312_) {
        if (portal.color == null) return;
        var mc = Minecraft.getInstance();
        var height = portal.height;
        pose.pushPose();
        var speed = 18.5f;
        long millis = System.currentTimeMillis();
        var rot = (millis % Math.round(speed * 2 * 360)) * 1f / speed;
        pose.translate(0.5f, 0, 0.5f);
        pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, rot, 0)));
        pose.translate(-0.5f, 0, -0.5f);
        BeaconRenderer.renderBeaconBeam(pose, buffers, BEAM_LOCATION, mc.getPartialTick(), 1,
                mc.level.getGameTime(), 0, height, portal.color, 0.2F, 0.3F);
        pose.popPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        var margin = 0.01f;
        pose.pushPose();
        pose.translate(0, margin, 0);
        renderPortal(pose, 1, portal.color);
        pose.translate(0, height - margin * 2, 0);
        renderPortal(pose, portal.radius + 1, portal.color);
        pose.popPose();
    }

    private void renderPortal(PoseStack pose, int size, float[] portalColor) {
        for (var i = 0; i <= size; i++) {
            var coeff = 1.4f;
            var shift = 0f - (i * (coeff - 1f) / size);
            var color = FastColor.ARGB32.color(255,
                    Mth.clamp(Math.round((portalColor[0] * coeff + shift) * 255), 0, 255),
                    Mth.clamp(Math.round((portalColor[1] * coeff + shift) * 255), 0, 255),
                    Mth.clamp(Math.round((portalColor[2] * coeff + shift) * 255), 0, 255)
            );
            if (i == 0) {
                renderPlane(pose, portalLocations[0], 1, 10, color);
            } else {
                renderPlane(pose, portalLocations[i], i * 2 - 1, Math.round((float) Math.pow(i, 1.5f) * 20), color);
            }
        }
    }

    private void renderPlane(PoseStack poseStack, ResourceLocation texture, int size, int speed, int color) {
        RenderSystem.setShaderTexture(0, texture);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0, 0.5f);
        long millis = System.currentTimeMillis();
        var rot = (millis % (speed * 360L)) * 1f / speed;
        poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, rot, 0)));
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        var pose = poseStack.last().pose();
        var r = size * 0.5f;

        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.vertex(pose, -r, 0, -r).uv(0, 0).color(color).endVertex();
        bufferbuilder.vertex(pose, -r, 0, r).uv(0, 1).color(color).endVertex();
        bufferbuilder.vertex(pose, r, 0, r).uv(1, 1).color(color).endVertex();
        bufferbuilder.vertex(pose, r, 0, -r).uv(1, 0).color(color).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());

        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.vertex(pose, r, 0, -r).uv(1, 0).color(color).endVertex();
        bufferbuilder.vertex(pose, r, 0, r).uv(1, 1).color(color).endVertex();
        bufferbuilder.vertex(pose, -r, 0, r).uv(0, 1).color(color).endVertex();
        bufferbuilder.vertex(pose, -r, 0, -r).uv(0, 0).color(color).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());

        poseStack.popPose();
    }
}
