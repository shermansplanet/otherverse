package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.demesnes.ISectionSetter;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelChunk.class)
public abstract class LevelChunkInjector extends ChunkAccess implements ISectionSetter, net.minecraftforge.common.capabilities.ICapabilityProviderImpl<LevelChunk> {
    private int minY, maxY;
    private boolean isSet = false;

    public LevelChunkInjector(ChunkPos p_187621_, UpgradeData p_187622_, LevelHeightAccessor p_187623_, Registry<Biome> p_187624_, long p_187625_, @Nullable LevelChunkSection[] p_187626_, @Nullable BlendingData p_187627_) {
        super(p_187621_, p_187622_, p_187623_, p_187624_, p_187625_, p_187626_, p_187627_);
    }

    @Override
    public void setSections(int minY, int maxY) {
        System.out.println("SETTING SECTIONS: " + minY + " TO " + maxY);
        this.minY = minY;
        this.maxY = maxY;
        isSet = true;
    }

    @Override
    public void clearSections() {
        isSet = false;
    }

    @Override
    public boolean isSet() {
        return isSet;
    }

    @Override
    public int getMin() {
        return minY;
    }

    @Override
    public int getMax() {
        return maxY;
    }
}
