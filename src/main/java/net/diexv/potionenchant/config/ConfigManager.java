package net.diexv.potionenchant.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {

    @SuppressWarnings("removal")
    public static void registerConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PotionEnchantConfig.COMMON_SPEC, "potionenchant-common.toml");
    }
}
