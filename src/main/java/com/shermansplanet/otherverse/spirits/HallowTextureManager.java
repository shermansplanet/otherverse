package com.shermansplanet.otherverse.spirits;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.textures.ForgeTextureMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class HallowTextureManager extends TextureAtlasHolder {

    public record TextureSetData(ResourceLocation loc, AbstractTexture texture, int width, int height) {
    }

    public record TextureFrameData(int width, int height, int yOffset) {
    }

    public static final ResourceLocation ATLAS_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/atlas/hallows.png");
    public static ArrayList<ResourceLocation> hallowResourceLocations = new ArrayList<>();
    public static final HashMap<ResourceLocation, TextureFrameData> offsetsByMaterial = new HashMap<>();

    public HallowTextureManager(TextureManager p_118802_) {
        super(p_118802_, ATLAS_LOCATION, ResourceLocation.parse("hallow"));
    }

    protected Stream<ResourceLocation> getResourcesToLoad() {
        return hallowResourceLocations.stream().filter(Objects::nonNull);
    }

    public void quietReload() {
        var pf = Minecraft.getInstance().getProfiler();
        var prep = SpriteLoader.create(this.textureAtlas).loadAndStitch(Minecraft.getInstance().getResourceManager(), ATLAS_LOCATION, 0, Minecraft.getInstance());
        this.apply(prep.join(), pf);
    }

    private void apply(SpriteLoader.Preparations p_252333_, ProfilerFiller p_250624_) {
        p_250624_.startTick();
        p_250624_.push("upload");
        this.textureAtlas.upload(p_252333_);
        p_250624_.pop();
        p_250624_.endTick();
    }

    public TextureAtlasSprite getSpritePublic(TextureSetData tex, Material material, HashMap<ResourceLocation, DynamicSprite> spriteCache) {
        if (spriteCache.containsKey(material.texture())) return spriteCache.get(material.texture());
        var frameData = offsetsByMaterial.getOrDefault(material.texture(), new TextureFrameData(16, 16, 0));
        var sprite = makeSprite(tex.loc, (DynamicTexture) tex.texture,
                frameData.width, frameData.height, tex.width, tex.height, frameData.yOffset);
        spriteCache.put(material.texture(), sprite);
        return sprite;
    }

    private static DynamicSprite makeSprite(ResourceLocation newTexLoc, DynamicTexture newTex, int frameWidth, int frameHeight, int wholeTextureWidth, int wholeTextureHeight, int yOffset) {
        var anim = new AnimationMetadataSection(ImmutableList.of(new AnimationFrame(0, -1)), frameWidth, frameHeight, 1, false);
        return new DynamicSprite(
                ATLAS_LOCATION, new SpriteContents(newTexLoc, new FrameSize(frameWidth, frameHeight), newTex.getPixels(), anim, ForgeTextureMetadata.EMPTY),
                wholeTextureWidth, wholeTextureHeight, 0, yOffset
        );
    }

    public static void GetBlockModels(UnbakedModel unbakedModel, Set<BlockModel> blockModels) {
        var bakery = Minecraft.getInstance().getModelManager().getModelBakery();
        if (unbakedModel instanceof BlockModel bm) {
            blockModels.add(bm);
            if (bm.parent != null) GetBlockModels(bm.parent, blockModels);
        } else if (unbakedModel instanceof MultiVariant mv) {
            for (var variant : mv.getVariants()) {
                var model = bakery.getModel(variant.getModelLocation());
                GetBlockModels(model, blockModels);
            }
        } else if (unbakedModel instanceof MultiPart mp) {
            for (var part : mp.getMultiVariants()) {
                GetBlockModels(part, blockModels);
            }
        }
    }
}