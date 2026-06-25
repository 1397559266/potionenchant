package net.diexv.potionenchant.config;

import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {

    @SuppressWarnings("removal")
    public static void registerConfigs() {
        // Server config - gameplay, auto-synced to clients
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PotionEnchantConfig.SERVER_SPEC, "potionenchant-common.toml");
        // Client config - display/local, not synced
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PotionEnchantConfig.CLIENT_SPEC, "potionenchant-client.toml");
        // Effect and enchantment values - also server-side
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EffectConfigValues.SPEC, "potionenchant-effects.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EnchantmentConfigValues.SPEC, "potionenchant-enchantments.toml");
    }
}
