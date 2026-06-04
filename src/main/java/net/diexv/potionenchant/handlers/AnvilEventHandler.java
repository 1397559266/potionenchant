package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.List;

public class AnvilEventHandler {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) return;

        // 检查右边物品是否是药水
        if (right.getItem() instanceof PotionItem) {
            // 获取药水效果
            List<net.minecraft.world.effect.MobEffectInstance> effects = PotionUtils.getMobEffects(right);
            if (effects.isEmpty()) return;

            // 检查左边物品是否是盔甲、工具或终极药水护符
            boolean isArmor = left.getItem() instanceof net.minecraft.world.item.ArmorItem;
            boolean isTool = left.getItem() instanceof net.minecraft.world.item.DiggerItem ||
                    left.getItem() instanceof net.minecraft.world.item.SwordItem ||
                    left.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem;
            boolean isUltimateAmulet = left.getItem() == net.diexv.potionenchant.item.ModItems.ULTIMATE_POTION_AMULET.get();
            boolean isAnyItem = left.getItem() instanceof net.minecraft.world.item.Item;

            if (!isArmor && !isTool && !isUltimateAmulet && !isAnyItem) return;

            // 检查药水是否包含黑名单中的效果
            if (containsBlacklistedEffects(effects)) {
                // 如果包含黑名单效果，取消附魔
                return;
            }

            // 检查全局限制（优先级高于特定类型限制）
            if (PotionEnchantConfig.COMMON.limitAllEnchants.get() && !isUltimateAmulet) {
                // 获取当前物品的所有药水附魔
                List<PotionEnchantData> currentEnchants = PotionEnchantManager.getPotionEnchantments(left);
                int maxEnchants = PotionEnchantConfig.COMMON.maxAllEnchants.get();
                int newEnchantsCount = effects.size();

                // 计算添加新效果后的总数量（考虑替换）
                int totalEnchants = currentEnchants.size() + newEnchantsCount;
                for (net.minecraft.world.effect.MobEffectInstance effectInstance : effects) {
                    net.minecraft.world.effect.MobEffect effect = effectInstance.getEffect();
                    for (PotionEnchantData existingEnchant : currentEnchants) {
                        if (existingEnchant.getEffect() == effect) {
                            totalEnchants--;
                            break;
                        }
                    }
                }

                if (totalEnchants > maxEnchants) {
                    return;
                }
            }

            // 检查盔甲附魔数量限制
            else if (isArmor && PotionEnchantConfig.COMMON.limitArmorEnchants.get() && !isUltimateAmulet) {
                // 获取当前物品的药水附魔数量
                List<PotionEnchantData> currentEnchants = PotionEnchantManager.getPotionEnchantments(left);
                int maxEnchants = PotionEnchantConfig.COMMON.maxArmorEnchants.get();
                int newEnchantsCount = effects.size(); // 新添加的药水效果数量

                // 计算添加新效果后的总数量
                int totalEnchants = currentEnchants.size() + newEnchantsCount;
                // 对于已存在的效果，会被替换而不是添加新的，所以需要调整
                for (net.minecraft.world.effect.MobEffectInstance effectInstance : effects) {
                    net.minecraft.world.effect.MobEffect effect = effectInstance.getEffect();
                    for (PotionEnchantData existingEnchant : currentEnchants) {
                        if (existingEnchant.getEffect() == effect) {
                            totalEnchants--; // 减去一个会被替换的效果
                            break;
                        }
                    }
                }

                // 如果超出最大限制，取消附魔
                if (totalEnchants > maxEnchants) {
                    return; // 超出限制，不执行附魔
                }
            }

            // 创建结果物品
            ItemStack result = left.copy();

            // 为每个效果创建附魔数据（使用效果自身颜色）
            for (net.minecraft.world.effect.MobEffectInstance effectInstance : effects) {
                net.minecraft.world.effect.MobEffect effect = effectInstance.getEffect();
                int amplifier = effectInstance.getAmplifier();

                PotionEnchantData enchantData = new PotionEnchantData(effect, amplifier, isArmor);
                if (isUltimateAmulet) {
                    // 对于终极药水护符，使用叠加逻辑
                    PotionEnchantManager.addPotionEnchantmentWithStack(result, enchantData);
                } else {
                    // 对于其他物品，使用普通逻辑
                    PotionEnchantManager.addPotionEnchantment(result, enchantData);
                }
            }

            // 设置固定成本为30级
            event.setCost(30);
            event.setMaterialCost(1);
            event.setOutput(result);

            // 保存原始修复成本，防止影响正常附魔
            saveOriginalRepairCost(result, left);
        }
    }

    // 检查效果列表是否包含黑名单中的效果
    private boolean containsBlacklistedEffects(List<net.minecraft.world.effect.MobEffectInstance> effects) {
        for (net.minecraft.world.effect.MobEffectInstance effectInstance : effects) {
            net.minecraft.world.effect.MobEffect effect = effectInstance.getEffect();
            net.minecraft.resources.ResourceLocation effectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect);

            if (effectId != null && PotionEnchantConfig.isEffectBlacklisted(effectId)) {
                return true;
            }
        }
        return false;
    }

    // 保存原始修复成本，防止药水附魔影响正常附魔的成本
    private void saveOriginalRepairCost(ItemStack result, ItemStack original) {
        // 获取原始修复成本
        int originalRepairCost = 0;
        if (original.hasTag() && original.getTag().contains("RepairCost")) {
            originalRepairCost = original.getTag().getInt("RepairCost");
        }

        // 保存原始修复成本到自定义标签
        CompoundTag tag = result.getOrCreateTag();
        tag.putInt("PotionEnchant_OriginalRepairCost", originalRepairCost);

        // 设置当前修复成本为原始值，不增加
        tag.putInt("RepairCost", originalRepairCost);
    }
}