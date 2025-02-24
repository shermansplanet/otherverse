package com.shermansplanet.otherverse.demesnes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DemesnesRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final List<ClientDemesnesData> ongoingRituals = new ArrayList<>();
    public static final List<ClientDemesnesData> claimedDemesnes = new ArrayList<>();
    public static final HashMap<Integer, HashMap<BlockPos, Integer>> chronoBeams = new HashMap<>();
    public static ClientDemesnesData myDemesne;
    public static int demesneMineAmount = 1;
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");
    public static final ResourceLocation BEAM_LOCATION_COLORLESS = new ResourceLocation(Otherverse.MODID, "textures/portal/beam.png");
    private static final float[] beamColor = new float[]{0.7f, 1f, 0f};
    private static final float[] chronoBeamColor = new float[]{1f, 0.8f, 0f};

    private static int currentCheckIndex = 0;
    public static ClientDemesnesData currentDemesne;

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event) {
        ongoingRituals.clear();
        claimedDemesnes.clear();
        chronoBeams.clear();
        myDemesne = null;
        demesneMineAmount = 1;
        currentCheckIndex = 0;
        currentDemesne = null;
    }

    public static class ClientDemesnesData {
        public AABB bounds;
        public Vec3i[] positions;
        public BlockPos minPos;
        public int levelId;
        public float timeRendered = 0;
        public boolean isEmpty = false;
        public boolean hasColor = false;
        public Vec3 color;
        public long dayTime = -1;

        public ClientDemesnesData() {
            isEmpty = true;
        }

        public ClientDemesnesData(BlockPos minPos, AABB bounds, int levelId) {
            this.bounds = bounds;
            this.minPos = minPos;
            var positionList = new ArrayList<Vec3i>();
            positionList.add(new Vec3i(bounds.minX, bounds.minY, bounds.minZ));
            positionList.add(new Vec3i(bounds.minX, bounds.minY, bounds.maxZ));
            positionList.add(new Vec3i(bounds.maxX, bounds.minY, bounds.minZ));
            positionList.add(new Vec3i(bounds.maxX, bounds.minY, bounds.maxZ));
            var size = (int) (bounds.maxX - bounds.minX);
            var divisions = size / 5;
            for (var i = 1; i < divisions; i++) {
                var offset = i * size / (float) divisions;
                positionList.add(new Vec3i(bounds.minX + offset, bounds.minY, bounds.minZ));
                positionList.add(new Vec3i(bounds.maxX - offset, bounds.minY, bounds.maxZ));
                positionList.add(new Vec3i(bounds.minX, bounds.minY, bounds.maxZ - offset));
                positionList.add(new Vec3i(bounds.maxX, bounds.minY, bounds.minZ + offset));
            }
            positions = positionList.toArray(new Vec3i[0]);
            this.levelId = levelId;
        }

        public void render(Minecraft mc, PoseStack pose, MultiBufferSource buffers, float partialTick, float thickness) {
            var ritualIntroLerp = timeRendered / DemesnesClaimRitual.INTRO_TIME_TICKS;
            for (int i = 0; i < positions.length; i++) {
                var lerp = Mth.clamp(ritualIntroLerp - i / (float) positions.length, 0, 1);
                var pos = positions[i];
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                BeaconRenderer.renderBeaconBeam(pose, buffers, BEAM_LOCATION, partialTick, 1,
                        mc.level.getGameTime(), 0, (int) (400 * lerp), beamColor, 0.2F * thickness, 0.25F);
                pose.popPose();
            }
        }

        public void setWeather(BlockPos blockPos) {
            hasColor = blockPos.getX() > 0;
            color = Vec3.fromRGB24(blockPos.getY());
            dayTime = blockPos.getZ() < 1 ? -1 : blockPos.getZ() * ClaimedDemesneData.TIME_COEFF;
        }
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.ComputeFogColor event) {
        if (currentDemesne == null || !currentDemesne.hasColor || event.getCamera().getFluidInCamera() != FogType.NONE)
            return;
        var col = currentDemesne.color;
        var add = (float) (col.x + col.y + col.z) * 0.1f + 0.1f;
        float f = Minecraft.getInstance().level.getTimeOfDay(Minecraft.getInstance().getPartialTick());
        float f1 = Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        var coeff = Mth.clamp(Mth.sqrt(f1), 0.0F, 1.0F);
        event.setRed(Math.min(1, (float) (col.x + add) * coeff));
        event.setGreen(Math.min(1, (float) (col.y + add) * coeff));
        event.setBlue(Math.min(1, (float) (col.z + add) * coeff));
    }

    @SubscribeEvent
    public static void renderTick(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var buffers = mc.renderBuffers().bufferSource();
        var pose = event.getPoseStack();
        pose.pushPose();
        var cam = event.getCamera();
        pose.translate(-cam.getPosition().x(), -cam.getPosition().y(), -cam.getPosition().z());
        var dimensionHash = DiagramManager.getDimensionHash(mc.level);
        var partialTick = event.getPartialTick();
        for (var ritual : ongoingRituals) {
            ritual.timeRendered += mc.getDeltaFrameTime();
            if (dimensionHash != ritual.levelId) continue;
            ritual.render(mc, pose, buffers, partialTick, 1);
        }
        if (SightManager.shouldRenderSight()) {
            for (var ritual : claimedDemesnes) {
                if (dimensionHash != ritual.levelId) continue;
                ritual.render(mc, pose, buffers, partialTick, 1);
            }
        }
        var chronoBeamsForLevel = chronoBeams.get(dimensionHash);
        if (chronoBeamsForLevel != null) {
            for (var beam : chronoBeamsForLevel.entrySet()) {
                var pos = beam.getKey();
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                BeaconRenderer.renderBeaconBeam(pose, buffers, BEAM_LOCATION_COLORLESS, partialTick * 10, 1,
                        mc.level.getGameTime() * 10, 0, beam.getValue(), chronoBeamColor, 0.2F, 0.25F);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var player = Minecraft.getInstance().player;
        if (player == null || claimedDemesnes.isEmpty()) return;
        currentCheckIndex = (currentCheckIndex + 1) % claimedDemesnes.size();
        var demesne = claimedDemesnes.get(currentCheckIndex);
        if (demesne.bounds.contains(player.position())) {
            currentDemesne = demesne;
        } else if (currentDemesne == demesne) {
            currentDemesne = null;
        }
    }

    private static void removeRitualAt(BlockPos minPos) {
        ClientDemesnesData ritualToRemove = null;
        for (var ritual : ongoingRituals) {
            if (!ritual.minPos.equals(minPos)) continue;
            ritualToRemove = ritual;
            break;
        }
        if (ritualToRemove != null) ongoingRituals.remove(ritualToRemove);
    }

    public static void handleEvent(DemesnesClientboundMessage message, Supplier<NetworkEvent.Context> ctx) {
        LOGGER.debug("DEMESNE EVENT:" + message.eventType());
        switch (message.eventType()) {
            case START, LOAD_RITUAL -> {
                for (var ritual : ongoingRituals) {
                    if (ritual.minPos.equals(message.minPos())) return;
                }
                ongoingRituals.add(new ClientDemesnesData(message.minPos(), new AABB(message.minPos(), message.maxPos()), message.levelId()));
            }
            case ABANDON -> removeRitualAt(message.minPos());
            case SUCCEED, LOAD_CLAIMED -> {
                removeRitualAt(message.minPos());
                for (var ritual : claimedDemesnes) {
                    if (ritual.minPos.equals(message.minPos())) return;
                }
                var clientData = new ClientDemesnesData(message.minPos(), new AABB(
                        message.minPos().offset(0.5f, 0.5f, 0.5f),
                        message.maxPos().offset(0.5f, 0.5f, 0.5f)), message.levelId());
                clientData.timeRendered = DemesnesClaimRitual.INTRO_TIME_TICKS * 10;
                claimedDemesnes.add(clientData);
                if (Objects.equals(message.playerName(), Minecraft.getInstance().player.getGameProfile().getName())) {
                    myDemesne = clientData;
                }
            }
            case MINING_SET -> demesneMineAmount = message.levelId();
            case CHRONO_SET -> {
                if (!chronoBeams.containsKey(message.levelId())) chronoBeams.put(message.levelId(), new HashMap<>());
                chronoBeams.get(message.levelId()).put(message.minPos(), message.maxPos().getY() - message.minPos().getY() + 1);
            }
            case CHRONO_UNSET -> {
                if (chronoBeams.containsKey(message.levelId())) {
                    chronoBeams.get(message.levelId()).remove(message.minPos());
                }
            }
            case COLOR_SET -> {
                for (var d : claimedDemesnes) {
                    if (d.minPos.equals(message.minPos()) && d.levelId == message.levelId()) {
                        d.setWeather(message.maxPos());
                        return;
                    }
                }
            }
        }
    }
}
