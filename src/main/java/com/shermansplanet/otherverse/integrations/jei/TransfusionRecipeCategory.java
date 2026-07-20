package com.shermansplanet.otherverse.integrations.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class TransfusionRecipeCategory implements IRecipeCategory<TransfusionRecipe> {

    public static final RecipeType<TransfusionRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "transfusion", TransfusionRecipe.class);

    private final IDrawable background;
    private final Component localizedName;
    private final IDrawable icon;
    private final IDrawable overlay;
    private final IGuiHelper guiHelper;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TransfusionRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(100, 32);
        localizedName = Component.translatable("otherverse.jei.transfusion");
        overlay = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                0, 0, 62, 11);
        icon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                0, 17, 16, 16);
        this.guiHelper = guiHelper;
    }

    @Override
    public int getWidth() {
        return 100;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public RecipeType<TransfusionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TransfusionRecipe recipe, IFocusGroup foci) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 8).addItemStack(recipe.itemFrom);
        var i = 0;
        var offset = ((recipe.transfusionSources.size() - 1) / 2f);
        for (var source : recipe.transfusionSources) {
            int x = 42 + (int) ((i - offset) * 18);
            builder.addSlot(RecipeIngredientRole.CATALYST, x, 2).addItemStack(source);
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(source);
            OtherverseJeiPlugin.tryAddEgg(builder, source);
            i++;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 10).addItemStack(recipe.itemTo);
    }

    @Override
    public void draw(TransfusionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        overlay.draw(graphics, 18, 8);
        RenderSystem.disableBlend();
        String s = recipe.cost + (recipe.isMob ? (recipe.transfusionSources.size() > 1 ? " total HP" : " HP") : "");
        graphics.drawString(mc.font, s, 50 - s.length() * 3, 20, 0x333333, false);
    }
}