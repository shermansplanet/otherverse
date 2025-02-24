package com.shermansplanet.otherverse.spirits;

public record SpiritType(String label, int id) {
public String GetResourceLocation(){
  return "spirit_" + label.replace(" ","_");
}
}
