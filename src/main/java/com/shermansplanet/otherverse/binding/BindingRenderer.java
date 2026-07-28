package com.shermansplanet.otherverse.binding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.ReskinManager;
import com.shermansplanet.otherverse.registries.CrownBlock;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
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
import net.minecraft.world.item.ItemDisplayContext;
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
import org.joml.Quaternionf;
import org.slf4j.Logger;
import software.bernie.geckolib.event.GeoRenderEvent;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.*;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BindingRenderer {

    private static ItemRenderer itemRenderer;
    private static BlockRenderDispatcher blockRenderer;
    private static EntityRenderDispatcher entityRenderDispatcher;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static HashSet<UUID> positiveBoundEntities = new HashSet<>();
    private static HashSet<UUID> negativeBoundEntities = new HashSet<>();
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
                List<EndCrystal> list = player.level().getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(32.0D));
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
    public static void onLivingRenderGecko(GeoRenderEvent.Entity.Post event) {
        if (event.getEntity() instanceof LivingEntity le) {
            renderBindings(le, event.getPoseStack(), event.getBufferSource(), event.getPackedLight(), event.hashCode());
        }
    }

    @SubscribeEvent
    public static void onLivingRenderGeckoReplaced(GeoRenderEvent.ReplacedEntity.Post event) {
        if (event.getReplacedEntity() instanceof LivingEntity le) {
            renderBindings(le, event.getPoseStack(), event.getBufferSource(), event.getPackedLight(), event.hashCode());
        }
    }

    @SubscribeEvent
    public static void onLivingRenderVanilla(RenderLivingEvent.Pre<?, ?> event) {
        renderBindings(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.hashCode());
    }

    private static void renderBindings(LivingEntity mob, PoseStack pose, MultiBufferSource multiBufferSource, int packedLight, int hashCode) {
        hashCode++;
        var entityId = mob.getUUID();
        var contractOnly = false;
        var isPositive = positiveBoundEntities.contains(entityId);
        var isFamiliar = familiars.containsKey(mob.getUUID());
        if (!isFamiliar && !isPositive && !negativeBoundEntities.contains(entityId)) {
            if (contractEntities.contains(entityId)) {
                contractOnly = true;
            } else {
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
        pose.pushPose();
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
        pose.mulPose(new Quaternionf().rotateY(rot * Mth.TWO_PI / 360));
        pose.pushPose();
        if(isFamiliar) LOGGER.debug("FAMILIAR: {}", mob);
        if (!mob.getPersistentData().contains("construct_type")) {
            if (isFamiliar || contractOnly || isPositive) {
                pose.translate(-0.5f, -0.4f, -0.5f);
                blockRenderer.renderSingleBlock(
                        OtherverseBlocks.FAMILIAR_CROWN.get().defaultBlockState().setValue(CrownBlock.demesne, !isFamiliar).setValue(CrownBlock.positive, true),
                        pose, multiBufferSource,
                        packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
            } else {
                pose.mulPose(new Quaternionf().rotateZ(Mth.PI / 2));
                for (int i = 0; i < 4; i++) {
                    pose.pushPose();
                    pose.mulPose(new Quaternionf().rotateXYZ(Mth.PI / 2, Mth.PI / 2, Mth.PI / 2 * i));
                    pose.translate(0.4f, 0f, 0f);
                    pose.mulPose(new Quaternionf().rotateXYZ(0, Mth.PI / 2, 0));
                    itemRenderer.renderStatic(Items.CHAIN.getDefaultInstance(), ItemDisplayContext.FIXED, packedLight,
                            OverlayTexture.NO_OVERLAY, pose, multiBufferSource, mob.level(), hashCode);
                    pose.popPose();
                }
            }
//            pose.translate(-0.5f, -0.4f, -0.5f);
//            blockRenderer.renderSingleBlock(
//                    OtherverseBlocks.FAMILIAR_CROWN.get().defaultBlockState()
//                            .setValue(CrownBlock.demesne, !isFamiliar && isPositive)
//                            .setValue(CrownBlock.positive, isPositive),
//                    pose, event.getMultiBufferSource(),
//                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());

        }
        pose.popPose();
        if (!BindingManager.getHeldItem(mob).isEmpty()) {
            pose.translate(0, Math.sin(millis / 250.0) * 0.2, 0);
            itemRenderer.renderStatic(BindingManager.getHeldItem(mob), ItemDisplayContext.FIXED, packedLight,
                    OverlayTexture.NO_OVERLAY, pose, multiBufferSource, mob.level(), hashCode);
        }
        pose.popPose();
    }

    public static boolean isBound(LivingEntity le) {
        if (positiveBoundEntities.contains(le.getUUID()) || negativeBoundEntities.contains(le.getUUID()) || familiars.containsKey(le.getUUID())) {
            return true;
        }
        if (le.getPersistentData().contains("construct_type")) return true;
        if (le.getPersistentData().contains("practitioner_loyalty")) return true;
        return false;
    }

    public static String getBindingInfo(LivingEntity le) {
        var boundBy = le.getPersistentData().getString("last_bound_by");
        if (familiars.containsKey(le.getUUID())) {
            return "Familiar of " + familiars.get(le.getUUID());
        } else if (positiveBoundEntities.contains(le.getUUID())) {
            return "Positively bound by " + boundBy;
        } else if (negativeBoundEntities.contains(le.getUUID())) {
            return "Negatively bound by " + boundBy;
        } else if (le.getPersistentData().contains("construct_type") && !boundBy.isEmpty()) {
            return "Created by " + boundBy;
        } else if (le.getPersistentData().contains("practitioner_loyalty")) {
            return "Loyal to " + le.getPersistentData().getString("practitioner_loyalty");
        }
        return "";
    }

    public static void updateBinding(LivingEntity le, BindingUpdateMessage update) {
        RandomSource r = le.level().random;
        var updateType = update.updateType;
        var silent = update.silent;
        switch (updateType) {
            case BIND -> {
                if (update.type == BindingUpdateMessage.BindingType.POSITIVE) {
                    positiveBoundEntities.add(le.getUUID());
                } else {
                    negativeBoundEntities.add(le.getUUID());
                }
                le.getPersistentData().putString("last_bound_by", update.data.getString("last_bound_by"));
                if (silent) {
                    return;
                }
                Vec3 v1 = le.getEyePosition();
                for (int i = 0; i < 8; i++) {
                    le.level().addParticle(ParticleTypes.INSTANT_EFFECT,
                            v1.x + (r.nextFloat() - 0.5) * 0.3,
                            v1.y + 0.5,
                            v1.z + (r.nextFloat() - 0.5) * 0.3,
                            0, 0, 0);
                }
                le.level().playSound(Minecraft.getInstance().player, le, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1, 1);
            }
            case CONTRACT -> {
                if (update.data.contains("construct_type")) {
                    var ct = update.data.getString("construct_type");
                    ReskinManager.reskinMob(le, ct);
                    le.getPersistentData().putString("construct_type", ct);
                }
                if (update.data.contains("practitioner_loyalty")) {
                    le.getPersistentData().putString("practitioner_loyalty", update.data.getString("practitioner_loyalty"));
                }
                if (update.data.contains("last_bound_by")) {
                    le.getPersistentData().putString("last_bound_by", update.data.getString("last_bound_by"));
                }
                contractEntities.add(le.getUUID());
                if (silent) {
                    return;
                }
                Vec3 v1 = le.position();
                for (int i = 0; i < 16; i++) {
                    le.level().addParticle(ParticleTypes.INSTANT_EFFECT,
                            v1.x + (r.nextFloat() - 0.5) * le.getBoundingBox().getXsize(),
                            v1.y + r.nextFloat() * le.getBoundingBox().getYsize(),
                            v1.z + (r.nextFloat() - 0.5) * le.getBoundingBox().getZsize(),
                            0, 0, 0);
                }
                le.level().playSound(Minecraft.getInstance().player, le, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1, 1);
            }
            case FAMILIAR -> {
                LOGGER.debug("RECEIVED FAMILIAR UPDATE");
                var name = update.data.getString("practitioner");
                for (var player : le.level().players()) {
                    if (player.getGameProfile().getName().equals(name)) {
                        ReskinManager.reskinAsFamiliar(le, player);
                        break;
                    }
                }
                familiars.put(le.getUUID(), name);
                familiarsByPract.put(name, (EntityType<LivingEntity>) le.getType());
                positiveBoundEntities.add(le.getUUID());
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
                    le.level().addParticle(ParticleTypes.HEART, le.getRandomX(1.0D), le.getRandomY() + 0.5D, le.getRandomZ(1.0D), d0, d1, d2);
                }
            }
            case BREAK -> {
                LOGGER.info("RECEIVED BROKEN BINDING UPDATE");
                positiveBoundEntities.remove(le.getUUID());
                negativeBoundEntities.remove(le.getUUID());
                contractEntities.remove(le.getUUID());
            }
        }
    }

    public static EntityType<LivingEntity> getFamiliarType(Player player) {
        var profile = player.getGameProfile();
        if (profile == null) return null;
        return familiarsByPract.get(profile.getName());
    }
}
