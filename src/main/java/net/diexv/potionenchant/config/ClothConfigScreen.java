package net.diexv.potionenchant.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.diexv.potionenchant.client.MenuResourceScanner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class ClothConfigScreen {

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.potionenchant.title"))
                .setSavingRunnable(() -> {
                    PotionEnchantConfig.SERVER_SPEC.save();
                    PotionEnchantConfig.CLIENT_SPEC.save();
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
                (List<String>) PotionEnchantConfig.SERVER.blacklistedEffects.get())
                .setDefaultValue(java.util.Collections.emptyList())

                .setTooltip(Component.translatable("config.potionenchant.blacklisted_effects.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.blacklistedEffects::set).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.limit_armor_enchants"),
                PotionEnchantConfig.SERVER.limitArmorEnchants.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.limit_armor_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.limitArmorEnchants::set).build());
        general.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_armor_enchants"),
                PotionEnchantConfig.SERVER.maxArmorEnchants.get())
                .setDefaultValue(2).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_armor_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.maxArmorEnchants::set).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.limit_all_enchants"),
                PotionEnchantConfig.SERVER.limitAllEnchants.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.limit_all_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.limitAllEnchants::set).build());
        general.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_all_enchants"),
                PotionEnchantConfig.SERVER.maxAllEnchants.get())
                .setDefaultValue(3).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_all_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.maxAllEnchants::set).build());
    }

    private static void buildPotionEnchantCategory(ConfigBuilder builder) {
        ConfigCategory pe = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.potion_enchant"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_potion_enchant_level"),
                PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get())
                .setDefaultValue(100).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_potion_enchant_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.maxPotionEnchantLevel::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_potion_enchant_level_per_item"),
                PotionEnchantConfig.SERVER.maxPotionEnchantLevelPerItem.get())
                .setDefaultValue(5).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.max_potion_enchant_level_per_item.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.maxPotionEnchantLevelPerItem::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_potion_level_beyond_255"),
                PotionEnchantConfig.SERVER.allowPotionLevelBeyond255.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.allow_potion_level_beyond_255.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.allowPotionLevelBeyond255::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap"),
                PotionEnchantConfig.SERVER.allowEnchantLevelBeyondCap.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.allowEnchantLevelBeyondCap::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.enchant_book_xp_cost"),
                PotionEnchantConfig.SERVER.enchantBookXpCost.get())
                .setDefaultValue(1000).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_xp_cost.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enchantBookXpCost::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.discoverable_in_enchanting_table"),
                PotionEnchantConfig.SERVER.discoverableInEnchantingTable.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.discoverable_in_enchanting_table.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.discoverableInEnchantingTable::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enchant_book_chest_loot"),
                PotionEnchantConfig.SERVER.enchantBookChestLoot.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_chest_loot.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enchantBookChestLoot::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enchant_book_villager_trades"),
                PotionEnchantConfig.SERVER.enchantBookVillagerTrades.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enchant_book_villager_trades.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enchantBookVillagerTrades::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.ultimate_table_xp_cost_per_level"),
                PotionEnchantConfig.SERVER.ultimateTableXpCostPerLevel.get())
                .setDefaultValue(1000).setMin(1).setMax(Integer.MAX_VALUE)

                .setTooltip(Component.translatable("config.potionenchant.ultimate_table_xp_cost_per_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.ultimateTableXpCostPerLevel::set).build());

        pe.addEntry(eb.startIntField(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance"),
                PotionEnchantConfig.SERVER.ultimatePotionAmuletLootChance.get())
                .setDefaultValue(1).setMin(0).setMax(100)

                .setTooltip(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.ultimatePotionAmuletLootChance::set).build());

        pe.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.allow_curio_potion_enchant"),
                PotionEnchantConfig.SERVER.allowCurioPotionEnchant.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.allow_curio_potion_enchant.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.allowCurioPotionEnchant::set).build());
    }

    private static void buildDisplayCategory(ConfigBuilder builder) {
        ConfigCategory display = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.display"));
        ConfigEntryBuilder eb = builder.entryBuilder();
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.custom_potion_hud"),
                PotionEnchantConfig.CLIENT.customPotionHud.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.custom_potion_hud.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.customPotionHud::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_potion_level"),
                PotionEnchantConfig.CLIENT.showPotionLevel.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_potion_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.showPotionLevel::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_text_background"),
                PotionEnchantConfig.CLIENT.showTextBackground.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_text_background.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.showTextBackground::set).build());
        display.addEntry(eb.startIntField(Component.translatable("config.potionenchant.max_visible_effects"),
                PotionEnchantConfig.CLIENT.maxVisibleEffects.get())
                .setDefaultValue(8).setMin(3).setMax(50)

                .setTooltip(Component.translatable("config.potionenchant.max_visible_effects.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.maxVisibleEffects::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.show_scroll_hint"),
                PotionEnchantConfig.CLIENT.showScrollHint.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.show_scroll_hint.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.showScrollHint::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.hud_high_priority"),
                PotionEnchantConfig.CLIENT.hudHighPriority.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.hud_high_priority.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.hudHighPriority::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_custom_potion_tooltip"),
                PotionEnchantConfig.SERVER.enableCustomPotionTooltip.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_custom_potion_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enableCustomPotionTooltip::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_vanilla_potion_description"),
                PotionEnchantConfig.SERVER.enableVanillaPotionDescription.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_vanilla_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enableVanillaPotionDescription::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_all_potion_description"),
                PotionEnchantConfig.SERVER.enableAllPotionDescription.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_all_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enableAllPotionDescription::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip"),
                PotionEnchantConfig.SERVER.enablePotionEnchantTooltip.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.enablePotionEnchantTooltip::set).build());
        display.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_armor_value_render"),
                PotionEnchantConfig.CLIENT.enableArmorValueRender.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_armor_value_render.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.enableArmorValueRender::set).build());
        display.addEntry(eb.startIntField(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column"),
                PotionEnchantConfig.SERVER.potionEnchantTooltipMaxPerColumn.get())
                .setDefaultValue(5).setMin(1).setMax(50)

                .setTooltip(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.SERVER.potionEnchantTooltipMaxPerColumn::set).build());
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
                .setDefaultValue(5).setMin(1).setMax(100)

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
                PotionEnchantConfig.CLIENT.enableCustomMainMenu.get())
                .setDefaultValue(false)

                .setTooltip(Component.translatable("config.potionenchant.enable_custom_main_menu.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.enableCustomMainMenu::set).build());


        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_menu_parallax"),
                PotionEnchantConfig.CLIENT.enableMenuParallax.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_menu_parallax.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.enableMenuParallax::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.menu_parallax_max_offset"),
                PotionEnchantConfig.CLIENT.menuParallaxMaxOffset.get(), 5, 60)
                .setDefaultValue(27)

                .setTooltip(Component.translatable("config.potionenchant.menu_parallax_max_offset.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.menuParallaxMaxOffset::set).build());

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.enable_menu_vignette"),
                PotionEnchantConfig.CLIENT.enableMenuVignette.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.enable_menu_vignette.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.enableMenuVignette::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.menu_fog_range"),
                PotionEnchantConfig.CLIENT.menuFogRange.get(), 0, 100)
                .setDefaultValue(3)

                .setTooltip(Component.translatable("config.potionenchant.menu_fog_range.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.menuFogRange::set).build());

        // ===== 用户自定义资源（从 config/potionenchant/menu/ 扫描） =====

        cm.addEntry(eb.<String>startDropdownMenu(Component.translatable("config.potionenchant.menu_background_file"),
                PotionEnchantConfig.CLIENT.menuBackgroundFile.get(),
                java.util.function.Function.identity(),
                net.minecraft.network.chat.Component::literal)
                .setSelections(java.util.Arrays.asList(net.diexv.potionenchant.client.MenuResourceScanner.getBackgroundOptions()))
                .setDefaultValue("main_menu_bg")
                .setTooltip(Component.translatable("config.potionenchant.menu_background_file.tooltip"))
                .setSaveConsumer(val -> PotionEnchantConfig.CLIENT.menuBackgroundFile.set(val))
                .build());

        cm.addEntry(eb.<String>startDropdownMenu(Component.translatable("config.potionenchant.menu_icon_file"),
                PotionEnchantConfig.CLIENT.menuIconFile.get(),
                java.util.function.Function.identity(),
                net.minecraft.network.chat.Component::literal)
                .setSelections(java.util.Arrays.asList(net.diexv.potionenchant.client.MenuResourceScanner.getIconOptions()))
                .setDefaultValue("main_menu_logo")
                .setTooltip(Component.translatable("config.potionenchant.menu_icon_file.tooltip"))
                .setSaveConsumer(val -> PotionEnchantConfig.CLIENT.menuIconFile.set(val))
                .build());

        cm.addEntry(eb.<String>startDropdownMenu(Component.translatable("config.potionenchant.menu_music_file"),
                PotionEnchantConfig.CLIENT.menuMusicFile.get(),
                java.util.function.Function.identity(),
                net.minecraft.network.chat.Component::literal)
                .setSelections(java.util.Arrays.asList(net.diexv.potionenchant.client.MenuResourceScanner.getMusicOptions()))
                .setDefaultValue("menu_music")
                .setTooltip(Component.translatable("config.potionenchant.menu_music_file.tooltip"))
                .setSaveConsumer(val -> PotionEnchantConfig.CLIENT.menuMusicFile.set(val))
                .build());

        cm.addEntry(eb.startStrField(Component.translatable("config.potionenchant.menu_title_text"),
                PotionEnchantConfig.CLIENT.menuTitleText.get())
                .setDefaultValue("")
                .setTooltip(Component.translatable("config.potionenchant.menu_title_text.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.menuTitleText::set).build());

        cm.addEntry(eb.<String>startDropdownMenu(Component.translatable("config.potionenchant.particle_tint_color"),
                PotionEnchantConfig.CLIENT.particleTintColor.get(),
                java.util.function.Function.identity(),
                net.minecraft.network.chat.Component::literal)
                .setSelections(java.util.Arrays.asList("random", "warm", "cool"))
                .setDefaultValue("")
                .setTooltip(Component.translatable("config.potionenchant.particle_tint_color.tooltip"))
                .setSaveConsumer(val -> PotionEnchantConfig.CLIENT.particleTintColor.set(val))
                .build());

        cm.addEntry(eb.<String>startDropdownMenu(Component.translatable("config.potionenchant.button_highlight_color"),
                PotionEnchantConfig.CLIENT.buttonHighlightColor.get(),
                java.util.function.Function.identity(),
                net.minecraft.network.chat.Component::literal)
                .setSelections(java.util.Arrays.asList("default", "red", "orange", "yellow", "green", "blue", "indigo", "violet"))
                .setDefaultValue("default")
                .setTooltip(Component.translatable("config.potionenchant.button_highlight_color.tooltip"))
                .setSaveConsumer(val -> PotionEnchantConfig.CLIENT.buttonHighlightColor.set(val))
                .build());
        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_base_size"),
                PotionEnchantConfig.CLIENT.particleBaseSize.get(), 8, 64)
                .setDefaultValue(21)
                .setTooltip(Component.translatable("config.potionenchant.particle_base_size.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleBaseSize::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_size_spread"),
                PotionEnchantConfig.CLIENT.particleSizeSpread.get(), 0, 48)
                .setDefaultValue(17)
                .setTooltip(Component.translatable("config.potionenchant.particle_size_spread.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleSizeSpread::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_h"),
                PotionEnchantConfig.CLIENT.particleSpeedH.get(), 0, 100)
                .setDefaultValue(17)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_h.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleSpeedH::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_v"),
                PotionEnchantConfig.CLIENT.particleSpeedV.get(), 1, 100)
                .setDefaultValue(8)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_v.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleSpeedV::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_speed_v_spread"),
                PotionEnchantConfig.CLIENT.particleSpeedVSpread.get(), 0, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_speed_v_spread.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleSpeedVSpread::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_in"),
                PotionEnchantConfig.CLIENT.particleFadeIn.get(), 1, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_in.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleFadeIn::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_out"),
                PotionEnchantConfig.CLIENT.particleFadeOut.get(), 1, 100)
                .setDefaultValue(40)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_out.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleFadeOut::set).build());

        cm.addEntry(eb.startBooleanToggle(Component.translatable("config.potionenchant.particle_go_up"),
                PotionEnchantConfig.CLIENT.particleGoUp.get())
                .setDefaultValue(true)

                .setTooltip(Component.translatable("config.potionenchant.particle_go_up.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleGoUp::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_max_count"),
                PotionEnchantConfig.CLIENT.particleMaxCount.get(), 5, 100)
                .setDefaultValue(25)

                .setTooltip(Component.translatable("config.potionenchant.particle_max_count.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleMaxCount::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_fade_y_start"),
                PotionEnchantConfig.CLIENT.particleFadeYStart.get(), 0, 100)
                .setDefaultValue(10)

                .setTooltip(Component.translatable("config.potionenchant.particle_fade_y_start.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleFadeYStart::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.particle_spawn_rate"),
                PotionEnchantConfig.CLIENT.particleSpawnRate.get(), 1, 20)
                .setDefaultValue(2)

                .setTooltip(Component.translatable("config.potionenchant.particle_spawn_rate.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.particleSpawnRate::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_size"),
                PotionEnchantConfig.CLIENT.mouseTrailSize.get(), 4, 64)
                .setDefaultValue(32)
                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_size.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.mouseTrailSize::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_lifetime"),
                PotionEnchantConfig.CLIENT.mouseTrailLifetime.get(), 200, 10000)
                .setDefaultValue(800)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_lifetime.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.mouseTrailLifetime::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_spawn_interval"),
                PotionEnchantConfig.CLIENT.mouseTrailSpawnInterval.get(), 10, 200)
                .setDefaultValue(30)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_spawn_interval.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.mouseTrailSpawnInterval::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_fade_delay"),
                PotionEnchantConfig.CLIENT.mouseTrailFadeDelay.get(), 0, 100)
                .setDefaultValue(0)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_fade_delay.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.mouseTrailFadeDelay::set).build());

        cm.addEntry(eb.startIntSlider(Component.translatable("config.potionenchant.mouse_trail_click_count"),
                PotionEnchantConfig.CLIENT.mouseTrailClickCount.get(), 0, 100)
                .setDefaultValue(5)

                .setTooltip(Component.translatable("config.potionenchant.mouse_trail_click_count.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.CLIENT.mouseTrailClickCount::set).build());
    }
}