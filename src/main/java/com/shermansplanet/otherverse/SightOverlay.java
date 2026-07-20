package com.shermansplanet.otherverse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.artifacts.SpiritAmountRenderer;
import com.shermansplanet.otherverse.familiar.MobRetexturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Quaternionf;
import org.slf4j.Logger;

public class SightOverlay implements IGuiOverlay {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SIGHT_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/overlay/sight.png");
    public static final SightOverlay instance = new SightOverlay();

    private float sightR = 1, sightG = 1, sightB = 1;
    private int intColor;

    private float opacity = 0f;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        var dt = Minecraft.getInstance().getDeltaFrameTime() / 8f;
        opacity = Mth.clamp(opacity + (SightManager.shouldRenderSight() ? dt : -dt), 0, 1);
        if (opacity == 0) return;
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(sightR, sightG, sightB, opacity);
        RenderSystem.setShaderTexture(0, SIGHT_LOCATION);
        var speed = 200;
        var rot = (System.currentTimeMillis() % (360 * speed)) / (float) speed;
        rot -= Mth.square(1 - opacity) * 30;
        poseStack.translate(screenWidth / 2f, screenHeight / 2f, 0);
        var scale = screenWidth * (1.5f - opacity * 0.3f) / 256f;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(new Quaternionf().rotateZ(rot * Mth.TWO_PI / 360));
        poseStack.translate(-128, -128, 0);
        guiGraphics.blit(SIGHT_LOCATION, 0, 0, 0, 0, 256, 256);
        poseStack.popPose();
        if (!SightManager.shouldRenderSight()) return;
        SpiritAmountRenderer.renderLabels(gui, guiGraphics, partialTick, screenWidth, screenHeight, intColor);
    }

    public void recalculateColor() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            System.out.println("Couldn't find player");
            return;
        }
        var palette = MobRetexturer.getPlayerPalette(player);
        if (palette == null) {
            System.out.println("Null palette");
            return;
        }
        if (palette.palettes.isEmpty()) {
            System.out.println("Empty palette");
            return;
        }
        var pixel = MobRetexturer.getPrimaryColor(palette);
        var r = pixel.r();
        var g = pixel.g();
        var b = pixel.b();
        sightR = r / 255f;
        sightG = g / 255f;
        sightB = b / 255f;
        var brightness = Mth.sqrt((float) pixel.getPerceptualBrightnessSqr() / 65025f);
        var coeff = brightness < 0.5f ? 1 : 0.4f / brightness;
        intColor = FastColor.ABGR32.color(255, (int) (coeff * r), (int) (coeff * g), (int) (coeff * b));
    }
}
