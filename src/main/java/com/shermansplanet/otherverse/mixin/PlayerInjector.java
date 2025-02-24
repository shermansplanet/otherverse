package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerInjector extends LivingEntity implements net.minecraftforge.common.extensions.IForgePlayer{
    protected PlayerInjector(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Override
    public LivingEntity self() {
        return super.self();
    }

    @Override
    public boolean canStandOnFluid(FluidState p_204067_) {
        return p_204067_.is(FluidTags.LAVA) && FamiliarManager.hasFamiliarType((Player) self(), EntityType.STRIDER);
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "HEAD"), cancellable = true)
    private static void findRespawnPositionAndUseSpawnBlock(ServerLevel p_36131_, BlockPos p_36132_, float p_36133_, boolean p_36134_, boolean p_36135_, CallbackInfoReturnable<Optional<Vec3>> ci) {
        BlockState blockstate = p_36131_.getBlockState(p_36132_);
        Block block = blockstate.getBlock();
        if(!(block instanceof BedBlock)) return;
        var demesne = DemesnesManager.getData(p_36131_, p_36132_);
        if(demesne == null || demesne.getPerkLevel(DemesnesManager.DemesnePerk.SPAWN_SET) == 0) return;
        ci.setReturnValue(BedBlock.findStandUpPosition(EntityType.PLAYER, p_36131_, p_36132_, p_36133_));
        ci.cancel();
    }
}
