package net.diexv.potionenchant.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class XToolTier implements Tier {
    @Override
    public int getUses() {
        return Integer.MAX_VALUE;
    }

    @Override
    public float getSpeed() {
        return 8.0F; // 与钻石工具相同
    }

    @Override
    public float getAttackDamageBonus() {
        return 3.0F; // 与钻石工具相同
    }

    @Override
    public int getLevel() {
        return 3; // 与钻石工具相同（钻石等级）
    }

    @Override
    public int getEnchantmentValue() {
        return 10; // 与钻石工具相同
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY; // 暂时设置为空，可以根据需要修改
    }


}
