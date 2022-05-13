package com.loren.testmod.rendering;

import com.loren.testmod.blocks.ChalkCircle;
import com.loren.testmod.init.ItemInit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

import java.util.Set;

public class ChalkCircleRenderer implements BlockEntityRenderer<ChalkCircle> {

    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderer;

    public static final float[][][] selfPositions = {
            {{0, 0}},
            {{-0.16f, 0f}, {0.16f, 0f}},
            {{0f, 0.12f}, {-0.16f, -0.12f}, {0.16f, -0.12f}},
            {{-0.16f, -0.16f}, {0.16f, -0.16f}, {0.16f, 0.16f}, {-0.16f, 0.16f}},
            {{-0.2f, -0.2f}, {0.2f, -0.2f}, {0.2f, 0.2f}, {-0.2f, 0.2f}, {0, 0}},
            {{-0.16f, -0.25f}, {0.16f, -0.25f}, {0.16f, 0.25f}, {-0.16f, 0.25f}, {-0.16f, 0}, {0.16f, 0}},
            {{0, 0}, {0, 0.25f}, {0, -0.25f}, {-0.2f, -0.14f}, {0.2f, -0.14f}, {0.2f, 0.14f}, {-0.2f, 0.14f}}
    };

    public ChalkCircleRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    private void renderItem(PoseStack poseStack, int lightVal, ChalkCircle chalkCircle,
                            MultiBufferSource multiBufferSource, float dx, float dz, float scale) {
        poseStack.translate(0.5f + dx, 0.03D, 0.5f + dz);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
        poseStack.scale(scale, scale, scale);
        this.itemRenderer.renderStatic(chalkCircle.item, ItemTransforms.TransformType.FIXED, lightVal,
                OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, chalkCircle.hashCode());
    }

    private static final Set<Item> dontRenderBlock = Set.of(Items.STRING);

    @Override
    public void render(ChalkCircle chalkCircle, float p_112308_, PoseStack poseStack, MultiBufferSource multiBufferSource, int lightVal, int p_112312_) {
        ItemStack itemstack = chalkCircle.item;
        if (itemstack.isEmpty()) return;
        poseStack.pushPose();
        Item item = itemstack.getItem();
        if (itemstack.is(ItemInit.SELF.get())) {
            int selfcount = itemstack.getCount();
            float scale = Math.min(0.6f, 3f / (selfcount + 2f));
            for (float[] offset : selfPositions[selfcount - 1]) {
                poseStack.pushPose();
                renderItem(poseStack, lightVal, chalkCircle, multiBufferSource,
                        offset[0] - scale / 32f, offset[1], scale);
                poseStack.popPose();
            }
        } else if (item instanceof BlockItem blockItem && !dontRenderBlock.contains(item)) {
            Block block = blockItem.getBlock();
            poseStack.translate(0.3D, 0D, 0.3D);
            poseStack.scale(0.4F, 0.4F, 0.4F);
            this.blockRenderer.renderSingleBlock(block.defaultBlockState(),
                    poseStack, multiBufferSource, lightVal, OverlayTexture.NO_OVERLAY,
                    net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        } else {
            renderItem(poseStack, lightVal, chalkCircle, multiBufferSource, 0, 0, 0.5f);
        }

        poseStack.popPose();
    }
}
