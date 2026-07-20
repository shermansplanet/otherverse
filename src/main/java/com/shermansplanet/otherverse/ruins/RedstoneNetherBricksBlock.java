package com.shermansplanet.otherverse.ruins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneNetherBricksBlock extends PressurePlateBlock {
    public RedstoneNetherBricksBlock(Properties p_273571_) {
        super(Sensitivity.EVERYTHING, p_273571_, BlockSetType.STONE);
    }

    protected static final VoxelShape PRESSED_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);
    protected static final VoxelShape AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    protected static final AABB TOUCH_AABB = new AABB(0, 0, 0, 1, 1.25D, 1);

    public VoxelShape getShape(BlockState p_49341_, BlockGetter p_49342_, BlockPos p_49343_, CollisionContext p_49344_) {
        return this.getSignalForState(p_49341_) > 0 ? PRESSED_AABB : AABB;
    }

    protected int getSignalStrength(Level p_55264_, BlockPos p_55265_) {
        return getEntityCount(p_55264_, TOUCH_AABB.move(p_55265_), LivingEntity.class) > 0 ? 15 : 0;
    }

    public boolean canSurvive(BlockState p_49325_, LevelReader p_49326_, BlockPos p_49327_) {
        return true;
    }
}
