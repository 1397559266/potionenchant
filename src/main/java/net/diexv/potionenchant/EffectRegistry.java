package net.diexv.potionenchant;

import net.diexv.potionenchant.effect.CriticalStrikeEffect;
import net.diexv.potionenchant.effect.FragilityEffect;
import net.diexv.potionenchant.effect.MendingEffect;
import net.diexv.potionenchant.effect.OverloadEffect;
import net.diexv.potionenchant.effect.PurificationEffect;
import net.diexv.potionenchant.effect.RevivalEffect;
import net.diexv.potionenchant.effect.SanctuaryEffect;
import net.diexv.potionenchant.effect.VoidPowerEffect;
import net.diexv.potionenchant.effect.VulnerabilityEffect;
import net.diexv.potionenchant.effect.SiphonEffect;
import net.diexv.potionenchant.effect.ArmorBreakEffect;
import net.diexv.potionenchant.effect.RangeExtensionEffect;
import net.diexv.potionenchant.effect.AgilityEffect;
import net.diexv.potionenchant.effect.ComboEffect;
import net.diexv.potionenchant.effect.PhaseLockEffect;
import net.diexv.potionenchant.effect.FirmnessEffect;
import net.diexv.potionenchant.effect.SymbiosisEffect;
import net.diexv.potionenchant.effect.MagicResistanceEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, PotionEnchantMod.MODID);
    // 易伤
    public static final RegistryObject<MobEffect> VULNERABILITY =
            EFFECTS.register("vulnerability", VulnerabilityEffect::new);
    // 修补
    public static final RegistryObject<MobEffect> MENDING =
            EFFECTS.register("mending", MendingEffect::new);
    // 净化
    public static final RegistryObject<MobEffect> PURIFICATION =
            EFFECTS.register("purification", PurificationEffect::new);
    // 圣洁
    public static final RegistryObject<MobEffect> SANCTUARY =
            EFFECTS.register("sanctuary", SanctuaryEffect::new);
    // 负载
    public static final RegistryObject<MobEffect> OVERLOAD =
            EFFECTS.register("overload", OverloadEffect::new);
    // 暴击
    public static final RegistryObject<MobEffect> CRITICAL_STRIKE =
            EFFECTS.register("critical_strike", CriticalStrikeEffect::new);
    // 虚空之力
    public static final RegistryObject<MobEffect> VOID_POWER =
            EFFECTS.register("void_power", VoidPowerEffect::new);
    // 脆弱
    public static final RegistryObject<MobEffect> FRAGILITY =
            EFFECTS.register("fragility", FragilityEffect::new);
    // 重生
    public static final RegistryObject<MobEffect> REVIVAL =
            EFFECTS.register("revival", RevivalEffect::new);
    // 虹吸
    public static final RegistryObject<MobEffect> SIPHON =
            EFFECTS.register("siphon", SiphonEffect::new);
    // 碎甲
    public static final RegistryObject<MobEffect> ARMOR_BREAK =
            EFFECTS.register("armor_break", ArmorBreakEffect::new);
    // 距离扩展
    public static final RegistryObject<MobEffect> RANGE_EXTENSION =
            EFFECTS.register("range_extension", RangeExtensionEffect::new);
    // 敏捷
    public static final RegistryObject<MobEffect> AGILITY =
            EFFECTS.register("agility", AgilityEffect::new);
    // 连招
    public static final RegistryObject<MobEffect> COMBO =
            EFFECTS.register("combo", ComboEffect::new);
    // 相位锁定
    public static final RegistryObject<MobEffect> PHASE_LOCK =
            EFFECTS.register("phase_lock", PhaseLockEffect::new);
    // 坚定
    public static final RegistryObject<MobEffect> FIRMNESS =
            EFFECTS.register("firmness", FirmnessEffect::new);
        // 共生
    public static final RegistryObject<MobEffect> SYMBIOSIS =
            EFFECTS.register("grudge", SymbiosisEffect::new);

    // 魔法抗性
    public static final RegistryObject<MobEffect> MAGIC_RESISTANCE =
            EFFECTS.register("magic_resistance", MagicResistanceEffect::new);
}