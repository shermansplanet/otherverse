package com.shermansplanet.otherverse.spirits;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class DynamicSprite extends TextureAtlasSprite {
    private ResourceLocation location;

    protected DynamicSprite(ResourceLocation p_250211_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_) {
        super(p_250211_, p_248526_, p_248950_, p_249741_, p_248672_, p_248637_);
    }

    public DynamicSprite(TextureAtlas p_118065_, int p_118066_, int p_118067_, int p_118068_, int p_118069_, int p_118070_, DynamicTexture texture, TextureAtlasSprite.Info info) {
        super(p_118065_, info, p_118066_, p_118067_, p_118068_, p_118069_, p_118070_, texture.getPixels());
    }

    public float uvShrinkRatio() {
        return 0;
    }

    public void close() {
        for (int i = 1; i < this.mainImage.length; ++i) {
            this.mainImage[i].close();
        }
    }
}