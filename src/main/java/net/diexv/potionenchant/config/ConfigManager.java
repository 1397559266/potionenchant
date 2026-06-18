package net.diexv.potionenchant.config;

import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {

    @SuppressWarnings("removal")
    public static void registerConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PotionEnchantConfig.COMMON_SPEC, "potionenchant-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EffectConfigValues.SPEC, "potionenchant-effects.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EnchantmentConfigValues.SPEC, "potionenchant-enchantments.toml");
    }
}
