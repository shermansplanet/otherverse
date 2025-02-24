package com.shermansplanet.otherverse.spirits;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class SimpleRecipeType<T extends Recipe<?>> implements RecipeType<T> {

  private final String identifier;

  public SimpleRecipeType(String identifier) {
    this.identifier = identifier;
  }

  public String getId() {
    return identifier;
  }

  @Override
  public String toString() {
    return identifier;
  }
}
