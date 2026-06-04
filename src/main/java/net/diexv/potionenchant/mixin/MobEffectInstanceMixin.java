package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.handlers.CombinedEffectHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = MobEffectInstance.class, priority = -2147483648)
public class MobEffectInstanceMixin {

    @Shadow @Final
    private MobEffect effect;
    
    @Shadow
    @Mutable
    private int duration;
    
    @Shadow
    @Mutable
    private int amplifier;

    // 拦截效果更新时的tick方法
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(LivingEntity entity, Runnable p_19554_, CallbackInfoReturnable<Boolean> cir) {
        // 检查是否为圣洁效果
        if (this.effect == EffectRegistry.SANCTUARY.get()) {
            // 对于圣洁效果，允许正常的tick逻辑（持续时间减少）
            return;
        }
        // 冻结倒计时功能已移除，改为使用每5秒重新给予效果的方式
    }
}