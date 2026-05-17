package com.shermansplanet.otherverse.spirits;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.ClientEvents;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.diagrams.ChalkCircleRenderer;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IBlockRenderGetter;
import com.shermansplanet.otherverse.familiar.MobRetexturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlacedHallowRenderer {
    private static final Map<BlockState, HashMap<String, Pair<BakedModel, RenderType>>> modelByStateCache = Maps.newIdentityHashMap();
    private static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    private static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCutoutShader);
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final HashSet<ShrineHelper.Shrine> shrinesToRender = new HashSet<>();

    private static final ModelBaker DUMMY_BAKER = new ModelBaker() {
        @Override
        public UnbakedModel getModel(ResourceLocation p_252194_) {
            return null;
        }

        @Override
        public @org.jetbrains.annotations.Nullable BakedModel bake(ResourceLocation p_250776_, ModelState p_251280_) {
            return null;
        }

        @Override
        public @org.jetbrains.annotations.Nullable BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            return null;
        }

        @Override
        public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
            return null;
        }
    };

    @SubscribeEvent
    public static void renderTick(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        renderHallows(player, event);
    }

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event) {
        shrinesToRender.clear();
    }

    /*@SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() % 5 != 0) return;
        if (SightManager.shouldRenderSight()) {
            for (var shrine : shrinesToRender) {
                ShrineHelper.recalculateShrine(shrine);
            }
        }
    }*/

    private static void renderHallows(LocalPlayer player, RenderLevelStageEvent event) {
        var levelData = DiagramManager.getOrCreateLevelData(player.level());
        var poseStack = event.getPoseStack();
        var camera = event.getCamera();
        poseStack.pushPose();
        var s = 0.995f;
        poseStack.scale(s, s, s);
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());
        var multiBufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderDist = 64 * 64;

        var renderShrineBounds = SightManager.shouldRenderSight();
        if (renderShrineBounds) shrinesToRender.clear();

        for (BlockPos pos : levelData.getAllPlacedItemPositions()) {
            if (player.position().distanceToSqr(new Vec3(pos.getX(), pos.getY(), pos.getZ())) > renderDist) continue;
            var tag = levelData.getPlacedItemTag(pos);
            var bs = player.level().getBlockState(pos);
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            var st = tag.getString("spirit_type");
            renderSingleBlock((IBlockRenderGetter) Minecraft.getInstance().getBlockRenderer(), bs, poseStack, multiBufferSource,
                    255, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout(), st);
            poseStack.popPose();
            if (!tag.contains("shrine") || !renderShrineBounds) continue;
            var shrine = ShrineHelper.shrinesByPosition.computeIfAbsent(player.level(), x -> new HashMap<>()).get(pos);
            if (shrine == null) {
                shrine = ShrineHelper.getShrine(player.level(), pos, Spirits.spiritsByLabel.get(st));
            }
            shrinesToRender.add(shrine.parentShrine == null ? shrine : shrine.parentShrine);
        }

        if (!renderShrineBounds) {
            poseStack.popPose();
            return;
        }

        var dir1 = new Vec3(1, 0, 0);
        var dir2 = new Vec3(0, 0, 1);
        var rot = (event.getRenderTick() + event.getPartialTick()) * 0.003f;

        var t = (player.level().getGameTime() + Minecraft.getInstance().getPartialTick()) / 20f;

        for (var shrine : shrinesToRender) {
            var behavior = ShrineHelper.overflowBehaviors.get(shrine.st);
            if (behavior == null) continue;
            poseStack.pushPose();
            var range = shrine.range;
            var shape = behavior.getShape();
            var cylinderTop = switch (shape) {
                case BELOW -> range.bottom();
                case ABOVE -> range.bottom() + range.height();
                case CENTERED -> (int) Math.ceil(range.center().y + range.height() / 2f);
            };
            poseStack.translate(range.center().x, cylinderTop, range.center().z);
            var r = shrine.isCombined ? (int) ((Math.sin(t) + 1) * 128) : 255;
            var g = shrine.isCombined ? (int) ((Math.sin(t + Math.PI * 2 / 3) + 1) * 128) : 255;
            var b = shrine.isCombined ? (int) ((Math.sin(t + Math.PI * 4 / 3) + 1) * 128) : 255;
            ChalkCircleRenderer.drawCylinder(poseStack, multiBufferSource, dir1, dir2, 32, range.radius(),
                    rot, r, g, b, 100, range.height());
            poseStack.popPose();
        }

