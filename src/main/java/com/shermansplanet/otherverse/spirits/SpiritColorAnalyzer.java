package com.shermansplanet.otherverse.spirits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class SpiritColorAnalyzer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final HashMap<Integer, Function<Float, Float>> colorCutoffs = new HashMap<>();
    private static final String[] colorNames = new String[]{
            "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"
    };

    private static final int[] colorCutoffKeys = new int[]{
            6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 5
    };

    private static JsonElement getJsonFor(ResourceLocation resourceLocation, ArrayList<PackResources> packs) {
        var resource = getStreamFor(resourceLocation, packs);
        if (resource == null) {
            return null;
        }
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(resource, "UTF-8"));
            return GSON.fromJson(streamReader, JsonElement.class);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getStreamFor(ResourceLocation resourceLocation, ArrayList<PackResources> packs) {
        for (PackResources pack : packs) {
            try {
                var resource = pack.getResource(PackType.CLIENT_RESOURCES, resourceLocation);
                if(resource == null) continue;
                return resource.get();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static ResourceLocation getLocationFor(String s, String directory, String extension) {
        int i = s.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(i == -1 ? "minecraft" : s.substring(0, i), directory + s.substring(i + 1) + extension);
    }

    public static void AnalyzeAllColors() {
        colorCutoffs.clear();
        colorCutoffs.put(6, v -> 14 / 360f);
        colorCutoffs.put(7, v -> 42 / 360f);
        colorCutoffs.put(8, v -> 68 / 360f);
        colorCutoffs.put(9, v -> 110 / 360f);
        colorCutoffs.put(10, v -> 150 / 360f);
        colorCutoffs.put(11, v -> 186 / 360f);
        colorCutoffs.put(12, v -> 205 / 360f);
        colorCutoffs.put(13, v -> 253 / 360f);
        colorCutoffs.put(14, v -> 284 / 360f);
        colorCutoffs.put(15, v -> (326 + (1 - v) * 14) / 360f);
        colorCutoffs.put(5, v -> (354 - (1 - v) * 14) / 360f);

        HashMap<Item, SpiritLabeler.SpiritAmount[]> allColorSpirits = new HashMap<>();
        for (var item : ForgeRegistries.ITEMS) {
            var amounts = getColorCounts(item);
            if (amounts == null) {
                continue;
            }
            allColorSpirits.put(item, amounts);
        }
        OtherversePacketHandler.INSTANCE.sendToServer(new ColorSpiritMessage(allColorSpirits));
    }

    private static ArrayList<PackResources> packCache;

    public static ArrayList<PackResources> getPacks() {
        if (packCache != null) return packCache;
        Minecraft mc = Minecraft.getInstance();
        ArrayList<PackResources> packs = new ArrayList<>();
        packs.add(mc.getVanillaPackResources());
        for (Pack pack : mc.getResourcePackRepository().getSelectedPacks()) {
            PackResources pr = pack.open();
            if (!packs.contains(pr)) {
                packs.add(pr);
            }
        }
        packCache = packs;
        return packs;
    }

    private static void getResourcesRecursive(JsonElement el, Set<ResourceLocation> resources, ArrayList<PackResources> packs) {
        JsonObject obj = el.getAsJsonObject();
        if (obj.has("parent")) {
            JsonElement subElement = getJsonFor(getLocationFor(obj.get("parent").getAsString(), "models/", ".json"), packs);
            if (subElement != null) {
                getResourcesRecursive(subElement, resources, packs);
            }
        }
        if (obj.has("textures")) {
            for (var texture : obj.get("textures").getAsJsonObject().entrySet()) {
                String s = texture.getValue().getAsString();
                if (s.startsWith("#")) {
                    continue;
                }
                var location = getLocationFor(s, "textures/", ".png");
                resources.add(location);
            }
        }
    }

    private static void getTexturesRecursive(Item item, JsonElement el, ArrayList<NativeImage> textures, ArrayList<PackResources> packs) {
        JsonObject obj = el.getAsJsonObject();
        if (obj.has("parent")) {
            JsonElement subElement = getJsonFor(getLocationFor(obj.get("parent").getAsString(), "models/", ".json"), packs);
            if (subElement != null) {
                getTexturesRecursive(item, subElement, textures, packs);
            }
        }
        if (obj.has("textures")) {
            for (var texture : obj.get("textures").getAsJsonObject().entrySet()) {
                String s = texture.getValue().getAsString();
                if (s.startsWith("#")) {
                    continue;
                }
                var location = getLocationFor(s, "textures/", ".png");
                var resource = getStreamFor(location, packs);
                if (resource == null) {
                    continue;
                }
                try {
                    textures.add(NativeImage.read(resource));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static SpiritLabeler.SpiritAmount[] getColorCounts(Item item) {

        if (item == Items.LAVA_BUCKET) {
            return new SpiritLabeler.SpiritAmount[]{
                    new SpiritLabeler.SpiritAmount(Spirits.COLOR_ORANGE, 12),
                    new SpiritLabeler.SpiritAmount(Spirits.COLOR_YELLOW, 4)
            };
        } else if (item == Items.GRASS_BLOCK) {
            return new SpiritLabeler.SpiritAmount[]{
                    new SpiritLabeler.SpiritAmount(Spirits.COLOR_BROWN, 10),
                    new SpiritLabeler.SpiritAmount(Spirits.COLOR_GREEN, 6)
            };
        } else if (item instanceof DyeableLeatherItem && item != OtherverseItems.CHALK.get()) {
            return new SpiritLabeler.SpiritAmount[]{
                    new SpiritLabeler.SpiritAmount(Spirits.COLOR_BROWN, 16)
            };
        }

        for (int i = 0; i < colorNames.length; i++) {
            if (item.toString().startsWith(colorNames[i] + "_")) {
                return new SpiritLabeler.SpiritAmount[]{new SpiritLabeler.SpiritAmount(Spirits.colorSpiritTypes[i], 16)};
            }
        }
        var textures = getTexturesRecursive(item);
        if (textures == null) return new SpiritLabeler.SpiritAmount[0];
        return getColorCountsFromTextures(textures);
    }

    public static ArrayList<ResourceLocation> getResourcesRecursive(ResourceLocation resourceLocation) {
        var packs = getPacks();
        JsonElement el = getJsonFor(resourceLocation, packs);
        if (el == null) {
            return null;
        }
        HashSet<ResourceLocation> resources = new HashSet<>();
        getResourcesRecursive(el, resources, packs);
        return new ArrayList<>(resources);
    }

    public static ArrayList<NativeImage> getTexturesRecursive(Item item) {
        var packs = getPacks();
        var resourceLocation = ForgeRegistries.ITEMS.getKey(item);
        resourceLocation = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), "models/item/" + resourceLocation.getPath() + ".json");
        JsonElement el = getJsonFor(resourceLocation, packs);
        if (el == null) {
            return null;
        }
        ArrayList<NativeImage> textures = new ArrayList<>();
        getTexturesRecursive(item, el, textures, packs);
        return textures;
    }

    private static SpiritLabeler.SpiritAmount[] getColorCountsFromTextures(ArrayList<NativeImage> textures) {
        HashMap<Integer, Integer> colorCounts = new HashMap<>();
        int r, g, b, a;
        float h, s, v;
        for (var tex : textures) {

            int closestColor;

            for (var x = 0; x < tex.getWidth(); x++) {
                for (var y = 0; y < tex.getHeight(); y++) {
                    var pixel = tex.getPixelRGBA(x, y);
                    r = (pixel & 255);
                    g = ((pixel >> 8) & 255);
                    b = ((pixel >> 16) & 255);
                    a = ((pixel >> 24) & 255);
                    if (a < 128) {
                        continue;
                    }
                    var hsb = new float[3];
                    Color.RGBtoHSB(r, g, b, hsb);
                    h = hsb[0];
                    s = hsb[1];
                    v = hsb[2];
                    var amount = 1;
                    if (v * s < 0.1f) {
                        closestColor = Math.min(3, (int) ((1 - v) * 4));
                    } else if (v * s < 0.5f && 0.02f + v * 0.03f < h && h < 0.12) {
                        closestColor = 4;
                    } else {
                        closestColor = 5;
                        for (int cutoff : colorCutoffKeys) {
                            if (colorCutoffs.get(cutoff).apply(v) > h) break;
                            closestColor = cutoff;
                        }
                        amount = 2;
                    }

                    if (closestColor == 3) {
                        if (x == tex.getWidth() - 1 || x == 0 || y == tex.getHeight() - 1 || y == 0
                                || ((tex.getPixelRGBA(x + 1, y) >> 24) & 255) < 0.5
                                || ((tex.getPixelRGBA(x - 1, y) >> 24) & 255) < 0.5
                                || ((tex.getPixelRGBA(x, y + 1) >> 24) & 255) < 0.5
                                || ((tex.getPixelRGBA(x, y - 1) >> 24) & 255) < 0.5) {
                            continue;
                        }
                    }

                    if (!colorCounts.containsKey(closestColor)) colorCounts.put(closestColor, 0);
                    colorCounts.put(closestColor, colorCounts.get(closestColor) + amount);
                }
            }
        }

        if (colorCounts.isEmpty()) {
            return null;
        }

        int sum = 0;
        for (var cc : colorCounts.values()) {
            sum += cc;
        }

        var endColorCount = 16;
        var progress = 0f;

        var colorKeys = colorCounts.keySet().stream().sorted((s1, s2) -> Integer.compare(colorCounts.get(s2), colorCounts.get(s1))).toList();
        ArrayList<SpiritLabeler.SpiritAmount> normalized = new ArrayList<>();
        for (var colorKey : colorKeys) {
            var colorCount = colorCounts.get(colorKey);
            var lastProgress = progress;
            progress += colorCount * endColorCount * 1f / sum;
            var amount = Math.round(progress) - Math.round(lastProgress);
            if (amount == 0) continue;
            normalized.add(new SpiritLabeler.SpiritAmount(Spirits.colorSpiritTypes[colorKey], amount));
        }
        return normalized.toArray(new SpiritLabeler.SpiritAmount[0]);
    }
}
