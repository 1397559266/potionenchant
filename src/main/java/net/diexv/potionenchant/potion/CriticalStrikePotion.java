package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class CriticalStrikePotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的暴击药水
    public static final RegistryObject<Potion> CRITICAL_STRIKE =
            POTIONS.register("critical_strike",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.CRITICAL_STRIKE.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_CRITICAL_STRIKE =
            POTIONS.register("long_critical_strike",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.CRITICAL_STRIKE.get(), 9600))); // 8分钟

    public static final RegistryObject<Potion> STRONG_CRITICAL_STRIKE =
            POTIONS.register("strong_critical_strike",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.CRITICAL_STRIKE.get(), 1800, 1))); // 1.5分钟，等级2
}