package net.diexv.potionenchant.item;

import net.diexv.potionenchant.client.font.DiexvFont;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PotionEnchantMod.MODID);
    public static final RegistryObject<Item> MYSTERIOUS_EMPTY_BOTTLE = ITEMS.register("mysterious_empty_bottle",
            () -> new MysteriousEmptyBottle(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ULTIMATE_POTION_AMULET = ITEMS.register("ultimate_potion_amulet",
            () -> new UltimatePotionAmulet());
    public static final RegistryObject<Item> UNIVERSAL_POTION_BOTTLE = ITEMS.register("universal_potion_bottle",
            () -> new UniversalPotionBottle(new Item.Properties().stacksTo(16)));
    // X套装护甲材质
    private static final XArmorMaterial X_ARMOR_MATERIAL = new XArmorMaterial();
    // X工具材质
    private static final XToolTier X_TOOL_TIER = new XToolTier();
    // X套装 - 头盔 (fireResistant + isDamageable=false)
    public static final RegistryObject<Item> X_HELMET = ITEMS.register("x_helmet",
            () -> new ArmorItem(X_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()) {
                @Override
                public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "potionenchant:models/armor/x_armor_layer_1.png";
                }
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X套装 - 胸甲
    public static final RegistryObject<Item> X_CHESTPLATE = ITEMS.register("x_chestplate",
            () -> new ArmorItem(X_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()) {
                @Override
                public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "potionenchant:models/armor/x_armor_layer_1.png";
                }
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X套装 - 护腿
    public static final RegistryObject<Item> X_LEGGINGS = ITEMS.register("x_leggings",
            () -> new ArmorItem(X_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()) {
                @Override
                public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "potionenchant:models/armor/x_armor_layer_2.png";
                }
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X套装 - 靴子
    public static final RegistryObject<Item> X_BOOTS = ITEMS.register("x_boots",
            () -> new ArmorItem(X_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()) {
                @Override
                public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "potionenchant:models/armor/x_armor_layer_1.png";
                }
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X工具 - 剑 (XSwordItem has isDamageable override in its own class)
    public static final RegistryObject<Item> X_SWORD = ITEMS.register("x_sword",
            () -> new XSwordItem(X_TOOL_TIER, 3, -2.4F, new Item.Properties().fireResistant()));
    // X工具 - 镐
    public static final RegistryObject<Item> X_PICKAXE = ITEMS.register("x_pickaxe",
            () -> new PickaxeItem(X_TOOL_TIER, 1, -2.8F, new Item.Properties().fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X工具 - 斧
    public static final RegistryObject<Item> X_AXE = ITEMS.register("x_axe",
            () -> new AxeItem(X_TOOL_TIER, 6.0F, -3.2F, new Item.Properties().fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // X工具 - 铲
    public static final RegistryObject<Item> X_SHOVEL = ITEMS.register("x_shovel",
            () -> new ShovelItem(X_TOOL_TIER, 1.5F, -3.0F, new Item.Properties().fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
    // 药水附魔台
    public static final RegistryObject<Item> POTION_ENCHANTING_TABLE = ITEMS.register("potion_enchanting_table",
        () -> new BlockItem(net.diexv.potionenchant.block.ModBlocks.POTION_ENCHANTING_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ULTIMATE_ENCHANT_TABLE = ITEMS.register("ultimate_enchant_table",
        () -> new BlockItem(net.diexv.potionenchant.block.ModBlocks.ULTIMATE_ENCHANT_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> UNIVERSAL_ENCHANTMENT_BOOK =
        ITEMS.register("universal_enchantment_book",
            () -> new UniversalEnchantmentBook(new Item.Properties().rarity(Rarity.EPIC)));
    // X工具 - 锄
    public static final RegistryObject<Item> X_HOE = ITEMS.register("x_hoe",
            () -> new HoeItem(X_TOOL_TIER, -3, 0.0F, new Item.Properties().fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(new IClientItemExtensions() {
                        @Override
                        public @org.jetbrains.annotations.NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                            return DiexvFont.getFont();
                        }
                    });
                }
            });
}

