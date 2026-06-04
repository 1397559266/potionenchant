package net.diexv.potionenchant.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

public class PotionBaneEnchantment extends Enchantment {

    public PotionBaneEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 5 + 8 * (level - 1);
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
        // 没有冲突的附魔
        return super.checkCompatibility(other);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @Override
    public float getDamageBonus(int level, MobType mobType) {
        return 0.0F;
    }

    // 检查目标是否有药水效果
    public static boolean hasPotionEffects(LivingEntity target) {
        Collection<MobEffectInstance> effects = target.getActiveEffects();
        return !effects.isEmpty();
    }

    // 获取伤害加成（每级提高50%伤害）
    public static float getDamageBonus(int level) {
        return 1.0f * level;
    }
}
