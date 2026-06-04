package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RangeExtensionPotion {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 距离扩展药水
    public static final RegistryObject<Potion> RANGE_EXTENSION = POTIONS.register("range_extension",
            () -> new Potion(new MobEffectInstance(EffectRegistry.RANGE_EXTENSION.get(), 3600)));

    public static final RegistryObject<Potion> LONG_RANGE_EXTENSION = POTIONS.register("long_range_extension",
            () -> new Potion(new MobEffectInstance(EffectRegistry.RANGE_EXTENSION.get(), 9600)));

    public static final RegistryObject<Potion> STRONG_RANGE_EXTENSION = POTIONS.register("strong_range_extension",
            () -> new Potion(new MobEffectInstance(EffectRegistry.RANGE_EXTENSION.get(), 1800, 1)));
}
