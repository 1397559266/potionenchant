package net.diexv.potionenchant.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * X护甲独立药水附魔管理器
 * 将全套X护甲视为一个整体，存储和管理药水附魔等级
 */
public class XArmorEnchantmentManager {
    private static final String NBT_KEY = "XArmorEnchantments";
    
    /**
     * 从玩家穿戴的全套X护甲中获取药水附魔数据
     * 从头盔读取（所有X护甲共享同一份数据）
     */
    public static Map<MobEffect, Integer> getXArmorEnchantments(Player player) {
        Map<MobEffect, Integer> enchantments = new HashMap<>();
        
        if (player == null) {
            return enchantments;
        }
        
        // 从头盔读取NBT数据
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !isXArmor(helmet)) {
            return enchantments;
        }
        
        CompoundTag tag = helmet.getTag();
        if (tag == null || !tag.contains(NBT_KEY)) {
            return enchantments;
        }
        
        ListTag enchantList = tag.getList(NBT_KEY, 10);
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag enchantTag = enchantList.getCompound(i);
            String effectId = enchantTag.getString("Effect");
            int level = enchantTag.getInt("Level");
            
            ResourceLocation location = ResourceLocationHelper.parse(effectId);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(location);
            
            if (effect != null && level > 0) {
                enchantments.put(effect, level);
            }
        }
        
        return enchantments;
    }
    
    /**
     * 设置X护甲的药水附魔等级（全套总等级）
     * 数据存储到头盔的NBT中
     */
    public static void setXArmorEnchantment(Player player, MobEffect effect, int level) {
        if (player == null || effect == null) {
            return;
        }
        
        // 获取头盔
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !isXArmor(helmet)) {
            return;
        }
        
        CompoundTag tag = helmet.getOrCreateTag();
        ListTag enchantList;
        
        if (tag.contains(NBT_KEY)) {
            enchantList = tag.getList(NBT_KEY, 10);
        } else {
            enchantList = new ListTag();
        }
        
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (effectId == null) {
            return;
        }
        
        // 查找是否已存在该效果
        boolean found = false;
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundTag enchantTag = enchantList.getCompound(i);
            String existingEffectId = enchantTag.getString("Effect");
            
            if (existingEffectId.equals(effectId.toString())) {
                if (level <= 0) {
                    // 等级为0或负数，移除该附魔
                    enchantList.remove(i);
                } else {
                    // 更新等级
                    enchantTag.putInt("Level", level);
                    enchantList.set(i, enchantTag);
                }
                found = true;
                break;
            }
        }
        
        if (!found && level > 0) {
            // 添加新附魔
            CompoundTag enchantTag = new CompoundTag();
            enchantTag.putString("Effect", effectId.toString());
            enchantTag.putInt("Level", level);
            enchantList.add(enchantTag);
        }
        
        tag.put(NBT_KEY, enchantList);
        
        // 同步到其他X护甲部位（胸甲、护腿、靴子）
        syncToAllXArmor(player, tag);
    }
    
    /**
     * 批量设置X护甲的药水附魔
     */
    public static void setXArmorEnchantments(Player player, Map<MobEffect, Integer> enchantments) {
        if (player == null || enchantments == null) {
            return;
        }
        
        // 获取当前的附魔列表
        Map<MobEffect, Integer> currentEnchantments = getXArmorEnchantments(player);
        
        // 创建一个新的Map来存储最终的附魔状态
        Map<MobEffect, Integer> finalEnchantments = new HashMap<>(currentEnchantments);
        
        // 应用所有的更改
        for (Map.Entry<MobEffect, Integer> entry : enchantments.entrySet()) {
            MobEffect effect = entry.getKey();
            int targetLevel = entry.getValue();
            
            if (targetLevel <= 0) {
                // 等级为0或负数，移除该附魔
                finalEnchantments.remove(effect);
            } else {
                // 设置新的等级
                finalEnchantments.put(effect, targetLevel);
            }
        }
        
        // 清空现有附魔
        clearAllXArmorEnchantments(player);
        
        // 设置最终的附魔
        for (Map.Entry<MobEffect, Integer> entry : finalEnchantments.entrySet()) {
            setXArmorEnchantment(player, entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 清除所有X护甲的药水附魔
     */
    public static void clearAllXArmorEnchantments(Player player) {
        if (player == null) {
            return;
        }
        
        // 清除所有部位的NBT数据
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && isXArmor(armor)) {
                CompoundTag tag = armor.getTag();
                if (tag != null) {
                    tag.remove(NBT_KEY);
                }
            }
        }
    }
    
    /**
     * 检查物品是否是X护甲
     */
    private static boolean isXArmor(ItemStack stack) {
        return stack.getItem() == net.diexv.potionenchant.item.ModItems.X_HELMET.get() ||
               stack.getItem() == net.diexv.potionenchant.item.ModItems.X_CHESTPLATE.get() ||
               stack.getItem() == net.diexv.potionenchant.item.ModItems.X_LEGGINGS.get() ||
               stack.getItem() == net.diexv.potionenchant.item.ModItems.X_BOOTS.get();
    }
    
    /**
     * 将头盔的NBT数据同步到其他X护甲部位
     */
    private static void syncToAllXArmor(Player player, CompoundTag helmetTag) {
        if (player == null || helmetTag == null) {
            return;
        }
        
        CompoundTag xArmorEnchantments = helmetTag.getCompound(NBT_KEY);
        
        // 同步到胸甲、护腿、靴子
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && isXArmor(armor)) {
                CompoundTag tag = armor.getOrCreateTag();
                tag.put(NBT_KEY, xArmorEnchantments.copy());
            }
        }
    }
    
    /**
     * 获取某个效果的当前等级（全套总等级）
     */
    public static int getEnchantmentLevel(Player player, MobEffect effect) {
        Map<MobEffect, Integer> enchantments = getXArmorEnchantments(player);
        return enchantments.getOrDefault(effect, 0);
    }
}
