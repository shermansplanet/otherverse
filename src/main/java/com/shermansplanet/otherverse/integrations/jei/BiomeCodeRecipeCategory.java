package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritLabeler.SpiritAmount;
import com.shermansplanet.otherverse.spirits.Spirits;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class BiomeCodeRecipeCategory implements IRecipeCategory<BiomeCodeRecipe> {

    public static final RecipeType<BiomeCodeRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "biome_code", BiomeCodeRecipe.class);

    private final IDrawable background;
    private final Component localizedName;
    private final IDrawable icon;
    private final IGuiHelper guiHelper;
    private final ItemStack renderStack = new ItemStack(OtherverseItems.BIOME_BRAZIER.get());

    public BiomeCodeRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 16);
        localizedName = Component.translatable("otherverse.jei.biome_code");
        renderStack.getOrCreateTag().putBoolean("RenderFull", true);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, renderStack.copy());
        this.guiHelper = guiHelper;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public RecipeType<BiomeCodeRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, BiomeCodeRecipe recipe,
                          IFocusGroup focuses) {

        var invisibleInput = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        var start = getWidth() - recipe.spirits.size() * 12;
        for (int i = 0; i < recipe.spirits.size(); i++) {
            var stack = new ItemStack(Spirits.spiritItems.get(recipe.spirits.get(i)).get(), 1);
            builder.addSlot(RecipeIngredientRole.OUTPUT, start + i * 12, 0).addItemStack(stack);
            invisibleInput.addItemStack(stack);
        }
    }

    @Override
    public void draw(BiomeCodeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(mc.font, Component.translatable("biome." + recipe.biome.toString().replace(':', '.')), 0, 4, 0, false);
    }
}
