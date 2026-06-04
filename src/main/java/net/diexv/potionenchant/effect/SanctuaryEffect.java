package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;

@Mod.EventBusSubscriber
public class SanctuaryEffect extends MobEffect {

    public SanctuaryEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF); // 纯白色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 每tick清除除自身外的所有效果
        if (!entity.level().isClientSide) {
            clearAllOtherEffects(entity);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每tick都执行
        return true;
    }

    // 清除除自身外的所有效果
    private void clearAllOtherEffects(LivingEntity entity) {
        // 复制效果列表以避免并发修改异常
        java.util.List<MobEffectInstance> effectsCopy = new java.util.ArrayList<>(entity.getActiveEffects());

        for (MobEffectInstance effectInstance : effectsCopy) {
            MobEffect effect = effectInstance.getEffect();

            // 检查是否为圣洁效果本身，如果不是则清除
            if (effect != this) {
                // 直接移除效果，即使处于破坏模式也能被圣洁清除
                entity.removeEffect(effect);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();

        // 添加null检查
        if (effectInstance != null && entity.hasEffect(EffectRegistry.SANCTUARY.get())) {
            // 检查要添加的效果是否为圣洁效果本身
            if (effectInstance.getEffect() != EffectRegistry.SANCTUARY.get()) {
                // 取消所有其他效果的添加
                event.setResult(Event.Result.DENY);
            }
        }
    }

    // 特殊方法：检查效果是否可以被清除
    public static boolean canBeRemoved(LivingEntity entity) {
        // 圣洁效果只能通过持续时间结束或死亡来移除
        // 这个方法会被Mixin调用
        return false;
    }
}