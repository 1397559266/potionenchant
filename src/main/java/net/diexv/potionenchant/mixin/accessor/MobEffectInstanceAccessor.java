package net.diexv.potionenchant.mixin.accessor;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor
    void setDuration(int duration);

    @Accessor
    void setAmplifier(int amplifier);
}
