package com.shermansplanet.otherverse.artifacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

public class SpawnAltarRenderer implements BlockEntityRenderer<SpawnAltarBlockEntity> {
    private final EntityRenderDispatcher entityRenderDispatcher;

    public SpawnAltarRenderer(BlockEntityRendererProvider.Context p_173673_) {
        this.entityRenderDispatcher = p_173673_.getEntityRenderer();
    }

    @Override
    public void render(SpawnAltarBlockEntity p_112307_, float p_112308_, PoseStack poseStack, MultiBufferSource bufferIn, int lightColor, int p_112312_) {
        if (p_112307_.displayEntity == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.25, 0.5);
        poseStack.scale(0.2f, 0.2f, 0.2f);
        long millis = System.currentTimeMillis();
        poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, (millis % 3600) / 5f, 0)));
        entityRenderDispatcher.render(p_112307_.displayEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, bufferIn, lightColor);
        poseStack.popPose();
    }
}
