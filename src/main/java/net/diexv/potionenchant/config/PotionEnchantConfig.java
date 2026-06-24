package net.diexv.potionenchant.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class PotionEnchantConfig {

    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedEffects;
        public final ForgeConfigSpec.BooleanValue limitArmorEnchants;
        public final ForgeConfigSpec.IntValue maxArmorEnchants;

        public final ForgeConfigSpec.BooleanValue limitAllEnchants;
        public final ForgeConfigSpec.IntValue maxAllEnchants;
        
        public final ForgeConfigSpec.BooleanValue customPotionHud;
        public final ForgeConfigSpec.BooleanValue showPotionLevel;
        public final ForgeConfigSpec.BooleanValue showTextBackground;
        public final ForgeConfigSpec.IntValue maxVisibleEffects;
        public final ForgeConfigSpec.BooleanValue showScrollHint;
        public final ForgeConfigSpec.BooleanValue hudHighPriority;
        public final ForgeConfigSpec.BooleanValue enableArmorValueRender;

        
        public final ForgeConfigSpec.BooleanValue enableCustomPotionTooltip;
        public final ForgeConfigSpec.BooleanValue enableVanillaPotionDescription;
        public final ForgeConfigSpec.BooleanValue enableAllPotionDescription;
        
        public final ForgeConfigSpec.BooleanValue enablePotionEnchantTooltip;
        
        public final ForgeConfigSpec.IntValue potionEnchantTooltipMaxPerColumn;
        
        public final ForgeConfigSpec.BooleanValue enableCustomMainMenu;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> customMainMenuMusic;
        public final ForgeConfigSpec.BooleanValue enableMenuParallax;
        public final ForgeConfigSpec.IntValue menuParallaxMaxOffset;
        public final ForgeConfigSpec.BooleanValue enableMenuVignette;
        public final ForgeConfigSpec.IntValue menuFogRange;
        public final ForgeConfigSpec.IntValue particleBaseSize;
        public final ForgeConfigSpec.IntValue particleSizeSpread;
        public final ForgeConfigSpec.IntValue particleSpeedH;
        public final ForgeConfigSpec.IntValue particleSpeedV;
        public final ForgeConfigSpec.IntValue particleSpeedVSpread;
        public final ForgeConfigSpec.IntValue particleFadeIn;
        public final ForgeConfigSpec.IntValue particleFadeOut;
        public final ForgeConfigSpec.BooleanValue particleGoUp;
        public final ForgeConfigSpec.IntValue particleMaxCount;
        public final ForgeConfigSpec.IntValue particleFadeYStart;
        public final ForgeConfigSpec.IntValue particleSpawnRate;
        public final ForgeConfigSpec.IntValue mouseTrailSize;
        public final ForgeConfigSpec.IntValue mouseTrailLifetime;
        public final ForgeConfigSpec.IntValue mouseTrailSpawnInterval;
        public final ForgeConfigSpec.IntValue mouseTrailFadeDelay;
        public final ForgeConfigSpec.IntValue mouseTrailClickCount;
        
        public final ForgeConfigSpec.IntValue ultimatePotionAmuletLootChance;
        
        public final ForgeConfigSpec.IntValue maxPotionEnchantLevel;
        public final ForgeConfigSpec.IntValue maxPotionEnchantLevelPerItem;
        public final ForgeConfigSpec.IntValue ultimateTableXpCostPerLevel;
        
        public final ForgeConfigSpec.BooleanValue allowCurioPotionEnchant;
        public final ForgeConfigSpec.BooleanValue allowPotionLevelBeyond255;
    public final ForgeConfigSpec.BooleanValue allowEnchantLevelBeyondCap;
        public final ForgeConfigSpec.IntValue enchantBookXpCost;
        public final ForgeConfigSpec.BooleanValue discoverableInEnchantingTable;
        public final ForgeConfigSpec.BooleanValue enchantBookChestLoot;
        public final ForgeConfigSpec.BooleanValue enchantBookVillagerTrades;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Potion Enchant Configuration")
                    .push("general");

            blacklistedEffects = builder
                    .comment("List of potion effects that cannot be enchanted onto armor or tools.",
                            "Format: modid:effect_name",
                            "Example: [\"minecraft:strength\", \"minecraft:jump_boost\"]",
                            "This will disable ALL potion variants (normal, long, strong) containing these effects.",
                            "不能附魔到盔甲或工具上的药水效果列表。",
                            "格式：modid:effect_name",
                            "示例：[\"minecraft:strength\", \"minecraft:jump_boost\"]",
                            "这将禁用包含这些效果的所有药水变种（普通、延长、强化）。"
                    )
                    .defineList("blacklisted_effects",
                            Arrays.asList(
                            ),
                            obj -> obj instanceof String);

            limitArmorEnchants = builder
                    .comment("",
                            "Whether to limit the number of potion enchantments on each armor piece.",
                            "是否限制每件盔甲能附魔的药水效果数量。",
                            "Default: false (关闭限制功能)")
                    .define("limit_armor_enchants", false);

            maxArmorEnchants = builder
                    .comment("Maximum number of potion enchantments allowed on each armor piece.",
                            "Only effective if limit_armor_enchants is true.",
                            "每件盔甲最多能附魔的药水效果数量。",
                            "仅在limit_armor_enchants为true时生效。",
                            "Default: 2")
                    .defineInRange("max_armor_enchants", 2, 1, Integer.MAX_VALUE);

            // 全局限制配置项
            limitAllEnchants = builder
                    .comment("",
                            "Whether to limit the number of potion enchantments for all items",
                            "是否限制所有物品的药水附魔数量")
                    .define("limit_all_enchants", false);

            maxAllEnchants = builder
                    .comment("Maximum number of potion effects that can be enchanted on all items",
                            "所有物品可附魔的最大药水效果数量")
                    .defineInRange("max_all_enchants", 3, 1, Integer.MAX_VALUE);
            
            
            ultimatePotionAmuletLootChance = builder
                    .comment("Chance for Ultimate Potion Amulet to generate in loot tables (0-100%)",
                            "终极药水护符在战利品表中的生成概率（0-100%）",
                            "0 means never generate, 100 means always generate when conditions are met",
                            "0表示不生成，100表示满足条件时必定生成",
                            "Default: 1")
                    .defineInRange("ultimate_potion_amulet_loot_chance", 1, 0, 100);
            
            maxPotionEnchantLevel = builder
                    .comment("Maximum level for potion enchantments on equipped items",
                            "装备的药水附魔等级上限",
                            "Default: 100")
                    .defineInRange("max_potion_enchant_level", 100, 1, Integer.MAX_VALUE);

            maxPotionEnchantLevelPerItem = builder
                    .comment("Maximum potion enchant level per piece of equipment",
                            "每件装备的药水附魔等级上限（等级叠加不能超过此值）",
                            "Default: 5")
                    .defineInRange("max_potion_enchant_level_per_item", 5, 1, Integer.MAX_VALUE);
            
            
            ultimateTableXpCostPerLevel = builder
                    .comment("XP points cost per level for the Ultimate Potion Enchanting Table",
                            "终极药水附魔台每级消耗的经验值点数",
                            "Default: 1000")
                    .defineInRange("ultimate_table_xp_cost_per_level", 1000, 1, 99999999);
            
            allowCurioPotionEnchant = builder
                    .comment("Allow potion enchantments on Curios accessories items",
                            "是否允许饰品进行药水附魔",
                            "Default: true (允许)")
                    .define("allow_curio_potion_enchant", true);

            allowPotionLevelBeyond255 = builder
                    .comment("Allow potion enchantment levels to exceed vanilla limit of 255",
                            "允许药水附魔等级突破原版255级上限",
                            "When enabled, potion effects can have levels beyond 255 (up to Integer.MAX_VALUE)",
                            "启用后，药水效果可以拥有超过255的等级（最高可达Integer.MAX_VALUE）",
                            "Default: false (关闭)")
                    .define("allow_potion_level_beyond_255", false);
            
            builder.comment("Enchant Settings")
                    .push("enchant");

            allowEnchantLevelBeyondCap = builder
                    .comment("Allow enchantment levels to break vanilla cap (no upper limit, can exceed 255)",
                            "允许附魔等级突破原版上限（无限制，可超过255级）",
                            "When enabled, enchantments applied via Universal Enchantment Book have no level cap",
                            "启用后，通过万能附魔书应用的附魔等级无上限",
                            "Default: false (关闭)")
                    .translation("config.potionenchant.allow_enchant_level_beyond_cap")
                    .define("allow_enchant_level_beyond_cap", false);

            enchantBookXpCost = builder
                    .comment("Experience level cost per enchantment level for the Universal Enchantment Book",
                            "万能附魔书每级附魔消耗的经验值（非等级），默认1000",
                            "Default: 1000 (experience points)")
                    .defineInRange("enchant_book_xp_cost", 1000, 1, Integer.MAX_VALUE);

            discoverableInEnchantingTable = builder
                    .comment("Whether mod enchantments can be obtained via vanilla enchanting table",
                            "本模组的附魔是否能通过原版附魔台获取",
                            "Default: false (关闭)")
                    .define("discoverable_in_enchanting_table", false);

            enchantBookChestLoot = builder
                    .comment("",
                            "Whether mod enchantment books can appear in chest loot",
                            "本模组的附魔书是否能通过箱子获取",
                            "Default: true (开启)")
                    .define("enchant_book_chest_loot", true);

            enchantBookVillagerTrades = builder
                    .comment("",
                            "Whether mod enchantment books can be obtained via villager trading",
                            "本模组的附魔书是否能通过村民交易获取",
                            "Default: true (开启)")
                    .define("enchant_book_villager_trades", true);

            builder.pop();

            builder.comment("Display Settings")
                    .push("display");
            
            customPotionHud = builder
                    .comment("Enable custom potion effect HUD (text-based instead of icons)",
                            "启用自定义药水效果HUD（文字显示替代图标）",
                            "Default: true (开启)")
                    .define("custom_potion_hud", true);
            
            showPotionLevel = builder
                    .comment("Show potion effect level in custom HUD",
                            "在自定义HUD中显示药水等级",
                            "Default: true (开启)")
                    .define("show_potion_level", true);
            
            showTextBackground = builder
                    .comment("Show text background box in custom HUD",
                            "在自定义HUD中显示文本背景框",
                            "Default: false (关闭)")
                    .define("show_text_background", false);
            
            maxVisibleEffects = builder
                    .comment("Maximum number of visible potion effects at once (scroll to see more)",
                            "同时显示的最大药水效果数量（滚动查看更多）",
                            "Default: 100")
                    .defineInRange("max_visible_effects", 10, 3, 50);
            
            showScrollHint = builder
                    .comment("Show scroll hint text (Tab+滚轮) in custom HUD",
                            "在自定义HUD中显示滚动提示文字（Tab+滚轮）",
                            "Default: true (开启)")
                    .define("show_scroll_hint", true);
            
            hudHighPriority = builder
                    .comment("Set custom potion HUD rendering priority to highest (renders above other overlays)",
                            "将自定义药水HUD渲染优先级调至最高（在其他覆盖层之上渲染）",
                            "Default: true (开启)")
                    .define("hud_high_priority", true);
            
            enableArmorValueRender = builder
                    .comment("Enable PotionCore armor value rendering (extra armor overlays)",
                            "启用药水核心盔甲值渲染（额外护甲覆盖层）",
                            "Default: true (开启)")
                    .define("enable_armor_value_render", true);

            enableCustomPotionTooltip = builder
                    .comment("Enable custom tooltip for potions to show effect descriptions",
                            "启用药水的自定义tooltip以显示效果描述",
                            "Default: true (开启)")
                    .define("enable_custom_potion_tooltip", true);
            
            enableVanillaPotionDescription = builder
                    .comment("Show description for vanilla Minecraft potions",
                            "显示原版Minecraft药水的描述",
                            "Default: false (关闭)")
                    .define("enable_vanilla_potion_description", false);
            
            enableAllPotionDescription = builder
                    .comment("Try to show descriptions for all potions from any mod",
                            "Requires language file key: effect.xxx.xxx.description",
                            "尝试显示所有模组药水的描述",
                            "需要语言文件键：effect.xxx.xxx.description",
                            "Default: false (关闭)")
                    .define("enable_all_potion_description", false);
            
            enablePotionEnchantTooltip = builder
                    .comment("Enable independent tooltip for potion enchantments",
                            "When enabled, items with potion enchantments will show a custom tooltip with enchantment details",
                            "启用药水附魔的独立tooltip",
                            "启用后，拥有药水附魔的物品将显示包含附魔详情的自定义tooltip",
                            "Default: false (关闭)")
                    .define("enable_potion_enchant_tooltip", false);
            
            potionEnchantTooltipMaxPerColumn = builder
                    .comment("Maximum number of potion enchantments per column in tooltip (vertical auto-wrap)",
                            "Tooltip中药水附魔每列的最大数量（竖向自动换列）",
                            "Default: 100")
                    .defineInRange("potion_enchant_tooltip_max_per_column", 10, 1, 50);
            
            builder.pop();

            builder.comment("Custom Main Menu Configuration")
                    .push("custom_main_menu");

            enableCustomMainMenu = builder
                    .comment("Enable custom main menu with custom title, background and music.",
                            "启用自定义主菜单界面（自定义标题、背景和音乐）。",
                            "Default: false (关闭)")
                    .define("enable_custom_main_menu", false);

            customMainMenuMusic = builder
                    .comment("List of custom music tracks to play on the main menu.",
                            "Format: modid:sound_event_name",
                            "Example: [\"potionenchant:menu_music\"]",
                            "The first track will be played by default if the list is not empty.",
                            "在主菜单播放的自定义音乐列表。",
                            "格式：modid:sound_event_name",
                            "示例：[\"potionenchant:menu_music\"]",
                            "如果列表不为空，将播放第一首曲目。")
                    .defineList("custom_main_menu_music",
                            Arrays.asList("potionenchant:menu_music"),
                            obj -> obj instanceof String);


            enableMenuParallax = builder
                    .comment("Enable parallax effect on custom menu background (follows mouse movement).",
                            "启用自定义菜单背景视差效果（跟随鼠标移动）。",
                            "Default: true (开启)")
                    .define("enable_menu_parallax", true);

            menuParallaxMaxOffset = builder
                    .comment("Maximum pixel offset for menu background parallax effect.",
                            "Each direction is capped independently at this value.",
                            "菜单背景视差效果的最大像素偏移量（每个方向独立限制）。",
                            "Default: 15 pixels")
                    .defineInRange("menu_parallax_max_offset", 27, 5, 60);

            enableMenuVignette = builder
                    .comment("Enable edge vignette (darkening) on custom menu background.",
                            "在自定义菜单背景边缘启用暗角效果。",
                            "Default: true (开启)")
                    .define("enable_menu_vignette", true);

            menuFogRange = builder
                    .comment("Fog range for menu edge blur effect (percentage of screen).",
                            "菜单边缘模糊范围（屏幕尺寸百分比）。",
                            "Default: 33 (1/3 of screen each side)")
                    .defineInRange("menu_fog_range", 3, 0, 100);

            // ====== 粒子设置 ======
            particleBaseSize = builder
                    .comment("Base size of menu particles in pixels.",
                            "粒子基础大小（像素）。",
                            "Default: 16")
                    .defineInRange("particle_base_size", 21, 8, 64);

            particleSizeSpread = builder
                    .comment("Random size addition range (0 = no randomness).",
                            "粒子大小随机浮动范围（0=固定大小）。",
                            "Default: 12")
                    .defineInRange("particle_size_spread", 17, 0, 48);

            particleSpeedH = builder
                    .comment("Horizontal movement speed (0 = no horizontal movement).",
                            "水平移动速度（0=不水平移动）。",
                            "Default: 12")
                    .defineInRange("particle_speed_h", 12, 0, 100);

            particleSpeedV = builder
                    .comment("Base vertical speed.",
                            "垂直基础移动速度。",
                            "Default: 8")
                    .defineInRange("particle_speed_v", 8, 1, 100);

            particleSpeedVSpread = builder
                    .comment("Random vertical speed addition.",
                            "垂直速度随机浮动。",
                            "Default: 25")
                    .defineInRange("particle_speed_v_spread", 25, 0, 100);

            particleFadeIn = builder
                    .comment("Duration of fade-in effect (ticks).",
                            "淡入持续时间（刻）。",
                            "Default: 25")
                    .defineInRange("particle_fade_in", 25, 1, 100);

            particleFadeOut = builder
                    .comment("Duration of fade-out effect before death (ticks).",
                            "淡出持续时间（粒子消失前，刻）。",
                            "Default: 40")
                    .defineInRange("particle_fade_out", 40, 1, 100);

            particleGoUp = builder
                    .comment("Particle movement direction: true = upward, false = downward.",
                            "粒子运动方向：true=向上，false=向下。",
                            "Default: true (向上)")
                    .define("particle_go_up", true);

            particleMaxCount = builder
                    .comment("Maximum number of menu particles.",
                            "主菜单粒子最大数量。",
                            "Default: 25")
                    .defineInRange("particle_max_count", 25, 5, 100);

            particleFadeYStart = builder
                    .comment("Screen percentage where particles start fading out (0=bottom, 100=top).",
                            "粒子淡出起始位置（屏幕百分比，0=底部 100=顶部）。",
                            "Default: 75")
                    .defineInRange("particle_fade_y_start", 10, 0, 100);

            particleSpawnRate = builder
                    .comment("Particle spawn rate per second.",
                            "每秒生成的粒子数量。",
                            "Default: 5")
                    .defineInRange("particle_spawn_rate", 2, 1, 20);

            mouseTrailSize = builder
                    .comment("Mouse trail particle base size in pixels.",
                            "鼠标拖尾粒子基础大小（像素）。",
                            "Default: 16")
                    .defineInRange("mouse_trail_size", 32, 4, 64);

            mouseTrailLifetime = builder
                    .comment("Mouse trail particle lifetime in milliseconds.",
                            "鼠标拖尾粒子存在时间（毫秒）。",
                            "Default: 800")
                    .defineInRange("mouse_trail_lifetime", 800, 200, 10000);

            mouseTrailSpawnInterval = builder
                    .comment("Mouse trail particle spawn interval in milliseconds.",
                            "鼠标拖尾粒子生成间隔（毫秒，越小越密集）。",
                            "Default: 30")
                    .defineInRange("mouse_trail_spawn_interval", 30, 10, 200);

            mouseTrailFadeDelay = builder
                    .comment("Percentage of lifetime before trail particles start fading (0=fade from start, 100=no fade).",
                            "拖尾粒子淡出延迟（生命周期百分比，0=立即淡出，100=永不淡出）。",
                            "Default: 0")
                    .defineInRange("mouse_trail_fade_delay", 0, 0, 100);

            mouseTrailClickCount = builder
                    .comment("Number of firework particles spawned on mouse click.",
                            "鼠标点击时生成的烟花粒子数量。",
                            "Default: 15")
                    .defineInRange("mouse_trail_click_count", 5, 0, 100);

            builder.pop();
        }
    }

    // 检查药水效果是否在黑名单中
    public static boolean isEffectBlacklisted(ResourceLocation effectId) {
        String effectString = effectId.toString();
        return COMMON.blacklistedEffects.get().contains(effectString);
    }

    // 检查药水效果是否在黑名单中（字符串版本）
    public static boolean isEffectBlacklisted(String effectId) {
        return COMMON.blacklistedEffects.get().contains(effectId);
    }

    // 检查药水是否包含黑名单中的效果
    public static boolean isPotionBlacklisted(Potion potion) {
        // 获取药水的所有效果
        return potion.getEffects().stream()
                .anyMatch(effectInstance -> {
                    MobEffect effect = effectInstance.getEffect();
                    ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                    return effectId != null && isEffectBlacklisted(effectId);
                });
    }

    // 检查药水注册名是否在黑名单中
    public static boolean isPotionBlacklisted(ResourceLocation potionId) {
        Potion potion = ForgeRegistries.POTIONS.getValue(potionId);
        return potion != null && isPotionBlacklisted(potion);
    }

    @SuppressWarnings("removal")
    // 获取所有黑名单中的药水效果
    public static Set<ResourceLocation> getBlacklistedEffects() {
        return COMMON.blacklistedEffects.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
    }

    // 获取所有包含黑名单效果的药水
    public static Set<ResourceLocation> getBlacklistedPotions() {
        Set<ResourceLocation> blacklistedPotions = new HashSet<>();
        Set<ResourceLocation> blacklistedEffects = getBlacklistedEffects();

        // 遍历所有注册的药水
        for (Potion potion : ForgeRegistries.POTIONS.getValues()) {
            ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(potion);
            if (potionId != null) {
                // 检查药水是否包含黑名单效果
                boolean containsBlacklisted = potion.getEffects().stream()
                        .anyMatch(effectInstance -> {
                            MobEffect effect = effectInstance.getEffect();
                            ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                            return effectId != null && blacklistedEffects.contains(effectId);
                        });

                if (containsBlacklisted) {
                    blacklistedPotions.add(potionId);
                }
            }
        }

        return blacklistedPotions;
    }

    // 重新加载配置时调用（可选）
    public static void onConfigReload() {
        // 可以在这里添加配置重载时的逻辑
        // 例如：清除缓存、重新计算黑名单等
    }
}





