package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.diagrams.ChalkLineBlock;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public class FlowingFluidInjector {

    @Inject(method = "canHoldFluid", at = @At("RETURN"), cancellable = true)
    private void canHoldFluid(BlockGetter p_75973_, BlockPos pos, BlockState state, Fluid p_75976_, CallbackInfoReturnable<Boolean> ci) {
        if(!state.is(OtherverseBlocks.CHALK_LINE.get()) || !state.getValue(ChalkLineBlock.hasScaffolding)) return;
        ci.setReturnValue(false);
        ci.cancel();
    }
}
