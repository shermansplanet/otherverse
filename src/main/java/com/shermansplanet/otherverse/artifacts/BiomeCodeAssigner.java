package com.shermansplanet.otherverse.artifacts;

import com.mojang.datafixers.util.Pair;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.integrations.jei.BiomeCodeRecipe;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class BiomeCodeAssigner {
    public static HashMap<ResourceLocation, List<SpiritType>> biomeCodes;
    public static final int SPIRIT_CODE_COUNT = 4;

    public static void AssignBiomeCodes(ServerLevel sl) {
        biomeCodes = new HashMap<>();
        var registry = sl.registryAccess().registryOrThrow(Registries.BIOME);
        var rawMapping = new ArrayList<Pair<ResourceLocation, List<SpiritType>>>();
        var biomes = registry.asHolderIdMap();
        for (var biome : biomes) {
            var spirits = MobBindingInfluenceUtils.getSpiritTypes(biome, sl);
            spirits.addAll(ArtifactManager.getColorsFor(biome, sl));
            var paredSpirits = new ArrayList<SpiritType>();
            for (var spirit : spirits) {
                if (paredSpirits.contains(spirit)) continue;
                paredSpirits.add(spirit);
            }
            rawMapping.add(new Pair<>(registry.getKey(biome.get()), paredSpirits));
        }
        rawMapping.sort(Comparator.comparing(Pair::getFirst));
        rawMapping.sort(Comparator.comparingInt((Pair<ResourceLocation, List<SpiritType>> a) -> a.getSecond().size()));
        var takenSpiritCodes = new HashSet<String>();
        for (var pair : rawMapping) {
            var biomeName = pair.getFirst();
            var spiritPreferences = pair.getSecond();
            var spirits = tryGetUniqueCombo(spiritPreferences, takenSpiritCodes);
            if (spirits == null) {
                for (var st : Spirits.colorSpiritTypes) {
                    if (spiritPreferences.contains(st)) continue;
                    spiritPreferences.add(st);
                }
                spirits = tryGetUniqueCombo(spiritPreferences, takenSpiritCodes);
            }
            biomeCodes.put(biomeName, spirits);
        }
    }

    private static List<SpiritType> tryGetUniqueCombo(List<SpiritType> spiritPreferences, HashSet<String> takenSpiritCodes) {
        var spirits = new SpiritType[SPIRIT_CODE_COUNT];
        var spiritIndices = new int[SPIRIT_CODE_COUNT];
        while (true) {
            SpiritType lastSpirit = null;
            var invalid = false;
            for (var i = 0; i < SPIRIT_CODE_COUNT; i++) {
                spirits[i] = spiritPreferences.get((spiritIndices[i] + i) % spiritPreferences.size());
                if (spirits[i] == lastSpirit) {
                    invalid = true;
                    break;
                }
                lastSpirit = spirits[i];
            }
            if (!invalid) {
                var key = getKey(spirits);
                if (!takenSpiritCodes.contains(key)) {
                    takenSpiritCodes.add(key);
                    return List.of(spirits);
                }
            }
            for (var i = SPIRIT_CODE_COUNT - 1; i >= 0; i--) {
                spiritIndices[i] += 1;
                if (spiritIndices[i] == spiritPreferences.size()) {
                    spiritIndices[i] = 0;
                    if (i == 0) return null;
                } else {
                    break;
                }
            }
        }
    }

    private static String getKey(SpiritType[] spirits) {
        StringBuilder builder = new StringBuilder();
        for (var spirit : spirits) {
            builder.append(spirit.label());
        }
        return builder.toString();
    }

    public static List<BiomeCodeRecipe> GenerateRecipes() {
        var recipes = new ArrayList<BiomeCodeRecipe>();
        for (var biome : biomeCodes.entrySet()) {
            var recipe = new BiomeCodeRecipe(ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, biome.getKey().toString().replace(':', '_')), biome.getValue(), biome.getKey());
            recipes.add(recipe);
        }
        return recipes;
    }

    public static ResourceLocation getBiomeFor(List<SpiritType> spiritCode) {
        for (var biomeCode : biomeCodes.entrySet()) {
            if (biomeCode.getValue().equals(spiritCode)) return biomeCode.getKey();
        }
        return null;
    }
}
