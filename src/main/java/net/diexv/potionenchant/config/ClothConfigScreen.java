package net.diexv.potionenchant.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class ClothConfigScreen {

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.potionenchant.title"))
                .setSavingRunnable(() -> {
                    PotionEnchantConfig.COMMON_SPEC.save();
                    EffectConfigValues.SPEC.save();
                    EnchantmentConfigValues.SPEC.save();
                });
        buildGeneralCategory(builder);
        buildPotionEnchantCategory(builder);
        buildDisplayCategory(builder);
        buildCustomMainMenuCategory(builder);
        buildEffectValuesCategory(builder);
        buildEnchantmentValuesCategory(builder);
        return builder.build();
    }


    private static void buildGeneralCategory(ConfigBuilder builder) {
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.general"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        general.addEntry(eb.startStrList(Component.translatable("config.potionenchant.blacklisted_effects"),
                (List<String>) PotionEnchantConfig.COMMON.blacklistedEffects.get())
                .setDefaultValue(java.util.Collections.emptyList())

                .setTooltip(Component.translatable("config.potionenchant.blacklisted_effects.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.blacklistedEffects::set).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.limit_armor_enchants"),
                PotionEnchantConfig.COMMON.limitArmorEnchants.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.limit_armor_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.limitArmorEnchants::set).build());
        general.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_armor_enchants"),
                PotionEnchantConfig.COMMON.maxArmorEnchants.get())
                .setDefaultValue(2).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_armor_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxArmorEnchants::set).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.limit_all_enchants"),
                PotionEnchantConfig.COMMON.limitAllEnchants.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.limit_all_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.limitAllEnchants::set).build());
        general.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_all_enchants"),
                PotionEnchantConfig.COMMON.maxAllEnchants.get())
                .setDefaultValue(33).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_all_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxAllEnchants::set).build());
    }

    private static void buildPotionEnchantCategory(ConfigBuilder builder) {
        ConfigCategory pe = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.potion_enchant"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_potion_enchant_level"),
                PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get())
                .setDefaultValue(100).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_potion_enchant_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxPotionEnchantLevel::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_potion_enchant_level_per_item"),
                PotionEnchantConfig.COMMON.maxPotionEnchantLevelPerItem.get())
                .setDefaultValue(2).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_potion_enchant_level_per_item.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxPotionEnchantLevelPerItem::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_potion_level_beyond_255"),
                PotionEnchantConfig.COMMON.allowPotionLevelBeyond255.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.allow_potion_level_beyond_255.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.allowPotionLevelBeyond255::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap"),
                PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchant_book_xp_cost"),
                PotionEnchantConfig.COMMON.enchantBookXpCost.get())
                .setDefaultValue(1000).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_xp_cost.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enchantBookXpCost::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.discoverable_in_enchanting_table"),
                PotionEnchantConfig.COMMON.discoverableInEnchantingTable.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.discoverable_in_enchanting_table.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.discoverableInEnchantingTable::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enchant_book_chest_loot"),
                PotionEnchantConfig.COMMON.enchantBookChestLoot.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_chest_loot.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enchantBookChestLoot::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enchant_book_villager_trades"),
                PotionEnchantConfig.COMMON.enchantBookVillagerTrades.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_villager_trades.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enchantBookVillagerTrades::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.ultimate_table_xp_cost_per_level"),
                PotionEnchantConfig.COMMON.ultimateTableXpCostPerLevel.get())
                .setDefaultValue(1000).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.ultimate_table_xp_cost_per_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.ultimateTableXpCostPerLevel::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance"),
                PotionEnchantConfig.COMMON.ultimatePotionAmuletLootChance.get())
                .setDefaultValue(1).setMin(0).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.ultimatePotionAmuletLootChance::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_curio_potion_enchant"),
                PotionEnchantConfig.COMMON.allowCurioPotionEnchant.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.allow_curio_potion_enchant.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.allowCurioPotionEnchant::set).build());
    }

    private static void buildDisplayCategory(ConfigBuilder builder) {
        ConfigCategory display = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.display"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.custom_potion_hud"),
                PotionEnchantConfig.COMMON.customPotionHud.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.custom_potion_hud.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.customPotionHud::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_potion_level"),
                PotionEnchantConfig.COMMON.showPotionLevel.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_potion_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showPotionLevel::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_text_background"),
                PotionEnchantConfig.COMMON.showTextBackground.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_text_background.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showTextBackground::set).build());
        display.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_visible_effects"),
                PotionEnchantConfig.COMMON.maxVisibleEffects.get())
                .setDefaultValue(10).setMin(3).setMax(50)

                .setTooltip(Component.translatable("config.potionenchant.max_visible_effects.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxVisibleEffects::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_scroll_hint"),
                PotionEnchantConfig.COMMON.showScrollHint.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_scroll_hint.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showScrollHint::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.hud_high_priority"),
                PotionEnchantConfig.COMMON.hudHighPriority.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.hud_high_priority.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.hudHighPriority::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_custom_potion_tooltip"),
                PotionEnchantConfig.COMMON.enableCustomPotionTooltip.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_custom_potion_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableCustomPotionTooltip::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_vanilla_potion_description"),
                PotionEnchantConfig.COMMON.enableVanillaPotionDescription.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enable_vanilla_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableVanillaPotionDescription::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_all_potion_description"),
                PotionEnchantConfig.COMMON.enableAllPotionDescription.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enable_all_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableAllPotionDescription::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip"),
                PotionEnchantConfig.COMMON.enablePotionEnchantTooltip.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enablePotionEnchantTooltip::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_armor_value_render"),
                PotionEnchantConfig.COMMON.enableArmorValueRender.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_armor_value_render.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableArmorValueRender::set).build());
        display.addEntry(eb.startIntField(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column"),
                PotionEnchantConfig.COMMON.potionEnchantTooltipMaxPerColumn.get())
                .setDefaultValue(10).setMin(1).setMax(50)

                .setTooltip(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.potionEnchantTooltipMaxPerColumn::set).build());
    }

    private static void buildEffectValuesCategory(ConfigBuilder builder) {
        ConfigCategory ec = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.effects"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        var cfg = EffectConfigValues.CONFIG;
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.critical_strike.base_chance"), cfg.criticalStrikeBaseChance.get())
                .setDefaultValue(0.2).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.critical_strike.base_chance.tooltip"))
                .setSaveConsumer(cfg.criticalStrikeBaseChance::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.critical_strike.chance_per_level"), cfg.criticalStrikeChancePerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.critical_strike.chance_per_level.tooltip"))
                .setSaveConsumer(cfg.criticalStrikeChancePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.critical_strike.damage_multiplier"), cfg.criticalStrikeDamageMultiplier.get())
                .setDefaultValue(1.5).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.critical_strike.damage_multiplier.tooltip"))
                .setSaveConsumer(cfg.criticalStrikeDamageMultiplier::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.void_power.damage_per_level"), cfg.voidPowerDamagePerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.void_power.damage_per_level.tooltip"))
                .setSaveConsumer(cfg.voidPowerDamagePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.armor_break.armor_ignore_per_level"), cfg.armorBreakIgnorePerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.armor_break.armor_ignore_per_level.tooltip"))
                .setSaveConsumer(cfg.armorBreakIgnorePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.armor_break.durability_damage_per_level"), cfg.armorBreakDurabilityPerLevel.get())
                .setDefaultValue(0.01).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.armor_break.durability_damage_per_level.tooltip"))
                .setSaveConsumer(cfg.armorBreakDurabilityPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.vulnerability.damage_per_level"), cfg.vulnerabilityDamagePerLevel.get())
                .setDefaultValue(0.5).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.vulnerability.damage_per_level.tooltip"))
                .setSaveConsumer(cfg.vulnerabilityDamagePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.siphon.lifesteal_base"), cfg.siphonLifestealBase.get())
                .setDefaultValue(0.05).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.siphon.lifesteal_base.tooltip"))
                .setSaveConsumer(cfg.siphonLifestealBase::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.siphon.lifesteal_per_level"), cfg.siphonLifestealPerLevel.get())
                .setDefaultValue(0.05).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.siphon.lifesteal_per_level.tooltip"))
                .setSaveConsumer(cfg.siphonLifestealPerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.effect.mending.repair_per_level"), cfg.mendingRepairPerLevel.get())
                .setDefaultValue(10).setMin(1).setMax(1000)

                .setTooltip(Component.translatable("config.potionenchant.effect.mending.repair_per_level.tooltip"))
                .setSaveConsumer(cfg.mendingRepairPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.phase_lock.damage_per_level"), cfg.phaseLockDamagePerLevel.get())
                .setDefaultValue(0.25).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.phase_lock.damage_per_level.tooltip"))
                .setSaveConsumer(cfg.phaseLockDamagePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.firmness.max_damage_base"), cfg.firmnessMaxDamageBase.get())
                .setDefaultValue(0.9).setMin(0.01).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.firmness.max_damage_base.tooltip"))
                .setSaveConsumer(cfg.firmnessMaxDamageBase::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.firmness.max_damage_per_level"), cfg.firmnessMaxDamagePerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.firmness.max_damage_per_level.tooltip"))
                .setSaveConsumer(cfg.firmnessMaxDamagePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.firmness.lock_duration_base"), cfg.firmnessLockDurationBase.get())
                .setDefaultValue(1.0).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.firmness.lock_duration_base.tooltip"))
                .setSaveConsumer(cfg.firmnessLockDurationBase::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.firmness.lock_duration_per_level"), cfg.firmnessLockDurationPerLevel.get())
                .setDefaultValue(0.5).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.firmness.lock_duration_per_level.tooltip"))
                .setSaveConsumer(cfg.firmnessLockDurationPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.fragility.damage_per_tick"), cfg.fragilityDamagePerTick.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.fragility.damage_per_tick.tooltip"))
                .setSaveConsumer(cfg.fragilityDamagePerTick::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.magic_resistance.reduction_per_level"), cfg.magicResistanceReductionPerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.magic_resistance.reduction_per_level.tooltip"))
                .setSaveConsumer(cfg.magicResistanceReductionPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.magic_resistance.max_reduction"), cfg.magicResistanceMaxReduction.get())
                .setDefaultValue(0.9).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.magic_resistance.max_reduction.tooltip"))
                .setSaveConsumer(cfg.magicResistanceMaxReduction::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.agility.movement_speed_per_level"), cfg.agilityMovementSpeedPerLevel.get())
                .setDefaultValue(0.3).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.agility.movement_speed_per_level.tooltip"))
                .setSaveConsumer(cfg.agilityMovementSpeedPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.agility.attack_speed_per_level"), cfg.agilityAttackSpeedPerLevel.get())
                .setDefaultValue(0.5).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.agility.attack_speed_per_level.tooltip"))
                .setSaveConsumer(cfg.agilityAttackSpeedPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.range_extension.range_per_level"), cfg.rangeExtensionIncreasePerLevel.get())
                .setDefaultValue(0.5).setMin(0.0).setMax(50.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.range_extension.range_per_level.tooltip"))
                .setSaveConsumer(cfg.rangeExtensionIncreasePerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.effect.overload.max_amplifier_before_explosion"), cfg.overloadMaxAmplifierBeforeExplosion.get())
                .setDefaultValue(9).setMin(1).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.effect.overload.max_amplifier_before_explosion.tooltip"))
                .setSaveConsumer(cfg.overloadMaxAmplifierBeforeExplosion::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.overload.explosion_power"), cfg.overloadExplosionPower.get())
                .setDefaultValue(4.0).setMin(0.0).setMax(50.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.overload.explosion_power.tooltip"))
                .setSaveConsumer(cfg.overloadExplosionPower::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.effect.overload.area_damage_radius"), cfg.overloadAreaDamageRadius.get())
                .setDefaultValue(5.0).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.effect.overload.area_damage_radius.tooltip"))
                .setSaveConsumer(cfg.overloadAreaDamageRadius::set).build());
    }

    private static void buildEnchantmentValuesCategory(ConfigBuilder builder) {
        ConfigCategory ec = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.enchantments"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        var cfg = EnchantmentConfigValues.CONFIG;
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.lifesteal.heal_percent_per_level"), cfg.lifestealHealPercentPerLevel.get())
                .setDefaultValue(0.05).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.lifesteal.heal_percent_per_level.tooltip"))
                .setSaveConsumer(cfg.lifestealHealPercentPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.advanced_sharpness.base_damage"), cfg.advancedSharpnessBaseDamage.get())
                .setDefaultValue(2.5).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.advanced_sharpness.base_damage.tooltip"))
                .setSaveConsumer(cfg.advancedSharpnessBaseDamage::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.advanced_sharpness.damage_per_level"), cfg.advancedSharpnessDamagePerLevel.get())
                .setDefaultValue(2.5).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.advanced_sharpness.damage_per_level.tooltip"))
                .setSaveConsumer(cfg.advancedSharpnessDamagePerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchantment.advanced_protection.protection_points_per_level"), cfg.advancedProtectionPointsPerLevel.get())
                .setDefaultValue(2).setMin(1).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.advanced_protection.protection_points_per_level.tooltip"))
                .setSaveConsumer(cfg.advancedProtectionPointsPerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchantment.blaze_aspect.fire_seconds_per_level"), cfg.blazeAspectFireSecondsPerLevel.get())
                .setDefaultValue(4).setMin(1).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.blaze_aspect.fire_seconds_per_level.tooltip"))
                .setSaveConsumer(cfg.blazeAspectFireSecondsPerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchantment.wither_aspect.wither_seconds_per_level"), cfg.witherAspectWitherSecondsPerLevel.get())
                .setDefaultValue(4).setMin(1).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.wither_aspect.wither_seconds_per_level.tooltip"))
                .setSaveConsumer(cfg.witherAspectWitherSecondsPerLevel::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchantment.wither_aspect.wither_amplifier"), cfg.witherAspectWitherLevel.get())
                .setDefaultValue(0).setMin(0).setMax(255)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.wither_aspect.wither_amplifier.tooltip"))
                .setSaveConsumer(cfg.witherAspectWitherLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.mana_focus.reduction_per_level"), cfg.manaFocusReductionPerLevel.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.mana_focus.reduction_per_level.tooltip"))
                .setSaveConsumer(cfg.manaFocusReductionPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.mana_focus.damage_increase_per_level"), cfg.manaFocusDamageIncreasePerLevel.get())
                .setDefaultValue(0.25).setMin(0.0).setMax(10.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.mana_focus.damage_increase_per_level.tooltip"))
                .setSaveConsumer(cfg.manaFocusDamageIncreasePerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.potion_bane.damage_multiplier_per_level"), cfg.potionBaneDamageMultiplierPerLevel.get())
                .setDefaultValue(1.0).setMin(0.0).setMax(100.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.potion_bane.damage_multiplier_per_level.tooltip"))
                .setSaveConsumer(cfg.potionBaneDamageMultiplierPerLevel::set).build());
        ec.addEntry(eb.startDoubleField(Component.translatable("config.potionenchant.enchantment.damage_storage.max_storage_multiplier"), cfg.damageStorageMaxMultiplier.get())
                .setDefaultValue(10.0).setMin(1.0).setMax(1000.0)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.damage_storage.max_storage_multiplier.tooltip"))
                .setSaveConsumer(cfg.damageStorageMaxMultiplier::set).build());
        ec.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchantment.damage_storage.decay_seconds"), cfg.damageStorageDecaySeconds.get())
                .setDefaultValue(60).setMin(1).setMax(3600)

                .setTooltip(Component.translatable("config.potionenchant.enchantment.damage_storage.decay_seconds.tooltip"))
                .setSaveConsumer(cfg.damageStorageDecaySeconds::set).build());
    }

    private static void buildCustomMainMenuCategory(ConfigBuilder builder) {
        ConfigCategory cm = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.custom_main_menu"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_custom_main_menu"),
                PotionEnchantConfig.COMMON.enableCustomMainMenu.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enable_custom_main_menu.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableCustomMainMenu::set).build());

        cm.addEntry(eb.startStrList(Component.translatable("config.potionenchant.custom_main_menu_music"),
                (List<String>) PotionEnchantConfig.COMMON.customMainMenuMusic.get())
                .setDefaultValue(java.util.Collections.singletonList("potionenchant:menu_music"))

                .setTooltip(Component.translatable("config.potionenchant.custom_main_menu_music.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.customMainMenuMusic::set).build());

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_menu_parallax"),
                PotionEnchantConfig.COMMON.enableMenuParallax.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_menu_parallax.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableMenuParallax::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.menu_parallax_max_offset"),
                PotionEnchantConfig.COMMON.menuParallaxMaxOffset.get(), 5, 60)
                .setDefaultValue(27)

                .setTooltip(Component.translatable("config.potionenchant.menu_parallax_max_offset.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.menuParallaxMaxOffset::set).build());

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_menu_vignette"),
                PotionEnchantConfig.COMMON.enableMenuVignette.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_menu_vignette.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableMenuVignette::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.menu_fog_range"),
                PotionEnchantConfig.COMMON.menuFogRange.get(), 0, 100)
                .setDefaultValue(3)

                .setTooltip(Component.translatable("config.potionenchant.menu_fog_range.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.menuFogRange::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_base_size"),
                PotionEnchantConfig.COMMON.particleBaseSize.get(), 8, 64)
                .setDefaultValue(21)
                .setTooltip(Component.translatable("config.potionenchant.particle_base_size.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleBaseSize::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_size_spread"),
                PotionEnchantConfig.COMMON.particleSizeSpread.get(), 0, 48)
                .setDefaultValue(17)
                .setTooltip(Component.translatable("config.potionenchant.particle_size_spread.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleSizeSpread::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_h"),
                PotionEnchantConfig.COMMON.particleSpeedH.get(), 0, 100)
                .setDefaultValue(17)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_h.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleSpeedH::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_v"),
                PotionEnchantConfig.COMMON.particleSpeedV.get(), 1, 100)
                .setDefaultValue(8)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_v.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleSpeedV::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_v_spread"),
                PotionEnchantConfig.COMMON.particleSpeedVSpread.get(), 0, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_v_spread.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleSpeedVSpread::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_in"),
                PotionEnchantConfig.COMMON.particleFadeIn.get(), 1, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_in.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleFadeIn::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_out"),
                PotionEnchantConfig.COMMON.particleFadeOut.get(), 1, 100)
                .setDefaultValue(40)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_out.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleFadeOut::set).build());

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.particle_go_up"),
                PotionEnchantConfig.COMMON.particleGoUp.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.particle_go_up.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleGoUp::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_max_count"),
                PotionEnchantConfig.COMMON.particleMaxCount.get(), 5, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_max_count.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleMaxCount::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_y_start"),
                PotionEnchantConfig.COMMON.particleFadeYStart.get(), 0, 100)
                .setDefaultValue(10)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_y_start.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleFadeYStart::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_spawn_rate"),
                PotionEnchantConfig.COMMON.particleSpawnRate.get(), 1, 20)
                .setDefaultValue(2)

                .setTooltip(Component.translatable("config.potionenchant.particle_spawn_rate.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.particleSpawnRate::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_size"),
                PotionEnchantConfig.COMMON.mouseTrailSize.get(), 4, 64)
                .setDefaultValue(32)
                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_size.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.mouseTrailSize::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_lifetime"),
                PotionEnchantConfig.COMMON.mouseTrailLifetime.get(), 200, 10000)
                .setDefaultValue(800)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_lifetime.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.mouseTrailLifetime::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_spawn_interval"),
                PotionEnchantConfig.COMMON.mouseTrailSpawnInterval.get(), 10, 200)
                .setDefaultValue(30)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_spawn_interval.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.mouseTrailSpawnInterval::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_fade_delay"),
                PotionEnchantConfig.COMMON.mouseTrailFadeDelay.get(), 0, 100)
                .setDefaultValue(0)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_fade_delay.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.mouseTrailFadeDelay::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_click_count"),
                PotionEnchantConfig.COMMON.mouseTrailClickCount.get(), 0, 100)
                .setDefaultValue(5)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_click_count.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.mouseTrailClickCount::set).build());
    }
}

