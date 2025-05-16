package com.shermansplanet.otherverse.binding;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootHelper extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = Deserializers.createLootTableSerializer().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final HashMap<Item, List<EntityType<?>>> entitiesThatDropItem = new HashMap<>();

    public LootHelper() {
        super(GSON, "loot_tables");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller filler) {
        entitiesThatDropItem.clear();
        for (var et : ForgeRegistries.ENTITY_TYPES) {
            var dropCount = 0;
            var lootTable = map.get(et.getDefaultLootTable());
            if (lootTable == null) {
                LOGGER.debug("NO LOOT TABLE FOR " + et);
                continue;
            }
            JsonObject table = lootTable.getAsJsonObject();
            if (!table.has("pools")) {
                LOGGER.debug("NO LOOT POOLS FOR " + et);
                continue;
            }
            for (var pool : table.get("pools").getAsJsonArray()) {
                if (!pool.getAsJsonObject().has("entries")) {
                    continue;
                }
                for (var entry : pool.getAsJsonObject().get("entries").getAsJsonArray()) {
                    if (!entry.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) {
                        continue;
                    }
                    var name = entry.getAsJsonObject().get("name").getAsString();
                    var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
                    addItemToType(item, et);
                    dropCount++;
                }
            }
            if(dropCount == 0) LOGGER.debug("NO DROPS FOR " + et);
        }
        for (var item : ForgeRegistries.ITEMS) {
            if (item.getDefaultInstance().is(ItemTags.WOOL)) addItemToType(item, EntityType.SHEEP);
        }
    }

    private void addItemToType(Item item, EntityType et) {
        if (!entitiesThatDropItem.containsKey(item)) {
            entitiesThatDropItem.put(item, new ArrayList<>());
        }
        entitiesThatDropItem.get(item).add(et);
    }

}
