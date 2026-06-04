package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SiphonPotion {

        public static final DeferredRegister<Potion> POTIONS =
                DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

        public static final RegistryObject<Potion> SIPHON = POTIONS.register("siphon",
                () -> new Potion(new MobEffectInstance(EffectRegistry.SIPHON.get(), 3600))); // 3分钟

        public static final RegistryObject<Potion> LONG_SIPHON = POTIONS.register("long_siphon",
                () -> new Potion(new MobEffectInstance(EffectRegistry.SIPHON.get(), 9600))); // 8分钟

        public static final RegistryObject<Potion> STRONG_SIPHON = POTIONS.register("strong_siphon",
                () -> new Potion(new MobEffectInstance(EffectRegistry.SIPHON.get(), 1800, 1)));
}
