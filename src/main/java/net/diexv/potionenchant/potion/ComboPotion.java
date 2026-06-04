package net.diexv.potionenchant.potion;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ComboPotion {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, PotionEnchantMod.MODID);

    // 连招药水
    public static final RegistryObject<Potion> COMBO = POTIONS.register("combo",
            () -> new Potion(new MobEffectInstance(EffectRegistry.COMBO.get(), 3600)));

    public static final RegistryObject<Potion> LONG_COMBO = POTIONS.register("long_combo",
            () -> new Potion(new MobEffectInstance(EffectRegistry.COMBO.get(), 9600)));
}
