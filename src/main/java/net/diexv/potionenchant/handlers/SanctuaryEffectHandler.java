package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SanctuaryEffectHandler {

    // 拦截效果移除事件
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();

        // 检查是否为圣洁效果
        if (effectInstance != null && effectInstance.getEffect() == EffectRegistry.SANCTUARY.get()) {
            // 阻止圣洁效果被移除
            event.setCanceled(true);
        }
    }

    // 拦截效果过期事件
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();

        // 检查是否为圣洁效果
        if (effectInstance != null && effectInstance.getEffect() == EffectRegistry.SANCTUARY.get()) {
            // 允许圣洁效果自然过期
            // 这里不取消，让原版逻辑继续执行
        }
    }

    // 拦截药水效果清除事件（来自命令等）
    @SubscribeEvent
    public static void onPotionEffectEvent(MobEffectEvent event) {
        if (event instanceof MobEffectEvent.Remove ||
                event instanceof MobEffectEvent.Expired) {
            // 这些事件我们已经单独处理了
            return;
        }

        // 对于其他药水效果事件，检查是否涉及圣洁效果
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() == EffectRegistry.SANCTUARY.get()) {
            // 阻止任何可能清除圣洁效果的事件
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }
}