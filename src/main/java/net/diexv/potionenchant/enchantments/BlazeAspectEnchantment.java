package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.FireAspectEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;

public class BlazeAspectEnchantment extends Enchantment {

    public BlazeAspectEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + 20 * (level - 1);
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
    @Override
    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
        // 使目标着火，持续时间为 4 * level 秒
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.setSecondsOnFire(4 * level);
        }
        super.doPostAttack(attacker, target, level);
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        // 与火焰附加不兼容
        return super.checkCompatibility(other) && !(other instanceof FireAspectEnchantment);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @Override
    public float getDamageBonus(int level, MobType mobType) {
        // 这个附魔的主要效果在事件处理器中实现
        return 0.0F;
    }
}
