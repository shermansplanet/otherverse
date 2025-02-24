package com.shermansplanet.otherverse.spirits;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class
Spirits {

  public static int spiritIdCounter = 0;

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final HashMap<SpiritType, RegistryObject<Item>> spiritItems = new HashMap<>();
  public static final List<SpiritType> allSpiritTypes = new LinkedList<>();
  public static final HashMap<String, SpiritType> spiritsByLabel = new HashMap<>();
  public static final HashMap<Integer, SpiritType> spiritsById = new HashMap<>();

  public static final SpiritType EARTH = register("earth");
  public static final SpiritType AIR = register("air");
  public static final SpiritType FIRE = register("fire");
  public static final SpiritType WATER = register("water");

  public static final SpiritType PHLOGISTON = register("phlogiston");
  public static final SpiritType PROTECTION = register("protection");
  public static final SpiritType FOOD = register("food");
  public static final SpiritType FLESH = register("flesh");
  public static final SpiritType LIGHT = register("light");
  public static final SpiritType TECH = register("technology");
  public static final SpiritType DARK = register("dark");
  public static final SpiritType COLD = register("cold");

  public static final SpiritType OVERWORLD = register("overworld");
  public static final SpiritType NETHER = register("nether");
  public static final SpiritType END = register("end");

  public static final SpiritType DEATH = register("death");
  public static final SpiritType WAR = register("war");
  public static final SpiritType NATURE = register("nature");
  public static final SpiritType FORTUNE = register("fortune");
  public static final SpiritType FATE = register("fate");
  public static final SpiritType TIME = register("time");

  public static final SpiritType COLOR_WHITE = register("white");
  public static final SpiritType COLOR_LIGHT_GRAY = register("light_gray");
  public static final SpiritType COLOR_GRAY = register("gray");
  public static final SpiritType COLOR_BLACK = register("black");
  public static final SpiritType COLOR_BROWN = register("brown");
  public static final SpiritType COLOR_RED = register("red");
  public static final SpiritType COLOR_ORANGE = register("orange");
  public static final SpiritType COLOR_YELLOW = register("yellow");
  public static final SpiritType COLOR_LIME = register("lime");
  public static final SpiritType COLOR_GREEN = register("green");
  public static final SpiritType COLOR_CYAN = register("cyan");
  public static final SpiritType COLOR_LIGHT_BLUE = register("light_blue");
  public static final SpiritType COLOR_BLUE = register("blue");
  public static final SpiritType COLOR_PURPLE = register("purple");
  public static final SpiritType COLOR_MAGENTA = register("magenta");
  public static final SpiritType COLOR_PINK = register("pink");

  public static SpiritType[] colorSpiritTypes = new SpiritType[]{
          COLOR_WHITE, COLOR_LIGHT_GRAY, COLOR_GRAY, COLOR_BLACK,
          COLOR_BROWN, COLOR_RED, COLOR_ORANGE, COLOR_YELLOW,
          COLOR_LIME, COLOR_GREEN, COLOR_CYAN, COLOR_LIGHT_BLUE,
          COLOR_BLUE, COLOR_PURPLE, COLOR_MAGENTA, COLOR_PINK
  };

  public static HashMap<DyeColor, SpiritType> colorsByDye = new HashMap<>();

  static {
    colorsByDye.put(DyeColor.WHITE, COLOR_WHITE);
    colorsByDye.put(DyeColor.LIGHT_GRAY, COLOR_LIGHT_GRAY);
    colorsByDye.put(DyeColor.GRAY, COLOR_GRAY);
    colorsByDye.put(DyeColor.BLACK, COLOR_BLACK);
    colorsByDye.put(DyeColor.BROWN, COLOR_BROWN);
    colorsByDye.put(DyeColor.RED, COLOR_RED);
    colorsByDye.put(DyeColor.ORANGE, COLOR_ORANGE);
    colorsByDye.put(DyeColor.YELLOW, COLOR_YELLOW);
    colorsByDye.put(DyeColor.LIME, COLOR_LIME);
    colorsByDye.put(DyeColor.GREEN, COLOR_GREEN);
    colorsByDye.put(DyeColor.CYAN, COLOR_CYAN);
    colorsByDye.put(DyeColor.LIGHT_BLUE, COLOR_LIGHT_BLUE);
    colorsByDye.put(DyeColor.BLUE, COLOR_BLUE);
    colorsByDye.put(DyeColor.PURPLE, COLOR_PURPLE);
    colorsByDye.put(DyeColor.MAGENTA, COLOR_MAGENTA);
    colorsByDye.put(DyeColor.PINK, COLOR_PINK);
  }

  private static SpiritType register(String label) {
    int id = spiritIdCounter++;
    SpiritType spiritType = new SpiritType(label, id);
    allSpiritTypes.add(spiritType);
    spiritsByLabel.put(label, spiritType);
    spiritsById.put(id, spiritType);
    return spiritType;
  }
}
