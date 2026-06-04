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
public class PurificationEffect extends MobEffect {

    public PurificationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x87CEEB); // 浅蓝色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 每tick清除负面效果
        if (!entity.level().isClientSide) {
            clearHarmfulEffects(entity);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每tick都执行
        return true;
    }

    // 清除负面效果
    private void clearHarmfulEffects(LivingEntity entity) {

        for (MobEffectInstance effectInstance : entity.getActiveEffects()) {
            MobEffect effect = effectInstance.getEffect();

            // 检查是否为负面效果且不是净化效果本身
            if (isHarmfulEffect(effect) && effect != this) {
                // 移除负面效果
                entity.removeEffect(effect);
            }
        }
    }

    // 判断效果是否为负面效果
    private static boolean isHarmfulEffect(MobEffect effect) {
        // 使用原版的分类方式判断是否为负面效果
        return effect.getCategory() == MobEffectCategory.HARMFUL;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();

        // 添加null检查
        if (effectInstance != null && entity.hasEffect(EffectRegistry.PURIFICATION.get())) {
            // 检查要添加的效果是否为负面效果
            if (isHarmfulEffect(effectInstance.getEffect())) {
                // 取消负面效果的添加
                event.setResult(Event.Result.DENY);
            }
        }
    }
}