package com.shermansplanet.otherverse.spirits;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class HallowTextureManager extends TextureAtlasHolder {

    public record TextureSetData(ResourceLocation loc, AbstractTexture texture, int width, int height) {
    }

    public record SubTextureData(int width, int height, int yOffset) {
    }

    public static final ResourceLocation ATLAS_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/atlas/hallows.png");
    public static ArrayList<ResourceLocation> hallowResourceLocations = new ArrayList<>();
    public static final HashMap<ResourceLocation, SubTextureData> offsetsByMaterial = new HashMap<>();
    private static final HashMap<ResourceLocation, DynamicSprite> spriteCache = new HashMap<>();
    private static final List<TextureAtlasSprite.Ticker> animatedTextures = new ArrayList<>();

    public HallowTextureManager(TextureManager p_118802_) {
        super(p_118802_, ATLAS_LOCATION, ResourceLocation.parse("hallow"));
    }

    protected Stream<ResourceLocation> getResourcesToLoad() {
        return hallowResourceLocations.stream().filter(Objects::nonNull);
    }

    public static void tickAnimatedTextures() {
//        for (var tex : animatedTextures) tex.tickAndUpload();
    }

    public static void resetData() {
        spriteCache.forEach((rl, sprite) -> sprite.contents().close());
        animatedTextures.forEach(TextureAtlasSprite.Ticker::close);
        hallowResourceLocations.clear();
        offsetsByMaterial.clear();
        spriteCache.clear();
        animatedTextures.clear();
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

    public TextureAtlasSprite getSpritePublic(TextureSetData tex, Material material) {
        if (spriteCache.containsKey(material.texture())) return spriteCache.get(material.texture());

        var subTextureData = offsetsByMaterial.getOrDefault(material.texture(), new SubTextureData(16, 16, 0));
        var contents = material.sprite().contents();
        var anim = ((IAnimatedTextureGetter) (contents)).getAnimationMetadata();

        var sprite = new DynamicSprite(
                ATLAS_LOCATION, new SpriteContents(tex.loc, anim.calculateFrameSize(subTextureData.width, subTextureData.height), ((DynamicTexture) tex.texture).getPixels(), anim, contents.forgeMeta),
                tex.width, tex.height, 0, subTextureData.yOffset
        );
        spriteCache.put(material.texture(), sprite);

        var ticker = sprite.createTicker();
        if (ticker != null) animatedTextures.add(ticker);

        return sprite;
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