package net.diexv.potionenchant.util;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * TACZ（Timeless and Classics Zero）集成工具类
 * 用于检测TACZ枪械并应用药水附魔效果
 * 作为可选依赖，只有安装TACZ时才会生效
 */
public class TaczIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // TACZ模组ID
    private static final String TACZ_MODID = "tacz";
    
    // TACZ基础物品ID
    private static final String GUN_ITEM_ID = "tacz:modern_kinetic_gun";
    private static final String HEAVY_WEAPON_ITEM_ID = "tacz:heavy_weapon";
    
    // 是否已加载TACZ
    private static Boolean taczLoaded = null;
    
    /**
     * 检查TACZ是否已加载
     */
    public static boolean isTaczLoaded() {
        if (taczLoaded == null) {
            taczLoaded = net.minecraftforge.fml.ModList.get().isLoaded(TACZ_MODID);
            if (taczLoaded) {
                LOGGER.info("[TaczIntegration] TACZ detected, enabling TACZ integration");
            } else {
                LOGGER.info("[TaczIntegration] TACZ not detected, skipping TACZ integration");
            }
        }
        return taczLoaded;
    }
    
    /**
     * 检查ItemStack是否是TACZ枪械或重武器
     */
    public static boolean isTaczWeapon(ItemStack stack) {
        if (!isTaczLoaded() || stack.isEmpty()) {
            return false;
        }
        
        try {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) {
                return false;
            }
            
            String idStr = itemId.toString();
            // 检查是否是TACZ的现代动能枪械或重武器
            return idStr.equals(GUN_ITEM_ID) || idStr.equals(HEAVY_WEAPON_ITEM_ID);
        } catch (Exception e) {
            LOGGER.debug("[TaczIntegration] Error checking TACZ weapon: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取TACZ武器的GunId或WeaponId
     */
    public static String getWeaponId(ItemStack stack) {
        if (!isTaczWeapon(stack)) {
            return null;
        }
        
        try {
            var tag = stack.getTag();
            if (tag == null) {
                return null;
            }
            
            // 尝试获取GunId（现代动能枪械）
            String gunId = tag.getString("GunId");
            if (!gunId.isEmpty()) {
                return gunId;
            }
            
            // 尝试获取WeaponId（重武器）
            String weaponId = tag.getString("WeaponId");
            if (!weaponId.isEmpty()) {
                return weaponId;
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.debug("[TaczIntegration] Error getting weapon ID: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查物品是否是TACZ的现代动能枪械
     */
    public static boolean isModernKineticGun(ItemStack stack) {
        if (!isTaczLoaded() || stack.isEmpty()) {
            return false;
        }
        
        try {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return itemId != null && itemId.toString().equals(GUN_ITEM_ID);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查物品是否是TACZ的重武器
     */
    public static boolean isHeavyWeapon(ItemStack stack) {
        if (!isTaczLoaded() || stack.isEmpty()) {
            return false;
        }
        
        try {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return itemId != null && itemId.toString().equals(HEAVY_WEAPON_ITEM_ID);
        } catch (Exception e) {
            return false;
        }
    }
}
