package com.shermansplanet.otherverse.registries;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.artifacts.BiomeBrazierBlock;
import com.shermansplanet.otherverse.artifacts.SpawnAltarBlock;
import com.shermansplanet.otherverse.demesnes.DemesnesBeaconBlock;
import com.shermansplanet.otherverse.demesnes.DemesnesPortalBlock;
import com.shermansplanet.otherverse.diagrams.ChalkLineBlock;
import com.shermansplanet.otherverse.diagrams.SlateScaffoldingBlock;
import com.shermansplanet.otherverse.ruins.RedstoneNetherBricksBlock;
import com.shermansplanet.otherverse.sympathy.FateWebBlock;
import com.shermansplanet.otherverse.sympathy.SelectorBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class OtherverseBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Otherverse.MODID);
    public static final RegistryObject<Block> CHALK_LINE = BLOCKS.register("chalk_line",
            ChalkLineBlock::new);
    public static final RegistryObject<Block> WEB_OF_FATE = BLOCKS.register("web_of_fate",
            () -> new FateWebBlock(BlockBehaviour.Properties.copy(Blocks.TRIPWIRE)));
    public static final RegistryObject<Block> SELECTOR = BLOCKS.register("selector",
            () -> new SelectorBlock(BlockBehaviour.Properties.copy(Blocks.AIR).noCollission()));
    public static final RegistryObject<Block> FAMILIAR_CROWN = BLOCKS.register("familiar_crown",
            () -> new CrownBlock(BlockBehaviour.Properties.copy(Blocks.AIR)));
    public static final RegistryObject<Block> SLATE_SCAFFOLDING = BLOCKS
            .register("slate_scaffolding", () -> new SlateScaffoldingBlock(
                    BlockBehaviour.Properties.copy(Blocks.STONE).noCollission().instabreak()));
    public static final RegistryObject<Block> SALT_CRYSTALS = BLOCKS.register("salt_crystals",
            () -> new AmethystClusterBlock(7, 3,
                    BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).forceSolidOn().noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER).lightLevel(x -> 0).strength(0.3F).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SULFUR_ORE = BLOCKS.register("sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHER_QUARTZ_ORE)));
    /*public static final RegistryObject<Block> BEAST_SKULL = BLOCKS.register("beastskull",
            () -> new AmethystClusterBlock(7, 3,
                    BlockBehaviour.Properties.copy(Blocks.BONE_BLOCK)));*/
    public static final RegistryObject<Block> SPAWN_ALTAR = BLOCKS.register("spawn_altar",
            () -> new SpawnAltarBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BARS)));
    public static final RegistryObject<Block> BIOME_BRAZIER = BLOCKS.register("biome_brazier",
            () -> new BiomeBrazierBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BARS).lightLevel(
                    (p_50763_) -> (p_50763_.getValue(BlockStateProperties.LIT) || p_50763_.getValue(BiomeBrazierBlock.SCRY)) ? 15 : 0)));
    public static final RegistryObject<Block> CINNABAR_BLOCK = BLOCKS.register("cinnabar_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)));
    public static final RegistryObject<Block> DEMESNE_BEACON = BLOCKS.register("demesne_beacon",
            () -> new DemesnesBeaconBlock(BlockBehaviour.Properties.copy(Blocks.BEACON)));
    public static final RegistryObject<Block> DEMESNE_PORTAL = BLOCKS.register("demesne_portal",
            () -> new DemesnesPortalBlock(BlockBehaviour.Properties.copy(Blocks.AIR)));
    public static final RegistryObject<Block> REDSTONE_NETHER_BRICKS = BLOCKS.register("redstone_nether_bricks",
            () -> new RedstoneNetherBricksBlock(BlockBehaviour.Properties.copy(Blocks.RED_NETHER_BRICKS)));

    public static final RegistryObject<Block> PLUM_BRICKS = BLOCKS.register("plum_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<Block> ANTI_PURPUR_BLOCK = BLOCKS.register("anti_purpur_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.PURPUR_BLOCK)));
    public static final RegistryObject<Block> BLANDSTONE = BLOCKS.register("blandstone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)));
    public static final RegistryObject<Block> BLANDESITE = BLOCKS.register("blandesite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.ANDESITE)));
    public static final RegistryObject<Block> SERPENT_SOIL = BLOCKS.register("serpent_soil",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SOUL_SOIL)));
    public static final RegistryObject<Block> COOL_BIRCH_PLANKS = BLOCKS.register("cool_birch_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
    public static final RegistryObject<Block> ROYAL_BRICKS = BLOCKS.register("royal_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE_BRICKS)));
    public static final RegistryObject<Block> VERDANT_MUD = BLOCKS.register("verdant_mud",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD)));
    public static final RegistryObject<Block> FOOLS_EMERALD = BLOCKS.register("fools_emerald",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)));
    public static final RegistryObject<Block> SKIN_CORAL_BLOCK = BLOCKS.register("skin_coral_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRAIN_CORAL_BLOCK)));
    public static final RegistryObject<Block> FORBIDDEN_CANDY_BLOCK = BLOCKS.register("forbidden_candy_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK)));
    public static final RegistryObject<Block> PASTEL_QUARTZ_BRICKS = BLOCKS.register("pastel_quartz_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.QUARTZ_BRICKS)));
    public static final RegistryObject<Block> BURGUNDY_TILES = BLOCKS.register("burgundy_tiles",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES)));
    public static final RegistryObject<Block> CHISELED_PLUMSTONE = BLOCKS.register("chiseled_plumstone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS)));
    public static final RegistryObject<Block> GHOSTLY_NETHERRACK = BLOCKS.register("ghostly_netherrack",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERRACK)));
    public static final RegistryObject<Block> MUSTARD_DIRT = BLOCKS.register("mustard_dirt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));

    public static final RegistryObject<Block> INTESTINES = BLOCKS.register("intestines",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD)));
    public static final RegistryObject<Block> TRUCHET = BLOCKS.register("truchet",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BLACKSTONE)));
    public static final RegistryObject<Block> GILDED_TRUCHET = BLOCKS.register("gilded_truchet",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GILDED_BLACKSTONE)));
    public static final RegistryObject<Block> CANDYCANE = BLOCKS.register("candycane",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));
}
