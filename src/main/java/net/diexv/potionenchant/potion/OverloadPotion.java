package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class OverloadPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的负载药水
    public static final RegistryObject<Potion> OVERLOAD =
            POTIONS.register("overload",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.OVERLOAD.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_OVERLOAD =
            POTIONS.register("long_overload",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.OVERLOAD.get(), 9600))); // 8分钟

    public static final RegistryObject<Potion> STRONG_OVERLOAD =
            POTIONS.register("strong_overload",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.OVERLOAD.get(), 1800, 1))); // 1.5分钟，等级2
}