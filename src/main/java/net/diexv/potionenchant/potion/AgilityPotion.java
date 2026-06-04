package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AgilityPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 敏捷药水 (原加速药水)
    public static final RegistryObject<Potion> AGILITY = POTIONS.register("agility",
            () -> new Potion(new MobEffectInstance(EffectRegistry.AGILITY.get(), 3600)));

    public static final RegistryObject<Potion> LONG_AGILITY = POTIONS.register("long_agility",
            () -> new Potion(new MobEffectInstance(EffectRegistry.AGILITY.get(), 9600)));

    public static final RegistryObject<Potion> STRONG_AGILITY = POTIONS.register("strong_agility",
            () -> new Potion(new MobEffectInstance(EffectRegistry.AGILITY.get(), 1800, 1)));
}
