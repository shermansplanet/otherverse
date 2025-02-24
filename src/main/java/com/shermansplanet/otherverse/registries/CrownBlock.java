package com.shermansplanet.otherverse.registries;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CrownBlock extends Block {
    public static final BooleanProperty demesne = BooleanProperty.create("demesne");
    public CrownBlock(Properties p_49795_) {
        super(p_49795_);
        registerDefaultState(stateDefinition.any().setValue(demesne, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(demesne);
    }
}
