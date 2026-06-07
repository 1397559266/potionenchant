package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ToolEffectHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // 处理直接攻击的情况（包括玩家和其他生物）
        if (event.getSource().getDirectEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getDirectEntity();
            LivingEntity target = event.getEntity();

            ItemStack weapon = attacker.getMainHandItem();
            
            // 检查是否是TACZ武器
            if (isTaczWeaponSafe(weapon)) {
                // TACZ枪械/重武器伤害处理
                if (PotionEnchantManager.hasPotionEnchantments(weapon)) {
                    List<PotionEnchantData> weaponEnchants = PotionEnchantManager.getPotionEnchantments(weapon);

                    for (PotionEnchantData enchant : weaponEnchants) {
                        try {
                            enchant.applyToolEffect(target, enchant.getAmplifier() + 1);
                        } catch (Exception e) {
                            LOGGER.warn("[TACZ Integration] Error applying potion effect from TACZ weapon: {}", e.getMessage());
                        }
                    }
                }
            } else if (!weapon.isEmpty() && PotionEnchantManager.hasPotionEnchantments(weapon)) {
                // 普通工具/武器伤害处理
                List<PotionEnchantData> toolEnchants = PotionEnchantManager.getPotionEnchantments(weapon);

                for (PotionEnchantData enchant : toolEnchants) {
                    enchant.applyToolEffect(target, enchant.getAmplifier() + 1);
                }
            }
        }
        // 处理箭矢造成的伤害情况
        else if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            AbstractArrow arrow = (AbstractArrow) event.getSource().getDirectEntity();
            // 检查箭矢是否有所有者
            if (arrow.getOwner() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) arrow.getOwner();
                LivingEntity target = event.getEntity();

                // 获取射手手中的弓/弩
                ItemStack bow = shooter.getMainHandItem();
                if (bow.isEmpty()) {
                    // 如果主手没有物品，检查副手
                    bow = shooter.getOffhandItem();
                }

                // 检查弓/弩是否有药水附魔
                if (!bow.isEmpty() && PotionEnchantManager.hasPotionEnchantments(bow)) {
                    List<PotionEnchantData> bowEnchants = PotionEnchantManager.getPotionEnchantments(bow);

                    // 应用所有药水效果
                    for (PotionEnchantData enchant : bowEnchants) {
                        enchant.applyToolEffect(target, enchant.getAmplifier() + 1);
                    }
                }
            }
        }
        // 处理TACZ子弹造成的伤害（TACZ可能使用自定义的实体类型）
        else if (isTaczLoadedSafe()) {
            try {
                // 尝试检测TACZ子弹实体
                var sourceEntity = event.getSource().getDirectEntity();
                if (sourceEntity != null) {
                    String entityTypeName = sourceEntity.getType().toString();
                    // TACZ子弹通常包含 "tacz" 在命名空间中
                    if (entityTypeName.contains("tacz")) {
                        
                        // 尝试从攻击者获取武器
                        if (event.getSource().getEntity() instanceof LivingEntity) {
                            LivingEntity shooter = (LivingEntity) event.getSource().getEntity();
                            LivingEntity target = event.getEntity();
                            
                            ItemStack weapon = shooter.getMainHandItem();
                            if (weapon.isEmpty()) {
                                weapon = shooter.getOffhandItem();
                            }
                            
                            // 检查TACZ武器是否有药水附魔
                            if (isTaczWeaponSafe(weapon) && 
                                PotionEnchantManager.hasPotionEnchantments(weapon)) {
                                List<PotionEnchantData> weaponEnchants = 
                                    PotionEnchantManager.getPotionEnchantments(weapon);
                                
                                for (PotionEnchantData enchant : weaponEnchants) {
                                    try {
                                        enchant.applyToolEffect(target, enchant.getAmplifier() + 1);
                                    } catch (Exception e) {
                                        LOGGER.warn("[TACZ Integration] Error applying potion effect from TACZ projectile: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略TACZ集成错误，不影响原版功能
                LOGGER.debug("[TACZ Integration] Error in projectile detection: {}", e.getMessage());
            }
        }
    }

    // Safe wrapper for TaczIntegration (optional dependency)
    private static boolean isTaczWeaponSafe(ItemStack weapon) {
        try {
            Class<?> cls = Class.forName("net.diexv.potionenchant.util.TaczIntegration");
            java.lang.reflect.Method m = cls.getMethod("isTaczWeapon", ItemStack.class);
            return (Boolean) m.invoke(null, weapon);
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean isTaczLoadedSafe() {
        try {
            Class<?> cls = Class.forName("net.diexv.potionenchant.util.TaczIntegration");
            java.lang.reflect.Method m = cls.getMethod("isTaczLoaded");
            return (Boolean) m.invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }
}