package net.diexv.potionenchant.craft;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, PotionEnchantMod.MODID);

    public static final RegistryObject<RecipeSerializer<RainbowPotionEssenceRecipe>> RAINBOW_ESSENCE =
            SERIALIZERS.register("rainbow_essence", () -> RainbowPotionEssenceRecipe.SERIALIZER);

    private ModRecipeSerializers() {
    }
}
