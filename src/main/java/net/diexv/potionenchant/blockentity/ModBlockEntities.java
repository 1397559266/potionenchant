package net.diexv.potionenchant.blockentity;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PotionEnchantMod.MODID);

    public static final RegistryObject<BlockEntityType<PotionEnchantingTableBlockEntity>> POTION_ENCHANTING_TABLE =
        BLOCK_ENTITIES.register("potion_enchanting_table",
            () -> BlockEntityType.Builder.of(
                PotionEnchantingTableBlockEntity::new,
                getBlock("potion_enchanting_table")
            ).build(null));

    @SuppressWarnings("removal")
    private static Block getBlock(String name) {
        return ForgeRegistries.BLOCKS.getValue(
            new net.minecraft.resources.ResourceLocation(PotionEnchantMod.MODID, name)
        );
    }
}
