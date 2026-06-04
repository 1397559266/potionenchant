package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;

public class AdvancedSharpnessEnchantment extends Enchantment {

    public AdvancedSharpnessEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        // 与所有伤害类附魔不兼容（包括原版锋利）
        return !(other instanceof DamageEnchantment) && super.checkCompatibility(other);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @Override
    public float getDamageBonus(int level, MobType mobType) {
        // 原版锋利每级增加 0.5 + 0.5 * level 伤害
        // 高级锋利是原版的2倍：每级增加 1.0 + 1.0 * level 伤害
        return 2.5F + 2.5F * level;
    }
}
