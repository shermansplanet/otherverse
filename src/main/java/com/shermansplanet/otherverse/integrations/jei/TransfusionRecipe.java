package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.Otherverse;

import java.util.HashMap;
import java.util.HashSet;

import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class TransfusionRecipe implements Recipe<Container> {

    private ResourceLocation id;

    public final ItemStack itemFrom;
    public final ItemStack itemTo;
    public final HashSet<ItemStack> transfusionSources;
    public final int cost;
    public boolean isMob;

    public TransfusionRecipe(ResourceLocation id, HashSet<ItemStack> transfusionSources, ItemStack itemFrom,
                             ItemStack itemTo, int cost) {
        this.id = id;
        this.itemFrom = itemFrom;
        this.itemTo = itemTo;
        this.cost = cost;
        this.transfusionSources = transfusionSources;
        for (var source : transfusionSources){
            isMob = source.is(OtherverseItems.IDOL.get());
            break;
        }
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
        return Otherverse.TRANSFUSION.get();
    }

    // IGNORE BELOW

    @Override
    public boolean matches(Container p_44002_, Level p_44003_) {
        return false;
    }

    @Override
    public ItemStack assemble(Container p_44001_, RegistryAccess p_267165_) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess p_267052_) {
        return ItemStack.EMPTY;
    }
}