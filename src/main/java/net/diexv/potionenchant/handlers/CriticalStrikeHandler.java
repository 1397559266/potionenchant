package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class CriticalStrikeHandler {

    // 使用WeakHashMap来存储玩家的暴击时间，避免内存泄漏
    private static final WeakHashMap<Player, Long> playerCriticalTimes = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 检查伤害来源是否是玩家
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            int criticalStrikeLevel = EnchantmentHelper.getEnchantmentLevel(
                    PotionEnchantMod.CRITICAL_STRIKE.get(), attacker);

            // 检查武器是否有致命一击附魔且攻击是暴击
            if (criticalStrikeLevel > 0 && attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.isSwimming() && !attacker.isPassenger() && !attacker.isSprinting()) {
                // 记录暴击时间，持续时间为等级+1秒
                long currentTime = System.currentTimeMillis();
                playerCriticalTimes.put(attacker, currentTime);
            }
        }
    }

    // 检查玩家是否在暴击后的无视无敌帧时间内
    public static boolean isInCriticalStrikeWindow(Player player) {
        if (playerCriticalTimes.containsKey(player)) {
            long criticalTime = playerCriticalTimes.get(player);
            int criticalStrikeLevel = EnchantmentHelper.getEnchantmentLevel(
                    PotionEnchantMod.CRITICAL_STRIKE.get(), player);

            // 计算持续时间（每级-1+1秒，所以总时间为等级的秒数）
            long duration = (criticalStrikeLevel -1 + 1) * 1000L;

            if (System.currentTimeMillis() - criticalTime <= duration) {
                return true;
            } else {
                // 时间已过，移除记录
                playerCriticalTimes.remove(player);
            }
        }
        return false;
    }
}
