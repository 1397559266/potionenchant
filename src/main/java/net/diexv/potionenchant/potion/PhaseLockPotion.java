package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class PhaseLockPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的相位锁定药水
    public static final RegistryObject<Potion> PHASE_LOCK =
            POTIONS.register("phase_lock",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.PHASE_LOCK.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_PHASE_LOCK =
            POTIONS.register("long_phase_lock",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.PHASE_LOCK.get(), 9600))); // 8分钟

    public static final RegistryObject<Potion> STRONG_PHASE_LOCK =
            POTIONS.register("strong_phase_lock",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.PHASE_LOCK.get(), 1800, 1))); // 1.5分钟，等级2
}
