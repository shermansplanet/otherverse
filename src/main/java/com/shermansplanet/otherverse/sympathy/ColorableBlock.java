package com.shermansplanet.otherverse.sympathy;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ColorableBlock extends Block {
    public static final EnumProperty<DyeColor> color = EnumProperty.create("color", DyeColor.class);

    public ColorableBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(color);
    }
}
