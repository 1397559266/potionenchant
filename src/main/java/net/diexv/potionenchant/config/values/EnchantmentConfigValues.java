package net.diexv.potionenchant.config.values;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 附魔数值配置
 * 可设置本模组所有附魔效果的数值参数，默认值为当前硬编码值
 */
public class EnchantmentConfigValues {

    public static final EnchantmentConfig CONFIG;
    public static final ForgeConfigSpec SPEC;

    static {
        final Pair<EnchantmentConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(EnchantmentConfig::new);
        CONFIG = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    public static class EnchantmentConfig {

        // ===== 吸血 (Lifesteal) =====
        public final ForgeConfigSpec.DoubleValue lifestealHealPercentPerLevel;

        // ===== 高级锋利 (Advanced Sharpness) =====
        public final ForgeConfigSpec.DoubleValue advancedSharpnessBaseDamage;
        public final ForgeConfigSpec.DoubleValue advancedSharpnessDamagePerLevel;

        // ===== 高级保护 (Advanced Protection) =====
        public final ForgeConfigSpec.IntValue advancedProtectionPointsPerLevel;

        // ===== 烈焰附加 (Blaze Aspect) =====
        public final ForgeConfigSpec.IntValue blazeAspectFireSecondsPerLevel;

        // ===== 凋零附加 (Wither Aspect) =====
        public final ForgeConfigSpec.IntValue witherAspectWitherSecondsPerLevel;
        public final ForgeConfigSpec.IntValue witherAspectWitherLevel;

        // ===== 魔力聚焦 (Mana Focus) =====
        public final ForgeConfigSpec.DoubleValue manaFocusReductionPerLevel;
        public final ForgeConfigSpec.DoubleValue manaFocusDamageIncreasePerLevel;

        // ===== 药剂克星 (Potion Bane) =====
        public final ForgeConfigSpec.DoubleValue potionBaneDamageMultiplierPerLevel;

        // ===== 伤害储存 (Damage Storage) =====
        public final ForgeConfigSpec.DoubleValue damageStorageMaxMultiplier;
        public final ForgeConfigSpec.IntValue damageStorageDecaySeconds;

        public EnchantmentConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Enchantment Numerical Values Configuration")
                   .push("enchantment_values");

            // --- 吸血 ---
            builder.push("lifesteal");
            lifestealHealPercentPerLevel = builder
                    .comment("每级吸血比例 (0.05 = 5%)", "Heal percent per level", "Default: 0.05")
                    .defineInRange("heal_percent_per_level", 0.05, 0.0, 1.0);
            builder.pop();

            // --- 高级锋利 ---
            builder.push("advanced_sharpness");
            advancedSharpnessBaseDamage = builder
                    .comment("基础伤害加成", "Base damage bonus", "Default: 2.5")
                    .defineInRange("base_damage", 2.5, 0.0, 100.0);
            advancedSharpnessDamagePerLevel = builder
                    .comment("每级额外伤害加成", "Damage bonus per level", "Default: 2.5")
                    .defineInRange("damage_per_level", 2.5, 0.0, 100.0);
            builder.pop();

            // --- 高级保护 ---
            builder.push("advanced_protection");
            advancedProtectionPointsPerLevel = builder
                    .comment("每级保护点数", "Protection points per level", "Default: 5")
                    .defineInRange("protection_points_per_level", 5, 1, 100);
            builder.pop();

            // --- 烈焰附加 ---
            builder.push("blaze_aspect");
            blazeAspectFireSecondsPerLevel = builder
                    .comment("每级着火秒数", "Fire seconds per level", "Default: 4")
                    .defineInRange("fire_seconds_per_level", 4, 1, 100);
            builder.pop();

            // --- 凋零附加 ---
            builder.push("wither_aspect");
            witherAspectWitherSecondsPerLevel = builder
                    .comment("每级凋零效果秒数", "Wither seconds per level", "Default: 4")
                    .defineInRange("wither_seconds_per_level", 4, 1, 100);
            witherAspectWitherLevel = builder
                    .comment("凋零效果等级（0 = 凋零 I）", "Wither effect amplifier level", "Default: 0")
                    .defineInRange("wither_amplifier", 0, 0, 255);
            builder.pop();

            // --- 魔力聚焦 ---
            builder.push("mana_focus");
            manaFocusReductionPerLevel = builder
                    .comment("每级魔法伤害减免 (0.1 = 10%)", "Magic damage reduction per level", "Default: 0.1")
                    .defineInRange("reduction_per_level", 0.1, 0.0, 1.0);
            manaFocusDamageIncreasePerLevel = builder
                    .comment("每级造成的魔法伤害提高 (0.25 = +25%)", "Magic damage increase per level", "Default: 0.25")
                    .defineInRange("damage_increase_per_level", 0.25, 0.0, 10.0);
            builder.pop();

            // --- 药剂克星 ---
            builder.push("potion_bane");
            potionBaneDamageMultiplierPerLevel = builder
                    .comment("每级额外伤害倍率 (1.0 = +100%)", "Extra damage multiplier per level", "Default: 1.0")
                    .defineInRange("damage_multiplier_per_level", 1.0, 0.0, 100.0);
            builder.pop();

            // --- 伤害储存 ---
            builder.push("damage_storage");
            damageStorageMaxMultiplier = builder
                    .comment("最大储存量倍率（最大生命值的倍数）", "Max storage multiplier (× max health)", "Default: 10.0")
                    .defineInRange("max_storage_multiplier", 10.0, 1.0, 1000.0);
            damageStorageDecaySeconds = builder
                    .comment("储存伤害衰减时间（秒，超过后重置为0）", "Storage decay time (seconds)", "Default: 60")
                    .defineInRange("decay_seconds", 60, 1, 3600);
            builder.pop();

            builder.pop(); // enchantment_values
        }
    }

    private EnchantmentConfigValues() {}
}
