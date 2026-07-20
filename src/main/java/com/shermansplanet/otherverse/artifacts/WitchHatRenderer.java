package com.shermansplanet.otherverse.artifacts;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@OnlyIn(Dist.CLIENT)
public class WitchHatRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack,
                                                                          SlotContext slotContext,
                                                                          PoseStack matrixStack,
                                                                          RenderLayerParent<T, M> renderLayerParent,
                                                                          MultiBufferSource renderTypeBuffer,
                                                                          int light, float limbSwing,
                                                                          float limbSwingAmount,
                                                                          float partialTicks,
                                                                          float ageInTicks,
                                                                          float netHeadYaw,
                                                                          float headPitch) {
        M parentModel = renderLayerParent.getModel();
        LivingEntity entity = slotContext.entity();

        // 1. Verify the parent entity model supports head transformations
        if (parentModel instanceof HeadedModel headedModel) {
            matrixStack.pushPose();

            // Note: If translateToHand behaves oddly on the head slot, use:
            headedModel.getHead().translateAndRotate(matrixStack);

            // 3. Counter-adjust vanilla item scaling & positioning for the head slot
            if (entity.isBaby()) {
                matrixStack.scale(0.75F, 0.75F, 0.75F);
                matrixStack.translate(0.0F, 0.5F, 0.0F);
            }

            CustomHeadLayer.translateToHead(matrixStack, false);

            // 4. Render the item using the standard game item renderer
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    entity,
                    stack,
                    ItemDisplayContext.HEAD,
                    false, // leftHanded
                    matrixStack,
                    renderTypeBuffer,
                    entity.level(),
                    light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                    entity.getId()
            );
            matrixStack.popPose();
        }
    }
}