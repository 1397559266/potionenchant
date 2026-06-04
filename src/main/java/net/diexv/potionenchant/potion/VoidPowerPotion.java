package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VoidPowerPotion {
        public static final DeferredRegister<Potion> POTIONS =
                DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

        // 注册不同等级的虚空之力药水
        public static final RegistryObject<Potion> VOID_POWER =
                POTIONS.register("void_power",
                        () -> new Potion(new MobEffectInstance(EffectRegistry.VOID_POWER.get(), 3600))); // 3分钟

        public static final RegistryObject<Potion> LONG_VOID_POWER =
                POTIONS.register("long_void_power",
                        () -> new Potion(new MobEffectInstance(EffectRegistry.VOID_POWER.get(), 9600))); // 8分钟

        public static final RegistryObject<Potion> STRONG_VOID_POWER =
                POTIONS.register("strong_void_power",
                        () -> new Potion(new MobEffectInstance(EffectRegistry.VOID_POWER.get(), 1800, 1))); // 1.5分钟，等级2
}
