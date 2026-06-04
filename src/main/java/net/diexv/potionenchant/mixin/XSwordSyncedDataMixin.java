package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.mixin.accessor.SynchedEntityDataAccessor;
import net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SynchedEntityData.class)
public class XSwordSyncedDataMixin {

    private static final ThreadLocal<Boolean> POTIONENCHANT_SETTING_HEALTH = ThreadLocal.withInitial(() -> false);

    // get(): 被标记实体的 health 永远返回 0
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    public <T> void onGet(EntityDataAccessor<T> key, CallbackInfoReturnable<T> cir) {
        if (key != LivingEntityAccessor.HEALTH()) return;
        if (XSwordTargetTracker.isMarked(((SynchedEntityDataAccessor) this).getEntity())) {
            cir.setReturnValue((T) Float.valueOf(0.0F));
        }
    }

    // set(key, value): 被标记实体的 health 强制改写为 0
    @Inject(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public <T> void onSet(EntityDataAccessor<T> key, T value, CallbackInfo ci) {
        if (POTIONENCHANT_SETTING_HEALTH.get()) return;
        if (key != LivingEntityAccessor.HEALTH()) return;
        if (XSwordTargetTracker.isMarked(((SynchedEntityDataAccessor) this).getEntity())) {
            POTIONENCHANT_SETTING_HEALTH.set(true);
            ((SynchedEntityData) (Object) this).set(key, (T) Float.valueOf(0.0F));
            POTIONENCHANT_SETTING_HEALTH.set(false);
            ci.cancel();
        }
    }

    // set(key, value, dirty): 同上
    @Inject(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public <T> void onSetDirty(EntityDataAccessor<T> key, T value, boolean dirty, CallbackInfo ci) {
        if (POTIONENCHANT_SETTING_HEALTH.get()) return;
        if (key != LivingEntityAccessor.HEALTH()) return;
        if (XSwordTargetTracker.isMarked(((SynchedEntityDataAccessor) this).getEntity())) {
            POTIONENCHANT_SETTING_HEALTH.set(true);
            ((SynchedEntityData) (Object) this).set(key, (T) Float.valueOf(0.0F), dirty);
            POTIONENCHANT_SETTING_HEALTH.set(false);
            ci.cancel();
        }
    }
}
