package net.diexv.potionenchant.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClothConfigScreen {

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.potionenchant.title"))
                .setSavingRunnable(() -> {
                    // 保存配置时重新加载
                    PotionEnchantConfig.COMMON_SPEC.save();
                });

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 黑名单效果配置
        general.addEntry(entryBuilder.startStrList((Component) Component.translatable("config.potionenchant.blacklisted_effects"),

(List<String>) PotionEnchantConfig.COMMON.blacklistedEffects.get())
                .setDefaultValue(java.util.Collections.emptyList())
                .setTooltip(Component.translatable("config.potionenchant.blacklisted_effects.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.blacklistedEffects::set)
                .build());

        // 盔甲附魔限制配置
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.limit_armor_enchants"),
                        PotionEnchantConfig.COMMON.limitArmorEnchants.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.limit_armor_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.limitArmorEnchants::set)
                .build());

        // 盔甲最大附魔数量配置
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.max_armor_enchants"),
                        PotionEnchantConfig.COMMON.maxArmorEnchants.get())
                .setDefaultValue(2)
                .setTooltip(Component.translatable("config.potionenchant.max_armor_enchants.tooltip"))
                .setMin(1)
                .setMax(99999)
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxArmorEnchants::set)
                .build());

        // 全局附魔限制配置
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.limit_all_enchants"),
                        PotionEnchantConfig.COMMON.limitAllEnchants.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.limit_all_enchants.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.limitAllEnchants::set)
                .build());

        // 全局最大附魔数量配置
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.max_all_enchants"),
                        PotionEnchantConfig.COMMON.maxAllEnchants.get())
                .setDefaultValue(3)
                .setTooltip(Component.translatable("config.potionenchant.max_all_enchants.tooltip"))
                .setMin(1)
                .setMax(99999)
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxAllEnchants::set)
                .build());
        
        
        // 终极药水护符战利品生成概率
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance"),
                        PotionEnchantConfig.COMMON.ultimatePotionAmuletLootChance.get())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(100)
                .setTooltip(Component.translatable("config.potionenchant.ultimate_potion_amulet_loot_chance.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.ultimatePotionAmuletLootChance::set)
                .build());
        
        // 装备的药水附魔等级上限
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.max_potion_enchant_level"),
                        PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(255)
                .setTooltip(Component.translatable("config.potionenchant.max_potion_enchant_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxPotionEnchantLevel::set)
                .build());
        
        // 允许突破255级上限
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.allow_potion_level_beyond_255"),
                        PotionEnchantConfig.COMMON.allowPotionLevelBeyond255.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.allow_potion_level_beyond_255.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.allowPotionLevelBeyond255::set)
                .build());
        
        // 创建设置分类
                ConfigCategory enchant = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.enchant"));

        // 允许附魔突破原版等级上限
        enchant.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap"),
                        PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.allow_enchant_level_beyond_cap.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap::set)
                .build());

        // 附魔书每级经验消耗
        enchant.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.enchant_book_xp_cost"),
                        PotionEnchantConfig.COMMON.enchantBookXpCost.get())
                .setDefaultValue(1000)
                .setMin(1)
                .setMax(Integer.MAX_VALUE)
                .setTooltip(Component.translatable("config.potionenchant.enchant_book_xp_cost.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enchantBookXpCost::set)
                .build());

        // 附魔是否能通过原版附魔台获取
        enchant.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.discoverable_in_enchanting_table"),
                        PotionEnchantConfig.COMMON.discoverableInEnchantingTable.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.discoverable_in_enchanting_table.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.discoverableInEnchantingTable::set)
                .build());

        ConfigCategory display = builder.getOrCreateCategory(Component.translatable("config.potionenchant.category.display"));
        
        // 自定义药水HUD配置
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.custom_potion_hud"),
                        PotionEnchantConfig.COMMON.customPotionHud.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.custom_potion_hud.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.customPotionHud::set)
                .build());
        
        // 显示药水等级配置
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.show_potion_level"),
                        PotionEnchantConfig.COMMON.showPotionLevel.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.show_potion_level.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showPotionLevel::set)
                .build());
        
        // 显示文本背景框配置
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.show_text_background"),
                        PotionEnchantConfig.COMMON.showTextBackground.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.show_text_background.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showTextBackground::set)
                .build());
        
        // 最大可见效果数量配置
        display.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.max_visible_effects"),
                        PotionEnchantConfig.COMMON.maxVisibleEffects.get())
                .setDefaultValue(10)
                .setTooltip(Component.translatable("config.potionenchant.max_visible_effects.tooltip"))
                .setMin(3)
                .setMax(50)
                .setSaveConsumer(PotionEnchantConfig.COMMON.maxVisibleEffects::set)
                .build());
        
        // 显示滚动提示配置
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.show_scroll_hint"),
                        PotionEnchantConfig.COMMON.showScrollHint.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.show_scroll_hint.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.showScrollHint::set)
                .build());
        
        // HUD渲染优先级配置
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.hud_high_priority"),
                        PotionEnchantConfig.COMMON.hudHighPriority.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.hud_high_priority.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.hudHighPriority::set)
                .build());
        
        // 启用自定义药水Tooltip
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.enable_custom_potion_tooltip"),
                        PotionEnchantConfig.COMMON.enableCustomPotionTooltip.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.enable_custom_potion_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableCustomPotionTooltip::set)
                .build());
        
        // 启用原版药水描述
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.enable_vanilla_potion_description"),
                        PotionEnchantConfig.COMMON.enableVanillaPotionDescription.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.enable_vanilla_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableVanillaPotionDescription::set)
                .build());
        
        // 启用所有模组药水描述
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.enable_all_potion_description"),
                        PotionEnchantConfig.COMMON.enableAllPotionDescription.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.enable_all_potion_description.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableAllPotionDescription::set)
                .build());
        
        // 启用药水附魔独立Tooltip
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip"),
                        PotionEnchantConfig.COMMON.enablePotionEnchantTooltip.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.potionenchant.enable_potion_enchant_tooltip.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enablePotionEnchantTooltip::set)
                .build());
        // PotionCore盔甲值渲染
        display.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.potionenchant.enable_armor_value_render"),
                        PotionEnchantConfig.COMMON.enableArmorValueRender.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.potionenchant.enable_armor_value_render.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.enableArmorValueRender::set)
                .build());


        
        // 药水附魔Tooltip每列最大数量
        display.addEntry(entryBuilder.startIntField(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column"),
                        PotionEnchantConfig.COMMON.potionEnchantTooltipMaxPerColumn.get())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(50)
                .setTooltip(Component.translatable("config.potionenchant.potion_enchant_tooltip_max_per_column.tooltip"))
                .setSaveConsumer(PotionEnchantConfig.COMMON.potionEnchantTooltipMaxPerColumn::set)
                .build());

        return builder.build();
    }
}

