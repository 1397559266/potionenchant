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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
                }
            });
    // X工具 - 剑 (XSwordItem has isDamageable override in its own class)
    // 攻击力：玩家基础 1 + 这里的 6 + XToolTier.getAttackDamageBonus() 3 = 10
    public static final RegistryObject<Item> X_SWORD = ITEMS.register("x_sword",
            () -> new XSwordItem(X_TOOL_TIER, 6, -2.4F, new Item.Properties().fireResistant()));
    // X工具 - 镐
    public static final RegistryObject<Item> X_PICKAXE = ITEMS.register("x_pickaxe",
            () -> new PickaxeItem(X_TOOL_TIER, 1, -2.8F, new Item.Properties().fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
                }
            });
    // 药水附魔台
    public static final RegistryObject<Item> POTION_ENCHANTING_TABLE = ITEMS.register("potion_enchanting_table",
        () -> new BlockItem(net.diexv.potionenchant.block.ModBlocks.POTION_ENCHANTING_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ULTIMATE_ENCHANT_TABLE = ITEMS.register("ultimate_enchant_table",
        () -> new BlockItem(net.diexv.potionenchant.block.ModBlocks.ULTIMATE_ENCHANT_TABLE.get(), new Item.Properties()));
    // 苦力怕药水罐
    public static final RegistryObject<Item> DIEXV_CREEPER_TANK = ITEMS.register("diexv_creeper_tank",
        () -> new BlockItem(net.diexv.potionenchant.block.ModBlocks.DIEXV_CREEPER_TANK.get(), new Item.Properties()) {
            @Override
            public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                        @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
                                        java.util.List<net.minecraft.network.chat.Component> tooltip,
                                        net.minecraft.world.item.TooltipFlag flag) {
                super.appendHoverText(stack, level, tooltip, flag);
                tooltip.add(net.minecraft.network.chat.Component
                        .translatable("item.potionenchant.dievx_creeper_tank.tooltip")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }

            /** 放置后从物品 NBT 恢复罐内药水（保留数据随方块掉落/再放置） */
            @Override
            public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
                net.minecraft.world.InteractionResult r = super.useOn(ctx);
                if (r.consumesAction()) {
                    net.minecraft.core.BlockPos bp = ctx.getClickedPos().relative(ctx.getClickedFace());
                    net.minecraft.world.level.Level lvl = ctx.getLevel();
                    if (lvl.getBlockState(bp).getBlock() instanceof net.diexv.potionenchant.block.DiexvCreeperTankBlock
                            && lvl.getBlockEntity(bp) instanceof net.diexv.potionenchant.blockentity.DiexvCreeperTankBlockEntity tank) {
                        net.minecraft.nbt.CompoundTag d = ctx.getItemInHand().getTagElement("diexv_tank_data");
                        if (d != null) {
                            tank.readDataTag(d);
                        }
                    }
                }
                return r;
            }

            @Override
            @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                    @Override
                    public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return net.diexv.potionenchant.client.renderer.DiexvCreeperTankItemRenderer.instance();
                    }
                });
            }
        });
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
                    // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
                    consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
                }
            });
    // DiexvSword（DiexvDreamItem 移植，仅特效渲染，无攻击功能）
    public static final RegistryObject<Item> DIEXV_SWORD = ITEMS.register("diexv_sword",
            () -> new DiexvSwordItem());
    // Code（code 物品移植，仅特效渲染，无实际功能）
    public static final RegistryObject<Item> CODE = ITEMS.register("code",
            () -> new CodeItem());
    // 七彩药水精华
    public static final RegistryObject<Item> RAINBOW_POTION_ESSENCE = ITEMS.register("rainbow_potion_essence",
            () -> new Item(new Item.Properties()));
}

