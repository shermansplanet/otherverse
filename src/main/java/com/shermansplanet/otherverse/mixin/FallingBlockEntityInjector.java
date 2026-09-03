package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityInjector extends Entity {

    private CompoundTag hallowTag = null;

    public FallingBlockEntityInjector(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Override
    public ItemEntity spawnAtLocation(ItemLike block) {
        if (hallowTag == null) return this.spawnAtLocation(block, 0);
        var stack = new ItemStack(block);
        stack.getOrCreateTag().put("hallow", hallowTag);
        return this.spawnAtLocation(stack, 0);
    }

    @Inject(method = "setStartPos", at = @At(value = "HEAD"))
    public void onSetStartPos(BlockPos pos, CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        var data = DiagramManager.getOrCreateLevelData(this.level());
        hallowTag = data.getPlacedItemTag(pos);
        if (hallowTag == null) return;
        data.removePlacedItemTag(pos);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap;broadcast(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/Packet;)V"))
    public void onTick(CallbackInfo ci) {
        if (hallowTag == null) return;
        if (this.level().isClientSide()) return;
        var data = DiagramManager.getOrCreateLevelData(this.level());
        data.putPlacedItemTag(blockPosition(), hallowTag);
    }
}
