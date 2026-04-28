package com.shermansplanet.otherverse.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.shermansplanet.otherverse.ClientEvents;
import com.shermansplanet.otherverse.familiar.MobRetexturer;
import com.shermansplanet.otherverse.spirits.HallowTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@Mixin(ItemRenderer.class)
public class ItemRendererInjector {

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

    private static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET = new RenderStateShard.OutputStateShard("item_entity_target", () -> {
        if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getItemEntityTarget().bindWrite(false);
        }

    }, () -> {
        if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }

    });
    private static final RenderStateShard.ShaderStateShard RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeItemEntityTranslucentCullShader);
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    private static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    private static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
    private static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(true, true);

    @Shadow
    public ItemModelShaper getItemModelShaper() {
        return null;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(ItemStack stack, ItemDisplayContext transformType, boolean p_115146_, PoseStack poseStack, MultiBufferSource p_115148_, int packedLight, int p_115150_, BakedModel bakedModel, CallbackInfo ci) {
        if (stack.isEmpty() || !stack.hasTag()) return;
        if (!stack.getTag().contains("hallow")) return;
        var spiritType = stack.getTag().getCompound("hallow").getString("spirit_type");
        var replacements = modelReplacements.get(stack.getItem());
        if (replacements == null) return;
        var pair = replacements.get(spiritType);
        if (pair == null) return;
        var testRenderType = pair.getSecond();
        poseStack.pushPose();
        boolean flag = transformType == ItemDisplayContext.GUI || transformType == ItemDisplayContext.GROUND || transformType == ItemDisplayContext.FIXED;
        if (flag) {
            if (stack.is(Items.TRIDENT)) {
                bakedModel = this.getItemModelShaper().getModelManager().getModel(ModelResourceLocation.parse("minecraft:trident#inventory"));
            } else if (stack.is(Items.SPYGLASS)) {
                bakedModel = this.getItemModelShaper().getModelManager().getModel(ModelResourceLocation.parse("minecraft:spyglass#inventory"));
            }
        }

        bakedModel = ForgeHooksClient.handleCameraTransforms(poseStack, bakedModel, transformType, p_115146_);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        if (!bakedModel.isCustomRenderer() && (!stack.is(Items.TRIDENT) || flag)) {
            boolean flag1;
            if (transformType != ItemDisplayContext.GUI && !transformType.firstPerson() && stack.getItem() instanceof BlockItem) {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
            } else {
                flag1 = true;
            }
            for (var model : bakedModel.getRenderPasses(stack, flag1)) {
                VertexConsumer vertexconsumer;
                if (stack.is(ItemTags.COMPASSES) && stack.hasFoil()) {
                    poseStack.pushPose();
                    PoseStack.Pose posestack$pose = poseStack.last();
                    if (transformType == ItemDisplayContext.GUI) {
                        posestack$pose.pose().scale(0.5F);
                    } else if (transformType.firstPerson()) {
                        posestack$pose.pose().scale(0.75F);
                    }

                    if (flag1) {
                        vertexconsumer = getCompassFoilBufferDirect(p_115148_, testRenderType, posestack$pose);
                    } else {
                        vertexconsumer = getCompassFoilBuffer(p_115148_, testRenderType, posestack$pose);
                    }

                    poseStack.popPose();
                } else if (flag1) {
                    vertexconsumer = getFoilBufferDirect(p_115148_, testRenderType, true, stack.hasFoil());
                } else {
                    vertexconsumer = getFoilBuffer(p_115148_, testRenderType, true, stack.hasFoil());
                }

                this.renderModelLists(model, stack, packedLight, p_115150_, poseStack, vertexconsumer);
            }
        } else {
            IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, transformType, poseStack, p_115148_, packedLight, p_115150_);
        }

        poseStack.popPose();

        ci.cancel();
    }

    @Shadow
    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource p_115181_, RenderType p_115182_, PoseStack.Pose p_115183_) {
        return null;
    }

    @Shadow
    public static VertexConsumer getCompassFoilBufferDirect(MultiBufferSource p_115208_, RenderType p_115209_, PoseStack.Pose p_115210_) {
        return null;
    }

    @Shadow
    public static VertexConsumer getFoilBuffer(MultiBufferSource p_115212_, RenderType p_115213_, boolean p_115214_, boolean p_115215_) {
        return null;
    }

    @Shadow
    public static VertexConsumer getFoilBufferDirect(MultiBufferSource p_115223_, RenderType p_115224_, boolean p_115225_, boolean p_115226_) {
        return null;
    }

    @Shadow
    public void renderModelLists(BakedModel p_115190_, ItemStack p_115191_, int p_115192_, int p_115193_, PoseStack p_115194_, VertexConsumer p_115195_) {
    }

    private static HashMap<Item, HashMap<String, Pair<BakedModel, RenderType>>> modelReplacements = new HashMap<>();

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    public void onGetModel(ItemStack stack, @Nullable Level p_174266_, @Nullable LivingEntity p_174267_, int p_174268_, CallbackInfoReturnable<BakedModel> ci) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains("hallow") || stack.is(Items.TRIDENT)) return;

        var spiritType = stack.getTag().getCompound("hallow").getString("spirit_type");
        if(spiritType.isEmpty()) return;

        var model = modelReplacements.computeIfAbsent(stack.getItem(), t -> new HashMap<>())
                .computeIfAbsent(spiritType, t -> getModel(stack.getItem(), t));

        if (model == null || model.getFirst().isCustomRenderer()) return;

        ci.setReturnValue(model.getFirst());

        ci.cancel();
    }

