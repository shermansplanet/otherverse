package com.shermansplanet.otherverse.echoes;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class EchoRenderer extends EntityRenderer<EchoEntity> {

  private final RenderPlayerSpirit playerRenderer;
  private final Map<GameProfile, GameProfile> checkedProfiles = Maps.newHashMap();

  protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentCullShader);
  protected static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
  protected static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);

  protected static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.DST_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
  }, () -> {
    RenderSystem.disableBlend();
    RenderSystem.defaultBlendFunc();
  });

  public EchoRenderer(EntityRendererProvider.Context context) {
    super(context);
    playerRenderer = new RenderPlayerSpirit(context);
  }

  @Override
  public void render(EchoEntity echo, float entityYaw, float partialTicks,
      PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
    super.render(echo, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    Mob innerEntity = echo.getInnerEntity();
    if (innerEntity == null) {
      return;
    }
    EntityRenderer render = entityRenderDispatcher.renderers.get(innerEntity.getType());
    if (render == null) {
      return;
    }

    matrixStackIn.translate(echo.glitchOffset.x, echo.glitchOffset.y, echo.glitchOffset.z);
    matrixStackIn.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, echo.getYRot(), 0)));

    MultiBufferSource bufferSub = renderType -> {
      ResourceLocation texLoc = (echo.isPlayer() ? playerRenderer : render).getTextureLocation(innerEntity);
      RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
          .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
          .setTextureState(new RenderStateShard.TextureStateShard(texLoc, false, false))
          .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
          .setLightmapState(LIGHTMAP)
          .setOverlayState(OVERLAY)
          .createCompositeState(true);
      renderType = RenderType.create("entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, rendertype$compositestate);
      return bufferIn.getBuffer(renderType);
    };

    try {
      if (echo.isPlayer()) {
        GameProfile gameProfile = new GameProfile(echo.getPlayerUUID(), echo.getPlayerName());
        ResourceLocation resourcelocation = DefaultPlayerSkin.getDefaultSkin();
        Minecraft minecraft = Minecraft.getInstance();
        // Check if we have loaded the (texturized) profile before, otherwise we load it and cache it.
        if (!checkedProfiles.containsKey(gameProfile)) {
          Property property = (Property) Iterables
              .getFirst(gameProfile.getProperties().get("textures"), (Object) null);
          if (property == null) {
            // The game profile enhanced with texture information.
            GameProfile newGameProfile = Minecraft.getInstance().getMinecraftSessionService()
                .fillProfileProperties(gameProfile, true);
            checkedProfiles.put(gameProfile, newGameProfile);
          }
        } else {
          Map map = minecraft.getSkinManager()
              .getInsecureSkinInformation(checkedProfiles.get(gameProfile));
          if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            resourcelocation = minecraft.getSkinManager().registerTexture(
                (MinecraftProfileTexture) map.get(MinecraftProfileTexture.Type.SKIN),
                MinecraftProfileTexture.Type.SKIN);
          }
        }
        playerRenderer.setPlayerTexture(resourcelocation);
        playerRenderer.render(innerEntity, entityYaw, partialTicks, matrixStackIn, bufferSub,
            packedLightIn);
      } else {
        // Make new PoseStack, to fix stack invalidity when a crash occurs.
        PoseStack poseStackInner = new PoseStack();
        poseStackInner.last().pose().load(matrixStackIn.last().pose());
        poseStackInner.last().normal().load(matrixStackIn.last().normal());
        render.render(innerEntity, entityYaw, 0, poseStackInner, bufferSub, packedLightIn);
      }
    } catch (Exception e) {
      // Invalid entity, so set as swarm.
      echo.setPlayerId(""); // Just in case the crash was caused by a player echo.
    }

  }

  @Override
  public ResourceLocation getTextureLocation(EchoEntity entity) {
    return null;
  }

  public static class RenderPlayerSpirit extends LivingEntityRenderer<Mob, PlayerModel<Mob>> {

    private ResourceLocation playerTexture;

    public RenderPlayerSpirit(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this,
          new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
          new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
      this.addLayer(new ArrowLayer<>(context, this));
      this.addLayer(
          new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
      return playerTexture;
    }

    public void setPlayerTexture(ResourceLocation resourcelocation) {
      playerTexture = resourcelocation;
    }
  }
}
