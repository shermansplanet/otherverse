package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.ISectionSetter;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.spirits.Chronomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelInjector extends Level implements WorldGenLevel {

    public ServerLevelInjector(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    public void tickNonPassenger(Entity e, CallbackInfo ci) {
        if (!Chronomancy.doesEntityTick(e)) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    public void onBSC(BlockPos p_8751_, BlockState p_8752_, BlockState p_8753_, CallbackInfo ci) {
        DiagramManager.BlockChanged(p_8751_, (ServerLevel) (Object) this);
    }

    @Inject(
            method = "tickChunk",
            at = @At(value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    args = "ldc=tickBlocks"),
            cancellable = true
    )
    private void onTick(LevelChunk chunk, int tickCount, CallbackInfo ci) {
        var modChunk = (ISectionSetter) chunk;
        var profilerfiller = this.getProfiler();
        ChunkPos chunkpos = chunk.getPos();
        int x = chunkpos.getMinBlockX();
        int z = chunkpos.getMinBlockZ();
        if (modChunk.isSet()) {
            var sections = chunk.getSections();
            var totalTicks = sections.length * tickCount;
            for (var ignored = 0; ignored < totalTicks; ignored++) {
                var pos = this.getBlockRandomPos(x, modChunk.getMin(), z, modChunk.getMax() - modChunk.getMin());
                var levelchunksection = sections[chunk.getSectionIndex(pos.getY())];
                BlockState blockstate = levelchunksection.getBlockState(pos.getX() - x, pos.getY() - levelchunksection.bottomBlockY(), pos.getZ() - z);
                tickBlocks(profilerfiller, pos, blockstate);
            }
        } else {
            if (tickCount > 0) {
                for (LevelChunkSection levelchunksection : chunk.getSections()) {
                    if (levelchunksection.isRandomlyTicking()) {
                        int l = levelchunksection.bottomBlockY();

                        for (int k = 0; k < tickCount; ++k) {
                            BlockPos blockpos1 = this.getBlockRandomPos(x, l, z, 15);
                            BlockState blockstate = levelchunksection.getBlockState(blockpos1.getX() - x, blockpos1.getY() - l, blockpos1.getZ() - z);
                            tickBlocks(profilerfiller, blockpos1, blockstate);
                        }
                    }
                }
            }
        }
        ci.cancel();
    }

    private void tickBlocks(ProfilerFiller profilerfiller, BlockPos pos, BlockState blockstate) {
        profilerfiller.push("randomTick");
        if (blockstate.isRandomlyTicking() && Chronomancy.doesBlockTick(getLevel(), pos, this.random)) {
            blockstate.randomTick(this.getLevel(), pos, this.random);
        }

        FluidState fluidstate = blockstate.getFluidState();
        if (fluidstate.isRandomlyTicking() && Chronomancy.doesBlockTick(getLevel(), pos, this.random)) {
            fluidstate.randomTick(this.getLevel(), pos, this.random);
        }
        profilerfiller.pop();
    }
}
