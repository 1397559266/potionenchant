package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ArmorBreakPotion {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    public static final RegistryObject<Potion> ARMOR_BREAK = POTIONS.register("armor_break",
            () -> new Potion(new MobEffectInstance(EffectRegistry.ARMOR_BREAK.get(), 3600)));

    public static final RegistryObject<Potion> LONG_ARMOR_BREAK = POTIONS.register("long_armor_break",
            () -> new Potion(new MobEffectInstance(EffectRegistry.ARMOR_BREAK.get(), 9600)));

    public static final RegistryObject<Potion> STRONG_ARMOR_BREAK = POTIONS.register("strong_armor_break",
            () -> new Potion(new MobEffectInstance(EffectRegistry.ARMOR_BREAK.get(), 1800, 1)));
}
