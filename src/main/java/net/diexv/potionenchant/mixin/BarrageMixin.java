package net.diexv.potionenchant.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class BarrageMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    public void onHitEntity(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        // 检查箭矢是否有无视无敌帧的标记
        if (arrow.getPersistentData().getBoolean("plentyofenchant:ignore_invulnerability")) {
            // 如果击中的是生物实体，将其无敌时间设置为0
            if (result.getEntity() instanceof LivingEntity livingEntity) {
                livingEntity.invulnerableTime = 0;
            }
        }
    }
}
