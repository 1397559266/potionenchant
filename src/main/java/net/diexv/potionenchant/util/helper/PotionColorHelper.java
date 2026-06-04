package net.diexv.potionenchant.util.helper;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;

public class PotionColorHelper {

    // 获取药水效果对应的颜色
    public static int getPotionColorForEffect(MobEffect effect, ItemStack potionStack) {
        // 首先尝试从药水物品获取颜色
        if (potionStack.getItem() instanceof PotionItem) {
            return PotionUtils.getColor(potionStack);
        }

        // 如果无法获取，使用效果自带的颜色
        try {
            return effect.getColor();
        } catch (Exception e) {
            return 0xFFFFFF; // 默认白色
        }
    }

    // 通过效果ID获取默认颜色
    public static int getDefaultColorForEffect(MobEffect effect) {
        try {
            return effect.getColor();
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
