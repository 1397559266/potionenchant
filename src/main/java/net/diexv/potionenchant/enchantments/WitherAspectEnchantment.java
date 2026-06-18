package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

public class WitherAspectEnchantment extends Enchantment {

    public WitherAspectEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
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
    public boolean checkCompatibility(Enchantment other) {
        // 没有冲突的附魔
        return super.checkCompatibility(other);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @Override
    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
        // 使目标获得凋零效果，持续时间为 4 * level 秒
        if (target instanceof LivingEntity livingTarget) {
            // 凋零效果等级为1，持续时间为4 * level秒
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, EnchantmentConfigValues.CONFIG.witherAspectWitherSecondsPerLevel.get() * 20 * level, EnchantmentConfigValues.CONFIG.witherAspectWitherLevel.get() * level));
        }
        super.doPostAttack(attacker, target, level);
    }

    @Override
    public float getDamageBonus(int level, MobType mobType) {
        return 0.0F;
    }
}
