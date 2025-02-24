package com.shermansplanet.otherverse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.familiar.MobRetexturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.slf4j.Logger;

public class SightOverlay implements IGuiOverlay {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SIGHT_LOCATION = new ResourceLocation(Otherverse.MODID, "textures/overlay/sight.png");
    public static final SightOverlay instance = new SightOverlay();

    private float sightR = 1, sightG = 1, sightB = 1;

    private float opacity = 0f;

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        var dt = Minecraft.getInstance().getDeltaFrameTime() / 8f;
        opacity = Mth.clamp(opacity + (SightManager.shouldRenderSight() ? dt : -dt), 0, 1);
        if (opacity == 0) return;
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
        poseStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0, 0, rot)));
        poseStack.translate(-128, -128, 0);
        gui.blit(poseStack, 0, 0, 0, 0, 256, 256);
        poseStack.popPose();
    }

    public void recalculateColor() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            LOGGER.debug("Couldn't find player");
            return;
        }
        var palette = MobRetexturer.getPlayerPalette(player);
        if (palette == null) {
            LOGGER.debug("Null palette");
            return;
        }
        if(palette.palettes.isEmpty()){
            LOGGER.debug("Empty palette");
            return;
        }
        var pixel = MobRetexturer.getPrimaryColor(palette);
        sightR = pixel.r() / 255f;
        sightG = pixel.g() / 255f;
        sightB = pixel.b() / 255f;
    }
}
