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

    /** 苦力怕药水罐：外观由 BlockEntityRenderer 用缩放苦力怕模型渲染 */
    public static final RegistryObject<Block> DIEXV_CREEPER_TANK = BLOCKS.register("diexv_creeper_tank",
        () -> new DiexvCreeperTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .strength(2.0F, 6.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)
            .noOcclusion()));
}
