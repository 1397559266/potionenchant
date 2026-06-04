package net.diexv.potionenchant;

import net.diexv.potionenchant.enchantment.PotionEnchantment;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EnchantmentRegistry {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, PotionEnchantMod.MODID);

    // 这里注册一些基础的药水附魔，但实际使用时会在铁砧中动态创建
    public static final RegistryObject<Enchantment> EXAMPLE_POTION_ENCHANT =
            ENCHANTMENTS.register("example_potion",
                    () -> new PotionEnchantment(MobEffects.MOVEMENT_SPEED, 1, true));
}