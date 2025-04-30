package com.shermansplanet.otherverse.integrations.jei;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.PracticeWorldManager;
import com.shermansplanet.otherverse.PracticeWorldUpdateMessage;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.SpiritTransfusions;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

@JeiPlugin
public class OtherverseJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static IRecipeRegistration registry;
    private static final ResourceLocation UID = new ResourceLocation(Otherverse.MODID, "jei_plugin");

    public static void handleWorldUpdate(PracticeWorldUpdateMessage msg, Supplier<NetworkEvent.Context> ctx) {
        SpiritLabeler.SPIRIT_TYPE_OF.data = msg.spiritMappings();
        SpiritTransfusions.ALL_SPIRIT_TRANSFUSIONS.data = msg.spiritTransfusions();
        MobTransfusions.ALL_MOB_TRANSFUSIONS.data = msg.mobTransfusions();
        MobBindingInfluenceUtils.putFoodsAndInfluences(msg.bindings());
        MobBindingInfluenceUtils.mobSpirits = msg.mobSpirits();
        PracticeWorldManager.worldSetUp = true;
        if (PracticeWorldManager.noJeiPending) {
            addPracticeRecipes();
        }
    }

    public static void tryAddEgg(IRecipeLayoutBuilder builder, ItemStack input) {
        if (!input.is(OtherverseItems.IDOL.get())) return;
        var egg = ForgeSpawnEggItem.fromEntityType(IdolItem.getType(input));
        if (egg == null) return;
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(egg.getDefaultInstance());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(egg.getDefaultInstance());
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(OtherverseItems.IDOL.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(
                new SpiritExtractionRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCategories(
                new BindingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCategories(
                new TransfusionRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        registry = r;
        if (!PracticeWorldManager.worldSetUp) {
            PracticeWorldManager.noJeiPending = true;
            return;
        }
        addPracticeRecipes();
    }

    public static void addPracticeRecipes() {
        registry.addRecipes(SpiritExtractionRecipeCategory.TYPE, SpiritLabeler.GenerateRecipes());
        registry.addRecipes(TransfusionRecipeCategory.TYPE, SpiritTransfusions.GenerateRecipes());
        registry.addRecipes(TransfusionRecipeCategory.TYPE, MobTransfusions.GenerateRecipes());
        registry.addRecipes(BindingRecipeCategory.TYPE, MobBindingInfluenceUtils.GenerateRecipes());
    }
}
