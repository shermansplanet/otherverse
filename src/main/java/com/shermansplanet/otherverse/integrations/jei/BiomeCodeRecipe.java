package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.spirits.SpiritType;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class BiomeCodeRecipe implements Recipe<Container> {

    private ResourceLocation id;
    public List<SpiritType> spirits;
    public ResourceLocation biome;

    public BiomeCodeRecipe(ResourceLocation id, List<SpiritType> spiritTypes, ResourceLocation biome) {
        this.id = id;
        this.spirits = spiritTypes;
        this.biome = biome;
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
        return Otherverse.BIOME_CODE.get();
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
