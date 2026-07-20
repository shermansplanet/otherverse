package com.shermansplanet.otherverse.diagrams;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.demesnes.DemesnesClaimStartMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChalkCircleScreen extends AbstractContainerScreen<ChalkCircleMenu> {
    static final ResourceLocation SCREEN_LOCATION = ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/chalkmark.png");

    private final ChalkCircleMenu menu;
    private EditBox editBox;
    private Button confirmButton;
    private Button cancelButton;
    private final List<AbstractWidget> children;

    protected int imageWidth = 182;
    protected int imageHeight = 79;

    public ChalkCircleScreen(ChalkCircleMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.menu = menu;

        this.cancelButton = new ImageButton(0, 0, 15, 15, 189, 3, 15, SCREEN_LOCATION, 256, 256, (p_170074_) -> {
            onClose();
        });
        this.confirmButton = new ImageButton(0, 0, 17, 17, 206, 2, 17, SCREEN_LOCATION, 256, 256, (p_170074_) -> {
            OtherversePacketHandler.INSTANCE.sendToServer(new SetInscriptionMessage(new BlockPos(menu.centerX.get(), menu.centerY.get(), menu.centerZ.get()), editBox.getValue()));
            onClose();
        });

        this.children = ImmutableList.of(this.cancelButton, this.confirmButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public record SetInscriptionMessage(BlockPos pos, String inscription) {

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeUtf(inscription);
        }

        public static SetInscriptionMessage decode(FriendlyByteBuf buffer) {
            return new SetInscriptionMessage(
                    buffer.readBlockPos(),
                    buffer.readUtf()
            );
        }
    }

    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    protected void init() {
        super.init();

        // Calculate X and Y coordinates to place the text box relative to your GUI
        int boxX = this.leftPos + 12;
        int boxY = this.topPos + 12;

        this.editBox = new EditBox(this.font, boxX, boxY, 81, 16, Component.literal("Text Box"));
        this.editBox.setCanLoseFocus(false);
        this.editBox.setMaxLength(12);

        this.addRenderableWidget(this.editBox);
        this.setInitialFocus(this.editBox);
    }

    @Override
    public void containerTick() {
        this.editBox.tick();
    }

    @Override
    protected void renderBg(GuiGraphics p_283065_, float p_97788_, int p_97789_, int p_97790_) {

    }

    @Override
    public void render(GuiGraphics graphics, int p_95529_, int p_95530_, float p_95531_) {
        this.renderBackground(graphics);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SCREEN_LOCATION);
        var downshift = 30;
        graphics.blit(SCREEN_LOCATION, leftPos, topPos + downshift, 0, 0, imageWidth, imageHeight);

        confirmButton.setX(leftPos + 100);
        confirmButton.setY(topPos + 28 + downshift);
        confirmButton.render(graphics, p_95529_, p_95530_, p_95531_);

        cancelButton.setX(leftPos + 102);
        cancelButton.setY(topPos + 5 + downshift);
        cancelButton.render(graphics, p_95529_, p_95530_, p_95531_);

        editBox.setX(leftPos + 12);
        editBox.setY(topPos + 10 + downshift);
        editBox.render(graphics, p_95529_, p_95530_, p_95531_);
    }
}
