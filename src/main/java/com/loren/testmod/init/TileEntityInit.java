package com.loren.testmod.init;

import com.loren.testmod.TestMod;
import com.loren.testmod.tiles.ChalkLineTile;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TileEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, TestMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ChalkLineTile>> CHALK_LINE = TILE_ENTITY_TYPES.register("chalk_line",
            () -> BlockEntityType.Builder.of(ChalkLineTile::new, BlockInit.CHALK_LINE.get()).build(null));

}
