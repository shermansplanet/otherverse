package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.spirits.IMultiPartExposer;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MultiPart.class)
public abstract class MultiPartInjector implements UnbakedModel, IMultiPartExposer {
    @Shadow
    private StateDefinition<Block, BlockState> definition;

    public StateDefinition<Block, BlockState> getDefinition(){
        return definition;
    }
}
