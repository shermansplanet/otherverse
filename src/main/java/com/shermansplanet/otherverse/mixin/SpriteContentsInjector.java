package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.spirits.IAnimatedTextureGetter;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsInjector implements Stitcher.Entry, AutoCloseable, IAnimatedTextureGetter {
    private AnimationMetadataSection cachedMetadata = null;

    public AnimationMetadataSection getAnimationMetadata() {
        return cachedMetadata;
    }


    class AnimatedTexture{}

    @Inject(method = "createAnimatedTexture", at = @At("HEAD"))
    private void onCreateTex(FrameSize p_250817_, int p_249792_, int p_252353_, AnimationMetadataSection p_250947_, CallbackInfoReturnable<AnimatedTexture> ci) {
        cachedMetadata = p_250947_;
    }
}
