package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.client.compat.oculus.CosmicItemLateRenderQueue;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 参考 Adorable Armory 的 LolaCosmicAfterLevelMixin
 * 在 GameRenderer.renderLevel() 中使用 Mixin 精确控制渲染时机：
 * 1. 手部渲染前 → 渲染非手部物品的星空层 (renderBeforeHand)
 * 2. 全部渲染完成后 → 渲染手部物品的星空层 (renderAfterHand)
 * 这样能确保与 Oculus 光影完全兼容
 */
@Mixin(value = GameRenderer.class, priority = 500)
public abstract class CosmicAfterLevelMixin {

    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", ordinal = 0))
    private void onRenderLevelBeforeHand(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderBeforeHand();
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelAfterShaderpackFinal(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderAfterHand();
        }
    }
}
