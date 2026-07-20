package com.shermansplanet.otherverse.integrations.jei;

import com.shermansplanet.otherverse.ItemOrEntityType;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.spirits.SpiritLabeler.SpiritAmount;
import java.util.HashMap;
import java.util.List;

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

public class BindingRecipe implements Recipe<Container> {

  private ResourceLocation id;
  public EntityType<? extends LivingEntity> entityType;
  public HashMap<ItemOrEntityType, Integer> influences;

  public BindingRecipe(ResourceLocation id, EntityType<? extends LivingEntity> entityType,
      HashMap<ItemOrEntityType, Integer> influences) {
    this.id = id;
    this.entityType = entityType;
    this.influences = influences;
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
    return Otherverse.BINDING.get();
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
