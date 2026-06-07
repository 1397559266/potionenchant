package net.diexv.potionenchant.block;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, PotionEnchantMod.MODID);

    public static final RegistryObject<Block> POTION_ENCHANTING_TABLE = BLOCKS.register("potion_enchanting_table",
        () -> new PotionEnchantingTableBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(5.0F, 1200.0F)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    public static final RegistryObject<Block> ULTIMATE_ENCHANT_TABLE = BLOCKS.register("ultimate_enchant_table",
        () -> new UltimateEnchantTableBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLUE)
            .strength(5.0F, 1200.0F)
            .requiresCorrectToolForDrops()
            .noOcclusion()));
}
