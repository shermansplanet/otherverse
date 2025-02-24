package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.diagrams.IBlockRenderGetter;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherInjector implements IBlockRenderGetter {

    @Shadow
    private final BlockColors blockColors;

    public BlockRenderDispatcherInjector(BlockColors blockColors) {
        this.blockColors = blockColors;
    }

    @Override
    public BlockColors getBlockColors() {
        return blockColors;
    }
}
