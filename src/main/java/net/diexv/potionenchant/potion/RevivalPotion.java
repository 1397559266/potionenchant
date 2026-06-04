package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RevivalPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 注册不同等级的重生药水
    public static final RegistryObject<Potion> REVIVAL =
            POTIONS.register("revival",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.REVIVAL.get(), 3600, 0))); // 3分钟，1次重生

    public static final RegistryObject<Potion> LONG_REVIVAL =
            POTIONS.register("long_revival",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.REVIVAL.get(), 9600, 0))); // 8分钟，1次重生

    public static final RegistryObject<Potion> STRONG_REVIVAL =
            POTIONS.register("strong_revival",
                    () -> new Potion(new MobEffectInstance(EffectRegistry.REVIVAL.get(), 1800, 1))); // 1.5分钟，2次重生
}