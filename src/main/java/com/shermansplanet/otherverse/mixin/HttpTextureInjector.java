package com.shermansplanet.otherverse.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.shermansplanet.otherverse.familiar.ITextureGetter;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HttpTexture.class)
public class HttpTextureInjector extends SimpleTexture implements ITextureGetter {

    public HttpTextureInjector(ResourceLocation p_118133_) {
        super(p_118133_);
    }

    private NativeImage nativeImage;

    @Inject(method = "loadCallback", at = @At("HEAD"))
    private void onLoadCallback(NativeImage ni, CallbackInfo ci) {
        nativeImage = new NativeImage(ni.getWidth(), ni.getHeight(), false);
        nativeImage.copyFrom(ni);
    }

    @Override
    public NativeImage getTexture() {
        return nativeImage;
    }
}
