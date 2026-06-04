package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class BarrageEnchantment extends Enchantment {

    public BarrageEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.BOW,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        // 能在附魔台获得
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return EnchantmentCategory.BOW.canEnchant(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return true; // 能在附魔台附魔
    }
}
