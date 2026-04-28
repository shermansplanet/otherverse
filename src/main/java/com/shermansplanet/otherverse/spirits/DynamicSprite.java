package com.shermansplanet.otherverse.spirits;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class DynamicSprite extends TextureAtlasSprite {
    private ResourceLocation location;

    protected DynamicSprite(ResourceLocation p_250211_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_) {
//      this.atlasLocation = p_250211_;
//      this.contents = p_248526_;
//      this.x = p_248672_;
//      this.y = p_248637_;
//      this.u0 = (float)p_248672_ / (float)p_248950_;
//      this.u1 = (float)(p_248672_ + p_248526_.width()) / (float)p_248950_;
//      this.v0 = (float)p_248637_ / (float)p_249741_;
//      this.v1 = (float)(p_248637_ + p_248526_.height()) / (float)p_249741_;
        super(p_250211_, p_248526_, p_248950_, p_249741_, p_248672_, p_248637_);
    }

//    public DynamicSprite(TextureAtlas p_118065_, int p_118066_, int p_118067_, int p_118068_, int p_118069_, int p_118070_, DynamicTexture texture, TextureAtlasSprite.Info info) {
        // old constructor:
        // protected TextureAtlasSprite(TextureAtlas p_118358_, TextureAtlasSprite.Info p_118359_, int p_118360_, int p_118361_, int p_118362_, int p_118363_, int p_118364_, NativeImage p_118365_) {
        //      this.atlas = p_118358_;
        //      this.width = p_118359_.width;
        //      this.height = p_118359_.height;
        //      this.name = p_118359_.name;
        //      this.x = p_118363_;
        //      this.y = p_118364_;
        //      this.u0 = (float)p_118363_ / (float)p_118361_;
        //      this.u1 = (float)(p_118363_ + this.width) / (float)p_118361_;
        //      this.v0 = (float)p_118364_ / (float)p_118362_;
        //      this.v1 = (float)(p_118364_ + this.height) / (float)p_118362_;
        //      this.animatedTexture = this.createTicker(p_118359_, p_118365_.getWidth(), p_118365_.getHeight(), p_118360_);
//        super(p_118065_, info, p_118066_, p_118067_, p_118068_, p_118069_, p_118070_, texture.getPixels());
//    }

    public float uvShrinkRatio() {
        return 0;
    }
}