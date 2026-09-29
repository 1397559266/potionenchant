package net.diexv.potionenchant.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 突破 256 级存档限制：
 * 原版 amplifier 以 NBT byte 保存（>127 会截断/变负）。
 * - save：amplifier > 127 时额外写自定义 int 键 potionenchant_amp，
 *   原版 "Amplifier" 钳制到 127（兼容外部读取）；
 * - load：优先读取 potionenchant_amp 恢复真实大等级。
 */
@Mixin(MobEffectInstance.class)
public class MobEffectInstanceLargeAmplifierMixin {

    private static final String AMP_KEY = "potionenchant_amp";

    @Shadow
    @Final
    private int amplifier;

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"))
    private void potionenchant$onSave(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.amplifier > 127) {
            tag.putInt(AMP_KEY, this.amplifier);
            tag.putByte("Amplifier", (byte) 127); // 对外兼容钳制
        }
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/effect/MobEffectInstance;",
            at = @At("RETURN"))
    private static void potionenchant$onLoad(CompoundTag tag, CallbackInfoReturnable<MobEffectInstance> cir) {
        MobEffectInstance inst = cir.getReturnValue();
        if (inst != null && tag.contains(AMP_KEY)) {
            int amp = tag.getInt(AMP_KEY);
            if (amp != inst.getAmplifier()) {
                // 直接在返回实例上改 amplifier（不取消注入，避免 CancellationException）
                ((net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor) (Object) inst).setAmplifier(amp);
            }
        }
    }
}
