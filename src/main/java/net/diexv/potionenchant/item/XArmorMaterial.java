package net.diexv.potionenchant.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class XArmorMaterial implements ArmorMaterial {
    private static final int[] HEALTH_PER_SLOT = new int[]{13, 16, 15, 11};
    private static final int[] DEFENSE_PER_SLOT = new int[]{6, 16, 12, 6}; // 钻石盔甲的两倍

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> DEFENSE_PER_SLOT[3];
            case LEGGINGS -> DEFENSE_PER_SLOT[2];
            case CHESTPLATE -> DEFENSE_PER_SLOT[1];
            case HELMET -> DEFENSE_PER_SLOT[0];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 10; // 与钻石盔甲相同
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND; // 使用钻石盔甲的装备音效
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY; // 暂时设置为空，可以根据需要修改
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getName() {
        return "x_armor";
    }

    @Override
    public float getToughness() {
        return 2.0F; // 与钻石盔甲相同
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F; // 与钻石盔甲相同
    }
}