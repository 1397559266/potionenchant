package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.handlers.CriticalStrikeHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LivingEntity.class)
public class HurtMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    public void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity)(Object)this;

        // 1. 脆败效果 - 通用无敌帧清除
        try {
            if (target.hasEffect(EffectRegistry.FRAGILITY.get())) {
                target.invulnerableTime = 0;
            }
        } catch (Exception ignored) {}

        // 2. 烬灭附魔：目标着火时无视无敌帧
        if (source.getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            int ashLevel = weapon.getEnchantmentLevel(PotionEnchantMod.ASH_EXTINCTION.get());
            if (ashLevel > 0 && target.isOnFire()) {
                target.invulnerableTime = 0;
            }
        }

        // 3. 致命一击：暴击窗口内无视无敌帧
        if (source.getDirectEntity() instanceof Player attacker) {
            if (CriticalStrikeHandler.isInCriticalStrikeWindow(attacker)) {
                target.invulnerableTime = 0;
            }
        }
    }
}
