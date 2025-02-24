package com.shermansplanet.otherverse.sympathy;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SelectorBlock extends ColorableBlock{
    public static final BooleanProperty filled = BooleanProperty.create("filled");
    public SelectorBlock(Properties p_49795_) {
        super(p_49795_);
        registerDefaultState(stateDefinition.any().setValue(color, DyeColor.WHITE).setValue(filled, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(filled);
    }
}
