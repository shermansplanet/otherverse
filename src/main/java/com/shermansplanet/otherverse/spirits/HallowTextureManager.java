package com.shermansplanet.otherverse.spirits;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class HallowTextureManager extends TextureAtlasHolder {

    public static final ResourceLocation ATLAS_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/atlas/hallows.png");
    public static ArrayList<ResourceLocation> hallowResourceLocations = new ArrayList<>();
    public static final HashMap<ResourceLocation, Pair<Integer, Integer>> offsetsByMaterial = new HashMap<>();

    public HallowTextureManager(TextureManager p_118802_) {
        super(p_118802_, ATLAS_LOCATION, ResourceLocation.parse("hallow"));
    }

    protected Stream<ResourceLocation> getResourcesToLoad() {
        return hallowResourceLocations.stream().filter(Objects::nonNull);
    }

    public void quietReload() {
        var pf = Minecraft.getInstance().getProfiler();
        this.apply(this.prepare(rm, pf), pf);
    }

    private void apply(SpriteLoader.Preparations p_252333_, ProfilerFiller p_250624_) {
        p_250624_.startTick();
        p_250624_.push("upload");
        this.textureAtlas.upload(p_252333_);
        p_250624_.pop();
        p_250624_.endTick();
    }

    public TextureAtlasSprite getSpritePublic(Pair<ResourceLocation, AbstractTexture> tex, Material material, HashMap<ResourceLocation, DynamicSprite> spriteCache) {
        if(spriteCache.containsKey(material.texture())) return spriteCache.get(material.texture());
        var p = offsetsByMaterial.getOrDefault(material.texture(), Pair.of(1,0));
        var sprite = makeSprite(tex.getFirst(), (DynamicTexture) tex.getSecond(),
                16, 16, p.getFirst(), p.getSecond());
        spriteCache.put(material.texture(), sprite);
        return sprite;
    }

    private static DynamicSprite makeSprite(ResourceLocation newTexLoc, DynamicTexture newTex, int WIDTH, int HEIGHT, int textureHeight, int offset) {
        var info = new TextureAtlasSprite.Info(newTexLoc, WIDTH, HEIGHT, new AnimationMetadataSection(ImmutableList.of(new AnimationFrame(0, -1)), WIDTH, HEIGHT, 1, false));
        return new DynamicSprite(
                Minecraft.getInstance().getModelManager().getAtlas(ATLAS_LOCATION),
                0, WIDTH, HEIGHT * textureHeight, 0, offset * HEIGHT,
                newTex, info
        );
    }
}