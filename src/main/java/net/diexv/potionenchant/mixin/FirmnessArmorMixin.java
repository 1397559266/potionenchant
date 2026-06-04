package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 坚韧效果的护甲耐久保护 Mixin
 * 将护甲单次耐久损耗上限与生命值伤害上限同步（均为最大生命值的百分比）
 */
@Mixin(LivingEntity.class)
public class FirmnessArmorMixin {

    @ModifyVariable(
        method = "hurtArmor",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private float onHurtArmorDamage(float pDamage, DamageSource pSource) {
        LivingEntity entity = (LivingEntity)(Object)this;
        MobEffectInstance effect = entity.getEffect(EffectRegistry.FIRMNESS.get());
        if (effect == null) return pDamage;

        int amplifier = effect.getAmplifier();

        // 与 FirmnessEffect.onLivingHurt 相同的公式
        float maxDamagePercent = 0.90f - (amplifier * 0.10f);
        maxDamagePercent = Math.max(maxDamagePercent, 0.01f);
        float maxDuraDamage = entity.getMaxHealth() * maxDamagePercent;

        if (pDamage > maxDuraDamage) {
            return maxDuraDamage;
        }
        return pDamage;
    }
}
