package com.shermansplanet.otherverse.artifacts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.BindingRenderer;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.ruins.MemorySnareBlockEntity;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpiritAmountRenderer {

    private static Vec3 labelPosition = Vec3.ZERO;
    private static MutableComponent[] labelTextLines = new MutableComponent[0];
    private static boolean shouldRenderLabel = false;
    private static Entity trackingEntity = null;
    static final ResourceLocation SCREEN_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/chalkmark.png");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var hit = Minecraft.getInstance().hitResult;
        shouldRenderLabel = false;
        trackingEntity = null;
        if (hit == null) return;
        if (hit.getType() == HitResult.Type.BLOCK) {
            var blockPos = ((BlockHitResult) hit).getBlockPos();
            labelPosition = blockPos.getCenter();
            var lvl = Minecraft.getInstance().level;
            if (lvl == null) return;
            var blockEntity = lvl.getBlockEntity(blockPos);
            if (blockEntity instanceof BiomeBrazierBlockEntity brazier) {
                if (brazier.labels == null) return;
                labelTextLines = brazier.labels;
                shouldRenderLabel = true;
                return;
            }
            if (blockEntity instanceof ChalkCircle cc) {
                if (cc.isEmpty() || cc.getItem().is(Items.AIR)) return;
                labelPosition = labelPosition.subtract(0, 0.5f, 0);
                if (!cc.inscription.isEmpty()) {
                    labelTextLines = new MutableComponent[]{Component.literal(cc.inscription)};
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
            if (blockEntity instanceof MemorySnareBlockEntity snare) {
                labelTextLines = new MutableComponent[]{
                        Component.literal("Stored XP: " + snare.storedExperience)
                };
                shouldRenderLabel = true;
                return;
            }
            var data = DiagramManager.getOrCreateLevelData(lvl);
            var sympathyPosition = data.getSympathyPosition(labelPosition.toString());
            if (sympathyPosition != null) {
                labelTextLines = new MutableComponent[]{
                        Component.literal("Bound to (" + sympathyPosition.getX() + ", " + sympathyPosition.getY() + ", " + sympathyPosition.getZ() + ")")
                };
                shouldRenderLabel = true;
                return;
            }
            var tag = data.getPlacedItemTag(blockPos);
            if (tag == null) return;
            var typeString = tag.getString("spirit_type");
            var countAndCapacity = HallowHelper.getShrineSpiritCountAndCapacity(lvl, blockPos, Spirits.spiritsByLabel.get(typeString));
            labelTextLines = new MutableComponent[]{
                    Component.literal(tag.contains("shrine") ? "Shrine:" : "Hallow:"),
                    Component.literal(countAndCapacity.getFirst() + "/"
                            + countAndCapacity.getSecond() + " " + typeString)
            };
            shouldRenderLabel = true;
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            var entity = ((EntityHitResult) hit).getEntity();
            if (!(entity instanceof LivingEntity le)) return;
            var name = (entity.hasCustomName() ? entity.getCustomName() : entity.getType().getDescription()).copy();
            if (le.getPersistentData().contains("construct_type")) {
                if (le.getPersistentData().getString("construct_type").equals("flesh")) {
                    name = name.append(" Homunculus");
                } else if (le.getPersistentData().getString("construct_type").equals("technology")) {
                    name = name.append(" Golem");
                }
            }
            var bindingInfo = BindingRenderer.getBindingInfo(le);
            try {
                var mainLine = name.append(Component.literal(" (" + (int) (le.getHealth()) + "/" + (int) le.getMaxHealth() + ")"));
                labelTextLines = bindingInfo.isEmpty() ? new MutableComponent[]{mainLine} : new MutableComponent[]{mainLine, Component.literal(bindingInfo)};
            } catch (Exception e) {
                return;
            }
            trackingEntity = le;
            shouldRenderLabel = true;
        }
    }

    public static void renderLabels(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, int color) {
        if (!shouldRenderLabel) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (trackingEntity != null) {
            var pos = trackingEntity.getPosition(partialTick);
            labelPosition = pos.add(0, trackingEntity.getBbHeight() / 2f, 0);
        }
        var diff = camera.getEntity().getEyePosition(partialTick).subtract(labelPosition);
        var rot = camera.rotation().invert();
        var localDiff = diff.toVector3f().rotate(rot);
        var fov = Minecraft.getInstance().options.fov().get() * ((float) Math.PI / 180F);
        var x = (float) (Mth.atan2(localDiff.x, -localDiff.z) * screenHeight / fov);
        var y = (float) (Mth.atan2(localDiff.y, -localDiff.z) * screenHeight / fov);

        var font = Minecraft.getInstance().font;
        var baseX = screenWidth / 2f + x;
        var baseY = screenHeight / 2f + y;
        var pose = guiGraphics.pose();
        pose.pushPose();

        var longestWidth = 0;
        for (MutableComponent labelTextLine : labelTextLines) {
            longestWidth = Math.max(longestWidth, font.width(labelTextLine));
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SCREEN_LOCATION);
        var scrollWidth = longestWidth + 24;
        var scrollHeight = Math.max(24, font.lineHeight * labelTextLines.length + 14);

        pose.translate(baseX - 4, baseY - scrollHeight + 8, 0);
        guiGraphics.blitNineSliced(SCREEN_LOCATION, 0, 0, scrollWidth, scrollHeight, 12, 6, 12, 12, 41, 36, 5, 85);

        //var col = FastColor.ABGR32.color(255, 91, 80, 61);
        for (var i = 0; i < labelTextLines.length; i++) {
            guiGraphics.drawString(font, labelTextLines[i].withStyle(Style.EMPTY.withColor(color)).getVisualOrderText(),
                    15, labelTextLines.length == 1 ? 7 : font.lineHeight * i + 7, color, false);
        }
        pose.popPose();
    }
}
