package com.shermansplanet.otherverse.diagrams;

import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;

public record PowerSource(Integer unitsOfPower, Integer powerPerUnit, Consumer<Integer> drainPower, EntityType<?> entityType) {
  public static final int POWER_FROM_SELF = 3;

    public String out() {
    return unitsOfPower + ", " + powerPerUnit;
  }

  public Integer totalPower(){
    return unitsOfPower * powerPerUnit;
  }
}
