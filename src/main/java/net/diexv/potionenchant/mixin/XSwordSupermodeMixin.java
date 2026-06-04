package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XSword 毁灭模式 Mixin
 *
 * 毁灭模式玩家攻击时：
 * 1. 标记目标（后续 SynchedEntityData get/set 强制 0）
 * 2. 不 cancel，让 hurt(Float.MAX_VALUE) 实际走伤害流程
 * 3. 实体受 MAX_VALUE 伤害 → 自然死亡
 * 4. XSwordSyncedDataMixin 确保 DATA_HEALTH_ID 双端同步为 0
 */
@Mixin(LivingEntity.class)
public class XSwordSupermodeMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getEntity() instanceof Player attacker)) return;
        if (!XSwordItem.isSupermode(attacker.getUUID())) return;

        LivingEntity self = (LivingEntity)(Object)this;
        if (self == attacker) return;

        // 标记目标：后续所有 SynchedEntityData 的 get/set 都会被强制改为 0
        XSwordTargetTracker.mark(self);
    }
}