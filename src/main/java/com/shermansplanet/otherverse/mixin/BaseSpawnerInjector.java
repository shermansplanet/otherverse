package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.artifacts.SpawnDataGetter;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseSpawner.class)
public class BaseSpawnerInjector implements SpawnDataGetter {
    @Shadow
    private SpawnData nextSpawnData = new SpawnData();

    @Override
    public SpawnData getNextSpawnData() {
        return nextSpawnData;
    }
}
