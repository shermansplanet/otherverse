package com.shermansplanet.otherverse.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryScreen.class)
public abstract class HorseInventoryScreenInjector extends AbstractContainerScreen<HorseInventoryMenu> {
    private static final ResourceLocation FAMILIAR_INVENTORY_LOCATION = new ResourceLocation(Otherverse.MODID, "textures/gui/familiar_mule.png");
    @Shadow
    private final AbstractHorse horse;

    public HorseInventoryScreenInjector(HorseInventoryMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        horse = null;
    }

    @Override
    protected void renderLabels(PoseStack p_97808_, int p_97809_, int p_97810_) {
        if (FamiliarManager.isChestedHorseFamiliar(this.horse)) return;
        this.font.draw(p_97808_, this.title, (float) this.titleLabelX, (float) this.titleLabelY, 4210752);
        this.font.draw(p_97808_, this.playerInventoryTitle, (float) this.inventoryLabelX, (float) this.inventoryLabelY, 4210752);
    }

    @Override
    protected void init() {
        if (FamiliarManager.isChestedHorseFamiliar(this.horse)) imageHeight = 210;
        super.init();
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    protected void renderMuleBg(PoseStack p_98821_, float p_98822_, int p_98823_, int p_98824_, CallbackInfo ci) {
        if (!FamiliarManager.isChestedHorseFamiliar(this.horse)) return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, FAMILIAR_INVENTORY_LOCATION);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(p_98821_, i, j, 0, 0, this.imageWidth, this.imageHeight);
        ci.cancel();
    }
}
