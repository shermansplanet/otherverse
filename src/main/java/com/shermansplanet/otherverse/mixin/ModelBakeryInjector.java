package com.shermansplanet.otherverse.mixin;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.IdolUnbakedModel;
import com.shermansplanet.otherverse.spirits.IModelGetter;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Map;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryInjector implements IModelGetter {

    @Shadow
    private static ItemModelGenerator ITEM_MODEL_GENERATOR;

    public ItemModelGenerator getItemModelGenerator(){
        return ITEM_MODEL_GENERATOR;
    }

    @Shadow
    private Map<ResourceLocation, Pair<TextureAtlas, TextureAtlas.Preparations>> atlasPreparations;
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private final Map<ResourceLocation, UnbakedModel> unbakedCache = Maps.newHashMap();

    @Shadow
    private void loadModel(ResourceLocation p_119363_) throws Exception {
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    protected void getModel(ResourceLocation resourceLocation, CallbackInfoReturnable<UnbakedModel> returnable) {
        if (resourceLocation.getNamespace().equals(Otherverse.MODID) && resourceLocation.getPath().equals("idol")) {
            returnable.setReturnValue(new IdolUnbakedModel());
            returnable.cancel();
        }
    }

    @Shadow
    protected BlockModel loadBlockModel(ResourceLocation p_119365_) throws IOException {
        return null;
    }

    public BlockModel loadBlockModelPublic(ResourceLocation p_119365_) throws IOException {
        return loadBlockModel(p_119365_);
    }
}
