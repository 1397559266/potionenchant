package net.diexv.potionenchant.enchantment;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.nbt.CompoundTag;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionEnchantment extends Enchantment {
    private final MobEffect effect;
    private final int amplifier;
    private final boolean isArmorEnchant;

    public PotionEnchantment(MobEffect effect, int amplifier, boolean isArmorEnchant) {
        super(Rarity.RARE, isArmorEnchant ? EnchantmentCategory.ARMOR : EnchantmentCategory.WEAPON,
                isArmorEnchant ?
                        new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET} :
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.effect = effect;
        this.amplifier = amplifier;
        this.isArmorEnchant = isArmorEnchant;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return amplifier + 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean isArmorEnchant() {
        return isArmorEnchant;
    }

    // 为工具应用效果到目标实体
    public void applyToolEffect(LivingEntity target, int level) {
        if (!isArmorEnchant) {
            int duration = 200; // 10秒
            target.addEffect(new MobEffectInstance(effect, duration, level - 1));
        }
    }

    // 为盔甲应用效果到穿戴者
    public void applyArmorEffect(LivingEntity wearer, int level) {
        if (isArmorEnchant) {
            int duration = 200; // 10秒
            wearer.addEffect(new MobEffectInstance(effect, duration, level - 1));
        }
    }

    // 保存附魔数据到NBT
    public void saveToNBT(CompoundTag tag) {
        tag.putString("Effect", ForgeRegistries.MOB_EFFECTS.getKey(effect).toString());
        tag.putInt("Amplifier", amplifier);
        tag.putBoolean("IsArmor", isArmorEnchant);
    }

    // 从NBT创建附魔实例
    public static PotionEnchantment loadFromNBT(CompoundTag tag) {
        ResourceLocation effectId = ResourceLocationHelper.parse(tag.getString("Effect"));
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        int amplifier = tag.getInt("Amplifier");
        boolean isArmor = tag.getBoolean("IsArmor");

        if (effect != null) {
            return new PotionEnchantment(effect, amplifier, isArmor);
        }
        return null;
    }

    // 检查物品是否有药水附魔
    public static boolean hasPotionEnchantment(ItemStack stack) {
        return getPotionEnchantment(stack) != null;
    }

    // 获取物品的药水附魔
    public static PotionEnchantment getPotionEnchantment(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("PotionEnchantment")) {
            CompoundTag enchantTag = stack.getTag().getCompound("PotionEnchantment");
            return loadFromNBT(enchantTag);
        }
        return null;
    }

    // 获取附魔的显示名称
    public String getDisplayName() {
        String effectName = effect.getDisplayName().getString();
        return effectName + " " + (amplifier + 1);
    }
}