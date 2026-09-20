package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.Otherverse;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SmeltingTransfusionRecipeCategory extends TransfusionRecipeCategory {

    public static final RecipeType<TransfusionRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "smelting_transfusion", TransfusionRecipe.class);

    private final Component localizedName;
    private final IDrawable icon;

    public SmeltingTransfusionRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper);
        localizedName = Component.literal("Smelting Transfusion");
        icon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                57, 17, 16, 16);
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