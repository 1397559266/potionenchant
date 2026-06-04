package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MagicResistancePotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 魔法抗性药水
    public static final RegistryObject<Potion> MAGIC_RESISTANCE = POTIONS.register("magic_resistance",
            () -> new Potion(new MobEffectInstance(EffectRegistry.MAGIC_RESISTANCE.get(), 3600)));

    public static final RegistryObject<Potion> LONG_MAGIC_RESISTANCE = POTIONS.register("long_magic_resistance",
            () -> new Potion(new MobEffectInstance(EffectRegistry.MAGIC_RESISTANCE.get(), 9600)));

    public static final RegistryObject<Potion> STRONG_MAGIC_RESISTANCE = POTIONS.register("strong_magic_resistance",
            () -> new Potion(new MobEffectInstance(EffectRegistry.MAGIC_RESISTANCE.get(), 1800, 1)));
}