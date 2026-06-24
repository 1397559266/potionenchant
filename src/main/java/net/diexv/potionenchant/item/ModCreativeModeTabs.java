package net.diexv.potionenchant.item;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.potion.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PotionEnchantMod.MODID);

    @SuppressWarnings("removal")
    private static final ResourceLocation BACKGROUND_IMAGE =
             		new ResourceLocation("potionenchant", "textures/gui/tab.png");

    // 注册创造模式标签页
    public static final RegistryObject<CreativeModeTab> POTION_ENCHANT_TAB = CREATIVE_MODE_TABS.register("potion_enchant_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.POTION)) // 使用原版水瓶作为图标
                    .title(Component.translatable("itemGroup.potionenchant")) // 标签页名称
                    .displayItems((parameters, output) -> {
                        // 添加神秘的空瓶
                        output.accept(ModItems.MYSTERIOUS_EMPTY_BOTTLE.get());

                        // 添加终极药水护符
                        output.accept(ModItems.ULTIMATE_POTION_AMULET.get());

                        // 添加万能药水附魔瓶
                        output.accept(ModItems.UNIVERSAL_POTION_BOTTLE.get());

                        // 添加万能附魔书
                        output.accept(ModItems.UNIVERSAL_ENCHANTMENT_BOOK.get());

                        // 添加药水附魔台
                        output.accept(ModItems.POTION_ENCHANTING_TABLE.get());

                        // 添加终极附魔台
                        output.accept(ModItems.ULTIMATE_ENCHANT_TABLE.get());

                        // 添加X套装护甲
                        output.accept(ModItems.X_HELMET.get());
                        output.accept(ModItems.X_CHESTPLATE.get());
                        output.accept(ModItems.X_LEGGINGS.get());
                        output.accept(ModItems.X_BOOTS.get());

                        // 添加X工具
                        output.accept(ModItems.X_SWORD.get());
                        output.accept(ModItems.X_PICKAXE.get());
                        output.accept(ModItems.X_AXE.get());
                        output.accept(ModItems.X_SHOVEL.get());
                        output.accept(ModItems.X_HOE.get());

                        // 添加易伤药水
                        addPotionAndVariants(output, VulnerabilityPotion.VULNERABILITY.get());
                        addPotionAndVariants(output, VulnerabilityPotion.LONG_VULNERABILITY.get());
                        addPotionAndVariants(output, VulnerabilityPotion.STRONG_VULNERABILITY.get());

                        // 添加修补药水
                        addPotionAndVariants(output, MendingPotion.MENDING.get());
                        addPotionAndVariants(output, MendingPotion.LONG_MENDING.get());
                        addPotionAndVariants(output, MendingPotion.STRONG_MENDING.get());

                        // 添加净化药水
                        addPotionAndVariants(output, PurificationPotion.PURIFICATION.get());
                        addPotionAndVariants(output, PurificationPotion.LONG_PURIFICATION.get());

                        // 添加圣洁药水
                        addPotionAndVariants(output, SanctuaryPotion.SANCTUARY.get());
                        addPotionAndVariants(output, SanctuaryPotion.LONG_SANCTUARY.get());

                        // 添加虚空之力药水
                        addPotionAndVariants(output, VoidPowerPotion.VOID_POWER.get());
                        addPotionAndVariants(output, VoidPowerPotion.LONG_VOID_POWER.get());
                        addPotionAndVariants(output, VoidPowerPotion.STRONG_VOID_POWER.get());

                        // 添加虹吸药水
                        addPotionAndVariants(output, SiphonPotion.SIPHON.get());
                        addPotionAndVariants(output, SiphonPotion.LONG_SIPHON.get());
                        addPotionAndVariants(output, SiphonPotion.STRONG_SIPHON.get());

                        // 添加负载药水
                        addPotionAndVariants(output, OverloadPotion.OVERLOAD.get());
                        addPotionAndVariants(output, OverloadPotion.LONG_OVERLOAD.get());
                        addPotionAndVariants(output, OverloadPotion.STRONG_OVERLOAD.get());

                        // 添加暴击药水
                        addPotionAndVariants(output, CriticalStrikePotion.CRITICAL_STRIKE.get());
                        addPotionAndVariants(output, CriticalStrikePotion.LONG_CRITICAL_STRIKE.get());
                        addPotionAndVariants(output, CriticalStrikePotion.STRONG_CRITICAL_STRIKE.get());

                        // 添加脆弱药水
                        addPotionAndVariants(output, FragilityPotion.FRAGILITY.get());
                        addPotionAndVariants(output, FragilityPotion.LONG_FRAGILITY.get());

                        // 添加重生药水
                        addPotionAndVariants(output, RevivalPotion.REVIVAL.get());
                        addPotionAndVariants(output, RevivalPotion.LONG_REVIVAL.get());
                        addPotionAndVariants(output, RevivalPotion.STRONG_REVIVAL.get());

                        // 添加碎甲药水
                        addPotionAndVariants(output, ArmorBreakPotion.ARMOR_BREAK.get());
                        addPotionAndVariants(output, ArmorBreakPotion.LONG_ARMOR_BREAK.get());
                        addPotionAndVariants(output, ArmorBreakPotion.STRONG_ARMOR_BREAK.get());

                        // 添加范围扩展药水
                        addPotionAndVariants(output, RangeExtensionPotion.RANGE_EXTENSION.get());
                        addPotionAndVariants(output, RangeExtensionPotion.LONG_RANGE_EXTENSION.get());
                        addPotionAndVariants(output, RangeExtensionPotion.STRONG_RANGE_EXTENSION.get());

                        // 添加敏捷药水
                        addPotionAndVariants(output, AgilityPotion.AGILITY.get());
                        addPotionAndVariants(output, AgilityPotion.LONG_AGILITY.get());
                        addPotionAndVariants(output, AgilityPotion.STRONG_AGILITY.get());

                        // 添加连招药水
                        addPotionAndVariants(output, ComboPotion.COMBO.get());
                        addPotionAndVariants(output, ComboPotion.LONG_COMBO.get());

                        // 添加相位锁定药水
                        addPotionAndVariants(output, PhaseLockPotion.PHASE_LOCK.get());
                        addPotionAndVariants(output, PhaseLockPotion.LONG_PHASE_LOCK.get());
                        addPotionAndVariants(output, PhaseLockPotion.STRONG_PHASE_LOCK.get());

                        // 添加坚定药水
                        addPotionAndVariants(output, FirmnessPotion.FIRMNESS.get());
                        addPotionAndVariants(output, FirmnessPotion.LONG_FIRMNESS.get());
                        addPotionAndVariants(output, FirmnessPotion.STRONG_FIRMNESS.get());

                        // 添加魔法抗性药水
                        addPotionAndVariants(output, MagicResistancePotion.MAGIC_RESISTANCE.get());
                        addPotionAndVariants(output, MagicResistancePotion.LONG_MAGIC_RESISTANCE.get());
                        addPotionAndVariants(output, MagicResistancePotion.STRONG_MAGIC_RESISTANCE.get());
                        // 添加恩怨药水
                        addPotionAndVariants(output, SymbiosisPotion.SYMBIOSIS.get());
                        addPotionAndVariants(output, SymbiosisPotion.LONG_SYMBIOSIS.get());

                        // ========================================
                        // PlentyOfEnchant 移植的附魔书（最高等级）
                        // ========================================
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ELEMENTAL_AFFINITY.get(), EnchantmentRegistry.ELEMENTAL_AFFINITY.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.REFORGE.get(), EnchantmentRegistry.REFORGE.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.LIFESTEAL.get(), EnchantmentRegistry.LIFESTEAL.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ADVANCED_MENDING.get(), EnchantmentRegistry.ADVANCED_MENDING.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ASH_EXTINCTION.get(), EnchantmentRegistry.ASH_EXTINCTION.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.COMBO.get(), EnchantmentRegistry.COMBO.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.TRACKING_ARROW.get(), EnchantmentRegistry.TRACKING_ARROW.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.BARRAGE.get(), EnchantmentRegistry.BARRAGE.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.BLAZE_ASPECT.get(), EnchantmentRegistry.BLAZE_ASPECT.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.CRITICAL_STRIKE.get(), EnchantmentRegistry.CRITICAL_STRIKE.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.LIFE_LINK.get(), EnchantmentRegistry.LIFE_LINK.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.AUTO_SMELT.get(), EnchantmentRegistry.AUTO_SMELT.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.WITHER_ASPECT.get(), EnchantmentRegistry.WITHER_ASPECT.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.POTION_BANE.get(), EnchantmentRegistry.POTION_BANE.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ADVANCED_SHARPNESS.get(), EnchantmentRegistry.ADVANCED_SHARPNESS.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ADVANCED_PROTECTION.get(), EnchantmentRegistry.ADVANCED_PROTECTION.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.MANA_FOCUS.get(), EnchantmentRegistry.MANA_FOCUS.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.DAMAGE_STORAGE.get(), EnchantmentRegistry.DAMAGE_STORAGE.get().getMaxLevel())));
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EnchantmentRegistry.ADVANCED_POWER.get(), EnchantmentRegistry.ADVANCED_POWER.get().getMaxLevel())));
                    })
                    // 设置自定义背景
                    .withBackgroundLocation(BACKGROUND_IMAGE)
                    .build());


    // 添加药水及其变种（喷溅、滞留、药箭）
    private static void addPotionAndVariants(CreativeModeTab.Output output, net.minecraft.world.item.alchemy.Potion potion) {
        // 普通药水
        output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
        // 喷溅药水
        output.accept(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        // 滞留药水
        output.accept(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), potion));
        // 药箭
        output.accept(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), potion));
    }

    // 注册方法
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}