/*        var levelShrines = ShrineHelper.shrinesByChunk.computeIfAbsent(player.level, x -> new HashMap<>());

        for (var chunkShrines : levelShrines.values()) {
            for (var shrine : chunkShrines) {
                if (shrinesToRender.contains(shrine)) continue;
                var behavior = ShrineHelper.overflowBehaviors.get(shrine.st);
                if (behavior == null) continue;
                poseStack.pushPose();
                var range = shrine.range;
                var shape = behavior.getShape();
                var cylinderTop = switch (shape) {
                    case BELOW -> range.bottom();
                    case ABOVE -> range.bottom() + range.height();
                    case CENTERED -> (int) Math.ceil(range.center().y + range.height() / 2f);
                };
                poseStack.translate(range.center().x, cylinderTop, range.center().z);
                var r = 0;
                var g = shrine.isCombined ? 50 : 255;
                var b = shrine.isCombined ? 200 : 255;
                ChalkCircleRenderer.drawCylinder(poseStack, multiBufferSource, dir1, dir2, 32, range.radius(),
                        rot, r, g, b, 100, range.height());
                poseStack.popPose();
            }
        }*/

        poseStack.popPose();
    }

    private static void renderSingleBlock(IBlockRenderGetter blockRenderGetter, BlockState p_110913_, PoseStack p_110914_, MultiBufferSource p_110915_, int p_110916_, int p_110917_, ModelData modelData, RenderType renderType, String spiritName) {
        RenderShape rendershape = p_110913_.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            switch (rendershape) {
                case MODEL:
                    var modelAndRender = getBlockModel(p_110913_, spiritName);
                    if (modelAndRender.getFirst() == null) return;
                    int i = blockRenderGetter.getBlockColors().getColor(p_110913_, null, null, 0);
                    float f = (float) (i >> 16 & 255) / 255.0F;
                    float f1 = (float) (i >> 8 & 255) / 255.0F;
                    float f2 = (float) (i & 255) / 255.0F;
                    for (RenderType rt : modelAndRender.getFirst().getRenderTypes(p_110913_, RandomSource.create(42), modelData))
                        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(p_110914_.last(), p_110915_.getBuffer(modelAndRender.getSecond()), p_110913_, modelAndRender.getFirst(), f, f1, f2, p_110916_, p_110917_, modelData, rt);
                    break;
                case ENTITYBLOCK_ANIMATED:
                    ItemStack stack = new ItemStack(p_110913_.getBlock());
                    IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, ItemDisplayContext.NONE, p_110914_, p_110915_, p_110916_, p_110917_);
            }

        }
    }

    private static Pair<BakedModel, RenderType> getBlockModel(BlockState state, String spiritType) {
        var cachedForState = modelByStateCache.computeIfAbsent(state, x -> new HashMap<>());
        var cachedModel = cachedForState.get(spiritType);
        if (cachedModel != null) return cachedModel;
        var pair = makeBlockModel(state, spiritType);
        if (pair == null) pair = new Pair<>(null, null);
        cachedForState.put(spiritType, pair);
        return pair;
    }

    private static Pair<BakedModel, RenderType> makeBlockModel(BlockState state, String spiritType) {
        var bakery = Minecraft.getInstance().getModelManager().getModelBakery();
        var blockKey = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        var modelLocation = BlockModelShaper.stateToModelLocation(state);
        var model = bakery.getModel(modelLocation);

        model.resolveParents(bakery::getModel);
        List<ResourceLocation> locations = new ArrayList<>();
        Set<BlockModel> blockModels = new HashSet<>();
        HallowTextureManager.GetBlockModels(model, blockModels);

        for (BlockModel blockModel : blockModels) {
            for (var s : blockModel.textureMap.keySet()) {
                System.out.println(s);
                Material material = blockModel.getMaterial(s);
                System.out.println(material.texture());
                if (MissingTextureAtlasSprite.getLocation().equals(material.texture())) continue;
                var rl = ResourceLocation.fromNamespaceAndPath(material.texture().getNamespace(),
                        "textures/" + material.texture().getPath() + ".png");
                if (locations.contains(rl)) continue;
                locations.add(rl);
            }
        }

        if (locations.isEmpty()) {
            LOGGER.warn("Couldn't find texture for " + blockKey);
            return null;
        }
        var primaryTex = MobRetexturer.makeSpiritVariant(locations, spiritType);

        if (primaryTex == null) {
            LOGGER.warn("Couldn't make texture for " + blockKey);
            return null;
        }

        for (int i = 0; i < locations.size(); i++) {
            var loc = locations.get(i);
            loc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(),
                    loc.getPath().substring(9, loc.getPath().length() - 4));
            HallowTextureManager.offsetsByMaterial.put(loc, Pair.of(locations.size(), i));
        }

        ClientEvents.HALLOW_TEXTURE_MANAGER.quietReload();

        Function<Material, TextureAtlasSprite> func = x -> ClientEvents.HALLOW_TEXTURE_MANAGER.getSpritePublic(primaryTex, x, new HashMap<>());

        var newLoc = new ModelResourceLocation(Otherverse.MODID,
                modelLocation.getNamespace() + "_" + modelLocation.getPath() + "_hallow_" + spiritType, modelLocation.getVariant());

        try {
            BakedModel m = model instanceof MultiVariant mv ? bakeMultiVariant(mv, bakery, func) :
                    model.bake(DUMMY_BAKER, func, BlockModelRotation.X0_Y0, newLoc);

            var key = "hallow_" + blockKey.getNamespace() + "_" + blockKey.getPath() + "_" + spiritType;
            RenderType rt = RenderType.create(key, DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 131072, true, false,
                    RenderType.CompositeState.builder()
                            .setLightmapState(LIGHTMAP)
                            .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(primaryTex.getFirst(), false, false))
                            .createCompositeState(true));

            var pair = Pair.of(m, rt);
            return pair;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BakedModel bakeMultiVariant(MultiVariant mv, ModelBakery bakery, Function<Material, TextureAtlasSprite> p_111851_) {
        if (mv.getVariants().isEmpty()) {
            return null;
        } else {
            WeightedBakedModel.Builder weightedbakedmodel$builder = new WeightedBakedModel.Builder();

            for (Variant variant : mv.getVariants()) {
                BakedModel bakedmodel = forceBake(bakery, variant.getModelLocation(), variant, p_111851_);
                weightedbakedmodel$builder.add(bakedmodel, variant.getWeight());
            }

            return weightedbakedmodel$builder.build();
        }
    }

    private static BakedModel forceBake(ModelBakery bakery, ResourceLocation p_119350_, ModelState p_119351_, Function<Material, TextureAtlasSprite> sprites) {
        UnbakedModel unbakedmodel = bakery.getModel(p_119350_);
        if (unbakedmodel instanceof BlockModel blockmodel) {
            if (blockmodel.getRootModel() == ModelBakery.GENERATION_MARKER) {
                return ((IModelGetter) bakery).getItemModelGenerator().generateBlockModel(sprites, blockmodel).bake(DUMMY_BAKER, blockmodel, sprites, p_119351_, p_119350_, false);
            }
        } else if (unbakedmodel instanceof MultiVariant mv) {
            return bakeMultiVariant(mv, bakery, sprites);
        }

        return unbakedmodel.bake(DUMMY_BAKER, sprites, p_119351_, p_119350_);
    }


}
