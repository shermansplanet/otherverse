package com.shermansplanet.otherverse.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class SpiritExtractionRecipeCategory implements IRecipeCategory<SpiritExtractionRecipe> {

    public static final RecipeType<SpiritExtractionRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "spirit_extraction", SpiritExtractionRecipe.class);

    private final IDrawable background;
    private final Component localizedName;
    private final IDrawable icon;
    private final IGuiHelper guiHelper;
    private final ItemStack renderStack = new ItemStack(OtherverseItems.SPIRIT_TABLET.get());

    private final IDrawable blockIcon;

    public SpiritExtractionRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 20);
        localizedName = Component.translatable("otherverse.jei.spirit_extraction");
        renderStack.getOrCreateTag().putBoolean("RenderFull", true);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, renderStack.copy());
        this.guiHelper = guiHelper;

        blockIcon = guiHelper.createDrawable(
                new ResourceLocation(Otherverse.MODID, "textures/gui/jei.png"),
                0, 34, 9, 10);
    }

    @Override
    public RecipeType<SpiritExtractionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SpiritExtractionRecipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 2).addItemStack(recipe.input);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.input);

        OtherverseJeiPlugin.tryAddEgg(builder, recipe.input);

        int total = 0;
        for (int i = 0; i < recipe.spirits.size(); i++) {
            SpiritAmount sa = recipe.spirits.get(i);
            total += sa.amount();
        }
        total = String.valueOf(total).length();

        var invisibleInput = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (int i = 0; i < recipe.spirits.size(); i++) {
            SpiritAmount spiritAmount = recipe.spirits.get(i);
            //var coeff = SpiritAffinityTracker.getCoeff(Minecraft.getInstance().player.getGameProfile().getName(), spiritAmount.type());
            //var newAmount = (int) (spiritAmount.amount() * coeff);
            var newAmount = spiritAmount.amount();
            if (recipe.input.is(OtherverseItems.IDOL.get())) {
                var et = (EntityType<? extends LivingEntity>) IdolItem.getType(recipe.input);
                newAmount = (int) DefaultAttributes.getSupplier(et).getValue(Attributes.MAX_HEALTH);
            }
            var stack = new ItemStack(Spirits.spiritItems.get(spiritAmount.type()).get(), newAmount);
            builder.addSlot(RecipeIngredientRole.OUTPUT, i * 15 + (total * 6) + 18, 2).addItemStack(stack);
            invisibleInput.addItemStack(stack);
        }
    }

    @Override
    public void draw(SpiritExtractionRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack,
                     double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int total = 0;
        for (int i = 0; i < recipe.spirits.size(); i++) {
            SpiritAmount sa = recipe.spirits.get(i);
            total += sa.amount();
        }

        var totalColor = 0x333333;
        var implementData = ImplementManager.getImplementData(Minecraft.getInstance().player);
        if (!implementData.isEmpty()
                && ForgeRegistries.ITEMS.getValue(new ResourceLocation(implementData.getString("item"))) == Items.BUCKET) {
            total = (int) (total * ImplementManager.BUCKET_BONUS);
            totalColor = ImplementManager.IMPLEMENT_UI_COLOR;
        }

        var totalString = String.valueOf(total);
        if(recipe.input.is(OtherverseItems.IDOL.get())){
            totalString = "";
        }
        mc.font.draw(stack, totalString, 19, 6, totalColor);
        if (ImplementManager.durabilities.containsKey(recipe.input)) {
            blockIcon.draw(stack, 110, 1);
            mc.font.draw(stack, String.valueOf(ImplementManager.durabilities.get(recipe.input)),
                    109, 12, ImplementManager.IMPLEMENT_UI_COLOR);
        }
    }
}
