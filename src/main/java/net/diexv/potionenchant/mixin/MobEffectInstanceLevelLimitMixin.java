package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceLevelLimitMixin {

    @Inject(
        method = "<init>(Lnet/minecraft/world/effect/MobEffect;IIZZZ)V",
        at = @At("TAIL")
    )
    private void onConstruct(MobEffect effect, int duration, int amplifier, boolean bl, boolean bl2, boolean bl3, CallbackInfo ci) {
        try {
            if (PotionEnchantConfig.COMMON != null &&
                PotionEnchantConfig.COMMON.allowPotionLevelBeyond255 != null &&
                PotionEnchantConfig.COMMON.allowPotionLevelBeyond255.get()) {
                return;
            }
        } catch (Exception e) {
            // 配置未加载时忽略异常，使用默认行为
        }
        if (amplifier > 254 || amplifier < 0) {
            ((MobEffectInstanceAccessor) (Object) this).setAmplifier(Math.min(Math.max(amplifier, 0), 254));
        }
    }
}
