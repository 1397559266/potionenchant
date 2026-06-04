package net.diexv.potionenchant.enchantments;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class LifestealEnchantment extends Enchantment {

    public LifestealEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // 直接返回canApplyAtEnchantingTable的结果
        return canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        Item item = stack.getItem();

        // 检查是否是武器或工具
        return item instanceof SwordItem ||
                item instanceof AxeItem ||
                item instanceof TridentItem ||
                item instanceof PickaxeItem ||
                item instanceof ShovelItem ||
                item instanceof HoeItem;
    }
}
