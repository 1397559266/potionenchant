package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "potionenchant", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LifestealHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // 检查伤害来源是否是玩家
        if (event.getSource().getEntity() instanceof Player) {
            Player player = (Player) event.getSource().getEntity();
            ItemStack weapon = player.getMainHandItem();

            int lifestealLevel = weapon.getEnchantmentLevel(PotionEnchantMod.LIFESTEAL.get());

            if (lifestealLevel > 0) {
                // 触发吸血效果
                triggerLifesteal(player, event.getAmount(), lifestealLevel);
            }
        }
    }

    private static void triggerLifesteal(Player player, float damageDealt, int enchantLevel) {
        // 计算恢复的生命值：敌人损失血量的5% × 附魔等级
        float healthToRestore = damageDealt * 0.05f * enchantLevel;

        if (healthToRestore > 0) {
            // 恢复生命值，但不能超过最大生命值
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float newHealth = Math.min(currentHealth + healthToRestore, maxHealth);

            player.setHealth(newHealth);

        }
    }
}
