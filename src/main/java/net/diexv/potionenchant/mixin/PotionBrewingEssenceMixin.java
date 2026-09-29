package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让七彩药水精华可放入酿造台材料槽（不影响原版混合配方，
 * 实际升级逻辑由 BrewingStandEssenceMixin 处理）。
 */
@Mixin(PotionBrewing.class)
public class PotionBrewingEssenceMixin {

    @Inject(method = "isIngredient", at = @At("RETURN"), cancellable = true)
    private static void potionenchant$allowEssence(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && stack != null
                && stack.getItem() == ModItems.RAINBOW_POTION_ESSENCE.get()) {
            cir.setReturnValue(true);
        }
    }
}
