package com.shermansplanet.otherverse.artifacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpiritAmountRenderer {

    private static BlockPos labelPosition;
    private static MutableComponent[] labelTextLines;
    private static boolean shouldRenderLabel = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var hit = Minecraft.getInstance().hitResult;
        shouldRenderLabel = false;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            labelPosition = ((BlockHitResult) hit).getBlockPos();
            var lvl = Minecraft.getInstance().level;
            if (lvl == null) return;
            var blockEntity = lvl.getBlockEntity(labelPosition);
            if (blockEntity instanceof BiomeBrazierBlockEntity brazier) {
                if (brazier.labels == null) return;
                labelTextLines = brazier.labels;
                shouldRenderLabel = true;
                return;
            }
            if (blockEntity instanceof ChalkCircle cc) {
                if (cc.isEmpty() || cc.getItem().is(Items.AIR)) return;
                if (cc.isNumber) {
                    labelTextLines = new MutableComponent[]{Component.literal(String.valueOf(cc.getItem().getCount()))};
                } else {
                    var item = cc.getItem();
                    var lines = item.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
                    labelTextLines = new MutableComponent[lines.size()];
                    for (var i = 0; i < lines.size(); i++) {
                        labelTextLines[i] = lines.get(i).copy();
                    }
                }
                shouldRenderLabel = true;
                return;
            }
            var tag = DiagramManager.getOrCreateLevelData(lvl).getPlacedItemTag(labelPosition);
            if (tag == null) return;
            var typeString = tag.getString("spirit_type");
            var countAndCapacity = HallowHelper.getShrineSpiritCountAndCapacity(lvl, labelPosition, Spirits.spiritsByLabel.get(typeString));
            labelTextLines = new MutableComponent[]{
                    Component.literal(tag.contains("shrine") ? "Shrine:" : "Hallow:"),
                    Component.literal(countAndCapacity.getFirst() + "/"
                            + countAndCapacity.getSecond() + " " + typeString)
            };
            shouldRenderLabel = true;
        }
    }

    @SubscribeEvent
    public static void onRenderTick(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!SightManager.shouldRenderSight()) return;
        if (!shouldRenderLabel) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        var center = Vec3.atCenterOf(labelPosition);
        var diff = center.subtract(event.getCamera().getPosition()).normalize();
        event.getPoseStack().translate(diff.x, diff.y, diff.z);
        var look = event.getCamera().getEntity().getLookAngle().normalize();
        var camRot = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var axis = look.cross(diff);
        var angle = Math.asin(axis.length()) * 4;
        pose.mulPose(new Quaternion(new Vector3f((float) axis.x, (float) axis.y, (float) axis.z), (float) angle, false));
        pose.mulPose(camRot);
        render(pose, Minecraft.getInstance().renderBuffers().bufferSource());
        event.getPoseStack().popPose();
    }

    public static void render(PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();
        var scale = 0.005f;
        pose.scale(-scale, -scale, scale);
        for (int i = 0; i < labelTextLines.length; i++) {
            var text = labelTextLines[i];
            pose.pushPose();
            Font font = Minecraft.getInstance().font;
            var hoverName = text.withStyle(text.getStyle().withColor(0xffffff));
            var blankName = hoverName.copy().withStyle(text.getStyle().withColor(0));
            float dx = (float) (-font.width(hoverName) / 2);
            float dy = (float) (-font.lineHeight / 2);
            pose.translate(0, dy * (i - (labelTextLines.length - 1) / 2f) * -4, 0);
            Matrix4f matrix4f = pose.last().pose();
            pose.pushPose();
            pose.scale((dx - 4) / dx, (dy - 4) / dy, 1);
            pose.translate(0, 0, 12);
            var newMatrix = pose.last().pose();
            font.drawInBatch(blankName, dx, dy, 0, false, newMatrix, buffers, true, 0xff000000, 0);
            pose.popPose();
            font.drawInBatch(hoverName, dx, dy, 0xffffff, false, matrix4f, buffers, true, 0, 0xf000f0);
            pose.popPose();
        }
        pose.popPose();
    }
}
