package com.shermansplanet.otherverse.binding;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IdolRenderer extends BlockEntityWithoutLevelRenderer {
    private final EntityRenderDispatcher entityRenderDispatcher;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static HashMap<EntityType, Entity> renderEntities = new HashMap<>();
    public static IdolRenderer instance;

    public IdolRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, EntityRenderDispatcher entityRenderDispatcher) {
        super(dispatcher, modelSet);
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SOLID = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntitySolidShader);
    protected static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    protected static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
    protected static final RenderStateShard.TransparencyStateShard PASSTHROUGH = new RenderStateShard.TransparencyStateShard("passthrough", () -> {
    }, () -> {
    });

    @SubscribeEvent
    public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
        instance = new IdolRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels(),
                Minecraft.getInstance().getEntityRenderDispatcher());
        event.registerReloadListener(instance);
    }

    @Override
    public void renderByItem(ItemStack itemStack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferIn, int lightColor, int overlayTexture) {
        EntityType<?> entityType = IdolItem.getType(itemStack);
        if (entityType == EntityType.PLAYER
                && transformType != ItemDisplayContext.GUI
                && transformType != ItemDisplayContext.GROUND
                && transformType != ItemDisplayContext.FIXED
                && transformType != ItemDisplayContext.NONE) {
            return;
        }
        if (entityType == null) {
            entityType = MobBindingInfluenceUtils.getCycleType();
        }
        Entity renderEntity = renderEntities.get(entityType);
        if (renderEntity == null) {
            renderEntity = entityType == EntityType.PLAYER ? Minecraft.getInstance().player : entityType.create(Minecraft.getInstance().level);
            renderEntities.put(entityType, renderEntity);
        }
        if (!(renderEntity instanceof LivingEntity)) {
            return;
        }
        /*if (itemStack.hasTag() && itemStack.getTag().contains("mob_data")) {
            renderEntity.load(itemStack.getTag().getCompound("mob_data").getCompound("EntityTag"));
            renderEntity.setXRot(0);
            renderEntity.setYRot(0);
            renderEntity.setYBodyRot(0);
            renderEntity.setYHeadRot(0);
        }*/
        AABB bounds = renderEntity.getBoundingBox();
        poseStack.pushPose();

        float scaleFactor = 0.5f / (float) Math.max(Math.max(bounds.getXsize(), bounds.getYsize() * 0.66f), bounds.getZsize());

        if (entityType == EntityType.SQUID || entityType == EntityType.GLOW_SQUID) {
            scaleFactor *= 0.4f;
            poseStack.translate(0, 0.3, 0);
        }
        else if (entityType == EntityType.ENDER_DRAGON) scaleFactor *= 4;

        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.5f, 0.15f, 0);
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
            poseStack.mulPose(new Quaternionf().rotateXYZ(0.5f, 0.5f, 0));
        } else if (transformType == ItemDisplayContext.FIXED) {
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
            poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.PI / -2, 0, 0));
        } else {
            poseStack.translate(0.5f, 0.4f, 0.5f);
            scaleFactor *= 0.6f;
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
            poseStack.mulPose(new Quaternionf().rotateXYZ(0f, (float) Math.PI, 0));
        }

        var bufferSub = bufferIn;
        if (itemStack.hasTag() && itemStack.getTag().contains("material")) {
            ResourceLocation texLoc = FleshbindingManager.texturesByLabel.get(itemStack.getTag().getString("material"));
            if (texLoc != null) {
                bufferSub = renderType -> {
                    if (renderType == RenderType.entityShadow(ResourceLocation.parse("textures/misc/shadow.png"))) {
                        return bufferIn.getBuffer(renderType);
                    }
                    RenderType.CompositeState compState = RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_SOLID)
                            .setTextureState(new RenderStateShard.TextureStateShard(texLoc, false, false))
                            .setTransparencyState(PASSTHROUGH)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true);
                    renderType = RenderType.create("idol_entity", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, compState);
                    return bufferIn.getBuffer(renderType);
                };
            }
        }
        entityRenderDispatcher.render(renderEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, bufferSub, lightColor);
        poseStack.popPose();
    }
}
