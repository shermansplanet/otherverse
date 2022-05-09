package com.loren.testmod.init;

import com.loren.testmod.TestMod;
import com.loren.testmod.blocks.ChalkLineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TestMod.MOD_ID);

    public static final RegistryObject<Block> CHALK_LINE = BLOCKS.register("chalk_line",
            () -> new ChalkLineBlock(BlockBehaviour.Properties.of(Material.STONE).strength(2F)));

    public static final RegistryObject<Block> CALCITE_BLOCK = BLOCKS.register("calcite",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f, 6.0F).requiresCorrectToolForDrops()));
}