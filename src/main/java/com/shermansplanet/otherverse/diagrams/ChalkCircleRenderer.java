package com.shermansplanet.otherverse.diagrams;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ChalkCircleRenderer implements BlockEntityRenderer<ChalkCircle> {

    private final ItemRenderer itemRenderer;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final long ANIM_CUTOFF = 100 * 1000;

    public ChalkCircleRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    private void renderItem(PoseStack poseStack, int lightVal, ChalkCircle chalkCircle,
                            MultiBufferSource multiBufferSource, float dx, float dz, float scale) {
        if (chalkCircle.animationTime < 0) {
            poseStack.translate(0.5f + dx, 0.0625D, 0.5f + dz);
            poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(90, 0, 0)));
        } else {
            if (Minecraft.getInstance().isPaused()) {
                return;
            }
            if (!Minecraft.getInstance().isPaused()) {
                chalkCircle.animationTime += (long) (Minecraft.getInstance().getDeltaFrameTime() * 50);
            }
            var rawt = chalkCircle.animationTime;
            var t = rawt % ANIM_CUTOFF;
            boolean isSpecial = rawt > ANIM_CUTOFF;
            float lerp = t / 4000f;
            float riseLerp = smootherstep(Math.min(1, lerp * 4));
            var riseAmount = 0.5f;
            riseAmount += isSpecial ? t / 5000f : 0;
            poseStack.translate(0.5f + dx, 0.0625D + riseLerp * riseAmount, 0.5f + dz);
            if (!isSpecial && lerp > 0.25) {
                float shakeLerp = Math.min(1, lerp * 4 - 1) * 0.2f;
                RandomSource random = chalkCircle.getLevel().getRandom();
                poseStack.translate((random.nextFloat() * 0.2 - 0.1) * shakeLerp,
                        (random.nextFloat() * 0.2 - 0.1) * shakeLerp,
                        (random.nextFloat() * 0.2 - 0.1) * shakeLerp);
            }
            if (isSpecial && Minecraft.getInstance().cameraEntity != null && t > 4000) {
                var circlePos = chalkCircle.getPos();
                var camPos = Minecraft.getInstance().cameraEntity.getPosition(Minecraft.getInstance().getPartialTick());
                var toCam = camPos.subtract(new Vec3(
                        circlePos.getX() + 0.5f, camPos.y, circlePos.getZ() + 0.5f));

                var side = toCam.cross(new Vec3(0, 1, 0)).normalize();
                var up = side.cross(toCam).normalize();
                var r = (float) Math.pow(t / 9000f, 4);
                var s = Math.min(1, (10000 - t) / 200f);

                poseStack.pushPose();
                poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, (float) -chalkCircle.angleDegrees, 0)));

                var fade1 = (int) Math.max(0, (5000 - t) * 255 / 1000);
                drawPolygon(poseStack, multiBufferSource, side, up, 4, 0.85f * s, r, 255, fade1, fade1, 255);
                drawPolygon(poseStack, multiBufferSource, side, up, 4, 0.85f * s, r + 0.125f, 255, fade1, fade1, 255);
                drawPolygon(poseStack, multiBufferSource, side, up, 24, 0.85f * s, r, 255, fade1, fade1, 255);

                if (t > 5000) {
                    var fade2 = (int) Math.max(0, (6000 - t) * 255 / 1000);
                    drawPolygon(poseStack, multiBufferSource, side, up, 6, s, -r, 255, fade2, fade2,255);
                    drawPolygon(poseStack, multiBufferSource, side, up, 24, s, -r, 255, fade2, fade2,255);

                    if (t > 6000) {
                        var fade3 = (int) Math.max(0, (7000 - t) * 255 / 1000);
                        drawPolygon(poseStack, multiBufferSource, side, up, 4, 1.414f * s, r, 255, fade3, fade3,255);
                        drawPolygon(poseStack, multiBufferSource, side, up, 4, 1.414f * s, r + 0.125f, 255, fade3, fade3,255);
                        drawPolygon(poseStack, multiBufferSource, side, up, 24, 1.414f * s, r, 255, fade3, fade3,255);
                    }
                }

                poseStack.popPose();
            }
            var spin = isSpecial ? (float) Math.pow(t, 2.1) / 40000f : 0;
            poseStack.mulPose(Quaternion.fromXYZDegrees(
                    new Vector3f(90 * (1 - riseLerp), 360 * riseLerp + spin, 0)));
        }
        poseStack.scale(scale, scale, scale);
        this.itemRenderer.renderStatic(chalkCircle.item, ItemTransforms.TransformType.FIXED, lightVal,
                OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, chalkCircle.hashCode());
    }

    private static float smootherstep(float x) {
        return x * x * x * (3.0f * x * (2.0f * x - 5.0f) + 10.0f);
    }

    @Override
    public void render(ChalkCircle chalkCircle, float p_112308_, PoseStack poseStack,
                       MultiBufferSource buffers, int lightVal, int p_112312_) {
        ItemStack itemstack = chalkCircle.item;
        if (itemstack.isEmpty() || itemstack.getItem() instanceof ChalkItem) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, (float) chalkCircle.angleDegrees, 0)));
        poseStack.translate(-0.5f, 0.0f, -0.5f);

        if (itemstack.is(OtherverseItems.SELF.get())) {
            int selfcount = itemstack.getCount();
            float scale = Math.min(0.6f, 3f / (selfcount + 2f));
            for (float[] offset : ChalkLineBlock.selfPositions[selfcount - 1]) {
                poseStack.pushPose();
                renderItem(poseStack, lightVal, chalkCircle, buffers,
                        offset[0] + scale / 32f, offset[1], scale);
                poseStack.popPose();
            }
        } else {
            poseStack.pushPose();
            renderItem(poseStack, lightVal, chalkCircle, buffers, 0, 0, 0.5f);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static void drawCylinder(PoseStack ps, MultiBufferSource bufferSource, Vec3 dir1, Vec3 dir2, int sides,
                                    float radius, float rotation, int r, int g, int b, int a, float height) {
        var bufferSides = bufferSource.getBuffer(RenderType.lineStrip());
        var sideVertical = height / (Mth.TWO_PI * radius / sides);
        for (var i = 0; i <= sides; i++) {
            var angle = Math.PI * 2 * (i / (float) sides + rotation);
            var cos = Math.cos(angle);
            var sin = Math.sin(angle);
            var pos = dir1.scale(cos).add(dir2.scale(sin)).scale(radius);
            var nrm = dir1.scale(-sin).add(dir2.scale(cos));
            drawLineVertex(ps,
                    (float) pos.x, (float) pos.y - (i % 2 == 0 ? 0 : height), (float) pos.z,
                    (float) nrm.x, (float) nrm.y + (i % 2 == 0 ? -sideVertical : sideVertical), (float) nrm.z,
                    r, g, b, a, bufferSides);
        }
        drawPolygon(ps, bufferSource, dir1, dir2, sides, radius, rotation,r,g,b,a);
        ps.pushPose();
        ps.translate(0,-height,0);
        drawPolygon(ps, bufferSource, dir1, dir2, sides, radius, rotation,r,g,b,a);
        ps.popPose();
    }

    public static void drawPolygon(PoseStack ps, MultiBufferSource bufferSource, Vec3 dir1, Vec3 dir2, int sides,
                                   float radius, float rotation, int r, int g, int b, int a) {
        var buffer = bufferSource.getBuffer(RenderType.lineStrip());
        for (var i = 0; i <= sides; i++) {
            var angle = Math.PI * 2 * (i / (float) sides + rotation);
            var cos = Math.cos(angle);
            var sin = Math.sin(angle);
            var pos = dir1.scale(cos).add(dir2.scale(sin)).scale(radius);
            var nrm = dir1.scale(-sin).add(dir2.scale(cos));
            drawLineVertex(ps,
                    (float) pos.x, (float) pos.y, (float) pos.z,
                    (float) nrm.x, (float) nrm.y, (float) nrm.z,
                    r, g, b, a, buffer);
        }
    }

    public static void drawLineVertex(PoseStack ps, float x, float y, float z, float nx, float ny, float nz, int r, int g, int b, int a, VertexConsumer consumer) {
        consumer.vertex(ps.last().pose(), x, y, z)
                .color(r, g, b, a)
                .normal(ps.last().normal(), nx, ny, nz)
                .endVertex();
    }
}
