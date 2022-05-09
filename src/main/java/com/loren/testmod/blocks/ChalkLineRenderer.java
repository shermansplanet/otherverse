package com.loren.testmod.blocks;

import com.loren.testmod.tiles.ChalkLineTile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ChalkLineRenderer implements BlockEntityRenderer<ChalkLineTile> {
    public static final ResourceLocation CHALK_LOCATION = new ResourceLocation("textures/blocks/calcite.png");

    public ChalkLineRenderer(BlockEntityRendererProvider.Context p_173529_) {
    }

    @Override
    public void render(@NotNull ChalkLineTile p_112307_, float p_112308_, PoseStack poseStack, MultiBufferSource multiBufferSource, int p_112311_, int p_112312_) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        PoseStack.Pose pose = poseStack.last();
        renderQuad(pose.pose(), multiBufferSource.getBuffer(RenderType.beaconBeam(CHALK_LOCATION, false)));
        poseStack.popPose();
    }

    private static void renderQuad(Matrix4f pos, VertexConsumer vc) {
        addVertex(pos, vc, 0.2f, 0.2f);
        addVertex(pos, vc, -0.2f, 0.2f);
        addVertex(pos, vc, -0.2f, -0.2f);
        addVertex(pos, vc, 0.2f, -0.2f);

        addVertex(pos, vc, 0.2f, 0.2f);
        addVertex(pos, vc, 0.2f, -0.2f);
        addVertex(pos, vc, -0.2f, -0.2f);
        addVertex(pos, vc, -0.2f, -0.2f);
    }

    private static void addVertex(Matrix4f pos, VertexConsumer vc, float x, float z){
        vc.vertex(pos, x, 0.1f, z).normal(0,1,0).uv(x,z).endVertex();
    }
}