//    119249_.popPush("special");
//      this.loadTopLevel(new ModelResourceLocation("minecraft:trident_in_hand#inventory"));
//      this.loadTopLevel(new ModelResourceLocation("minecraft:spyglass_in_hand#inventory"));
//      Set<ResourceLocation> additionalModels = Sets.newHashSet();
//      net.minecraftforge.client.ForgeHooksClient.onRegisterAdditionalModels(additionalModels);
//      for (ResourceLocation rl : additionalModels) {
//         UnbakedModel unbakedmodel = this.getModel(rl); // loadTopLevel(...), but w/o ModelResourceLocation limitation
//         this.unbakedCache.put(rl, unbakedmodel);
//         this.topLevelModels.put(rl, unbakedmodel);
//      }
//      p_119249_.popPush("textures");
//      Set<Pair<String, String>> set = Sets.newLinkedHashSet();
//      Set<Material> set1 = this.topLevelModels.values().stream().flatMap((p_119340_) -> {
//         return p_119340_.getMaterials(this::getModel, set).stream();
//      }).collect(Collectors.toSet());
//      set1.addAll(UNREFERENCED_TEXTURES);
//      net.minecraftforge.client.ForgeHooksClient.gatherFluidTextures(set1);
//      set.stream().filter((p_119357_) -> {
//         return !p_119357_.getSecond().equals(MISSING_MODEL_LOCATION_STRING);
//      }).forEach((p_119292_) -> {
//         LOGGER.warn("Unable to resolve texture reference: {} in {}", p_119292_.getFirst(), p_119292_.getSecond());
//      });
//      Map<ResourceLocation, List<Material>> map = set1.stream().collect(Collectors.groupingBy(Material::atlasLocation));
//      p_119249_.popPush("stitching");
//      this.atlasPreparations = Maps.newHashMap();
//
//      for(Map.Entry<ResourceLocation, List<Material>> entry : map.entrySet()) {
//         TextureAtlas textureatlas = new TextureAtlas(entry.getKey());
//         TextureAtlas.Preparations textureatlas$preparations = textureatlas.prepareToStitch(this.resourceManager, entry.getValue().stream().map(Material::texture), p_119249_, p_119250_);
//         this.atlasPreparations.put(entry.getKey(), Pair.of(textureatlas, textureatlas$preparations));
//      }
//
//      p_119249_.pop();
//   }

    private Pair<BakedModel, RenderType> getModel(Item item, String spiritName) {
        var bakery = Minecraft.getInstance().getModelManager().getModelBakery();
        var itemKey = ForgeRegistries.ITEMS.getKey(item);
        var modelLocation = new ModelResourceLocation(itemKey.getNamespace(), itemKey.getPath(), "inventory");
        var model = bakery.getModel(modelLocation);

        System.out.println("GOT MODEL: " + modelLocation);
        System.out.println("MODEL TYPE: " + model.getClass().getName());

        model.resolveParents(bakery::getModel);
        List<ResourceLocation> locations = new ArrayList<>();
        Set<BlockModel> blockModels = new HashSet<>();
        HallowTextureManager.GetBlockModels(model, blockModels);

        for(BlockModel blockModel : blockModels) {
            for (var s : blockModel.textureMap.keySet()) {
                System.out.println(s);
                Material material = blockModel.getMaterial(s);
                System.out.println(material.texture());
                if (MissingTextureAtlasSprite.getLocation().equals(material.texture())) continue;
                var rl = ResourceLocation.fromNamespaceAndPath(material.texture().getNamespace(),
                        "textures/" + material.texture().getPath() + ".png");
                if(locations.contains(rl)) continue;
                locations.add(rl);
            }
        }
        if(locations.isEmpty()) return null;
        var primaryTex = MobRetexturer.makeSpiritVariant(locations, spiritName);
        if (primaryTex == null) {
            return null;
        }

        for (int i = 0; i < locations.size(); i++) {
            var loc = locations.get(i);
            loc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(),
                    loc.getPath().substring(9, loc.getPath().length() - 4));
            HallowTextureManager.offsetsByMaterial.put(loc, Pair.of(locations.size(), i));
        }

        ClientEvents.HALLOW_TEXTURE_MANAGER.quietReload();
        BakedModel m = model.bake(DUMMY_BAKER, x -> ClientEvents.HALLOW_TEXTURE_MANAGER.getSpritePublic(primaryTex, x, new HashMap<>()), BlockModelRotation.X0_Y0, modelLocation);

        RenderType.CompositeState compState = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(primaryTex.getFirst(), false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(true);

        var key = "hallow_item_" + itemKey.getNamespace() + "_" + itemKey.getPath() + "_" + spiritName;
        var rt = RenderType.create(key, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, compState);

        return Pair.of(m, rt);
    }
}
