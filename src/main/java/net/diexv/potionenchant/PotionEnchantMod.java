package net.diexv.potionenchant;

import net.diexv.potionenchant.command.ArmorXCommand;
import net.diexv.potionenchant.command.ClearPotionEnchantCommand;
import net.diexv.potionenchant.config.ConfigManager;
import net.diexv.potionenchant.entity.ModEntities;
import net.diexv.potionenchant.handlers.*;
import net.diexv.potionenchant.block.ModBlocks;
import net.diexv.potionenchant.blockentity.ModBlockEntities;
import net.diexv.potionenchant.item.ModCreativeModeTabs;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.network.ArmorXPacketHandler;
import net.diexv.potionenchant.network.UniversalBottlePacketHandler;
import net.diexv.potionenchant.network.EnchantBookPacketHandler;
import net.diexv.potionenchant.potion.*;
import net.diexv.potionenchant.potion.PhaseLockPotion;
import net.diexv.potionenchant.potion.FirmnessPotion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.renderer.item.ItemProperties;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(PotionEnchantMod.MODID)
public class PotionEnchantMod {
    public static final String MODID = "potionenchant";
    public static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("removal")
    public PotionEnchantMod() {
        System.out.println("[PotionEnchant] ===== MOD CONSTRUCTOR START =====");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ConfigManager.registerConfigs();
        // 仅在客户端注册配置屏幕（避免服务端崩溃）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientConfigScreen();
        }

        // 注册效果和药水
        EnchantmentRegistry.ENCHANTMENTS.register(modEventBus);
        EffectRegistry.EFFECTS.register(modEventBus);
        MendingPotion.POTIONS.register(modEventBus);
        PurificationPotion.POTIONS.register(modEventBus);
        SanctuaryPotion.POTIONS.register(modEventBus);
        SiphonPotion.POTIONS.register(modEventBus);
        VulnerabilityPotion.POTIONS.register(modEventBus);
        VoidPowerPotion.POTIONS.register(modEventBus);
        OverloadPotion.POTIONS.register(modEventBus);
        CriticalStrikePotion.POTIONS.register(modEventBus);
        FragilityPotion.POTIONS.register(modEventBus);
        RevivalPotion.POTIONS.register(modEventBus);
        ArmorBreakPotion.POTIONS.register(modEventBus);
        RangeExtensionPotion.POTIONS.register(modEventBus);
        AgilityPotion.POTIONS.register(modEventBus);
        ComboPotion.POTIONS.register(modEventBus);
        PhaseLockPotion.POTIONS.register(modEventBus);
        FirmnessPotion.POTIONS.register(modEventBus);
        SymbiosisPotion.POTIONS.register(modEventBus);
        MagicResistancePotion.POTIONS.register(modEventBus);

        // 注册音效
        net.diexv.potionenchant.sound.ModSounds.register(modEventBus);
        // 注册自定义菜单音乐资源包
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onAddPackFinders);
        }

        // 注册方块
        ModBlocks.BLOCKS.register(modEventBus);

        // 注册方块实体
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // 注册物品
        ModItems.ITEMS.register(modEventBus);
        // 注册实体
        ModEntities.ENTITIES.register(modEventBus);

        // 注册创造模式标签页
        ModCreativeModeTabs.register(modEventBus);

        // 注册网络数据包处理器
        UniversalBottlePacketHandler.register();
        EnchantBookPacketHandler.register();
        ArmorXPacketHandler.register();
        net.diexv.potionenchant.network.PotionEnchantTableNetwork.register();
        net.diexv.potionenchant.network.UltimateTableNetwork.register();
        // 注册事件处理器（服务端和客户端通用）
        // 注册移植附魔的事件处理器
        MinecraftForge.EVENT_BUS.register(new net.diexv.potionenchant.handlers.EnchantmentEventHandler());
        MinecraftForge.EVENT_BUS.register(new AnvilEventHandler());
        MinecraftForge.EVENT_BUS.register(new ArmorEffectHandler());
        MinecraftForge.EVENT_BUS.register(new ToolEffectHandler());
        MinecraftForge.EVENT_BUS.register(new CuriosEffectHandler());
        MinecraftForge.EVENT_BUS.register(new CombinedEffectHandler());
        MinecraftForge.EVENT_BUS.register(new MilkDrinkHandler());
        MinecraftForge.EVENT_BUS.register(new UltimatePotionAmuletHandler());

        // ArmorXFeatureHandler 已通过 @Mod.EventBusSubscriber 自动注册，无需手动注册
        
        // 服务端清除 XSword supermode 残留状态（防止重登后永久无敌）
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            XSwordItem.clearSupermodeState(e.getEntity().getUUID());
        });

        // 仅在客户端注册Tooltip事件处理器（避免服务端加载客户端类）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new TooltipEventHandler()); }

        // 注册物品属性覆盖（用于X剑supermode贴图切换）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        LOGGER.info("Potion Enchant mod loaded successfully!");
    }

    @SuppressWarnings("removal")
    // 客户端初始化：注册ItemProperties
    private void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 注册药水附魔台方块实体渲染器
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(net.diexv.potionenchant.blockentity.ModBlockEntities.POTION_ENCHANTING_TABLE.get(), net.diexv.potionenchant.render.PotionEnchantingTableRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(net.diexv.potionenchant.blockentity.ModBlockEntities.ULTIMATE_ENCHANT_TABLE.get(), net.diexv.potionenchant.render.UltimateEnchantTableRenderer::new);

        event.enqueueWork(() -> {

            // 注册 ItemProperties
            ItemProperties.register(
                ModItems.X_SWORD.get(),
                new ResourceLocation(MODID, "supermode"),
                (stack, level, entity, seed) -> {
                    if (entity instanceof net.minecraft.world.entity.player.Player player) {
                        return net.diexv.potionenchant.item.XSwordItem.isSupermode(player.getUUID()) ? 1.0F : 0.0F;
                    }
                    return 0.0F;
                }
            );
        });    }

    // 注册 AddPackFindersEvent
    private void onAddPackFinders(net.minecraftforge.event.AddPackFindersEvent event) {
        if (event.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(consumer -> {
                var pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                    net.diexv.potionenchant.sound.CustomMenuMusicPack.PACK_ID,
                    net.minecraft.network.chat.Component.literal("PotionEnchant Menu Music"),
                    true,
                    id -> new net.diexv.potionenchant.sound.CustomMenuMusicPack(),
                    net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                    net.minecraft.server.packs.repository.Pack.Position.BOTTOM,
                    net.minecraft.server.packs.repository.PackSource.DEFAULT
                );
                if (pack != null) consumer.accept(pack);
            });
        }
    }

    // 注册命令
    private void registerCommands(RegisterCommandsEvent event) {
        ClearPotionEnchantCommand.register(event.getDispatcher());
        ArmorXCommand.register(event.getDispatcher());
    }

    // 仅在客户端执行的配置屏幕注册（避免服务端加载客户端类）
    private void registerClientConfigScreen() {
        // 委托给客户端专用类处理，该类只在客户端存在
        net.diexv.potionenchant.client.ClientProxy.registerConfigScreen();
    }

    // 辅助方法：创建 ResourceLocation
    public static ResourceLocation rl(String path) {
        return ResourceLocationHelper.fromNamespaceAndPath(MODID, path);
    }
}


