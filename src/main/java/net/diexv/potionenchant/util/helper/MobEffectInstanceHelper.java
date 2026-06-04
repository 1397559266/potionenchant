package net.diexv.potionenchant.util.helper;

import net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * MobEffectInstance工具类
 * 使用Mixin Accessor修改字段
 */
public class MobEffectInstanceHelper {
    
    /**
     * 设置MobEffectInstance的amplifier字段
     * 使用Mixin Accessor访问
     */
    public static void setAmplifier(MobEffectInstance instance, int newAmplifier) {
        MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) instance;
        accessor.setAmplifier(newAmplifier);
    }
}