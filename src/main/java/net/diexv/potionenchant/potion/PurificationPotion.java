package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class PurificationPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的净化药水
    public static final RegistryObject<Potion> PURIFICATION =
            POTIONS.register("purification",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.PURIFICATION.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_PURIFICATION =
            POTIONS.register("long_purification",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.PURIFICATION.get(), 9600))); // 8分钟
}