package com.shermansplanet.otherverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.util.Map;

public class MobSpawnAnalyzer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    public static void addListeners(AddReloadListenerEvent event) {
        //event.addListener(new WorldGenListener());
    }

    private static class WorldGenListener extends SimpleJsonResourceReloadListener {
        public WorldGenListener() {
            super(GSON, "worldgen/biome");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller
                profilerFiller) {
            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                LOGGER.debug("FOUND BIOME: " + entry.getKey());
                JsonObject practice = entry.getValue().getAsJsonObject();
            }
        }
    }
}
