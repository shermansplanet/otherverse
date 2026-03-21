package com.shermansplanet.otherverse.integrations.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.spirits.Spirits;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

public class BindingRecipeCategory implements IRecipeCategory<BindingRecipe> {

    public static final RecipeType<BindingRecipe> TYPE =
            RecipeType.create(Otherverse.MODID, "binding", BindingRecipe.class);

    private final IDrawable background;
    private final Component localizedName;
    private final IDrawable icon;
    private final IDrawable heartIcon, chainIcon, bindingBreakIcon;
    private final IGuiHelper guiHelper;

    public BindingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 80);
        localizedName = Component.translatable("otherverse.jei.binding");
        heartIcon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                0, 11, 5, 5);
        chainIcon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                15, 11, 5, 7);
        bindingBreakIcon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                22, 11, 7, 7);
        icon = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "textures/gui/jei.png"),
                63, 0, 16, 16);
        this.guiHelper = guiHelper;
    }

    @Override
    public RecipeType<BindingRecipe> getRecipeType() {
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

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BindingRecipe recipe, IFocusGroup focuses) {
        var idol = MobBindingInfluenceUtils.getIdol(recipe.entityType);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 2, 2).addItemStack(idol);
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(idol);
        OtherverseJeiPlugin.tryAddEgg(builder, idol);
        int row = 0;
        int column = 0;
        HashMap<Integer, IRecipeSlotBuilder> slotsByAmount = new HashMap<>();
        var uniqueInfluences = recipe.influences.values().stream().distinct().count();
        var columns = Math.ceil(uniqueInfluences / 5F);
        for (var influence : recipe.influences.entrySet()) {
            var stack = influence.getKey().getItemStack();
            OtherverseJeiPlugin.tryAddEgg(builder, stack);
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(stack);
            if (slotsByAmount.containsKey(influence.getValue())) {
                slotsByAmount.get(influence.getValue()).addItemStack(stack);
                if (stack.is(Items.WATER_BUCKET))
                    slotsByAmount.get(influence.getValue()).addFluidStack(Fluids.WATER, 1000);
                continue;
            }
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, column * 22 + 2, row++ * 30 + 24);
            slot.addItemStack(stack);
            slotsByAmount.put(influence.getValue(), slot);
            if (stack.is(Items.WATER_BUCKET)) slot.addFluidStack(Fluids.WATER, 1000);
            if (row >= columns) {
                row = 0;
                column++;
            }
        }
    }

    @Override
    public void draw(BindingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        int row = 0;
        int column = 0;
        HashSet<Integer> takenAmounts = new HashSet<>();
        var uniqueInfluences = recipe.influences.values().stream().distinct().count();
        var columns = Math.ceil(uniqueInfluences / 5F);
        for (var influence : recipe.influences.entrySet()) {
            if (takenAmounts.contains(influence.getValue())) continue;
            takenAmounts.add(influence.getValue());
            float val = (float) influence.getValue();
            IDrawable icon = chainIcon;
            if (val < 0) {
                val = -val;
                icon = heartIcon;
            }
            var spiritType = MobBindingInfluenceUtils.mobSpirits.get(recipe.entityType);
            if (spiritType != null && Spirits.spiritItems.get(spiritType).get() == influence.getKey().item) {
                val = 0.5f;
            }
            String s = Float.toString(val).replace(".0", "");
            if (val > 999) {
                s = "";
            }
            int x = column * 22 + 10 - (s.length() + 1) * 3;
            int y = row++ * 30 + 42;
            icon.draw(graphics, x, val >= 0 ? y : y + 1);
            graphics.drawString(mc.font, s, x + 6, y, 0x333333);
            if (row >= columns) {
                row = 0;
                column++;
            }
        }
        graphics.drawString(mc.font, recipe.entityType.getDescription().getString(), 22, 3, 0x333333);
        int hp = (int) DefaultAttributes.getSupplier(recipe.entityType).getValue(Attributes.MAX_HEALTH);
        String heartstring = Float.toString(hp).replace(".0", "");
        heartIcon.draw(graphics, 22, 13);
        graphics.drawString(mc.font, heartstring, 28, 12, 0x333333);
        int x = 30 + heartstring.length() * 6;
        chainIcon.draw(graphics, x, 12);
        int coeff = 3;
        int color = 0x333333;
        var implementData = ImplementManager.getImplementData(Minecraft.getInstance().player);
        if (!implementData.isEmpty() && ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(implementData.getString("item"))) == Items.CHAIN) {
            coeff = 2;
            color = ImplementManager.IMPLEMENT_UI_COLOR;
        }
        var s = Integer.toString(hp * coeff).replace(".0", "");
        graphics.drawString(mc.font, s, x + 6, 12, color);
        if (BindingManager.drainsBindings(recipe.entityType)) {
            x += (s.length() + 1) * 6 + 2;
            bindingBreakIcon.draw(graphics, x, 12);
            s = BindingManager.getSpiritDrain(hp) + "/" + BindingManager.getBindingWearInterval(hp) + "s";
            graphics.drawString(mc.font, s, x + 8, 12, color);
        }
        RenderSystem.disableBlend();
    }
}
