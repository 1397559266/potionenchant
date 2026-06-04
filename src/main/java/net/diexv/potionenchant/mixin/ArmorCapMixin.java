package net.diexv.potionenchant.mixin;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedAttribute.class)
public abstract class ArmorCapMixin {

    @Shadow @Final @Mutable
    private double maxValue;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void removeArmorCap(CallbackInfo ci) {
        if (((Attribute) (Object) this).getDescriptionId().equals("attribute.name.generic.armor")) {
            this.maxValue = Double.MAX_VALUE;
        }
    }
}