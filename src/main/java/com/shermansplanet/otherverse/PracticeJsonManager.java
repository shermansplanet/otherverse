package com.shermansplanet.otherverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.SpiritTransfusions;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;

public class PracticeJsonManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    public PracticeJsonManager() {
        super(GSON, "practice");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {

        MobTransfusions.setupReplacements();
        SpiritTransfusions.onStartLoadingJson();
        SpiritLabeler.onStartLoadingJson();
        MobTransfusions.onStartLoadingJson();
        MobBindingInfluenceUtils.onStartLoadingJson();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonObject practice = entry.getValue().getAsJsonObject();
            if (practice.has("practice")) {
                switch (practice.get("practice").getAsString()) {
                    case "spirit_transfusion":
                        SpiritTransfusions.loadJsonTransfusion(practice);
                        break;
                    case "spirit_composition":
                        SpiritLabeler.loadJsonSpirits(practice);
                        break;
                    case "enchantment":
                        if (practice.has("mob_transfusion")) {
                            loadMobEnchants(practice.getAsJsonObject("mob_transfusion"));
                        }
                        if (practice.has("spirit_transfusion")) {
                            loadSpiritEnchants(practice.getAsJsonObject("spirit_transfusion"));
                        }
                        break;
                }
            } else if (practice.has("other")) {
                EntityType<? extends LivingEntity> other = (EntityType<? extends LivingEntity>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(practice.get("other").getAsString()));
                MobTransfusions.processOther(other, practice);
                MobBindingInfluenceUtils.processOther(other, practice);
            }
        }
        SpiritLabeler.onDoneLoadingJson();
        SpiritTransfusions.onDoneLoadingJson();
        MobTransfusions.onDoneLoadingJson();
        MobBindingInfluenceUtils.onFinishLoadingJson();
    }

    private void loadMobEnchants(JsonObject enchantments) {
        for (var enchantment : enchantments.entrySet()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantment.getKey()));
            var enchantmentData = enchantment.getValue().getAsJsonObject();
            var levels = enchantmentData.get("health").getAsJsonArray();
            var required = new HashSet<EntityType<?>>();
            if (enchantmentData.has("required")) {
                for (var mobId : enchantmentData.get("required").getAsJsonArray()) {
                    EntityType<?> mob = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mobId.getAsString()));
                    required.add(mob);
                }
            }
            if(enchantmentData.has("can_be_any") && enchantmentData.get("can_be_any").getAsBoolean()){
                for(var r : required){
                    var mobset = new HashSet<EntityType<?>>();
                    mobset.add(r);
                    for (int i = 0; i < levels.size(); i++) {
                        MobTransfusions.register(mobset, Items.BOOK,
                                MobTransfusions.GetEnchantedBook(enchantmentId, i + 1),
                                levels.get(i).getAsInt(), false
                        );
                    }
                }
            }else {
                for (int i = 0; i < levels.size(); i++) {
                    MobTransfusions.register(required, Items.BOOK,
                            MobTransfusions.GetEnchantedBook(enchantmentId, i + 1),
                            levels.get(i).getAsInt(), false
                    );
                }
            }
        }
    }

    private void loadSpiritEnchants(JsonObject enchantments) {
        for (var enchantment : enchantments.entrySet()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantment.getKey()));
            var enchantmentData = enchantment.getValue().getAsJsonObject();
            var levels = enchantmentData.get("price").getAsJsonArray();
            var spiritType = Spirits.spiritsByLabel.get(enchantmentData.get("spirit").getAsString());
            for (int i = levels.size() - 1; i >= 0; i--) {
                SpiritTransfusions.register(Items.BOOK, spiritType, levels.get(i).getAsInt(),
                        MobTransfusions.GetEnchantedBook(enchantmentId, i + 1), null);
            }
        }
    }
}
