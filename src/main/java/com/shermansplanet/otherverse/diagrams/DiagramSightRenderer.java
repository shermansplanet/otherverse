package com.shermansplanet.otherverse.diagrams;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.spirits.particles.OtherverseParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Type;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class DiagramSightRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static BlockPos symmetryCenter = null;
    public static BlockPos symmetryLock = null;
    public static ArrayList<Diagram> toUnload = new ArrayList<>();

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event) {
        symmetryCenter = null;
        symmetryLock = null;
        toUnload = new ArrayList<>();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || event.type != Type.CLIENT) {
            return;
        }
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || Minecraft.getInstance().isPaused()) {
            return;
        }

        TransientDiagramData levelData = DiagramManager.getOrCreateLevelData(camera.level());
        if (camera.level().getGameTime() % 16 == 0) {
            RandomSource random = camera.level().random;
            for (BlockPos pos : levelData.getAllPlacedItemPositions()) {
                CompoundTag tag = levelData.getPlacedItemTag(pos);
                SpiritType spiritType = Spirits.spiritsByLabel.get(tag.getString("spirit_type"));
                if (tag.contains("shrine")) {
                    Vec3 v1 = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    Vec3 offset = random.nextBoolean()
                            ? new Vec3(random.nextBoolean() ? -0.6D : 0.6D,
                            random.nextDouble() - 0.5D,
                            random.nextDouble() - 0.5D)
                            : new Vec3(random.nextDouble() - 0.5D,
                            random.nextDouble() - 0.5D,
                            random.nextBoolean() ? -0.6D : 0.6D);
                    v1 = v1.add(offset);
                    camera.level().addParticle(
                            new ItemParticleOption(OtherverseParticles.HALLOW_PARTICLE_TYPE,
                                    Spirits.spiritItems.get(spiritType).get().getDefaultInstance()), v1.x, v1.y, v1.z,
                            0, 0.04D, 0);
                }
            }
        }

        if (camera.level().getGameTime() % 20 != 0) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!SightManager.shouldRenderSight()) {
            return;
        }

        for (BlockPos pos : levelData.diagramsByPrimary.keySet()) {
            if (new Vec3(pos.getX(), pos.getY(), pos.getZ()).distanceToSqr(camera.position()) > 32 * 32) {
                continue;
            }
            Diagram diagram = levelData.diagramsByPrimary.get(pos);
            if (!(player.level().getBlockEntity(pos) instanceof ChalkCircle cc) || cc.diagram != diagram) {
                toUnload.add(diagram);
                continue;
            }

            for (BlockPos p1 : diagram.allFocusPositions) {
                BlockPos p2 = diagram.influences.get(p1);
                if (p2 == null) {
                    continue;
                }
                Vec3 v1 = new Vec3(p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5);
                BlockPos diff = p2.subtract(p1);
                Vec3 v2 = new Vec3(diff.getX(), diff.getY(), diff.getZ()).scale(0.095);
                camera.level()
                        .addParticle(ParticleTypes.END_ROD, v1.x, v1.y - 0.45, v1.z, v2.x, 0.02D, v2.z);
            }
        }
        for (Diagram diagram : toUnload) {
            LOGGER.debug("unloading diagram from sight renderer");
            DiagramManager.unloadDiagram(diagram, player.level());
        }
        toUnload.clear();
    }

    @SubscribeEvent
    public static void renderTick(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();

        HitResult hitresult = Minecraft.getInstance().hitResult;
        BlockPos selectedBlockPosition = null;
        if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            selectedBlockPosition = ((BlockHitResult) hitresult).getBlockPos();
        }

        if (selectedBlockPosition == null) {
            return;
        }

        Level level = event.getCamera().getEntity().level();
        Camera camera = event.getCamera();

        if (symmetryCenter != null) {
            if (symmetryLock != null) selectedBlockPosition = symmetryLock;
            selectedBlockPosition = selectedBlockPosition.atY(symmetryCenter.getY());
            Vec3i diff = selectedBlockPosition.subtract(symmetryCenter);
            boolean needsMirror =
                    diff.getX() != 0 && diff.getZ() != 0 && Math.abs(diff.getX()) != Math.abs(diff.getZ());

            poseStack.pushPose();
            poseStack.translate(
                    -camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

            RenderBlock(poseStack, new Vec3(symmetryCenter.getX() + 0.5, symmetryCenter.getY() + 0.5,
                    symmetryCenter.getZ() + 0.5), 0.75f, false, true);

            for (var mirrored = 0; mirrored < (needsMirror ? 2 : 1); mirrored++) {
                for (var i = 0; i < 4; i++) {
                    diff = new BlockPos(diff.getZ(), 0, -diff.getX());
                    BlockPos newPos = symmetryCenter.offset(diff);
                    RenderBlock(poseStack,
                            new Vec3(newPos.getX() + 0.5, newPos.getY() + 0.5, newPos.getZ() + 0.5), 0.75f, false,
                            false);
                }
                diff = new BlockPos(diff.getX(), 0, -diff.getZ());
            }

            poseStack.popPose();
            return;
        }

        if (!SightManager.shouldRenderSight()) return;

        TransientDiagramData levelData = DiagramManager.getOrCreateLevelData(player.level());

        Diagram diagram;
        if (level.getBlockEntity(selectedBlockPosition) instanceof ChalkCircle cc) {
            diagram = levelData.diagramsByPrimary.get(cc.diagramPrimary);
        } else {
            IFocus focus = levelData.allBlockFoci.get(selectedBlockPosition);
            if (focus == null) {
                selectedBlockPosition = selectedBlockPosition.above();
                focus = levelData.allBlockFoci.get(selectedBlockPosition);
            }
            if (focus == null) {
                return;
            }
            diagram = focus.getDiagram();
        }

        if (diagram == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                -camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        for (BlockPos focusPos : diagram.allFocusPositions) {
            BlockPos chosenTarget = diagram.influences.get(focusPos);
            BlockPos[] allTargets = diagram.rawInfluences.get(focusPos);
            if (allTargets == null) {
                continue;
            }

            for (BlockPos targetPos : allTargets) {
                boolean focusSelected = selectedBlockPosition.equals(focusPos);
                boolean targetSelected = selectedBlockPosition.equals(targetPos);

                if (!focusSelected && !targetSelected) {
                    continue;
                }

                BlockPos bp = (focusSelected ? targetPos : focusPos);
                float s = 1.2f + (float) Math.sin(System.currentTimeMillis() / 100d)
                        * (focusSelected ? 0.1f : -0.1f);

                boolean notChosen = chosenTarget == null || !chosenTarget.equals(targetPos);
                if (notChosen) {
                    s *= 0.5;
                }

                RenderBlock(poseStack, new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5), s,
                        notChosen, focusSelected);
            }
        }
        poseStack.popPose();
    }

    private static void RenderBlock(PoseStack poseStack, Vec3 pos, float scale, boolean darken,
                                    boolean focusSelected) {
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y - (scale < 1 ? 0.5 : 0.51), pos.z);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                OtherverseBlocks.CHALK_LINE.get().defaultBlockState()
                        .setValue(ChalkLineBlock.chalkCircle, true)
                        .setValue(ChalkLineBlock.color, focusSelected ? DyeColor.ORANGE : DyeColor.CYAN),
                poseStack, Minecraft.getInstance().renderBuffers().bufferSource(),
                darken ? 128 : 255,
                OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.ModelData.EMPTY, RenderType.cutout());
        poseStack.popPose();
    }

    public static void setCenter(BlockPos pos, Player player) {
        if (pos != null) {
            if (symmetryCenter != null && symmetryLock == null) {
                player.displayClientMessage(Component.translatable("otherverse.diagram.symmetry_lock"), true);
                symmetryLock = pos;
            } else {
                player.displayClientMessage(Component.translatable("otherverse.diagram.symmetry_set"), true);
                symmetryCenter = pos;
                symmetryLock = null;
            }
        } else if (symmetryCenter != null) {
            player.displayClientMessage(Component.translatable("otherverse.diagram.symmetry_clear"), true);
            symmetryCenter = null;
            symmetryLock = null;
        }
    }
}
