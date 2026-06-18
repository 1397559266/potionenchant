package net.diexv.potionenchant.config.values;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 药水效果数值配置
 * 可设置本模组所有药水效果的数值参数，默认值为当前硬编码值
 */
public class EffectConfigValues {

    public static final EffectConfig CONFIG;
    public static final ForgeConfigSpec SPEC;

    static {
        final Pair<EffectConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(EffectConfig::new);
        CONFIG = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    public static class EffectConfig {

        // ===== 暴击 (Critical Strike) =====
        public final ForgeConfigSpec.DoubleValue criticalStrikeBaseChance;
        public final ForgeConfigSpec.DoubleValue criticalStrikeChancePerLevel;
        public final ForgeConfigSpec.DoubleValue criticalStrikeDamageMultiplier;

        // ===== 虚空之力 (Void Power) =====
        public final ForgeConfigSpec.DoubleValue voidPowerDamagePerLevel;

        // ===== 碎甲 (Armor Break) =====
        public final ForgeConfigSpec.DoubleValue armorBreakIgnorePerLevel;
        public final ForgeConfigSpec.DoubleValue armorBreakDurabilityPerLevel;

        // ===== 易伤 (Vulnerability) =====
        public final ForgeConfigSpec.DoubleValue vulnerabilityDamagePerLevel;

        // ===== 虹吸 (Siphon) =====
        public final ForgeConfigSpec.DoubleValue siphonLifestealBase;
        public final ForgeConfigSpec.DoubleValue siphonLifestealPerLevel;

        // ===== 修补 (Mending) =====
        public final ForgeConfigSpec.IntValue mendingRepairPerLevel;

        // ===== 相位锁定 (Phase Lock) =====
        public final ForgeConfigSpec.DoubleValue phaseLockDamagePerLevel;

        // ===== 坚定 (Firmness) =====
        public final ForgeConfigSpec.DoubleValue firmnessMaxDamageBase;
        public final ForgeConfigSpec.DoubleValue firmnessMaxDamagePerLevel;
        public final ForgeConfigSpec.DoubleValue firmnessLockDurationBase;
        public final ForgeConfigSpec.DoubleValue firmnessLockDurationPerLevel;

        // ===== 脆弱 (Fragility) =====
        public final ForgeConfigSpec.DoubleValue fragilityDamagePerTick;

        // ===== 魔法抗性 (Magic Resistance) =====
        public final ForgeConfigSpec.DoubleValue magicResistanceReductionPerLevel;
        public final ForgeConfigSpec.DoubleValue magicResistanceMaxReduction;

        // ===== 敏捷 (Agility) =====
        public final ForgeConfigSpec.DoubleValue agilityMovementSpeedPerLevel;
        public final ForgeConfigSpec.DoubleValue agilityAttackSpeedPerLevel;

        // ===== 距离扩展 (Range Extension) =====
        public final ForgeConfigSpec.DoubleValue rangeExtensionIncreasePerLevel;

        // ===== 负载 (Overload) =====
        public final ForgeConfigSpec.IntValue overloadMaxAmplifierBeforeExplosion;
        public final ForgeConfigSpec.DoubleValue overloadExplosionPower;
        public final ForgeConfigSpec.DoubleValue overloadAreaDamageRadius;

        public EffectConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Potion Effect Numerical Values Configuration")
                   .push("effect_values");

            // --- 暴击 ---
            builder.push("critical_strike");
            criticalStrikeBaseChance = builder
                    .comment("暴击基础触发概率 (0.0 ~ 1.0)", "Base critical strike chance", "Default: 0.2")
                    .defineInRange("base_chance", 0.2, 0.0, 1.0);
            criticalStrikeChancePerLevel = builder
                    .comment("每级暴击概率增加", "Critical strike chance per level", "Default: 0.1")
                    .defineInRange("chance_per_level", 0.1, 0.0, 1.0);
            criticalStrikeDamageMultiplier = builder
                    .comment("暴击伤害倍率 (原始伤害 × 此值)", "Critical strike damage multiplier", "Default: 1.5")
                    .defineInRange("damage_multiplier", 1.5, 0.0, 10.0);
            builder.pop();

            // --- 虚空之力 ---
            builder.push("void_power");
            voidPowerDamagePerLevel = builder
                    .comment("每级额外伤害百分比 (0.1 = 10%)", "Extra void damage per level", "Default: 0.1")
                    .defineInRange("damage_per_level", 0.1, 0.0, 10.0);
            builder.pop();

            // --- 碎甲 ---
            builder.push("armor_break");
            armorBreakIgnorePerLevel = builder
                    .comment("每级无视护甲比例 (0.1 = 10%)", "Armor ignore per level", "Default: 0.1")
                    .defineInRange("armor_ignore_per_level", 0.1, 0.0, 1.0);
            armorBreakDurabilityPerLevel = builder
                    .comment("每级盔甲耐久损耗比例 (0.01 = 1%)", "Durability damage per level", "Default: 0.01")
                    .defineInRange("durability_damage_per_level", 0.01, 0.0, 1.0);
            builder.pop();

            // --- 易伤 ---
            builder.push("vulnerability");
            vulnerabilityDamagePerLevel = builder
                    .comment("每级伤害加成 (0.5 = +50%)", "Damage multiplier per level", "Default: 0.5")
                    .defineInRange("damage_per_level", 0.5, 0.0, 100.0);
            builder.pop();

            // --- 虹吸 ---
            builder.push("siphon");
            siphonLifestealBase = builder
                    .comment("基础吸血比例 (0.05 = 5%)", "Base lifesteal percent", "Default: 0.05")
                    .defineInRange("lifesteal_base", 0.05, 0.0, 1.0);
            siphonLifestealPerLevel = builder
                    .comment("每级吸血增加比例 (0.05 = 5%)", "Lifesteal percent per level", "Default: 0.05")
                    .defineInRange("lifesteal_per_level", 0.05, 0.0, 1.0);
            builder.pop();

            // --- 修补 ---
            builder.push("mending");
            mendingRepairPerLevel = builder
                    .comment("每级每秒修复耐久值", "Repair amount per level per second", "Default: 10")
                    .defineInRange("repair_per_level", 10, 1, 1000);
            builder.pop();

            // --- 相位锁定 ---
            builder.push("phase_lock");
            phaseLockDamagePerLevel = builder
                    .comment("每级伤害增加比例 (0.25 = +25%)", "Damage bonus per level", "Default: 0.25")
                    .defineInRange("damage_per_level", 0.25, 0.0, 10.0);
            builder.pop();

            // --- 坚定 ---
            builder.push("firmness");
            firmnessMaxDamageBase = builder
                    .comment("基础单次最大受伤比例 (0.9 = 90%)", "Base max damage percent", "Default: 0.9")
                    .defineInRange("max_damage_base", 0.9, 0.01, 1.0);
            firmnessMaxDamagePerLevel = builder
                    .comment("每级减少最大受伤比例 (0.1 = 10%)", "Max damage reduction per level", "Default: 0.1")
                    .defineInRange("max_damage_per_level", 0.1, 0.0, 1.0);
            firmnessLockDurationBase = builder
                    .comment("基础血量锁定持续时间（秒）", "Base health lock duration (seconds)", "Default: 1.0")
                    .defineInRange("lock_duration_base", 1.0, 0.0, 10.0);
            firmnessLockDurationPerLevel = builder
                    .comment("每级增加锁定持续时间（秒）", "Lock duration per level (seconds)", "Default: 0.5")
                    .defineInRange("lock_duration_per_level", 0.5, 0.0, 10.0);
            builder.pop();

            // --- 脆弱 ---
            builder.push("fragility");
            fragilityDamagePerTick = builder
                    .comment("每tick伤害值", "Damage per tick", "Default: 0.1")
                    .defineInRange("damage_per_tick", 0.1, 0.0, 100.0);
            builder.pop();

            // --- 魔法抗性 ---
            builder.push("magic_resistance");
            magicResistanceReductionPerLevel = builder
                    .comment("每级魔法伤害减免比例 (0.1 = 10%)", "Magic damage reduction per level", "Default: 0.1")
                    .defineInRange("reduction_per_level", 0.1, 0.0, 1.0);
            magicResistanceMaxReduction = builder
                    .comment("最大减免比例 (0.9 = 90%)", "Maximum reduction percent", "Default: 0.9")
                    .defineInRange("max_reduction", 0.9, 0.0, 1.0);
            builder.pop();

            // --- 敏捷 ---
            builder.push("agility");
            agilityMovementSpeedPerLevel = builder
                    .comment("每级移动速度加成 (0.3 = +30%)", "Movement speed per level", "Default: 0.3")
                    .defineInRange("movement_speed_per_level", 0.3, 0.0, 10.0);
            agilityAttackSpeedPerLevel = builder
                    .comment("每级攻击速度加成 (0.5 = +50%)", "Attack speed per level", "Default: 0.5")
                    .defineInRange("attack_speed_per_level", 0.5, 0.0, 10.0);
            builder.pop();

            // --- 距离扩展 ---
            builder.push("range_extension");
            rangeExtensionIncreasePerLevel = builder
                    .comment("每级增加攻击/交互距离（格）", "Range increase per level (blocks)", "Default: 0.5")
                    .defineInRange("range_per_level", 0.5, 0.0, 50.0);
            builder.pop();

            // --- 负载 ---
            builder.push("overload");
            overloadMaxAmplifierBeforeExplosion = builder
                    .comment("触发自爆所需等级（从0开始）", "Amplifier level to trigger explosion", "Default: 9")
                    .defineInRange("max_amplifier_before_explosion", 9, 1, 100);
            overloadExplosionPower = builder
                    .comment("自爆爆炸威力", "Explosion power", "Default: 4.0")
                    .defineInRange("explosion_power", 4.0, 0.0, 50.0);
            overloadAreaDamageRadius = builder
                    .comment("范围伤害半径（格）", "Area damage radius (blocks)", "Default: 5.0")
                    .defineInRange("area_damage_radius", 5.0, 0.0, 100.0);
            builder.pop();

            builder.pop(); // effect_values
        }
    }

    private EffectConfigValues() {}
}
