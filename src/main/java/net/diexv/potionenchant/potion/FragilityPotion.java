package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FragilityPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的脆弱药水
    public static final RegistryObject<Potion> FRAGILITY =
            POTIONS.register("fragility",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.FRAGILITY.get(), 3600))); // 3分钟

    public static final RegistryObject<Potion> LONG_FRAGILITY =
            POTIONS.register("long_fragility",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.FRAGILITY.get(), 9600))); // 8分钟
}