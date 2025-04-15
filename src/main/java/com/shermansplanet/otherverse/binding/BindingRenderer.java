package com.shermansplanet.otherverse.binding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.ReskinManager;
import com.shermansplanet.otherverse.registries.CrownBlock;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.*;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BindingRenderer {

    private static ItemRenderer itemRenderer;
    private static BlockRenderDispatcher blockRenderer;
    private static EntityRenderDispatcher entityRenderDispatcher;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static HashSet<UUID> boundEntities = new HashSet<>();
    private static HashSet<UUID> contractEntities = new HashSet<>();
    private static HashMap<UUID, String> familiars = new HashMap<>();
    private static HashMap<String, EntityType<LivingEntity>> familiarsByPract = new HashMap<>();
    private static HashMap<Player, EndCrystal> endCrystals = new HashMap<>();
    private static HashSet<EntityType<?>> bigHeadMobs = new HashSet<>(Set.of(EntityType.WITHER, EntityType.WARDEN, EntityType.GHAST));

    @SubscribeEvent
    public static void OnRenderName(RenderNameTagEvent event) {
        if (!familiars.containsKey(event.getEntity().getUUID())) return;
        var content = event.getContent();
        event.setContent(content.copy().setStyle(content.getStyle().withColor(0xffdd00).withBold(true)));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        var lvl = Minecraft.getInstance().level;
        if (lvl == null || lvl.getGameTime() % 40 != 0) return;

        for (var player : lvl.players()) {
            var type = familiarsByPract.get(player.getGameProfile().getName());
            if (type == null) continue;
            if (type.equals(EntityType.ENDER_DRAGON)) {
                List<EndCrystal> list = player.level.getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(32.0D));
                EndCrystal endcrystal = null;
                double d0 = Double.MAX_VALUE;

                for (EndCrystal endcrystal1 : list) {
                    double d1 = endcrystal1.distanceToSqr(player);
                    if (d1 < d0) {
                        d0 = d1;
                        endcrystal = endcrystal1;
                    }
                }

                if (endcrystal == null) {
                    endCrystals.remove(player);
                } else {
                    endCrystals.put(player, endcrystal);
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderTick(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS
                || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var crystal = endCrystals.get(player);
        if (crystal == null) return;
        var partialTick = event.getPartialTick();
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0, -4f, 0f);
        float f6 = (float) (crystal.getX() - Mth.lerp(partialTick, player.xo, player.getX()));
        float f8 = (float) (crystal.getY() + 2f - Mth.lerp(partialTick, player.yo, player.getY()));
        float f9 = (float) (crystal.getZ() - Mth.lerp(partialTick, player.zo, player.getZ()));
        EnderDragonRenderer.renderCrystalBeams(f6, f8 + EndCrystalRenderer.getY(crystal, partialTick), f9, partialTick, player.tickCount, poseStack,
                Minecraft.getInstance().renderBuffers().bufferSource(), 255);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        var crystal = endCrystals.get(player);
        if (crystal == null) return;
        var partialTick = event.getPartialTick();
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0, -0.8f, 0f);
        float f6 = (float) (crystal.getX() - Mth.lerp(partialTick, player.xo, player.getX()));
        float f8 = (float) (crystal.getY() + 0.8f - Mth.lerp(partialTick, player.yo, player.getY()));
        float f9 = (float) (crystal.getZ() - Mth.lerp(partialTick, player.zo, player.getZ()));
        EnderDragonRenderer.renderCrystalBeams(f6, f8 + EndCrystalRenderer.getY(crystal, partialTick), f9, partialTick, player.tickCount, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onLivingRender(RenderLivingEvent.Pre<?, ?> event) {
        var entityId = event.getEntity().getUUID();
        var contractOnly = false;
        if (!boundEntities.contains(entityId)) {
            if(contractEntities.contains(entityId)){
                contractOnly = true;
            }else {
                return;
            }
        }
        if (itemRenderer == null) {
            itemRenderer = Minecraft.getInstance().getItemRenderer();
        }
        if (blockRenderer == null) {
            blockRenderer = Minecraft.getInstance().getBlockRenderer();
        }
        if (entityRenderDispatcher == null) {
            entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        }
        LivingEntity mob = event.getEntity();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        var isFamiliar = familiars.containsKey(mob.getUUID());
        var upnudge = 0.5f;
        if (mob.shouldShowName() && mob.hasCustomName()) {
            double d0 = entityRenderDispatcher.distanceToSqr(mob);
            if (net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(mob, d0)) {
                upnudge = 1f;
            }
        }
        if (bigHeadMobs.contains(mob.getType())) {
            upnudge += 0.5f;
        }
        pose.translate(0, mob.getBbHeight() / ScaleTypes.HEIGHT.getScaleData(mob).getScale() + upnudge, 0);
        long millis = System.currentTimeMillis();
        float rot = (contractEntities.contains(mob.getUUID()) || isFamiliar) ? (millis % 3600) / 10f : -mob.yHeadRot;
        var s = 10f / 16f;
        pose.scale(s, s, s);
        pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, rot, 0)));
        pose.pushPose();
        if (isFamiliar || contractOnly) {
            pose.translate(-0.5f, -0.4f, -0.5f);
            blockRenderer.renderSingleBlock(
                    OtherverseBlocks.FAMILIAR_CROWN.get().defaultBlockState().setValue(CrownBlock.demesne, contractOnly),
                    pose, event.getMultiBufferSource(),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        } else {
            pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, 0, 90)));
            for (int i = 0; i < 4; i++) {
                pose.pushPose();
                pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(90, 90, 90 * i)));
                pose.translate(0.4f, 0f, 0f);
                pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, 90, 0)));
                itemRenderer.renderStatic(Items.CHAIN.getDefaultInstance(), ItemTransforms.TransformType.FIXED, event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY, event.getPoseStack(), event.getMultiBufferSource(), event.hashCode());
                pose.popPose();
            }
        }
        pose.popPose();
        if (!BindingManager.getHeldItem(mob).isEmpty()) {
            pose.translate(0, Math.sin(millis / 250.0) * 0.2, 0);
            itemRenderer.renderStatic(BindingManager.getHeldItem(mob), ItemTransforms.TransformType.FIXED, event.getPackedLight(),
                    OverlayTexture.NO_OVERLAY, event.getPoseStack(), event.getMultiBufferSource(), event.hashCode());
        }
        pose.popPose();
    }

    public static void updateBinding(LivingEntity le, BindingUpdateMessage update) {
        RandomSource r = le.level.random;
        var updateType = update.updateType;
        var silent = update.silent;
        if (updateType == BindingUpdateMessage.BindingUpdateType.BIND) {
            boundEntities.add(le.getUUID());
            if (silent) {
                return;
            }
            Vec3 v1 = le.getEyePosition();
            for (int i = 0; i < 8; i++) {
                le.level.addParticle(ParticleTypes.INSTANT_EFFECT,
                        v1.x + (r.nextFloat() - 0.5) * 0.3,
                        v1.y + 0.5,
                        v1.z + (r.nextFloat() - 0.5) * 0.3,
                        0, 0, 0);
            }
            le.level.playSound(Minecraft.getInstance().player, le, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1, 1);
        } else if (updateType == BindingUpdateMessage.BindingUpdateType.CONTRACT) {
            LOGGER.debug("CONSTRUCT TYPE: " + update.data.getString("construct_type"));
            if(update.data.contains("construct_type")){
                ReskinManager.reskinMob(le, update.data.getString("construct_type"));
            }else {
                contractEntities.add(le.getUUID());
            }
            if (silent) {
                return;
            }
            Vec3 v1 = le.position();
            for (int i = 0; i < 16; i++) {
                le.level.addParticle(ParticleTypes.INSTANT_EFFECT,
                        v1.x + (r.nextFloat() - 0.5) * le.getBoundingBox().getXsize(),
                        v1.y + r.nextFloat() * le.getBoundingBox().getYsize(),
                        v1.z + (r.nextFloat() - 0.5) * le.getBoundingBox().getZsize(),
                        0, 0, 0);
            }
            le.level.playSound(Minecraft.getInstance().player, le, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1, 1);
        } else if (updateType == BindingUpdateMessage.BindingUpdateType.FAMILIAR) {
            LOGGER.info("RECEIVED FAMILIAR UPDATE");
            var name = update.data.getString("practitioner");
            ReskinManager.reskinAsFamiliar(le, Minecraft.getInstance().player);
            familiars.put(le.getUUID(), name);
            familiarsByPract.put(name, (EntityType<LivingEntity>) le.getType());
            boundEntities.add(le.getUUID());
            le.getPersistentData().putString("practitioner", name);
            var lvl = Minecraft.getInstance().level;
            if (lvl != null) {
                for (var p : lvl.players()) {
                    p.refreshDimensions();
                }
            }
            if (silent) {
                return;
            }
            for (int i = 0; i < 7; ++i) {
                double d0 = r.nextGaussian() * 0.02D;
                double d1 = r.nextGaussian() * 0.02D;
                double d2 = r.nextGaussian() * 0.02D;
                le.level.addParticle(ParticleTypes.HEART, le.getRandomX(1.0D), le.getRandomY() + 0.5D, le.getRandomZ(1.0D), d0, d1, d2);
            }
        } else {
            boundEntities.remove(le.getUUID());
            contractEntities.remove(le.getUUID());
        }
    }

    public static EntityType<LivingEntity> getFamiliarType(Player player) {
        var profile = player.getGameProfile();
        if (profile == null) return null;
        return familiarsByPract.get(profile.getName());
    }
}
