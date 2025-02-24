package com.shermansplanet.otherverse.diagrams;

import com.shermansplanet.otherverse.spirits.SpiritTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IFocus {

  ItemStack getItem();

  Diagram getDiagram();

  BlockPos getPos();

  void removeItem();

  Level getFocusLevel();

  boolean isBlock();

  void setProcess(DiagramProcess process);

  DiagramProcess getProcess();
}
