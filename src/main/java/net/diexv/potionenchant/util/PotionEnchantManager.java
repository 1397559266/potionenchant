package net.diexv.potionenchant.util;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class PotionEnchantManager {
    private static final String NBT_KEY = "PotionEnchantments";

    // 获取物品的所有药水附魔
    public static List<PotionEnchantData> getPotionEnchantments(ItemStack stack) {
        List<PotionEnchantData> enchantments = new ArrayList<>();

        if (stack.hasTag() && stack.getTag().contains(NBT_KEY)) {
            ListTag enchantList = stack.getTag().getList(NBT_KEY, 10); // 10表示CompoundTag

            for (int i = 0; i < enchantList.size(); i++) {
                CompoundTag enchantTag = enchantList.getCompound(i);
                PotionEnchantData data = PotionEnchantData.loadFromNBT(enchantTag);
                if (data != null) {
                    enchantments.add(data);
                }
            }
        }

        return enchantments;
    }

    // 添加药水附魔到物品
    public static void addPotionEnchantment(ItemStack stack, PotionEnchantData data) {
        // Global blacklist check - prevent blacklisted effects from being applied anywhere
        if (data != null && data.getEffect() != null) {
            net.minecraft.resources.ResourceLocation effectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(data.getEffect());
            if (effectId != null && net.diexv.potionenchant.config.PotionEnchantConfig.isEffectBlacklisted(effectId)) {
                return; // Blacklisted, skip
            }
        }
        CompoundTag tag = stack.getOrCreateTag();
        ListTag enchantList;

        if (tag.contains(NBT_KEY)) {
            enchantList = tag.getList(NBT_KEY, 10);
        } else {
            enchantList = new ListTag();
        }

        // 检查是否已存在相同效果，如果存在则更新
        boolean found = false;
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag enchantTag = enchantList.getCompound(i);
            ResourceLocation existingEffectId = ResourceLocationHelper.parse(enchantTag.getString("Effect"));
            ResourceLocation newEffectId = ForgeRegistries.MOB_EFFECTS.getKey(data.getEffect());

            if (existingEffectId.equals(newEffectId)) {
                // 更新现有效果
                data.saveToNBT().getAllKeys().forEach(key -> {
                    enchantTag.put(key, data.saveToNBT().get(key));
                });
                found = true;
                break;
            }
        }

        if (!found) {
            enchantList.add(data.saveToNBT());
        }

        tag.put(NBT_KEY, enchantList);
    }

    // 添加药水附魔到物品（叠加模式，用于终极药水护符）
    public static void addPotionEnchantmentWithStack(ItemStack stack, PotionEnchantData data) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag enchantList;

        if (tag.contains(NBT_KEY)) {
            enchantList = tag.getList(NBT_KEY, 10);
        } else {
            enchantList = new ListTag();
        }

        // 检查是否已存在相同效果，如果存在则叠加等级
        boolean found = false;
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag enchantTag = enchantList.getCompound(i);
            ResourceLocation existingEffectId = ResourceLocationHelper.parse(enchantTag.getString("Effect"));
            ResourceLocation newEffectId = ForgeRegistries.MOB_EFFECTS.getKey(data.getEffect());

            if (existingEffectId.equals(newEffectId)) {
                // 叠加等级（加法）- 正确的逻辑：总等级 = 当前等级之和
                int existingAmplifier = enchantTag.getInt("Amplifier");
                // 转换为实际等级（1-based）后相加，再转换回amplifier（0-based）
                int existingLevel = existingAmplifier + 1;
                int addedLevel = data.getAmplifier() + 1;
                int totalLevel = existingLevel + addedLevel;
                int newAmplifier = totalLevel - 1; // 转换回amplifier
                
                // 终极药水护符不受等级上限限制
                if (!isUltimatePotionAmulet(stack)) {
                    // 应用等级上限
                    int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
                    newAmplifier = Math.min(newAmplifier, maxLevel - 1); // -1 因为amplifier从0开始
                }
                
                enchantTag.putInt("Amplifier", newAmplifier);
                found = true;
                break;
            }
        }

        if (!found) {
            // 对于新添加的效果，也要应用等级上限（如果不是终极药水护符）
            if (!isUltimatePotionAmulet(stack)) {
                int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
                if (data.getAmplifier() >= maxLevel) {
                    data = new PotionEnchantData(data.getEffect(), maxLevel - 1, data.isArmorEnchant(), data.getColor());
                }
            }
            enchantList.add(data.saveToNBT());
        }

        tag.put(NBT_KEY, enchantList);
    }

    // 检查物品是否有药水附魔
    public static boolean hasPotionEnchantments(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_KEY) &&
                !stack.getTag().getList(NBT_KEY, 10).isEmpty();
    }

    // 移除所有药水附魔
    public static void clearPotionEnchantments(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(NBT_KEY);
        }
    }

    // 获取特定类型的附魔（盔甲或工具）
    public static List<PotionEnchantData> getEnchantmentsByType(ItemStack stack, boolean isArmor) {
        List<PotionEnchantData> allEnchantments = getPotionEnchantments(stack);
        List<PotionEnchantData> filtered = new ArrayList<>();

        for (PotionEnchantData data : allEnchantments) {
            if (data.isArmorEnchant() == isArmor) {
                filtered.add(data);
            }
        }

        return filtered;
    }
    
    // 检查物品是否是终极药水护符
    private static boolean isUltimatePotionAmulet(ItemStack stack) {
        return stack.getItem() == ModItems.ULTIMATE_POTION_AMULET.get();
    }

    // 便捷方法：直接通过MobEffect添加药水附魔
    public static void addPotionEnchantment(ItemStack stack, MobEffect effect, int amplifier, int duration) {
        if (effect == null) return;
        
        // 获取效果颜色
        int color = 0xFFFFFF; // 默认白色
        try {
            Color effectColor = new Color(effect.getColor());
            color = effectColor.getRGB();
        } catch (Exception e) {
            // 如果获取颜色失败，使用默认值
        }
        
        // 判断是否为盔甲附魔（根据效果类型）
        boolean isArmor = false;
        if (effect.isBeneficial()) {
            // 增益效果通常是盔甲附魔
            isArmor = true;
        }
        
        PotionEnchantData data = new PotionEnchantData(effect, amplifier, isArmor, color);
        addPotionEnchantment(stack, data);
    }
}