package com.shermansplanet.otherverse.integrations.jei;

import com.mojang.blaze3d.systems.RenderSystem;
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

public class ColorTransfusionRecipeCategory extends TransfusionRecipeCategory {

    public static final RecipeType<TransfusionRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "color_transfusion", TransfusionRecipe.class);

    private final Component localizedName;
    private final IDrawable icon;

    public ColorTransfusionRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper);
        localizedName = Component.literal("Color Transfusion");
        icon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                39, 17, 16, 16);
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
}