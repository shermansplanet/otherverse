package com.shermansplanet.otherverse.familiar;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.spirits.SpiritColorAnalyzer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

public class MobRetexturer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static HashMap<ResourceLocation, Palette> paletteCache = new HashMap<>();
    private static HashMap<String, Palette> spiritPaletteCache = new HashMap<>();
    private static HashMap<String, Palette> spiritPaletteCacheSplit = new HashMap<>();

    public static Pair<ResourceLocation, AbstractTexture> makeSpiritVariant(List<ResourceLocation> textureSet, String spiritType) {
        var SIZE = 16;
        NativeImage tex = new NativeImage(SIZE, SIZE * textureSet.size(), true);

        var shouldAdjustBrightness = !Objects.equals(spiritType, "end");

        for (int imageIndex = 0; imageIndex < textureSet.size(); imageIndex++) {
            var oldTexture = textureSet.get(imageIndex);
            var image = getNativeImage(new ResourceLocation(oldTexture.getNamespace(), oldTexture.getPath()));
            if (image == null) {
                LOGGER.error("COULDN'T LOAD TEXTURE " + oldTexture.getPath());
                return null;
            }

            var spiritPalettes = spiritPaletteCacheSplit.computeIfAbsent(spiritType, MobRetexturer::paletteFromSpirit);
            var itemPalettes = new Palette(Collections.singleton(image), true);

            for (var paletteIndex = 0; paletteIndex < itemPalettes.palettes.size(); paletteIndex++) {
                var itemPalette = itemPalettes.palettes.get(paletteIndex);
                var pixelCount = itemPalette.size();
                var correspondingPalette = spiritPalettes.palettes.get(paletteIndex % spiritPalettes.palettes.size());

            /*var overallCoeff = (float) Math.sqrt(getAverageBrightnessSqr(correspondingPalette)
                    / getAverageBrightnessSqr(itemPalette));*/

                for (var i = 0; i < pixelCount; i++) {
                    var itemPixel = itemPalette.get(i);
                    var lerp = i / (float) pixelCount;
                    var pixelIndex = Mth.floor(lerp * correspondingPalette.size());
                    var spiritPixel = correspondingPalette.get(pixelIndex);
                    var mixel = shouldAdjustBrightness ? getPixelBlend(itemPixel, spiritPixel) : spiritPixel;
                    var pixelInt = (mixel.r) | ((mixel.g << 8) & 0xff00) | ((mixel.b << 16) & 0xff0000) | 0xff000000;
                    tex.setPixelRGBA(itemPixel.x, itemPixel.y + imageIndex * SIZE, pixelInt);
                }
            }
        }
        DynamicTexture newTex = new DynamicTexture(tex);
        var textureManager = Minecraft.getInstance().getTextureManager();
        var nameTex = textureSet.get(0);
        var newTexLoc = new ResourceLocation(Otherverse.MODID, "hallow_" + nameTex.getNamespace() + "_" + nameTex.getPath() + "_" + spiritType);
        textureManager.register(newTexLoc, newTex);
        return Pair.of(newTexLoc, newTex);
    }

    private static Palette.Pixel getPixelBlend(Palette.Pixel itemPixel, Palette.Pixel spiritPixel) {
        var coeff = ((float) Math.sqrt(itemPixel.getPerceptualBrightnessSqr() / spiritPixel.getPerceptualBrightnessSqr()) + 2) / 3;
        var mixel = new Palette.Pixel(
                Mth.clamp(Math.round(spiritPixel.r * coeff), 0, 255),
                Mth.clamp(Math.round(spiritPixel.g * coeff), 0, 255),
                Mth.clamp(Math.round(spiritPixel.b * coeff), 0, 255),
                itemPixel.x, itemPixel.y
        );
        return mixel;
    }

    private static double getAverageBrightnessSqr(ArrayList<Palette.Pixel> itemPalette) {
        double avg = 0;
        for (var pixel : itemPalette) {
            avg += pixel.getPerceptualBrightnessSqr();
        }
        return avg / itemPalette.size();
    }

    private static Palette paletteFromSpirit(String s) {
        var image = getNativeImage(new ResourceLocation(Otherverse.MODID, "textures/item/spirit_" + s + ".png"));
        return new Palette(Collections.singleton(image), true);
    }
    private static Palette paletteFromSpiritSplit(String s) {
        var image = getNativeImage(new ResourceLocation(Otherverse.MODID, "textures/item/spirit_" + s + ".png"));
        return new Palette(Collections.singleton(image), false);
    }

    public static ResourceLocation retextureMob(ResourceLocation originalTextureLoc, String spiritType) {
        LOGGER.debug("MAKING SPLIT PALETTE : " + spiritType);
        var spiritPalettes = spiritPaletteCache.computeIfAbsent(spiritType, MobRetexturer::paletteFromSpiritSplit);
        var originalTexture = getNativeImage(originalTextureLoc);
        var originalPalettes = new Palette(Collections.singleton(originalTexture));
        var rawTex = MakeTexture(originalTexture, originalPalettes, spiritPalettes);
        var newTexLoc = new ResourceLocation(Otherverse.MODID,
                "skin_" + originalTextureLoc.getNamespace() + "_" + originalTextureLoc.getPath() + "_" + spiritType);
        DynamicTexture newTex = new DynamicTexture(rawTex);
        var textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.register(newTexLoc, newTex);
        return newTexLoc;
    }

    public static boolean retexture(Collection<ResourceLocation> textureLocations, AbstractClientPlayer player) {
        // GET MOB TEXTURE
        var textures = new ArrayList<NativeImage>();
        for (var texLoc : textureLocations) {
            textures.add(getNativeImage(texLoc));
        }
        if (textures.isEmpty()) {
            LOGGER.error("NO MOB TEXTURES FOUND");
            return false;
        }
        var palette = new Palette(textures);
        ((ITextureSetter) player).setTexture(null);
        var rawTex = MakePlayerTexture(player, palette);
        if (rawTex == null) {
            LOGGER.error("TEXTURE GENERATION FAILED");
            return false;
        }
        DynamicTexture newTex = new DynamicTexture(rawTex);
        var textureManager = Minecraft.getInstance().getTextureManager();
        var newTexLoc = new ResourceLocation(Otherverse.MODID, player.getGameProfile().getName().toLowerCase() + "_familiar_texture");
        textureManager.register(newTexLoc, newTex);
        ((ITextureSetter) player).setTexture(newTexLoc);
        paletteCache.put(newTexLoc, palette);
        return true;
    }

    private static NativeImage getNativeImage(ResourceLocation texLoc) {
        var resource = SpiritColorAnalyzer.getStreamFor(texLoc, SpiritColorAnalyzer.getPacks());
        if (resource == null) {
            return null;
        }
        NativeImage texture;
        try {
            texture = NativeImage.read(resource);
        } catch (IOException e) {
            LOGGER.error("CANNOT READ");
            return null;
        }
        return texture;
    }

    private static NativeImage MakePlayerTexture(AbstractClientPlayer player, Palette mobPalette) {
        var playerTexture = getPlayerTexture(player);
        if (playerTexture == null) return null;
        var playerPalettes = new Palette(Collections.singleton(playerTexture));
        paletteCache.put(player.getSkinTextureLocation(), playerPalettes);
        return MakeTexture(playerTexture, playerPalettes, mobPalette);
    }

    private static NativeImage MakeTexture(NativeImage originalTex, Palette originalPalettes, Palette targetPalette) {
        NativeImage tex = new NativeImage(originalTex.getWidth(), originalTex.getHeight(), true);
        var r = new Random();
        var debug = false;
        if (debug) {
            for (var x = 0; x < originalTex.getWidth(); x++) {
                for (var y = 0; y < originalTex.getHeight(); y++) {
                    var randomColor = (r.nextInt(256)) | ((r.nextInt(256) << 8) & 0xff00) | ((r.nextInt(256) << 16) & 0xff0000) | 0xff000000;
                    tex.setPixelRGBA(x, y, randomColor);
                }
            }
        } else {
            for (var paletteIndex = 0; paletteIndex < originalPalettes.palettes.size(); paletteIndex++) {
                var originalPalette = originalPalettes.palettes.get(paletteIndex);
                var pixelCount = originalPalette.size();
                var eligiblePalettes = targetPalette.palettes.stream()
                        .filter(x -> (x.size() > 8) == (originalPalette.size() > 8)).toList();
                if (eligiblePalettes.isEmpty()) eligiblePalettes = targetPalette.palettes;
                var correspondingPalette = eligiblePalettes.get(paletteIndex % eligiblePalettes.size());
                for (var i = 0; i < pixelCount; i++) {
                    var playerPixel = originalPalette.get(i);
                    var pixelIndex = Mth.floor(i * correspondingPalette.size() / (float) pixelCount);
                    var mobPixel = correspondingPalette.get(pixelIndex);
                    var pixelInt = (mobPixel.r) | ((mobPixel.g << 8) & 0xff00) | ((mobPixel.b << 16) & 0xff0000) | 0xff000000;
                    tex.setPixelRGBA(playerPixel.x, playerPixel.y, pixelInt);
                }
            }
        }
        return tex;
    }

    private static NativeImage getPlayerTexture(AbstractClientPlayer player) {
        var skinLoc = player.getSkinTextureLocation();
        var ni = getNativeImage(skinLoc);
        if (ni != null) return ni;
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstracttexture = texturemanager.getTexture(skinLoc, MissingTextureAtlasSprite.getTexture());
        if (abstracttexture == MissingTextureAtlasSprite.getTexture()) {
            LOGGER.error("MISSING TEXTURE");
        }
        if (abstracttexture instanceof HttpTexture tex) {
            return ((ITextureGetter) tex).getTexture();
        }
        LOGGER.error("NOT HTTP TEXTURE");
        return null;
    }

    public static Palette getPlayerPalette(AbstractClientPlayer player) {
        if (paletteCache.containsKey(player.getSkinTextureLocation())) {
            return paletteCache.get(player.getSkinTextureLocation());
        }
        var playerTexture = getPlayerTexture(player);
        if (playerTexture == null) return null;
        var palette = new Palette(Collections.singleton(playerTexture));
        paletteCache.put(player.getSkinTextureLocation(), palette);
        return palette;
    }

    public static Palette.Pixel getPrimaryColor(Palette palette) {
        var p = palette.palettes.get(0);
        return p.get(p.size() - 1);
    }

    public static class Palette {

        public ArrayList<ArrayList<Pixel>> palettes = new ArrayList<>();

        public void printInfo(String name) {
            LOGGER.debug(name);
            LOGGER.debug(palettes.size() + " PALETTES");
            for(var palette : palettes) {
                LOGGER.debug(palette.size() + " PIXELS");
            }
        }

        public record Pixel(int r, int g, int b, int x, int y) {
            public double getPerceptualBrightnessSqr() {
                return 0.299 * r * r + 0.587 * g * g + 0.114 * b * b;
            }

            public String makStr() {
                return "(" + r + ", " + g + ", " + b + ")";
            }
        }

        private static final float CONNECTED_CUTOFF = 42;
        private static final float CONNECTED_CUTOFF_MEDIUM = 60;
        private static final float CONNECTED_CUTOFF_SMALL = 15;
        private static final float UNCONNECTED_CUTOFF = 30;

        private static final int[][] directions = new int[][]{
                new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0}, new int[]{0, 1},
                new int[]{-1, -1}, new int[]{1, -1}, new int[]{1, 1}, new int[]{-1, 1}
        };

        private float colorDistanceSqr(Pixel pixel, Pixel otherPixel) {
            return Mth.square(pixel.r - otherPixel.r)
                    + Mth.square(pixel.g - otherPixel.g)
                    + Mth.square(pixel.b - otherPixel.b);
        }

        public Palette(Collection<NativeImage> images) {
            this(images, false);
        }

        public Palette(Collection<NativeImage> images, boolean isForItem) {
            var blobs = new ArrayList<ArrayList<Pixel>>();
            for (var image : images) {
                var WIDTH = isForItem ? Math.min(image.getWidth(), image.getHeight()) : image.getWidth();
                var HEIGHT = isForItem ? Math.min(image.getWidth(), image.getHeight()) : image.getHeight();
                var cutoff = isForItem || HEIGHT > 32 ? CONNECTED_CUTOFF : HEIGHT > 16 ? CONNECTED_CUTOFF_MEDIUM : CONNECTED_CUTOFF_SMALL;
                cutoff *= cutoff;
                var searchedPixels = new HashSet<Pixel>();
                var pixels = new Pixel[WIDTH][HEIGHT];
                for (var x = 0; x < WIDTH; x++) {
                    pixels[x] = new Pixel[HEIGHT];
                    for (var y = 0; y < HEIGHT; y++) {
                        var color = image.getPixelRGBA(x, y);
                        if (((color >> 24) & 0xff) < 0x88) {
                            pixels[x][y] = null;
                            continue;
                        }
                        var pixel = new Pixel(color & 0xff, (color >> 8) & 0xff, (color >> 16) & 0xff, x, y);
                        pixels[x][y] = pixel;
                    }
                }
                for (var x = 0; x < WIDTH; x++) {
                    for (var y = 0; y < HEIGHT; y++) {
                        var startPixel = pixels[x][y];
                        if (startPixel == null || searchedPixels.contains(startPixel)) continue;
                        var blob = new ArrayList<Pixel>();
                        var toSearch = new ArrayDeque<Pixel>();
                        toSearch.add(startPixel);
                        searchedPixels.add(startPixel);
                        while (!toSearch.isEmpty()) {
                            var pixel = toSearch.pop();
                            blob.add(pixel);
                            for (var dir : directions) {
                                var newX = pixel.x + dir[0];
                                if (newX < 0 || newX >= WIDTH) continue;
                                var newY = pixel.y + dir[1];
                                if (newY < 0 || newY >= HEIGHT) continue;
                                var otherPixel = pixels[newX][newY];
                                if (otherPixel == null || searchedPixels.contains(otherPixel)) continue;
                                if (!isForItem && colorDistanceSqr(pixel, otherPixel) > cutoff)
                                    continue;
                                toSearch.add(otherPixel);
                                searchedPixels.add(otherPixel);
                            }
                        }
                        blobs.add(blob);
                    }
                }
            }
            var averageColors = new Pixel[blobs.size()];
            var blobLinks = new ArrayList<Vec3i>();
            for (var i1 = 0; i1 < blobs.size(); i1++) {
                var blob1 = blobs.get(i1);
                var r = 0;
                var g = 0;
                var b = 0;
                for (var pixel : blob1) {
                    r += pixel.r;
                    g += pixel.g;
                    b += pixel.b;
                }
                averageColors[i1] = new Pixel(r / blob1.size(), g / blob1.size(), b / blob1.size(), 0, 0);
                for (var i2 = 0; i2 < i1; i2++) {
                    var distsqr = colorDistanceSqr(averageColors[i1], averageColors[i2]);
                    if (distsqr > UNCONNECTED_CUTOFF * UNCONNECTED_CUTOFF) continue;
                    blobLinks.add(new Vec3i(i1, i2, 0));
                    blobLinks.add(new Vec3i(i2, i1, 0));
                }
            }
            var searchedBlobs = new HashSet<Integer>();
            for (var i = 0; i < blobs.size(); i++) {
                if (searchedBlobs.contains(i)) continue;
                var palette = new ArrayList<Pixel>();
                var blobsToSearch = new ArrayDeque<Integer>();
                blobsToSearch.add(i);
                searchedBlobs.add(i);
                while (!blobsToSearch.isEmpty()) {
                    int blobIndex = blobsToSearch.pop();
                    palette.addAll(blobs.get(blobIndex));
                    for (var link : blobLinks) {
                        if (link.getX() != blobIndex) continue;
                        var otherIndex = link.getY();
                        if (searchedBlobs.contains(otherIndex)) continue;
                        blobsToSearch.add(otherIndex);
                        searchedBlobs.add(otherIndex);
                    }
                }
                palettes.add(palette);
            }
            sortPalettes();
        }

        private void sortPalettes() {
            palettes.removeIf(ArrayList::isEmpty);
            for (var palette : palettes) {
                palette.sort(Comparator.comparingInt(p -> -p.y));
                palette.sort(Comparator.comparingDouble(Pixel::getPerceptualBrightnessSqr));
                LOGGER.debug("COUNT: " + palette.size());
                LOGGER.debug("FROM " + palette.get(0).makStr() + " TO " + palette.get(palette.size()-1).makStr());
            }
            palettes.sort((a, b) -> Integer.compare(b.size(), a.size()));
        }
    }
}
