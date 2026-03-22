package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.ISectionSetter;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.spirits.Chronomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
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


    protected ServerLevelInjector(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    public void tickNonPassenger(Entity e, CallbackInfo ci) {
        if (!Chronomancy.doesEntityTick(e)) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    public void onBSC(BlockPos p_8751_, BlockState p_8752_, BlockState p_8753_, CallbackInfo ci) {
        DiagramManager.blockChanged(p_8751_, (ServerLevel) (Object) this);
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
                int sectionIndex = chunk.getSectionIndex(pos.getY());
                var levelchunksection = sections[sectionIndex];
                int j1 = chunk.getSectionYFromSectionIndex(sectionIndex);
                int k1 = SectionPos.sectionToBlockCoord(j1);
                BlockState blockstate = levelchunksection.getBlockState(pos.getX() - x, pos.getY() - k1, pos.getZ() - z);
                tickBlocks(profilerfiller, pos, blockstate);
            }
        } else {
            if (tickCount > 0) {
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection levelchunksection = sections[i];
                    if (levelchunksection.isRandomlyTicking()) {
                        int j1 = chunk.getSectionYFromSectionIndex(i);
                        int l = SectionPos.sectionToBlockCoord(j1);
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
