package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;

public class AdvancedProtectionEnchantment extends Enchantment {

    public AdvancedProtectionEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR,
                new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
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
        return 4;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        // 与所有保护类附魔不兼容（包括原版保护、火焰保护、爆炸保护、弹射物保护）
        if (other instanceof ProtectionEnchantment) {
            return false;
        }
        return super.checkCompatibility(other);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        // 原版保护每级提供1点保护，高级保护是原版的5倍：每级提供5点保护
        // 原版公式：每点保护减少4%伤害，所以高级保护每级减少8%伤害
        return level * 5;
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
}
