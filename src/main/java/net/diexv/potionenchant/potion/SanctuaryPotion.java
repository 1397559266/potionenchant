package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SanctuaryPotion {

        public static final DeferredRegister<Potion> POTIONS =
                DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

        // 注册不同等级的圣洁药水
        public static final RegistryObject<Potion> SANCTUARY =
                POTIONS.register("sanctuary",
                        () -> new Potion(new MobEffectInstance(EffectRegistry.SANCTUARY.get(), 3600))); // 3分钟

        public static final RegistryObject<Potion> LONG_SANCTUARY =
                POTIONS.register("long_sanctuary",
                        () -> new Potion(new MobEffectInstance(EffectRegistry.SANCTUARY.get(), 9600))); // 8分钟
}
