package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SymbiosisPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    public static final RegistryObject<Potion> SYMBIOSIS =
            POTIONS.register("grudge",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.SYMBIOSIS.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_SYMBIOSIS =
            POTIONS.register("long_grudge",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.SYMBIOSIS.get(), 9600))); // 8分钟
}
