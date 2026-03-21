package com.shermansplanet.otherverse.demesnes;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DemesnesClaimScreen extends AbstractContainerScreen<DemesnesClaimMenu> {
    static final String ritualStartText = "CLAIM";
    static final ResourceLocation SCREEN_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/demesnes.png");
    static final ResourceLocation UI_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png");

    private final DemesnesClaimMenu menu;

    private Button startButton;

    private final List<AbstractWidget> children;
    private static final Logger LOGGER = LogUtils.getLogger();

    public DemesnesClaimScreen(DemesnesClaimMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.menu = menu;

        this.startButton = new ImageButton(0, 0, 79, 23, 0, 44, 25, UI_LOCATION, 256, 256, (p_170074_) -> {
            onClose();
            OtherversePacketHandler.INSTANCE.sendToServer(new DemesnesClaimStartMessage(
                    new BlockPos(menu.centerX.get(), menu.centerY.get(), menu.centerZ.get()),
                    menu.range.get())
            );
        }, Component.literal(ritualStartText)) {
            protected MutableComponent createNarrationMessage() {
                return Component.literal(ritualStartText);
            }
        };

        this.children = ImmutableList.of(this.startButton);
    }

    @Override
    protected void renderBg(GuiGraphics p_283065_, float p_97788_, int p_97789_, int p_97790_) {

    }

    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public void render(GuiGraphics graphics, int p_95529_, int p_95530_, float p_95531_) {
        var pose = graphics.pose();
        this.renderBackground(graphics);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SCREEN_LOCATION);

        int i = (this.width - 232) / 2;
        int j = (this.height - 232) / 2;
        graphics.blit(SCREEN_LOCATION, i, j, 0, 0, 232, 232);

        var margin = 35;
        graphics.fill(i + margin, j + margin, i + 232 - margin, j + 232 - margin, 0xff4f3f31);

        int midx = this.width / 2 - 1;
        int midy = this.height / 2 - 14;

        int range = menu.range.get();
        if (menu.canClaim.get() == 0) range = -1;
        int maxsize = 3;
        for (var x = -maxsize; x <= maxsize; x++) {
            for (var y = -maxsize; y <= maxsize; y++) {
                var isActive = x >= -range && x <= range && y >= -range && y <= range;
                graphics.blit(SCREEN_LOCATION, midx + 20 * x - 10, midy + 20 * y - 10, isActive ? 0 : 31, 234, 22, 22);
                if (x != 0 || y != 0) continue;
                graphics.blit(SCREEN_LOCATION, midx - 8, midy - 8, 58, 236, 17, 17);
            }
        }
        var levelCost = Mth.square(range * 2 + 1);
        var canStart = range >= 0;
        this.startButton.active = canStart;
        this.startButton.visible = canStart;

        DemesnesScreen.drawSpiritType(graphics, menu.spiritType.get(), midx, midy);

        if (canStart) {
            var player = Minecraft.getInstance().player;
            var hasLevels = player.getAbilities().instabuild || player.experienceLevel >= levelCost;
            if (hasLevels) {
                this.startButton.setX(midx + 7);
                this.startButton.setY(midy + 77);
                this.startButton.render(graphics, p_95529_, p_95530_, p_95531_);
                graphics.drawString(font, ritualStartText, midx + 33, midy + 85, startButton.isHoveredOrFocused() ? 0xaaff00 : 0xded4bc);
            }

            graphics.drawString(font, "Level cost:", midx - 78, midy + 85, 0xbab3a0);
            graphics.drawString(font, String.valueOf(levelCost), midx - 20, midy + 85, hasLevels ? 0xaaff00 : 0xe6272a);
        } else {
            var s = menu.canClaim.get() == 1 ? "No pyramid found." : "Area already claimed.";
            graphics.drawString(font, s, midx - 78, midy + 82, 0xbab3a0);
        }
    }

    @Override
    public DemesnesClaimMenu getMenu() {
        return menu;
    }
}
