package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.demesnes.IPowerGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityInjector extends BlockEntity implements MenuProvider, IPowerGetter {

    @Shadow
    MobEffect primaryPower;
    @Shadow
    MobEffect secondaryPower;
    public BeaconBlockEntityInjector(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Shadow
    private static int updateBase(Level p_155093_, int p_155094_, int p_155095_, int p_155096_) {
        return 0;
    }

    @Shadow
    private static void applyEffects(Level p_155098_, BlockPos p_155099_, int p_155100_, @Nullable MobEffect p_155101_, @Nullable MobEffect p_155102_) {

    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void onTick(Level level, BlockPos pos, BlockState p_155110_, BeaconBlockEntity p_155111_, CallbackInfo ci) {
        if(!(level instanceof ServerLevel sl)) return;
        var demesne = DemesnesManager.getData(sl, pos);
        if(demesne == null || demesne.getPerkLevel(DemesnesManager.DemesnePerk.BEACON_HIDE) == 0) return;

        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        var levels = updateBase(level, i, j, k);

        if (levels > 0) {
            applyEffects(level, pos, levels, ((IPowerGetter)p_155111_).getPrimaryPower(), ((IPowerGetter) p_155111_).getSecondaryPower());
        }
    }

    public MobEffect getPrimaryPower(){
        return primaryPower;
    }

    public MobEffect getSecondaryPower(){
        return secondaryPower;
    }
}
