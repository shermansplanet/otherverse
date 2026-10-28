package com.shermansplanet.otherverse.spirits;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record SpiritType(String label, int id) {
public String GetResourceLocation(){
  return "spirit_" + label.replace(" ","_");
}

public MutableComponent getDisplayName(){
  return getDisplayName(label);
}

public static MutableComponent getDisplayName(String label){
  return Component.translatable("otherverse.spirit_type." + label);
}
}
