package net.diexv.potionenchant.client;

import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.DecimalFormat;

/**
 * 药水/附魔描述提供器（客户端）
 *
 * - 本模组自身效果与附魔：描述文本位于语言文件（effect.potionenchant.*.description /
 *   enchantment.potionenchant.*.desc），其中的 %s 占位符由
 *   potionenchant-effects.toml / potionenchant-enchantments.toml 配置文件中的
 *   实际数值动态填充，保证描述始终与"我自己的药水描述配置文件"一致。
 * - 原版及其他模组效果：直接读取语言文件 effect.&lt;modid&gt;.&lt;name&gt;.description，
 *   没有对应键时返回"暂无描述"。
 */
@OnlyIn(Dist.CLIENT)
public final class PotionDescriptionProvider {

    private static final DecimalFormat NUM = new DecimalFormat("0.##");
    private static final String NO_DESCRIPTION = "gui.potionenchant.no_description";

    private PotionDescriptionProvider() {}

    /** 0.3 -> "30"（百分比展示用） */
    private static String pct(double value) {
        return NUM.format(value * 100.0);
    }

    /** 1.5 -> "1.5"，10.0 -> "10"（数值展示用） */
    private static String num(double value) {
        return NUM.format(value);
    }

    // ==================== 药水效果 ====================

    public static boolean hasEffectDescription(MobEffect effect) {
        return effectLangKey(effect) != null;
    }

    public static Component getEffectDescription(MobEffect effect) {
        String langKey = effectLangKey(effect);
        if (langKey == null) {
            return Component.translatable(NO_DESCRIPTION);
        }
        Object[] args = buildModEffectArgs(ForgeRegistries.MOB_EFFECTS.getKey(effect).getPath());
        if (args != null) {
            return Component.translatable(langKey, args);
        }
        return Component.translatable(langKey);
    }

    /**
     * 返回效果的描述语言键；若语言文件中存在该键（本模组效果总是提供模板键）则返回，
     * 否则返回 null（表示没有可用描述）。
     */
    private static String effectLangKey(MobEffect effect) {
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (key == null) return null;
        String langKey = "effect." + key.getNamespace() + "." + key.getPath() + ".description";
        return I18n.exists(langKey) ? langKey : null;
    }

    /**
     * 本模组效果的配置数值参数（与语言文件模板中的 %s 一一对应）。
     * 没有配置数值的效果（连招/恩怨/净化/重生/圣洁）返回 null，使用静态描述。
     */
    private static Object[] buildModEffectArgs(String path) {
        EffectConfigValues.EffectConfig c = EffectConfigValues.CONFIG;
        switch (path) {
            case "agility":
                return new Object[]{ pct(c.agilityMovementSpeedPerLevel.get()), pct(c.agilityAttackSpeedPerLevel.get()) };
            case "armor_break":
                return new Object[]{ pct(c.armorBreakIgnorePerLevel.get()), pct(c.armorBreakDurabilityPerLevel.get()) };
            case "critical_strike":
                return new Object[]{ pct(c.criticalStrikeBaseChance.get()), pct(c.criticalStrikeChancePerLevel.get()), num(c.criticalStrikeDamageMultiplier.get()) };
            case "firmness":
                return new Object[]{ pct(c.firmnessMaxDamageBase.get()), pct(c.firmnessMaxDamagePerLevel.get()), num(c.firmnessLockDurationBase.get()), num(c.firmnessLockDurationPerLevel.get()) };
            case "fragility":
                return new Object[]{ num(c.fragilityDamagePerTick.get()) };
            case "magic_resistance":
                return new Object[]{ pct(c.magicResistanceReductionPerLevel.get()), pct(c.magicResistanceMaxReduction.get()) };
            case "mending":
                return new Object[]{ String.valueOf(c.mendingRepairPerLevel.get()) };
            case "overload":
                return new Object[]{ num(c.overloadAreaDamageRadius.get()), String.valueOf(c.overloadMaxAmplifierBeforeExplosion.get()), num(c.overloadExplosionPower.get()) };
            case "phase_lock":
                return new Object[]{ pct(c.phaseLockDamagePerLevel.get()) };
            case "range_extension":
                return new Object[]{ num(c.rangeExtensionIncreasePerLevel.get()) };
            case "siphon":
                return new Object[]{ pct(c.siphonLifestealBase.get()), pct(c.siphonLifestealPerLevel.get()) };
            case "void_power":
                return new Object[]{ pct(c.voidPowerDamagePerLevel.get()) };
            case "vulnerability":
                return new Object[]{ pct(c.vulnerabilityDamagePerLevel.get()) };
            default:
                return null;
        }
    }

    // ==================== 附魔 ====================

    public static boolean hasEnchantmentDescription(Enchantment enchantment) {
        return enchantmentLangKey(enchantment) != null;
    }

    public static Component getEnchantmentDescription(Enchantment enchantment) {
        String langKey = enchantmentLangKey(enchantment);
        if (langKey == null) {
            return Component.translatable(NO_DESCRIPTION);
        }
        Object[] args = buildModEnchantArgs(ForgeRegistries.ENCHANTMENTS.getKey(enchantment).getPath());
        if (args != null) {
            return Component.translatable(langKey, args);
        }
        return Component.translatable(langKey);
    }

    private static String enchantmentLangKey(Enchantment enchantment) {
        ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (key == null) return null;
        String langKey = "enchantment." + key.getNamespace() + "." + key.getPath() + ".desc";
        return I18n.exists(langKey) ? langKey : null;
    }

    /**
     * 本模组附魔的配置数值参数（与语言文件模板中的 %s 一一对应）。
     * 没有配置数值的附魔返回 null，使用静态描述。
     */
    private static Object[] buildModEnchantArgs(String path) {
        EnchantmentConfigValues.EnchantmentConfig c = EnchantmentConfigValues.CONFIG;
        switch (path) {
            case "lifesteal":
                return new Object[]{ pct(c.lifestealHealPercentPerLevel.get()) };
            case "advanced_sharpness":
                return new Object[]{ num(c.advancedSharpnessBaseDamage.get()), num(c.advancedSharpnessDamagePerLevel.get()) };
            case "advanced_protection":
                return new Object[]{ String.valueOf(c.advancedProtectionPointsPerLevel.get()) };
            case "blaze_aspect":
                return new Object[]{ String.valueOf(c.blazeAspectFireSecondsPerLevel.get()) };
            case "wither_aspect":
                return new Object[]{ String.valueOf(c.witherAspectWitherSecondsPerLevel.get()), String.valueOf(c.witherAspectWitherLevel.get() + 1) };
            case "mana_focus":
                return new Object[]{ pct(c.manaFocusReductionPerLevel.get()), pct(c.manaFocusDamageIncreasePerLevel.get()) };
            case "potion_bane":
                return new Object[]{ pct(c.potionBaneDamageMultiplierPerLevel.get()) };
            case "damage_storage":
                return new Object[]{ num(c.damageStorageMaxMultiplier.get()), String.valueOf(c.damageStorageDecaySeconds.get()) };
            default:
                return null;
        }
    }
}
