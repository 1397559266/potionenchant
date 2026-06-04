package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;

public class CriticalStrikeEnchantment extends Enchantment {

    public CriticalStrikeEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 9;
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
    public boolean canEnchant(ItemStack stack) {
        // 可以附魔到武器和工具上
        return stack.getItem() instanceof SwordItem ||
                stack.getItem() instanceof AxeItem ||
                stack.getItem() instanceof PickaxeItem ||
                stack.getItem() instanceof ShovelItem ||
                stack.getItem() instanceof HoeItem;
    }

    @Override
    public float getDamageBonus(int level, MobType mobType) {
        return 0.0F;
    }
}
