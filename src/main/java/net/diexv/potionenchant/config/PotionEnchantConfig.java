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

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
        final Pair<ClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    // ===== Server Config (gameplay, auto-synced) =====
    public static class ServerConfig {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedEffects;
        public final ForgeConfigSpec.BooleanValue limitArmorEnchants;
        public final ForgeConfigSpec.IntValue maxArmorEnchants;
        public final ForgeConfigSpec.BooleanValue limitAllEnchants;
        public final ForgeConfigSpec.IntValue maxAllEnchants;
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
        public final ForgeConfigSpec.BooleanValue enableCustomPotionTooltip;
        public final ForgeConfigSpec.BooleanValue enableVanillaPotionDescription;
        public final ForgeConfigSpec.BooleanValue enableAllPotionDescription;
        public final ForgeConfigSpec.BooleanValue enablePotionEnchantTooltip;
        public final ForgeConfigSpec.IntValue potionEnchantTooltipMaxPerColumn;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Potion Enchant Configuration - Server (Synced)").push("general");

            blacklistedEffects = builder
                .comment("List of potion effects that cannot be enchanted.", "Format: modid:effect_name", "Default: []")
                .defineListAllowEmpty("blacklisted_effects", List.of(), o -> o instanceof String);

            limitArmorEnchants = builder
                .comment("Limit potion enchantments on armor pieces.", "Default: false")
                .define("limit_armor_enchants", false);
            maxArmorEnchants = builder
                .comment("Max potion enchants per armor piece.", "Default: 2")
                .defineInRange("max_armor_enchants", 2, 1, Integer.MAX_VALUE);

            limitAllEnchants = builder
                .comment("Limit potion enchantments for all items.", "Default: false")
                .define("limit_all_enchants", false);
            maxAllEnchants = builder
                .comment("Max potion effects on all items.", "Default: 3")
                .defineInRange("max_all_enchants", 3, 1, Integer.MAX_VALUE);

            ultimatePotionAmuletLootChance = builder
                .comment("Loot chance for Ultimate Potion Amulet (0-100%).", "Default: 1")
                .defineInRange("ultimate_potion_amulet_loot_chance", 1, 0, 100);

            maxPotionEnchantLevel = builder
                .comment("Max level for potion enchantments.", "Default: 100")
                .defineInRange("max_potion_enchant_level", 100, 1, Integer.MAX_VALUE);
            maxPotionEnchantLevelPerItem = builder
                .comment("Max potion enchant level per equipment piece.", "Default: 5")
                .defineInRange("max_potion_enchant_level_per_item", 5, 1, Integer.MAX_VALUE);

            ultimateTableXpCostPerLevel = builder
                .comment("XP cost per level for Ultimate Table.", "Default: 1000")
                .defineInRange("ultimate_table_xp_cost_per_level", 1000, 1, 99999999);

            allowCurioPotionEnchant = builder
                .comment("Allow potion enchants on Curios accessories.", "Default: true")
                .define("allow_curio_potion_enchant", true);
            allowPotionLevelBeyond255 = builder
                .comment("Allow potion levels beyond 255.", "Default: false")
                .define("allow_potion_level_beyond_255", false);

            builder.comment("Enchant Settings").push("enchant");

            allowEnchantLevelBeyondCap = builder
                .comment("Allow enchant levels to break vanilla cap.", "Default: false")
                .translation("config.potionenchant.allow_enchant_level_beyond_cap")
                .define("allow_enchant_level_beyond_cap", false);
            enchantBookXpCost = builder
                .comment("XP cost per enchant level for Universal Book.", "Default: 1000")
                .defineInRange("enchant_book_xp_cost", 1000, 1, Integer.MAX_VALUE);
            discoverableInEnchantingTable = builder
                .comment("Mod enchants obtainable via vanilla table.", "Default: false")
                .define("discoverable_in_enchanting_table", false);
            enchantBookChestLoot = builder
                .comment("Mod enchant books in chest loot.", "Default: false")
                .define("enchant_book_chest_loot", false);
            enchantBookVillagerTrades = builder
                .comment("Mod enchant books from villagers.", "Default: false")
                .define("enchant_book_villager_trades", false);

            builder.pop();

            builder.comment("Tooltip Settings").push("tooltip");
            enableCustomPotionTooltip = builder
                .comment("Enable custom potion tooltip.", "Default: true")
                .define("enable_custom_potion_tooltip", true);
            enableVanillaPotionDescription = builder
                .comment("Enable description for vanilla potions.", "Default: true")
                .define("enable_vanilla_potion_description", true);
            enableAllPotionDescription = builder
                .comment("Enable description for all potions.", "Default: true")
                .define("enable_all_potion_description", true);
            enablePotionEnchantTooltip = builder
                .comment("Enable potion enchant tooltips.", "Default: true")
                .define("enable_potion_enchant_tooltip", true);
            potionEnchantTooltipMaxPerColumn = builder
                .comment("Max enchants per tooltip column.", "Default: 5")
                .defineInRange("potion_enchant_tooltip_max_per_column", 5, 1, 50);

            builder.pop();
            builder.pop();
        }
    }

    // ===== Client Config (display, local only) =====
    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue customPotionHud;
        public final ForgeConfigSpec.BooleanValue showPotionLevel;
        public final ForgeConfigSpec.BooleanValue showTextBackground;
        public final ForgeConfigSpec.IntValue maxVisibleEffects;
        public final ForgeConfigSpec.BooleanValue showScrollHint;
        public final ForgeConfigSpec.BooleanValue hudHighPriority;
        public final ForgeConfigSpec.BooleanValue enableArmorValueRender;
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
        // ===== 用户自定义资源（来自 config/potionenchant/menu/） =====
        public final ForgeConfigSpec.ConfigValue<String> menuBackgroundFile;
        public final ForgeConfigSpec.ConfigValue<String> menuIconFile;
        public final ForgeConfigSpec.ConfigValue<String> menuMusicFile;
        public final ForgeConfigSpec.ConfigValue<String> menuTitleText;
        public final ForgeConfigSpec.ConfigValue<String> particleTintColor;
        public final ForgeConfigSpec.ConfigValue<String> buttonHighlightColor;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Potion Enchant Configuration - Client (Local)").push("display");

            customPotionHud = builder
                .comment("Enable custom Potion HUD.", "Default: true")
                .define("custom_potion_hud", true);
            showPotionLevel = builder
                .comment("Show potion level in HUD.", "Default: true")
                .define("show_potion_level", true);
            showTextBackground = builder
                .comment("Show text background in HUD.", "Default: true")
                .define("show_text_background", true);
            maxVisibleEffects = builder
                .comment("Max visible effects in HUD.", "Default: 8")
                .defineInRange("max_visible_effects", 8, 1, 100);
            showScrollHint = builder
                .comment("Show scroll hint in HUD.", "Default: true")
                .define("show_scroll_hint", true);
            hudHighPriority = builder
                .comment("High priority HUD rendering.", "Default: true")
                .define("hud_high_priority", true);
            enableArmorValueRender = builder
                .comment("Enable armor value rendering.", "Default: true")
                .define("enable_armor_value_render", true);

            builder.comment("Main Menu").push("main_menu");
            enableCustomMainMenu = builder
                .comment("Enable custom main menu.", "Default: false")
                .define("enable_custom_main_menu", false);
            customMainMenuMusic = builder
                .comment("Custom main menu music.", "Default: []")
                .defineListAllowEmpty("custom_main_menu_music", List.of("potionenchant:menu_music"), o -> o instanceof String);
            enableMenuParallax = builder
                .comment("Enable menu parallax.", "Default: true")
                .define("enable_menu_parallax", true);
            menuParallaxMaxOffset = builder
                .comment("Max parallax offset.", "Default: 27")
                .defineInRange("menu_parallax_max_offset", 27, 1, 100);
            enableMenuVignette = builder
                .comment("Enable menu vignette.", "Default: true")
                .define("enable_menu_vignette", true);
            menuFogRange = builder
                .comment("Menu fog range.", "Default: 3")
                .defineInRange("menu_fog_range", 3, 1, 1000);

            menuBackgroundFile = builder
                .comment("Custom menu background image file (from config/potionenchant/menu/menu/ folder).", "Default: none")
                .define("menu_background_file", "main_menu_bg");
            menuIconFile = builder
                .comment("Custom menu icon/logo image file (from config/potionenchant/menu/icon/ folder).", "Default: none")
                .define("menu_icon_file", "main_menu_logo");
            menuMusicFile = builder
                .comment("Custom menu music file (from config/potionenchant/menu/music/ folder).", "Default: none")
                .define("menu_music_file", "menu_music");
            menuTitleText = builder
                .comment("Custom main menu title text (leave empty for default).", "Default: ''")
                .define("menu_title_text", "");
            buttonHighlightColor = builder
                .comment("Button border highlight color preset.", "Options: default, red, orange, yellow, green, blue, indigo, violet", "Default: default")
                .define("button_highlight_color", "default");
            builder.pop();

            builder.comment("Particles").push("particles");
            particleBaseSize = builder.comment("Particle base size.", "Default: 21").defineInRange("particle_base_size", 21, 1, 64);
            particleSizeSpread = builder.comment("Particle size spread.", "Default: 17").defineInRange("particle_size_spread", 17, 0, 64);
            particleSpeedH = builder.comment("Particle horizontal speed.", "Default: 17").defineInRange("particle_speed_h", 17, 0, 20);
            particleSpeedV = builder.comment("Particle vertical speed.", "Default: 8").defineInRange("particle_speed_v", 8, 0, 20);
            particleSpeedVSpread = builder.comment("Particle vertical speed spread.", "Default: 25").defineInRange("particle_speed_v_spread", 25, 0, 20);
            particleFadeIn = builder.comment("Particle fade-in (ticks).", "Default: 25").defineInRange("particle_fade_in", 25, 0, 100);
            particleFadeOut = builder.comment("Particle fade-out (ticks).", "Default: 40").defineInRange("particle_fade_out", 40, 0, 100);
            particleGoUp = builder.comment("Particles float upward.", "Default: true").define("particle_go_up", true);
            particleTintColor = builder
                .comment("Particle tint color (hex RGB/RGBA, e.g. '#FF00FF' for magenta, or '' for random).", "Default: '' (random)")
                .define("particle_tint_color", "");
            particleMaxCount = builder.comment("Max particles on screen.", "Default: 25").defineInRange("particle_max_count", 25, 1, 500);
            particleFadeYStart = builder.comment("Particle fade Y start.", "Default: 10").defineInRange("particle_fade_y_start", 10, 0, 256);
            particleSpawnRate = builder.comment("Particle spawn rate/sec.", "Default: 2").defineInRange("particle_spawn_rate", 2, 1, 20);
            mouseTrailSize = builder.comment("Trail particle size.", "Default: 32").defineInRange("mouse_trail_size", 32, 4, 64);
            mouseTrailLifetime = builder.comment("Trail lifetime (ms).", "Default: 800").defineInRange("mouse_trail_lifetime", 800, 200, 10000);
            mouseTrailSpawnInterval = builder.comment("Trail spawn interval (ms).", "Default: 30").defineInRange("mouse_trail_spawn_interval", 30, 10, 200);
            mouseTrailFadeDelay = builder.comment("Trail fade delay (%).", "Default: 0").defineInRange("mouse_trail_fade_delay", 0, 0, 100);
            mouseTrailClickCount = builder.comment("Trail click count.", "Default: 5").defineInRange("mouse_trail_click_count", 5, 0, 100);

            builder.pop();
            builder.pop();
        }
    }

    // ===== Static helper methods =====
    public static boolean isEffectBlacklisted(ResourceLocation effectId) {
        return SERVER.blacklistedEffects.get().contains(effectId.toString());
    }
    public static boolean isEffectBlacklisted(String effectId) {
        return SERVER.blacklistedEffects.get().contains(effectId);
    }
    public static boolean isPotionBlacklisted(Potion potion) {
        return potion.getEffects().stream().anyMatch(ei -> {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(ei.getEffect());
            return id != null && isEffectBlacklisted(id);
        });
    }
    public static boolean isPotionBlacklisted(ResourceLocation potionId) {
        Potion p = ForgeRegistries.POTIONS.getValue(potionId);
        return p != null && isPotionBlacklisted(p);
    }
    @SuppressWarnings("removal")
    public static Set<ResourceLocation> getBlacklistedEffects() {
        return SERVER.blacklistedEffects.get().stream().map(ResourceLocation::new).collect(Collectors.toSet());
    }
    public static Set<ResourceLocation> getBlacklistedPotions() {
        Set<ResourceLocation> r = new HashSet<>();
        Set<ResourceLocation> bl = getBlacklistedEffects();
        for (Potion p : ForgeRegistries.POTIONS.getValues()) {
            ResourceLocation id = ForgeRegistries.POTIONS.getKey(p);
            if (id != null && p.getEffects().stream().anyMatch(ei -> {
                ResourceLocation eid = ForgeRegistries.MOB_EFFECTS.getKey(ei.getEffect());
                return eid != null && bl.contains(eid);
            })) { r.add(id); }
        }
        return r;
    }
    public static void onConfigReload() {}
}
