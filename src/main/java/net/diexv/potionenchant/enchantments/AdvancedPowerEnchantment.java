package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

public class AdvancedPowerEnchantment extends Enchantment {

    public AdvancedPowerEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BOW,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        // 通过注册表名称检查是否与原版力量附魔冲突
        String otherEnchantmentName = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(other).toString();
        return !otherEnchantmentName.equals("minecraft:power") && super.checkCompatibility(other);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // 可以附魔在弓和弩上
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return this.canEnchant(stack);
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
