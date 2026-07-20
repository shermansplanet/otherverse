package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.spirits.SpiritLabeler.SpiritAmount;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class SpiritExtractionRecipe implements Recipe<Container> {

    private ResourceLocation id;
    public List<SpiritAmount> spirits;
    public ItemStack input;

    public SpiritExtractionRecipe(ResourceLocation id, List<SpiritAmount> spiritTypes, ItemStack input) {
        this.id = id;
        this.spirits = spiritTypes;
        this.input = input;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return Otherverse.SPIRIT_EXTRACTION.get();
    }

    // IGNORE BELOW

    @Override
    public boolean matches(Container p_44002_, Level p_44003_) {
        return false;
    }

    @Override
    public ItemStack assemble(Container p_44001_, RegistryAccess p_267165_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess p_267052_) {
        return null;
    }
}
