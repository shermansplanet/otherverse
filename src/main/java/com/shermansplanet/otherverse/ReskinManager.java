package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.familiar.ITextureSetter;
import com.shermansplanet.otherverse.familiar.MobRetexturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ReskinManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final HashMap<UUID, List<ResourceLocation>> cachedSkinUpdates = new HashMap<>();
    private static final HashMap<UUID, List<ResourceLocation>> sightSkins = new HashMap<>();
    private static final HashMap<UUID, List<ResourceLocation>> alwaysSkins = new HashMap<>();

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        var lvl = Minecraft.getInstance().level;
        if (lvl == null) return;
        for (var player : lvl.players()) {
            if (!((ITextureSetter) player).isSkinLoadedForReskin()) continue;
            var skins = cachedSkinUpdates.get(player.getUUID());
            if (skins == null) continue;
            if (skins.isEmpty()) {
                //LOGGER.error("CLEANING SKIN");
                ((ITextureSetter) player).setTexture(null);
            } else {
                var retextureSuccess = MobRetexturer.retexture(skins, player);
                if (!retextureSuccess) {
                    LOGGER.error("RETEXTURE FAILURE");
                    continue;
                }
            }
            cachedSkinUpdates.remove(player.getUUID());
            SightOverlay.instance.recalculateColor();
        }
    }

    public static void reskinAsFamiliar(LivingEntity le, LocalPlayer player) {
        //LOGGER.error("RESKINNING FROM FAMILIAR");
        var mobRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(le);
        var texLoc = mobRenderer.getTextureLocation(le);
        sightSkins.put(player.getUUID(), List.of(texLoc));
        if (SightManager.shouldRenderSight()) {
            cachedSkinUpdates.put(player.getUUID(), List.of(texLoc));
        }
    }

    public static void reskin(List<ResourceLocation> resourceLocations, LocalPlayer player) {
        //LOGGER.error("RESKINNING");
        alwaysSkins.put(player.getUUID(), resourceLocations);
        cachedSkinUpdates.put(player.getUUID(), resourceLocations);
    }

    public static void onSightUpdate() {
        //LOGGER.info("SETTING SKINS " + (SightManager.shouldRenderSight() ? "ON" : "OFF"));
        for (var id : sightSkins.keySet()) {
            cachedSkinUpdates.remove(id);
            cachedSkinUpdates.put(id, SightManager.shouldRenderSight() ? sightSkins.get(id) : alwaysSkins.getOrDefault(id, new ArrayList<>()));
        }
    }

    private static final HashMap<ResourceLocation, HashMap<String, ResourceLocation>> mobSkinCache = new HashMap<>();
    private static final HashMap<LivingEntity, ResourceLocation> reskinnedMobs = new HashMap<>();

    public static void reskinMob(LivingEntity le, String spiritType) {
        var originalTexture = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(le).getTextureLocation(le);
        //ResourceLocation actualPath = new ResourceLocation(originalTexture.getNamespace(), "textures/" + originalTexture.getPath() + ".png");
        var newTexture = mobSkinCache.computeIfAbsent(originalTexture, x -> new HashMap<>())
                .computeIfAbsent(spiritType, x -> MobRetexturer.retextureMob(originalTexture, spiritType));
        reskinnedMobs.put(le, newTexture);
    }

    public static boolean shouldReskin(LivingEntity le) {
        return reskinnedMobs.containsKey(le);
    }

    public static ResourceLocation getSkinLocation(LivingEntity le) {

        return reskinnedMobs.get(le);
    }
}
