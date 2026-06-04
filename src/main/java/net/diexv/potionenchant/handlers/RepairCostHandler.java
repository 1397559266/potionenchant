package net.diexv.potionenchant.handlers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RepairCostHandler {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        // 这个处理器在其他所有处理器之后运行，确保修复成本正确设置
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        ItemStack output = event.getOutput();

        if (output == null || output.isEmpty()) return;

        // 检查是否是药水附魔操作
        boolean isPotionEnchant = right.getItem() instanceof net.minecraft.world.item.PotionItem;

        if (isPotionEnchant) {
            // 对于药水附魔，确保修复成本不变
            ensureFixedRepairCost(output, left);
        } else {
            // 对于正常附魔，恢复原始修复成本逻辑
            restoreNormalRepairCost(output, left);
        }
    }

    // 确保药水附魔的修复成本固定
    private void ensureFixedRepairCost(ItemStack output, ItemStack original) {
        CompoundTag tag = output.getOrCreateTag();

        // 如果还没有保存原始修复成本，则保存
        if (!tag.contains("PotionEnchant_OriginalRepairCost")) {
            int originalRepairCost = 0;
            if (original.hasTag() && original.getTag().contains("RepairCost")) {
                originalRepairCost = original.getTag().getInt("RepairCost");
            }
            tag.putInt("PotionEnchant_OriginalRepairCost", originalRepairCost);
        }

        // 获取保存的原始修复成本
        int originalRepairCost = tag.getInt("PotionEnchant_OriginalRepairCost");

        // 设置修复成本为原始值，不增加
        tag.putInt("RepairCost", originalRepairCost);
    }

    // 恢复正常附魔的修复成本逻辑
    private void restoreNormalRepairCost(ItemStack output, ItemStack original) {
        CompoundTag tag = output.getOrCreateTag();

        // 如果有保存的原始修复成本，使用它作为基础
        if (tag.contains("PotionEnchant_OriginalRepairCost")) {
            int originalRepairCost = tag.getInt("PotionEnchant_OriginalRepairCost");

            // 计算正常附魔应该增加的修复成本
            int newRepairCost = calculateNormalRepairCost(originalRepairCost);

            // 设置新的修复成本
            tag.putInt("RepairCost", newRepairCost);

            // 更新保存的原始修复成本，以便下次药水附魔使用
            tag.putInt("PotionEnchant_OriginalRepairCost", newRepairCost);
        }
    }

    // 计算正常附魔的修复成本（原版逻辑）
    private int calculateNormalRepairCost(int originalCost) {
        // 原版铁砧修复成本计算逻辑
        return originalCost * 2 + 1;
    }
}