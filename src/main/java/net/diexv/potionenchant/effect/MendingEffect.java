package net.diexv.potionenchant.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MendingEffect extends MobEffect {

    public MendingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF00); // 浅绿色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 这个效果在每tick时执行修复逻辑
        if (entity instanceof Player player) {
            repairPlayerEquipment(player, amplifier);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每秒执行一次 (20 ticks = 1秒)
        return duration % 20 == 0;
    }

    // 修复玩家装备
    private void repairPlayerEquipment(Player player, int amplifier) {
        if (player.level().isClientSide) return;

        int repairAmount = (amplifier + 1) * EffectConfigValues.CONFIG.mendingRepairPerLevel.get(); // 每级修复10点耐久

        // 修复主手和副手物品
        repairItem(player.getMainHandItem(), repairAmount);
        repairItem(player.getOffhandItem(), repairAmount);

        // 修复盔甲
        for (ItemStack armor : player.getArmorSlots()) {
            repairItem(armor, repairAmount);
        }

        // 修复背包中的所有工具和装备
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isRepairableItem(stack)) {
                repairItem(stack, repairAmount);
            }
        }
    }

    // 修复单个物品
    private void repairItem(ItemStack stack, int repairAmount) {
        if (stack.isEmpty() || !stack.isDamaged()) return;

        // 检查物品是否可修复
        if (isRepairableItem(stack)) {
            int currentDamage = stack.getDamageValue();
            int newDamage = Math.max(0, currentDamage - repairAmount);
            stack.setDamageValue(newDamage);
        }
    }

    // 检查物品是否可修复
    private boolean isRepairableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 检查物品是否有耐久度
        if (!stack.isDamageableItem()) return false;

        // 检查物品是否已损坏
        if (!stack.isDamaged()) return false;

        // 允许修复所有有耐久的物品
        return true;
    }
}