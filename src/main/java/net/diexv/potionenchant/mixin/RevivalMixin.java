package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.util.RevivalManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 复活 Mixin — 替代原 agent 对 setHealth/getHealth/addEffect 的 ASM 注入。
 * 当玩家拥有 Revival 效果时拦截死亡，执行复活逻辑。
 * 注意：setHealth/getHealth 中有相互调用，通过 RevivalManager 内部的 boolean guard 防递归。
 */
@Mixin(LivingEntity.class)
public class RevivalMixin {

    // ==================== setHealth(float) ====================
    // 替代 ASM: RevivalSetHealthVisitor — setHealth 开头 if (health <= 0) -> 复活检查
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        if (health > 0.0F) return;
        if (RevivalManager.tryRevive((LivingEntity)(Object)this)) {
            ci.cancel();
        }
    }

    // ==================== getHealth() ====================
    // Revival 激活时防止外部系统读取到 <=0 的血量
    @Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
    private void onGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof ServerPlayer player)) return;
        float current = cir.getReturnValue();
        if (current <= 0.0F && player.hasEffect(EffectRegistry.REVIVAL.get())) {
            cir.setReturnValue(player.getMaxHealth());
        }
    }

    // ==================== addEffect(MobEffectInstance, Entity) ====================
    // 当 Revival 效果被应用时（可选的初始化逻辑）
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void onAddEffect(MobEffectInstance effectInstance, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != Boolean.TRUE) return;
        if (!((Object)this instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (effectInstance.getEffect() == EffectRegistry.REVIVAL.get()) {
            // Revival 已生效 — 预留后续可能的初始化逻辑
        }
    }
}
